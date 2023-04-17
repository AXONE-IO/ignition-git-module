package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.SshTransportConfigCallback;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonPrimitive;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.gateway.images.ImageManager;
import com.inductiveautomation.ignition.gateway.images.ImageRecord;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.inductiveautomation.ignition.common.tags.TagUtilities.TAG_GSON;

public class GitManager {
    private final static Logger logger = LoggerFactory.getLogger(GitManager.class);

    static public Git getGit(String projectName){
        Git git;
        try {
            git = Git.open(new File(getProjectFolderPath(projectName) + ".git"));
            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean( "http", null, "sslVerify", false );
            config.save();
        } catch (IOException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
        return git;
    }

    public static String getProjectFolderPath(String projectName){
        return System.getProperty("user.dir") + "/data/projects/" + projectName + "/";
    }

    public static void clearDirectory(String folderPath){
        try {
            FileUtils.cleanDirectory(new File(folderPath));
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public static void exportTag(String projectName){
        String projectFolderPath = getProjectFolderPath(projectName);
        String tagFolderPath = projectFolderPath + "tags/";
        clearDirectory(tagFolderPath);

        try {
            Files.createDirectories(Paths.get(tagFolderPath));

            for(TagProvider tagProvider: context.getTagManager().getTagProviders()){
                TagPath typesPath = TagPathParser.parse("");
                List<TagPath> tagPaths = new ArrayList<>();
                tagPaths.add(typesPath);

                CompletableFuture<List<TagConfigurationModel>> cfTagModels = tagProvider.getTagConfigsAsync(tagPaths, true, true);
                List<TagConfigurationModel> tModels =cfTagModels.get();

                JsonObject json = TagUtilities.toJsonObject(tModels.get(0));

                String ret = TAG_GSON.toJson(json);
                File newFile = new File(tagFolderPath + tagProvider.getName() + ".json");

                try (FileOutputStream outputStream = new FileOutputStream(newFile)) {
                    outputStream.write(ret.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    logger.error(e.toString(), e);
                }

            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    public static void exportTheme(String projectName) {
        String projectFolderPath = getProjectFolderPath(projectName);
        String themeFolderPath = projectFolderPath + "themes/";
        clearDirectory(themeFolderPath);
        Gson g = new Gson();


        String content;
        String theme = "light";
        try {
            Files.createDirectories(Paths.get(themeFolderPath));
            content = new String(Files.readAllBytes(Paths.get(projectFolderPath + "com.inductiveautomation.perspective/session-props/props.json")));
            JsonObject json = (JsonObject) g.fromJson(content, JsonElement.class);
            JsonObject props = json.getAsJsonObject("props");
            if (props.has("theme")){
                theme = props.getAsJsonPrimitive("theme").getAsString();
            }


            String dataFolderPath = System.getProperty("user.dir") + "/data";
            String themePathFolder = dataFolderPath + "/modules/com.inductiveautomation.perspective/themes/" + theme +"/";
            String themePathFile = dataFolderPath + "/modules/com.inductiveautomation.perspective/themes/" + theme +".css";

            File destDir = new File(themeFolderPath);

            FileUtils.copyDirectoryToDirectory(new File(themePathFolder), destDir);
            FileUtils.copyFile(new File(themePathFile), new File(themeFolderPath + theme + ".css"));

        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
    }

    public static void exportImages(String projectName) {
        String projectFolderPath = getProjectFolderPath(projectName);

        String imageFolderPath = projectFolderPath + "images/";
        clearDirectory(imageFolderPath);
        try {
            Files.createDirectories(Paths.get(imageFolderPath));
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        saveFolderImage(imageFolderPath,"");
    }

    public static void saveFolderImage(String folderPath, String directory){
        ImageManager imageManager = context.getImageManager();
        for(ImageRecord imageRecord: imageManager.getImages(directory)){
            String path = imageRecord.getString(ImageRecord.Path);
            if(imageRecord.isDirectory()){
                try {
                    Files.createDirectories(Paths.get(folderPath + path));
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }

                saveFolderImage(folderPath,path);
            }else{
                byte[] data = imageManager.getImage(path).getBytes(ImageRecord.Data);
                File newFile = new File(folderPath + path);

                try (FileOutputStream outputStream = new FileOutputStream(newFile)) {
                    outputStream.write(data);
                } catch (Exception e) {
                    logger.error(e.toString(), e);
                }
            }
        }
    }

    public static void setAuthentication(TransportCommand command, String projectName, String userName) throws Exception {
        GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
        GitReposUsersRecord user = getGitReposUserRecord(gitProjectsConfigRecord, userName);

        if (gitProjectsConfigRecord.isSSHAuthentication()){
            command.setTransportConfigCallback(getSshTransportConfigCallback(user));
        }else {
            command.setCredentialsProvider(getUsernamePasswordCredentialsProvider(user));
        }
    }

    public static void setCommitAuthor(CommitCommand command, String projectName, String userName){
        try {
            GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
            GitReposUsersRecord user = getGitReposUserRecord(gitProjectsConfigRecord, userName);
            command.setAuthor("", user.getEmail());
        } catch (Exception e){
            logger.error("An error occurred while setting up commit author.", e);
        }

    }

    public static GitProjectsConfigRecord getGitProjectConfigRecord(String projectName) throws Exception {
        SQuery<GitProjectsConfigRecord> projectQuery = new SQuery<>(GitProjectsConfigRecord.META).eq(GitProjectsConfigRecord.ProjectName, projectName);
        GitProjectsConfigRecord gitProjectsConfigRecord = context.getPersistenceInterface().queryOne(projectQuery);

        if (gitProjectsConfigRecord == null) throw new Exception("Git Project not configured.");

        return gitProjectsConfigRecord;
    }

    public static GitReposUsersRecord getGitReposUserRecord(GitProjectsConfigRecord gitProjectsConfigRecord, String userName) throws Exception {
        SQuery<GitReposUsersRecord> userQuery = new SQuery<>(GitReposUsersRecord.META).eq(GitReposUsersRecord.ProjectId, gitProjectsConfigRecord.getId()).eq(GitReposUsersRecord.IgnitionUser, userName);
        GitReposUsersRecord user = context.getPersistenceInterface().queryOne(userQuery);

        if (user == null) throw new Exception("Git User not configured.");

        return user;
    }

    public static UsernamePasswordCredentialsProvider getUsernamePasswordCredentialsProvider(GitReposUsersRecord user) {
        return new UsernamePasswordCredentialsProvider(user.getUserName(), user.getPassword());
    }

    public static SshTransportConfigCallback getSshTransportConfigCallback(GitReposUsersRecord user) {
        return new SshTransportConfigCallback(user.getSSHKey());
    }
    public static void uncomittedChangesBuilder(String projectPath, Set<String> updates, String type, List<String> changes, DatasetBuilder builder){
        for (String update: updates) {
            String[] rowData = new String[3];

            String path = update;
            if (hasActor(path)){
                String[] pathSplitted = update.split("/");
                path = String.join("/", Arrays.copyOf(pathSplitted,pathSplitted.length -1));
            }

            rowData[0] = path;
            rowData[1] = type;
            if(!changes.contains(path)){
                rowData[2] = getActor(projectPath + path);
                changes.add(path);
                builder.addRow(rowData);
            }
        }
    }

    public static boolean hasActor(String resource){
        boolean hasActor = false;
        if (resource.startsWith("ignition")) hasActor = Boolean.TRUE;
        if (resource.startsWith("com.inductiveautomation.")) hasActor = Boolean.TRUE;

        return hasActor;
    }

    public static String getActor(String path){
        Gson g = new Gson();
        String actor = "";
        try {
            String content = new String(Files.readAllBytes(Paths.get(path + "/resource.json")));

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
    public static  List getAddedFiles(String projectName) {
        List<String> fileList = new ArrayList<>();
        Git git = getGit(projectName);
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

}
