package com.axone_io.ignition.git.managers;

import com.inductiveautomation.ignition.common.JsonUtilities;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.model.TagProviderInformation;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.axone_io.ignition.git.managers.GitManager.clearDirectory;
import static com.axone_io.ignition.git.managers.GitManager.getProjectFolderPath;
import static com.inductiveautomation.ignition.common.tags.TagUtilities.TAG_GSON;

public class GitTagManager {
    private final static LoggerEx logger = LoggerEx.newBuilder().build(GitTagManager.class);

    public static void importTagManager(String projectName) {
        Path projectDir = getProjectFolderPath(projectName);
        File tagsProjectDir = projectDir.resolve("tags").toFile();

        GatewayTagManager gatewayTagManager = context.getTagManager();
        if (tagsProjectDir.exists()) {
            File[] files = tagsProjectDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String providerName = FilenameUtils.removeExtension(file.getName());
                    TagProvider tagProvider = gatewayTagManager.getTagProvider(providerName);
                    if (tagProvider != null) {
                        try {
                            tagProvider.importTagsAsync(new BasicTagPath(""), FileUtils.readFileToString(file, StandardCharsets.UTF_8.toString()), "JSON", CollisionPolicy.Overwrite, null);
                        } catch (IOException e) {
                            logger.warn("An error occurred while importing '" + providerName + "' tags.", e);
                        }
                    }
                }
            }
        }
    }

    public static void exportTag(Path projectFolderPath) {
        Path tagFolderPath = projectFolderPath.resolve("tags");
        clearDirectory(tagFolderPath);

        try {
            Files.createDirectories(tagFolderPath);

            for (TagProvider tagProvider : context.getTagManager().getTagProviders()) {
                logger.info("Attempting to export tags for provider " + tagProvider.getName());
                try {
                    CompletableFuture<TagProviderInformation> cfProviderInfo = tagProvider.getStatusInformation();
                    TagProviderInformation providerInfo = cfProviderInfo.get();
                    if (providerInfo == null || !providerInfo.isAvailable()) {
                        logger.info("Tag provider unavailable. Skipping export for provider " + tagProvider.getName());
                        continue;
                    }

                    TagPath typesPath = TagPathParser.parse("");
                    List<TagPath> tagPaths = new ArrayList<>();
                    tagPaths.add(typesPath);
                    CompletableFuture<List<TagConfigurationModel>> cfTagModels =
                            tagProvider.getTagConfigsAsync(tagPaths, true, true);
                    List<TagConfigurationModel> tModels = cfTagModels.get();
                    JsonObject json = TagUtilities.toJsonObject(tModels.get(0));
                    JsonElement sortedJson = JsonUtilities.createDeterministicCopy(json);
                    Path newFile = tagFolderPath.resolve(tagProvider.getName() + ".json");
                    Files.writeString(newFile, TAG_GSON.toJson(sortedJson));
                    logger.info("Successfully exported tags for provider " + tagProvider.getName());
                } catch (Exception e) {
                    logger.error(e.toString(), e);
                }
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }
}
