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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.axone_io.ignition.git.managers.GitManager.cloneRepo;

public class GitCommissioningUtils {
    private final static LoggerEx logger = LoggerEx.newBuilder().build(GitCommissioningUtils.class);


    @Subscribe
    public static void loadConfiguration() {
        Path dataDir = context.getSystemManager().getDataDir().toPath();
        File ignitionConf = dataDir.resolve("git.conf").toFile();
        ProjectManager projectManager = context.getProjectManager();

        try {
            GitCommissioningConfig config = parseConfigLines(FileUtils.readFileToByteArray(ignitionConf));
            if(projectManager.getProjectNames().contains(config.getIgnitionProjectName())){
                logger.info("The configuration of the git module was interrupted because the project '" + config.getIgnitionProjectName() + "' already exist.");
                return;
            }
            projectManager.createProject(config.getIgnitionProjectName(), new ProjectManifest(config.getIgnitionProjectName(), "", false, false,""), new ArrayList());

            Path projectDir = dataDir.resolve("projects").resolve(config.getIgnitionProjectName());
            FileUtils.cleanDirectory(projectDir.toFile());

            // Creation of records
            PersistenceInterface persistenceInterface = context.getPersistenceInterface();
            SQuery<GitProjectsConfigRecord> query = new SQuery<>(GitProjectsConfigRecord.META).eq(GitProjectsConfigRecord.ProjectName, config.getIgnitionProjectName());
            if(persistenceInterface.queryOne(query) != null){
                logger.info("The configuration of the git module was interrupted because the GitProjectsConfigRecord '" + config.getIgnitionProjectName() + "' already exist.");
                return;
            }
            GitProjectsConfigRecord projectsConfigRecord = persistenceInterface.createNew(GitProjectsConfigRecord. META);
            projectsConfigRecord.setProjectName(config.getIgnitionProjectName());
            projectsConfigRecord.setURI(config.getRepoURI());
            persistenceInterface.save(projectsConfigRecord);

            GitReposUsersRecord reposUsersRecord = persistenceInterface.createNew(GitReposUsersRecord. META);
            reposUsersRecord.setUserName(config.getUserName());
            reposUsersRecord.setIgnitionUser(config.getIgnitionUserName());
            reposUsersRecord.setProjectId(projectsConfigRecord.getId());
            reposUsersRecord.setPassword(config.getUserPassword());
            reposUsersRecord.setEmail(config.getUserEmail());
            persistenceInterface.save(reposUsersRecord);

           // CLONE PROJECT
            cloneRepo(config.getIgnitionProjectName(), config.getUserName(), config.getRepoURI(), config.getRepoBranch());

            // IMPORT PROJECT
            GitProjectManager.importProject(config.getIgnitionProjectName());

            // IMPORT TAGS
            if(config.isImportTags()){
                GitTagManager.importTagManager(config.getIgnitionProjectName());
            }

            // IMPORT THEMES
            if(config.isImportThemes()){
                GitThemeManager.importTheme(config.getIgnitionProjectName());
            }

            // IMPORT IMAGES
            if(config.isImportImages()){
                GitImageManager.importImages(config.getIgnitionProjectName());
            }

        } catch (Exception e) {
            logger.error("An error occurred while git configuration settings up." ,e);
        }
    }



    static protected GitCommissioningConfig parseConfigLines(byte[] ignitionConf) throws Exception {
        Pattern repoUriPattern = Pattern.compile("repo.uri");
        Pattern repoBranchPattern = Pattern.compile("repo.branch");
        Pattern projectNamePattern = Pattern.compile("ignition.project.name");

        Pattern ignitionUserName = Pattern.compile("ignition.user.name");
        Pattern userNamePattern = Pattern.compile("user.name");
        Pattern passwordPattern = Pattern.compile("user.password");
        Pattern emailPattern = Pattern.compile("user.email");
        Pattern importTags = Pattern.compile("commissioning.import.tags");
        Pattern importThemes = Pattern.compile("commissioning.import.themes");
        Pattern importImages = Pattern.compile("commissioning.import.images");

        GitCommissioningConfig config = new GitCommissioningConfig();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(ignitionConf), StandardCharsets.UTF_8));
        try {
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

                if (repoUriMatcher.find()) {
                    config.setRepoURI(line.split("=")[1]);
                } else if (repoBranchMatcher.find()) {
                    config.setRepoBranch(line.split("=")[1]);
                }else if (projectNameMatcher.find()) {
                    config.setIgnitionProjectName(line.split("=")[1]);
                }else if (ignitionUserNameMatcher.find()) {
                    config.setIgnitionUserName(line.split("=")[1]);
                }else if (userNameMatcher.find()) {
                    config.setUserName(line.split("=")[1]);
                }else if (passwordMatcher.find()) {
                    config.setUserPassword(line.split("=")[1]);
                }else if (emailMatcher.find()) {
                    config.setUserEmail(line.split("=")[1]);
                }else if(importTagsMatcher.find()){
                    config.setImportTags(Boolean.parseBoolean(line.split("=")[1]));
                }else if(importThemesMatcher.find()){
                    config.setImportThemes(Boolean.parseBoolean(line.split("=")[1]));
                }else if(importImagesMatcher.find()){
                    config.setImportImages(Boolean.parseBoolean(line.split("=")[1]));
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while importing the Git configuration file.", e);
            throw new RuntimeException(e);
        }finally {
            reader.close();
        }
        return config;
    }
}
