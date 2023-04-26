package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.SshTransportConfigCallback;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.common.project.RuntimeProject;
import com.inductiveautomation.ignition.common.project.resource.LastModification;
import com.inductiveautomation.ignition.common.project.resource.ProjectResource;
import com.inductiveautomation.ignition.common.project.resource.ResourcePath;
import com.inductiveautomation.ignition.common.project.resource.ResourceType;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import simpleorm.dataset.SQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.axone_io.ignition.git.GatewayHook.context;

public class GitManager {
    private final static LoggerEx logger = LoggerEx.newBuilder().build(GitManager.class);

    static public Git getGit(Path projectFolderPath) {
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

    public static Path getProjectFolderPath(String projectName) {
        Path dataDir = getDataFolderPath();
        return dataDir.resolve("projects").resolve(projectName);
    }

    public static Path getDataFolderPath() {
        return context.getSystemManager().getDataDir().toPath();
    }


    public static void clearDirectory(Path folderPath) {
        try {
            if (folderPath.toFile().exists()) {
                FileUtils.cleanDirectory(folderPath.toFile());
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public static void setAuthentication(TransportCommand<?, ?> command, String projectName, String userName) throws Exception {
        GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
        GitReposUsersRecord user = getGitReposUserRecord(gitProjectsConfigRecord, userName);

        setAuthentication(command, gitProjectsConfigRecord, user);
    }

    public static void setAuthentication(TransportCommand<?, ?> command, GitProjectsConfigRecord gitProjectsConfigRecord, GitReposUsersRecord user) {

        if (gitProjectsConfigRecord.isSSHAuthentication()) {
            command.setTransportConfigCallback(getSshTransportConfigCallback(user));
        } else {
            command.setCredentialsProvider(getUsernamePasswordCredentialsProvider(user));
        }
    }


    public static void setCommitAuthor(CommitCommand command, String projectName, String userName) {
        try {
            GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
            GitReposUsersRecord user = getGitReposUserRecord(gitProjectsConfigRecord, userName);
            command.setAuthor("", user.getEmail());
        } catch (Exception e) {
            logger.error("An error occurred while setting up commit author.", e);
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
        return LastModification.of(projectResource).map(LastModification::getActor).orElse("unknown");
    }

    public static List getAddedFiles(String projectName) {
        List<String> fileList = new ArrayList<>();
        Git git = getGit(getProjectFolderPath(projectName));
        try {
            Status status = git.status().call();
            fileList.addAll(status.getAdded());
            git.close();
        } catch (Exception e) {
            logger.info(e.toString(), e);
            throw new RuntimeException(e);
        }
        return fileList;
    }

    public static void cloneRepo(String projectName, String userName, String URI, String branchName) {
        File projectDirFile = getProjectFolderPath(projectName).toFile();
        if (projectDirFile.exists()) {
            try (Git git = Git.init().setDirectory(projectDirFile).call()) {
                disableSsl(git);

                // GIT REMOTE ADD
                URIish urIish = new URIish(URI);
                git.remoteAdd()
                        .setName(urIish.getHumanishName())
                        .setUri(urIish).call();

                //GIT FETCH
                FetchCommand fetch = git.fetch()
                        .setRemote(urIish.getHumanishName())
                        .setRefSpecs(new RefSpec("refs/heads/" + branchName + ":refs/remotes/" + urIish.getHumanishName() + "/" + branchName));

                setAuthentication(fetch, projectName, userName);
                fetch.call();

                //GIT CHECKOUT
                CheckoutCommand checkout = git.checkout()
                        .setCreateBranch(true)
                        .setName(branchName)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint(urIish.getHumanishName() + "/" + branchName);
                checkout.call();
            } catch (Exception e) {
                logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }
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
