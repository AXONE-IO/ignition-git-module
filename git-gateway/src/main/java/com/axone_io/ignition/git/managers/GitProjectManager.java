package com.axone_io.ignition.git.managers;

import com.inductiveautomation.ignition.common.StringPath;
import com.inductiveautomation.ignition.common.gson.JsonSyntaxException;
import com.inductiveautomation.ignition.common.project.ProjectInvalidException;
import com.inductiveautomation.ignition.common.project.ProjectManifest;
import com.inductiveautomation.ignition.common.project.resource.*;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.axone_io.ignition.git.managers.GitManager.getProjectFolderPath;

public class GitProjectManager {
    private static final LoggerEx logger = LoggerEx.newBuilder().build(GitProjectManager.class);

    public static void importProject(String projectName) {
        ProjectManager projectManager = context.getProjectManager();
        Path projectDir = getProjectFolderPath(projectName);

        try {
            Set<ProjectResource> resources = importFromFolder(projectDir, projectName);
            ProjectManifest projectManifest = loadProjectManifest(projectDir);
            projectManager.createOrReplaceProject(projectName, projectManifest, new ArrayList(resources));

        } catch (ProjectInvalidException | IOException e) {
            logger.error("An error occurred while importing '" + projectName + "' project.", e);
            throw new RuntimeException(e);
        }

    }

    public static Set<Map.Entry<String, byte[]>> listFiles(Path projectPath) {
        Set<Map.Entry<String, byte[]>> resources = new HashSet<>();
        Stack<File> stack = new Stack<>();
        File directory = projectPath.toFile();

        stack.push(directory);

        while (!stack.empty()) {
            File current = stack.pop();

            File[] files = current.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        stack.push(file);
                    } else {
                        try {
                            String path = file.getAbsolutePath().replace(projectPath.toFile().getAbsolutePath(), "");
                            path = path.substring(1);
                            path = path.replace("\\", "/");
                            resources.add(new AbstractMap.SimpleEntry<>(path, Files.readAllBytes(file.toPath())));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return resources;
    }

    public static boolean isAnIgnitionResource(String resource) {
        return !resource.startsWith(".git")
                && !resource.startsWith("tags")
                && !resource.startsWith("images")
                && !resource.startsWith("themes")
                && !resource.equals("project.json");
    }

    public static Set<ProjectResource> importFromFolder(Path projectPath, String projectName) throws ProjectInvalidException, IOException {
        Set<ProjectResource> resources = new HashSet<>();
        Set<StringPath> createdFolders = new HashSet<>();

        Set<Map.Entry<String, byte[]>> files = listFiles(projectPath);
        files.removeIf(e -> !isAnIgnitionResource(e.getKey()));
        files.stream().collect(Collectors.groupingBy(e -> StringUtils.substringBeforeLast(e.getKey(), "/")))
                .forEach((resourcePath, listOfFileNodes) -> {
                    if (isAnIgnitionResource(resourcePath)) {
                        StringPath stringPath = StringPath.parse(resourcePath);
                        resources.addAll(createParentFolderResources(projectName, stringPath, createdFolders));
                        String manifestPath = String.format("%s/%s", resourcePath, "resource.json");

                        ProjectResourceManifest resourceManifest = removeResourceManifest(manifestPath, listOfFileNodes);

                        if (resourceManifest != null) {

                            Map<String, byte[]> dataMap = createDataMap(resourceManifest, listOfFileNodes);

                            resources.add(createResourceBuilder(projectName, stringPath, resourceManifest, dataMap).build());
                        } else if (!createdFolders.contains(stringPath)) {
                            resources.add(createResourceBuilder(projectName, stringPath, ProjectResourceManifest.newBuilder().build(), new HashMap<>()).setFolder(true).build());
                            createdFolders.add(stringPath);
                        }
                    }
                });

        return resources;
    }

    private static ProjectResourceManifest removeResourceManifest(String manifestPath, List<Map.Entry<String, byte[]>> listOfFileNodes) {
        return listOfFileNodes.stream()
                .filter(e -> manifestPath.equals(e.getKey()))
                .findFirst()
                .map(entry -> {
                    listOfFileNodes.remove(entry);
                    try {
                        return ProjectResourceManifest.fromJson(new String(entry.getValue(), StandardCharsets.UTF_8));
                    } catch (JsonSyntaxException e) {
                        logger.infof("Malformed resource.json at %s, unable to remove", entry.getKey(), e);
                        return null;
                    }
                }).orElse(null);
    }


    private static List<ProjectResource> createParentFolderResources(String projectName, StringPath resourcePath, Set<StringPath> ignoreList) {
        List<ProjectResource> folders = new ArrayList<>();
        StringPath currentPath = resourcePath.getParentPath();
        while (currentPath != null && currentPath.getPathLength() > 0 &&
                !ignoreList.contains(currentPath)) {
            folders.add(createResourceBuilder(projectName, currentPath,
                    ProjectResourceManifest.newBuilder().build(), new HashMap<>())
                    .setFolder(true)
                    .build());
            ignoreList.add(currentPath);
            currentPath = currentPath.getParentPath();
        }

        return folders;
    }

    private static Map<String, byte[]> createDataMap(ProjectResourceManifest resourceManifest, List<Map.Entry<String, byte[]>> listOfFileNodes) {
        List<String> allowedFiles = resourceManifest.getFiles();
        HashMap<String, byte[]> dataMap = new HashMap<>();
        listOfFileNodes.forEach(e -> {
            String filename = StringUtils.substringAfterLast(e.getKey(), "/");
            if (allowedFiles.contains(filename))
                dataMap.put(filename, e.getValue());
        });
        return dataMap;
    }

    private static ProjectResourceBuilder createResourceBuilder(String projectName, StringPath resourcePath, ProjectResourceManifest manifest, Map<String, byte[]> dataMap) {
        String moduleId = resourcePath.getPathComponent(0);
        String resourceType = (resourcePath.getPathLength() > 1) ? resourcePath.getPathComponent(1) : null;
        String subPath = (resourcePath.getPathLength() > 2) ? resourcePath.subPath().subPath().toString() : "";
        return ProjectResource.newBuilder()
                .setProjectName(projectName)
                .setResourcePath(new ResourcePath(new ResourceType(moduleId, resourceType), subPath))
                .setData(dataMap)
                .setRestricted(manifest.isRestricted())
                .setAttributes(manifest.getAttributes())
                .setApplicationScope(manifest.getScope())
                .setDocumentation(manifest.getDocumentation())
                .setVersion(manifest.getVersion())
                .setOverridable(manifest.isOverridable());
    }

    public static ProjectManifest loadProjectManifest(Path projectPath) throws IOException {
        String json = new String(Files.readAllBytes(projectPath.resolve("project.json")), StandardCharsets.UTF_8);
        return ProjectManifest.fromJson(json);
    }

}
