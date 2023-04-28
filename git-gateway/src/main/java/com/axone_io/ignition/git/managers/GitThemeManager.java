package com.axone_io.ignition.git.managers;

import com.inductiveautomation.ignition.common.JsonUtilities;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.axone_io.ignition.git.managers.GitManager.*;

public class GitThemeManager {
    private final static LoggerEx logger = LoggerEx.newBuilder().build(GitThemeManager.class);

    public static void importTheme(String projectName) {
        Path dataDir = getDataFolderPath();
        Path projectDir = getProjectFolderPath(projectName);
        Path themesDir = dataDir.resolve("modules").resolve("com.inductiveautomation.perspective").resolve("themes");
        Path themesProjectDir = projectDir.resolve("themes");
        File themesProjectDirFile = themesProjectDir.toFile();

        if (themesProjectDirFile.exists()) {
            File[] files = themesProjectDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String themeName = FilenameUtils.removeExtension(file.getName());
                        try {
                            Path destinationDirectoryMain = themesDir.resolve(file.getName());
                            Path sourceDirectoryMain = themesProjectDir.resolve(file.getName());
                            Files.copy(sourceDirectoryMain, destinationDirectoryMain, StandardCopyOption.REPLACE_EXISTING);

                            File destinationDirectory = themesDir.resolve(themeName).toFile();
                            File sourceDirectory = themesProjectDir.resolve(themeName).toFile();
                            FileUtils.deleteDirectory(destinationDirectory);
                            FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
                        } catch (IOException e) {
                            logger.warn("An error occurred while importing '" + themeName + "' theme.", e);
                        }
                    }
                }
            }
        }
    }

    public static void exportTheme(Path projectFolderPath) {
        try {
            Path sessionPropsPath = projectFolderPath.resolve("com.inductiveautomation.perspective")
                    .resolve("session-props")
                    .resolve("props.json");
            String content = Files.readString(sessionPropsPath);
            Gson g = new Gson();
            JsonObject json = g.fromJson(content, JsonObject.class);
            String theme = JsonUtilities.readString(json, "props.theme", "light");

            Path themesDir = getDataFolderPath()
                    .resolve("modules")
                    .resolve("com.inductiveautomation.perspective")
                    .resolve("themes");

            Path themeFolder = themesDir.resolve(theme);
            Path themeFile = themesDir.resolve(theme + ".css");

            Path themeFolderPath = projectFolderPath.resolve("themes");
            clearDirectory(themeFolderPath);
            Files.createDirectories(themeFolderPath);
            FileUtils.copyDirectoryToDirectory(themeFolder.toFile(), themeFolderPath.toFile());
            Files.copy(themeFile, themeFolderPath.resolve(themeFile.getFileName()));
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
    }
}
