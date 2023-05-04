package com.axone_io.ignition.git;

import com.axone_io.ignition.git.commissioning.utils.GitCommissioningUtils;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.util.DatasetBuilder;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.axone_io.ignition.git.managers.GitImageManager.exportImages;
import static com.axone_io.ignition.git.managers.GitManager.*;
import static com.axone_io.ignition.git.managers.GitTagManager.exportTag;
import static com.axone_io.ignition.git.managers.GitThemeManager.exportTheme;

public class GatewayScriptModule extends AbstractScriptModule {
    private final LoggerEx logger = LoggerEx.newBuilder().build(getClass());
    private final GatewayContext context;

    GatewayScriptModule(GatewayContext context) {
        this.context = context;
    }

    @Override
    public boolean pullImpl(String projectName, String userName) throws Exception {
        try (Git git = getGit(getProjectFolderPath(projectName))) {
            PullCommand pull = git.pull();
            setAuthentication(pull, projectName, userName);

            PullResult result = pull.call();
            if (!result.isSuccessful()) {
                logger.warn("Cannot pull from git");
            } else {
                logger.info("Pull was successful.");
            }

        } catch (GitAPIException e) {
            logger.error(e.toString());
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean pushImpl(String projectName, String userName) throws Exception {
        try (Git git = getGit(getProjectFolderPath(projectName))) {
            PushCommand push = git.push();

            setAuthentication(push, projectName, userName);

            Iterable<PushResult> results = push.setPushAll().setPushTags().call();
            for (PushResult result : results) {
                logger.trace(result.getMessages());
            }

        } catch (GitAPIException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    protected boolean commitImpl(String projectName, String userName, List<String> changes, String message) {
        try (Git git = getGit(getProjectFolderPath(projectName))) {
            for (String change : changes) {
                git.add().addFilepattern(change).call();
                git.add().setUpdate(true).addFilepattern(change).call();
            }

            CommitCommand commit = git.commit().setMessage(message);
            setCommitAuthor(commit, projectName, userName);
            commit.call();
        } catch (GitAPIException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public Dataset getUncommitedChangesImpl(String projectName, String userName) {
        Path projectPath = getProjectFolderPath(projectName);
        Dataset ds;
        List<String> changes = new ArrayList<>();
        DatasetBuilder builder = new DatasetBuilder();
        builder.colNames(List.of("resource", "type", "actor"));
        builder.colTypes(List.of(String.class, String.class, String.class));

        try (Git git = getGit(projectPath)) {
            Status status = git.status().call();

            Set<String> missing = status.getMissing();
            uncommittedChangesBuilder(projectName, missing, "Deleted", changes, builder);

            Set<String> uncommittedChanges = status.getUncommittedChanges();
            uncommittedChangesBuilder(projectName, uncommittedChanges, "Uncommitted", changes, builder);

            Set<String> untracked = status.getUntracked();
            uncommittedChangesBuilder(projectName, untracked, "Created", changes, builder);

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
            getGitReposUserRecord(gitProjectsConfigRecord, userName);
            registered = true;
        } catch (Exception e) {
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

        if (!Files.exists(path)) {
            String defaultBranch = GitCommissioningUtils.config != null ? GitCommissioningUtils.config.getInitDefaultBranch() : null;
            try (Git git = Git.init().setInitialBranch(defaultBranch).setDirectory(projectFolderPath.toFile()).call()) {
                disableSsl(git);

                git.remoteAdd().setName("origin").setUri(new URIish(gitProjectsConfigRecord.getURI())).call();

                git.add().addFilepattern(".").call();

                CommitCommand commit = git.commit().setMessage("Initial commit");
                setCommitAuthor(commit, projectName, userName);
                commit.call();

                PushCommand pushCommand = git.push();

                setAuthentication(pushCommand, projectName, userName);

                String branch = git.getRepository().getBranch();
                pushCommand.setRemote("origin").setRefSpecs(new RefSpec(branch)).call();
            } catch (Exception e) {
                logger.warn("An error occurred while setting up local repo for '" + projectName + "' project.");
            }
        }
    }

    private Path getProjectFolderPath(String projectName) {
        Path dataDir = context.getSystemManager().getDataDir().toPath();
        return dataDir.resolve("projects").resolve(projectName);
    }
}
