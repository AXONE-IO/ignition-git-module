package com.axone_io.ignition.git;

import com.axone_io.ignition.git.managers.GitManager;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.gson.*;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.gateway.images.ImageManager;
import com.inductiveautomation.ignition.gateway.images.ImageRecord;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;


import simpleorm.dataset.SQuery;

import static com.axone_io.ignition.git.managers.GitManager.*;
import static com.inductiveautomation.ignition.common.tags.TagUtilities.TAG_GSON;

public class GatewayScriptModule extends AbstractScriptModule {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean pullImpl(String projectName, String userName) throws Exception {
        Git git = getGit(projectName);

        PullCommand pull = git.pull();
        setAuthentication(pull, projectName, userName);

        PullResult result;

        try {
            result = pull.call();
            if (!result.isSuccessful()) {
                logger.warn("Cannot pull from git");
            } else {
                logger.info("Pull was successful.");
            }
        } catch (GitAPIException e) {
            logger.error(e.toString());
            throw new RuntimeException(e);
        }

        git.close();
        return true;
    }

    @Override
    public boolean pushImpl(String projectName, String userName) throws Exception {
        Git git = getGit(projectName);
        PushCommand push = git.push();

        setAuthentication(push, projectName, userName);

        Iterable<PushResult> results;

        try {
            results = push.setPushAll().setPushTags().call();
            for(PushResult result: results){
                logger.trace(result.getMessages());
            }
        } catch (GitAPIException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }

        git.close();
        return true;
    }

    @Override
    protected boolean commitImpl(String projectName, String userName, List<String> changes, String message) {
        Git git = getGit(projectName);
        try {
            for(String change: changes){
                String folderPath = change;
                git.add().addFilepattern(folderPath).call();
                git.add().setUpdate(true).addFilepattern(folderPath).call();
            }

            CommitCommand commit = git.commit().setMessage(message);
            setCommitAuthor(commit, projectName, userName);
            commit.call();

            git.close();
        } catch (GitAPIException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public Dataset getUncommitedChangesImpl(String projectName, String userName) {
        String projectPath = System.getProperty("user.dir").replace("\\", "/") + "/data/projects/" + projectName + "/";
        Dataset ds;
        List<String> colNames = new ArrayList<>();
        colNames.add("resource");
        colNames.add("type");
        colNames.add("actor");

        List<String> changes = new ArrayList<>();
        Class[] types = new Class[colNames.size()];
        Arrays.fill(types, String.class);

        DatasetBuilder builder = new DatasetBuilder();
        builder.colNames(colNames.toArray(new String[0]));
        builder.colTypes(types);

        try {
            Git git = getGit(projectName);
            Status status = git.status().call();

            Set<String> missing = status.getMissing();
            uncomittedChangesBuilder(projectPath, missing, "Deleted", changes, builder);

            Set<String> uncommittedChanges = status.getUncommittedChanges();
            uncomittedChangesBuilder(projectPath, uncommittedChanges, "Uncommitted", changes, builder);

            Set<String> untracked = status.getUntracked();
            uncomittedChangesBuilder(projectPath, untracked, "Created", changes, builder);

            git.close();
        } catch (Exception e) {
            logger.info(e.toString(), e);
        }
        ds = builder.build();

        return ds != null ? ds : new BasicDataset();
    }

    @Override
    public boolean isRegisteredUserImpl(String projectName, String userName) {
        boolean registered;
        try {
            GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);
            registered = getGitReposUserRecord(gitProjectsConfigRecord, userName) != null;
        }catch (Exception e){
            registered = false;
        }
        return registered;
    }

    @Override
    protected boolean exportConfigImpl(String projectName) {
        exportImages(projectName);
        exportTheme(projectName);
        exportTag(projectName);
        return true;
    }

    @Override
    public void setupLocalRepoImpl(String projectName, String userName) throws Exception {
        String projectFolderPath = getProjectFolderPath(projectName);
        GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);

        Path path = Paths.get(projectFolderPath + ".git");
        if(!Files.exists(path)) {
            Git git = Git.init().setDirectory(new File(projectFolderPath)).call();
            git.remoteAdd().setName("origin").setUri(new URIish(gitProjectsConfigRecord.getURI())).call();

            git.add().addFilepattern(".").call();
            CommitCommand commit = git.commit().setMessage("Initial commit");
            setCommitAuthor(commit, projectName, userName);
            commit.call();
            PushCommand pushCommand = git.push();
            setAuthentication(pushCommand, projectName, userName);
            pushCommand.setRemote("origin").setRefSpecs(new RefSpec("master")).call();
        }
    }
}
