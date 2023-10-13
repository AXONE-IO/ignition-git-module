package com.axone_io.ignition.git.commissioning.utils;

import com.axone_io.ignition.git.commissioning.GitCommissioningConfig;
import com.axone_io.ignition.git.managers.GitImageManager;
import com.axone_io.ignition.git.managers.GitProjectManager;
import com.axone_io.ignition.git.managers.GitTagManager;
import com.axone_io.ignition.git.managers.GitThemeManager;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.google.common.eventbus.Subscribe;
import com.inductiveautomation.ignition.common.project.ProjectManifest;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import org.apache.commons.io.FileUtils;
import simpleorm.dataset.SQuery;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.axone_io.ignition.git.managers.GitManager.*;

public class GitCommissioningUtils {
    private static final LoggerEx logger = LoggerEx.newBuilder().build(GitCommissioningUtils.class);

    public static GitCommissioningConfig config;

    @Subscribe
    public static void loadConfiguration() {
        Path dataDir = getDataFolderPath();
        File ignitionConf = dataDir.resolve("git.conf").toFile();
        ProjectManager projectManager = context.getProjectManager();

        try {
            if (ignitionConf.exists() && ignitionConf.isFile()) {
                config = parseConfigLines(FileUtils.readFileToByteArray(ignitionConf));
                if (projectManager.getProjectNames().contains(config.getIgnitionProjectName())) {
                    logger.info("The configuration of the git module was interrupted because the project '" + config.getIgnitionProjectName() + "' already exist.");
                    return;
                }

                if (config.getRepoURI() == null || config.getRepoBranch() == null
                        || config.getIgnitionProjectName() == null || config.getIgnitionUserName() == null
                        || config.getUserName() == null || (config.getUserPassword() == null && config.getSshKey() == null)
                        || config.getUserEmail() == null) {
                    throw new RuntimeException("Incomplete git configuration file.");
                }

                projectManager.createProject(config.getIgnitionProjectName(), new ProjectManifest(config.getIgnitionProjectName(), "", false, false, ""), new ArrayList());

                Path projectDir = getProjectFolderPath(config.getIgnitionProjectName());
                clearDirectory(projectDir);

                // Creation of records
                PersistenceInterface persistenceInterface = context.getPersistenceInterface();
                SQuery<GitProjectsConfigRecord> query = new SQuery<>(GitProjectsConfigRecord.META).eq(GitProjectsConfigRecord.ProjectName, config.getIgnitionProjectName());
                if (persistenceInterface.queryOne(query) != null) {
                    logger.info("The configuration of the git module was interrupted because the GitProjectsConfigRecord '" + config.getIgnitionProjectName() + "' already exist.");
                    return;
                }
                GitProjectsConfigRecord projectsConfigRecord = persistenceInterface.createNew(GitProjectsConfigRecord.META);
                projectsConfigRecord.setProjectName(config.getIgnitionProjectName());
                projectsConfigRecord.setURI(config.getRepoURI());

                String userSecretFilePath = System.getenv("GATEWAY_GIT_USER_SECRET_FILE");
                if (userSecretFilePath != null) {
                    config.setSecretFromFilePath(Paths.get(userSecretFilePath), projectsConfigRecord.isSSHAuthentication());
                }
                if (config.getSshKey() == null && config.getUserPassword() == null) {
                    throw new Exception("Git User Password or SSHKey not configured.");
                }
                persistenceInterface.save(projectsConfigRecord);

                GitReposUsersRecord reposUsersRecord = persistenceInterface.createNew(GitReposUsersRecord.META);
                reposUsersRecord.setUserName(config.getUserName());
                reposUsersRecord.setIgnitionUser(config.getIgnitionUserName());
                reposUsersRecord.setProjectId(projectsConfigRecord.getId());
                if (projectsConfigRecord.isSSHAuthentication()) {
                    reposUsersRecord.setSSHKey(config.getSshKey());
                } else {
                    reposUsersRecord.setPassword(config.getUserPassword());
                }
                reposUsersRecord.setEmail(config.getUserEmail());
                persistenceInterface.save(reposUsersRecord);

                // CLONE PROJECT
                cloneRepo(config.getIgnitionProjectName(), config.getIgnitionUserName(), config.getRepoURI(), config.getRepoBranch());

                // IMPORT PROJECT
                GitProjectManager.importProject(config.getIgnitionProjectName());

                // IMPORT TAGS
                if (config.isImportTags()) {
                    GitTagManager.importTagManager(config.getIgnitionProjectName());
                }

                // IMPORT THEMES
                if (config.isImportThemes()) {
                    GitThemeManager.importTheme(config.getIgnitionProjectName());
                }

                // IMPORT IMAGES
                if (config.isImportImages()) {
                    GitImageManager.importImages(config.getIgnitionProjectName());
                }
            } else {
                logger.info("No git configuration file was found.");
            }

        } catch (Exception e) {
            logger.error("An error occurred while git configuration settings up.", e);
        }
    }


