package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.SshTransportConfigCallback;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonPrimitive;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import simpleorm.dataset.SQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
            initGitConfig(git.getRepository().getConfig());
        } catch (IOException e) {
            logger.error("Unable to retrieve Git repository", e);
            throw new RuntimeException(e);
        }
        return git;
    }

    static public void initGitConfig(StoredConfig config) throws IOException {
        config.setBoolean("http", null, "sslVerify", false);
        config.save();
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
            FileUtils.cleanDirectory(folderPath.toFile());
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

    public static GitReposUsersRecord getGitReposUserRecord(GitProjectsConfigRecord gitProjectsConfigRecord, String userName) throws Exception {
        SQuery<GitReposUsersRecord> userQuery = new SQuery<>(GitReposUsersRecord.META)
                .eq(GitReposUsersRecord.ProjectName, gitProjectsConfigRecord)
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

    public static void uncomittedChangesBuilder(Path projectPath, Set<String> updates, String type, List<String> changes, DatasetBuilder builder) {
        for (String update : updates) {
            String[] rowData = new String[3];

            String path = update;
            if (hasActor(path)) {
                String[] pathSplitted = update.split("/");
                path = String.join("/", Arrays.copyOf(pathSplitted, pathSplitted.length - 1));
            }

            rowData[0] = path;
            rowData[1] = type;
            if (!changes.contains(path)) {
                rowData[2] = getActor(projectPath.resolve(path));
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

    public static String getActor(Path path) {
        Gson g = new Gson();
        String actor = "";
        try {
            String content = new String(Files.readAllBytes(path.resolve("resource.json")));

            JsonObject j = (JsonObject) g.fromJson(content, JsonElement.class);

            JsonObject a = j.getAsJsonObject("attributes");
            JsonObject b = a.getAsJsonObject("lastModification");
            JsonPrimitive c = b.getAsJsonPrimitive("actor");

            actor = c.getAsString();
        } catch (Exception e) {
            logger.trace(e.toString(), e);
        }
        return actor;
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

            try {
                // GIT INIT
                Git git = Git.init()
                        .setDirectory(projectDirFile)
                        .call();

                try {
                    initGitConfig(git.getRepository().getConfig());

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

                } catch (Exception ex) {
                    throw ex;
                } finally {
                    git.close();
                }
            } catch (Exception e) {
                logger.error(e.toString());
                throw new RuntimeException(e);
            }

        }
    }

}
