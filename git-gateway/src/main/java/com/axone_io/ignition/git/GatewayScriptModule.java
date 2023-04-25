package com.axone_io.ignition.git;

import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.axone_io.ignition.git.managers.GitImageManager.exportImages;
import static com.axone_io.ignition.git.managers.GitManager.*;
import static com.axone_io.ignition.git.managers.GitTagManager.exportTag;
import static com.axone_io.ignition.git.managers.GitThemeManager.exportTheme;

public class GatewayScriptModule extends AbstractScriptModule {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean pullImpl(String projectName, String userName) throws Exception {
        Path projectFolderPath = getProjectFolderPath(projectName);
        Git git = getGit(projectFolderPath);

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
        Path projectFolderPath = getProjectFolderPath(projectName);
        Git git = getGit(projectFolderPath);
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
        Path projectFolderPath = getProjectFolderPath(projectName);
        Git git = getGit(projectFolderPath);
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

        Path projectFolderPath = getProjectFolderPath(projectName);
        try {
            Git git = getGit(projectFolderPath);
            Status status = git.status().call();

            Set<String> missing = status.getMissing();
            uncomittedChangesBuilder(projectFolderPath, missing, "Deleted", changes, builder);

            Set<String> uncommittedChanges = status.getUncommittedChanges();
            uncomittedChangesBuilder(projectFolderPath, uncommittedChanges, "Uncommitted", changes, builder);

            Set<String> untracked = status.getUntracked();
            uncomittedChangesBuilder(projectFolderPath, untracked, "Created", changes, builder);

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
        Path projectFolderPath = getProjectFolderPath(projectName);
        exportImages(projectFolderPath);
        exportTheme(projectFolderPath);
        exportTag(projectFolderPath);
        return true;
    }

    @Override
    public void setupLocalRepoImpl(String projectName, String userName) throws Exception {
        Path projectFolderPath = getProjectFolderPath(projectName);
        GitProjectsConfigRecord gitProjectsConfigRecord = getGitProjectConfigRecord(projectName);

        Path path = projectFolderPath.resolve(".git");
        if(!Files.exists(path)) {
            PushCommand pushCommand;
            try (Git git = Git.init().setDirectory(projectFolderPath.toFile()).call()) {
                git.remoteAdd().setName("origin").setUri(new URIish(gitProjectsConfigRecord.getURI())).call();

                git.add().addFilepattern(".").call();
                git.commit().setMessage("Initial commit").call();
                pushCommand = git.push();
            }
            setAuthentication(pushCommand, projectName, userName);
            pushCommand.setRemote("origin").setRefSpecs(new RefSpec("master")).call();
        }
    }
}