    static protected GitCommissioningConfig parseConfigLines(byte[] ignitionConf) {
        Pattern repoUriPattern = Pattern.compile("repo.uri");
        Pattern repoBranchPattern = Pattern.compile("repo.branch");

        Pattern projectNamePattern = Pattern.compile("ignition.project.name");
        Pattern ignitionUserName = Pattern.compile("ignition.user.name");

        Pattern userNamePattern = Pattern.compile("user.name");
        Pattern passwordPattern = Pattern.compile("user.password");
        Pattern emailPattern = Pattern.compile("user.email");
        Pattern sshKeyFilePath = Pattern.compile("user.shh.key.file.path");

        Pattern importTags = Pattern.compile("commissioning.import.tags");
        Pattern importThemes = Pattern.compile("commissioning.import.themes");
        Pattern importImages = Pattern.compile("commissioning.import.images");

        Pattern initDefaultBranch = Pattern.compile("init.defaultBranch");

        GitCommissioningConfig config = new GitCommissioningConfig();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(ignitionConf), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher repoUriMatcher = repoUriPattern.matcher(line);
                Matcher repoBranchMatcher = repoBranchPattern.matcher(line);
                Matcher projectNameMatcher = projectNamePattern.matcher(line);
                Matcher userNameMatcher = userNamePattern.matcher(line);
                Matcher passwordMatcher = passwordPattern.matcher(line);
                Matcher emailMatcher = emailPattern.matcher(line);
                Matcher ignitionUserNameMatcher = ignitionUserName.matcher(line);
                Matcher importTagsMatcher = importTags.matcher(line);
                Matcher importThemesMatcher = importThemes.matcher(line);
                Matcher importImagesMatcher = importImages.matcher(line);
                Matcher sshKeyFilePathMatcher = sshKeyFilePath.matcher(line);
                Matcher initDefaultBranchPathMatcher = initDefaultBranch.matcher(line);

                if (repoUriMatcher.find()) {
                    config.setRepoURI(line.split("=")[1]);
                } else if (repoBranchMatcher.find()) {
                    config.setRepoBranch(line.split("=")[1]);
                } else if (projectNameMatcher.find()) {
                    config.setIgnitionProjectName(line.split("=")[1]);
                } else if (ignitionUserNameMatcher.find()) {
                    config.setIgnitionUserName(line.split("=")[1]);
                } else if (userNameMatcher.find()) {
                    config.setUserName(line.split("=")[1]);
                } else if (passwordMatcher.find()) {
                    config.setUserPassword(line.split("=")[1]);
                } else if (emailMatcher.find()) {
                    config.setUserEmail(line.split("=")[1]);
                } else if (importTagsMatcher.find()) {
                    config.setImportTags(Boolean.parseBoolean(line.split("=")[1]));
                } else if (importThemesMatcher.find()) {
                    config.setImportThemes(Boolean.parseBoolean(line.split("=")[1]));
                } else if (importImagesMatcher.find()) {
                    config.setImportImages(Boolean.parseBoolean(line.split("=")[1]));
                } else if (sshKeyFilePathMatcher.find()) {
                    config.setSecretFromFilePath(Paths.get(line.split("=")[1]), true);
                } else if (initDefaultBranchPathMatcher.find()) {
                    config.setInitDefaultBranch(line.split("=")[1]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Invalid git configuration file.", e);
        } catch (Exception e) {
            logger.error("An error occurred while importing the Git configuration file.", e);
            throw new RuntimeException(e);
        }
        return config;
    }
}
