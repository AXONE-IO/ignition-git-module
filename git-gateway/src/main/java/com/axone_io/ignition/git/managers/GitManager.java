package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.SshTransportConfigCallback;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.common.JsonUtilities;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.project.RuntimeProject;
import com.inductiveautomation.ignition.common.project.resource.LastModification;
import com.inductiveautomation.ignition.common.project.resource.ProjectResource;
import com.inductiveautomation.ignition.common.project.resource.ResourcePath;
import com.inductiveautomation.ignition.common.project.resource.ResourceType;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.images.ImageManager;
import com.inductiveautomation.ignition.gateway.images.ImageRecord;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import simpleorm.dataset.SQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.axone_io.ignition.git.GatewayHook.context;

public class GitManager {
    private final static LoggerEx logger = LoggerEx.newBuilder().build(GitManager.class);

    public static Git getGit(Path projectFolderPath) {
        Git git;
        try {
            git = Git.open(projectFolderPath.resolve(".git").toFile());
            disableSsl(git);
        } catch (IOException e) {
            logger.error("Unable to retrieve Git repository", e);
            throw new RuntimeException(e);
        }
        return git;
    }

    public static void clearDirectory(Path folderPath) {
        try {
            FileUtils.cleanDirectory(folderPath.toFile());
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public static void exportTag(Path projectFolderPath) {
        Path tagFolderPath = projectFolderPath.resolve("tags");
        if (tagFolderPath.toFile().exists()) {
            clearDirectory(tagFolderPath);
        }

        try {
            Files.createDirectories(tagFolderPath);

            for (TagProvider tagProvider : context.getTagManager().getTagProviders()) {
                TagPath typesPath = TagPathParser.parse("");
                List<TagPath> tagPaths = new ArrayList<>();
                tagPaths.add(typesPath);

                CompletableFuture<List<TagConfigurationModel>> cfTagModels =
                        tagProvider.getTagConfigsAsync(tagPaths, true, true);
                List<TagConfigurationModel> tModels = cfTagModels.get();

                JsonObject json = TagUtilities.toJsonObject(tModels.get(0));
                JsonElement sortedJson = JsonUtilities.createDeterministicCopy(json);

                Path newFile = tagFolderPath.resolve(tagProvider.getName() + ".json");

                Files.writeString(newFile, sortedJson.toString());
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    public static void exportTheme(Path projectFolderPath) {
        Path themeFolderPath = projectFolderPath.resolve("themes");
        if (themeFolderPath.toFile().exists()) {
            clearDirectory(themeFolderPath);
        }
        try {
            Path perspectiveFolderPath = projectFolderPath.resolve("com.inductiveautomation.perspective");

            if (perspectiveFolderPath.toFile().exists()) {
                Path sessionPropsPath = perspectiveFolderPath
                        .resolve("session-props")
                        .resolve("props.json");
                String content = Files.readString(sessionPropsPath);
                Gson g = new Gson();
                JsonObject json = g.fromJson(content, JsonObject.class);
                String theme = JsonUtilities.readString(json, "props.theme", "light");

                Path themesDir = context.getSystemManager().getDataDir().toPath()
                        .resolve("modules")
                        .resolve("com.inductiveautomation.perspective")
                        .resolve("themes");

                Path themeFolder = themesDir.resolve(theme);
                Path themeFile = themesDir.resolve(theme + ".css");

                Files.createDirectories(themeFolderPath);
                FileUtils.copyDirectoryToDirectory(themeFolder.toFile(), themeFolderPath.toFile());
                Files.copy(themeFile, themeFolderPath.resolve(themeFile.getFileName()));
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
    }

    public static void exportImages(Path projectFolderPath) {
        Path imageFolderPath = projectFolderPath.resolve("images");
        if (imageFolderPath.toFile().exists()) {
            clearDirectory(imageFolderPath);
        }

        try {
            Files.createDirectories(imageFolderPath);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        saveFolderImage(imageFolderPath, "");
    }

    public static void saveFolderImage(Path folderPath, String directory) {
        ImageManager imageManager = context.getImageManager();
        for (ImageRecord imageRecord : imageManager.getImages(directory)) {
            String path = imageRecord.getString(ImageRecord.Path);
            if (imageRecord.isDirectory()) {
                try {
                    Files.createDirectories(folderPath.resolve(path));
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
                saveFolderImage(folderPath, path);
            } else {
                byte[] data = imageManager.getImage(path).getBytes(ImageRecord.Data);
                try {
                    Files.write(folderPath.resolve(path), data);
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
            }
        }
    }

    public static void setAuthentication(TransportCommand<?, ?> command, String projectName, String userName)
            throws Exception {
        GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
        GitReposUsersRecord user = getGitReposUserRecord(gitProjectsConfigRecord, userName);

        if (gitProjectsConfigRecord.isSSHAuthentication()) {
            command.setTransportConfigCallback(getSshTransportConfigCallback(user));
        } else {
            command.setCredentialsProvider(getUsernamePasswordCredentialsProvider(user));
        }
    }

    public static GitProjectsConfigRecord getGitProjectConfigRecord(String projectName) throws Exception {
        SQuery<GitProjectsConfigRecord> projectQuery = new SQuery<>(GitProjectsConfigRecord.META)
                .eq(GitProjectsConfigRecord.ProjectName, projectName);
        GitProjectsConfigRecord gitProjectsConfigRecord = context.getPersistenceInterface().queryOne(projectQuery);

        if (gitProjectsConfigRecord == null) {
            throw new Exception("Git Project not configured.");
        }

        return gitProjectsConfigRecord;
    }

    public static GitReposUsersRecord getGitReposUserRecord(GitProjectsConfigRecord gitProjectsConfigRecord,
                                                            String userName) throws Exception {
        SQuery<GitReposUsersRecord> userQuery = new SQuery<>(GitReposUsersRecord.META)
                .eq(GitReposUsersRecord.ProjectId, gitProjectsConfigRecord.getId())
                .eq(GitReposUsersRecord.IgnitionUser, userName);
        GitReposUsersRecord user = context.getPersistenceInterface().queryOne(userQuery);

        if (user == null) {
            throw new Exception("Git User not configured.");
        }

        return user;
    }

    public static UsernamePasswordCredentialsProvider getUsernamePasswordCredentialsProvider(GitReposUsersRecord user) {
        return new UsernamePasswordCredentialsProvider(user.getUserName(), user.getPassword());
    }

    public static SshTransportConfigCallback getSshTransportConfigCallback(GitReposUsersRecord user) {
        return new SshTransportConfigCallback(user.getSSHKey());
    }

    public static void uncommittedChangesBuilder(String projectName,
                                                 Set<String> updates,
                                                 String type,
                                                 List<String> changes,
                                                 DatasetBuilder builder) throws IOException {
        for (String update : updates) {
            String[] rowData = new String[3];
            String actor = "unknown";
            String path = update;
            if (hasActor(path)) {
                String[] pathSplitted = update.split("/");
                path = String.join("/", Arrays.copyOf(pathSplitted, pathSplitted.length - 1));
                actor = getActor(projectName, path);
            }

            rowData[0] = path;
            rowData[1] = type;
            if (!changes.contains(path)) {
                rowData[2] = actor;
                changes.add(path);
                builder.addRow((Object[]) rowData);
            }
        }
    }

    public static boolean hasActor(String resource) {
        boolean hasActor = false;
        if (resource.startsWith("ignition")) {
            hasActor = Boolean.TRUE;
        }
        if (resource.startsWith("com.inductiveautomation.")) {
            hasActor = Boolean.TRUE;
        }

        return hasActor;
    }

    public static String getActor(String projectName, String path) {
        ProjectManager projectManager = context.getProjectManager();
        RuntimeProject project = projectManager.getProject(projectName).get();

        ProjectResource projectResource = project.getResource(getResourcePath(path)).get();
        String actor = LastModification.of(projectResource).map(LastModification::getActor).orElse("unknown");
        ;

        return actor;
    }


    public static ResourcePath getResourcePath(String resourcePath) {
        String moduleId = "";
        String typeId = "";
        String resource = "";
        String[] paths = resourcePath.split("/");

        if (paths.length > 0) moduleId = paths[0];
        if (paths.length > 1) typeId = paths[1];
        if (paths.length > 2) resource = resourcePath.replace(moduleId + "/" + typeId + "/", "");

        return new ResourcePath(new ResourceType(moduleId, typeId), resource);
    }

    public static void disableSsl(Git git) throws IOException {
        StoredConfig config = git.getRepository().getConfig();
        config.setBoolean("http", null, "sslVerify", false);
        config.save();
    }
}
