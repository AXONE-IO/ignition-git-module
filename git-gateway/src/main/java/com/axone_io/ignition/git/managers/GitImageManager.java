package com.axone_io.ignition.git.managers;

import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.images.ImageFormat;
import com.inductiveautomation.ignition.gateway.images.ImageManager;
import com.inductiveautomation.ignition.gateway.images.ImageRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import simpleorm.dataset.SQuery;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.axone_io.ignition.git.GatewayHook.context;
import static com.axone_io.ignition.git.managers.GitManager.clearDirectory;
import static com.axone_io.ignition.git.managers.GitManager.getProjectFolderPath;

public class GitImageManager {
    private static final LoggerEx logger = LoggerEx.newBuilder().build(GitImageManager.class);

    public static void importImages(String projectName) {
        Path projectDir = getProjectFolderPath(projectName);
        File directory = projectDir.resolve("images").toFile();

        // DELETION
        PersistenceInterface persistenceInterface = context.getPersistenceInterface();
        List<ImageRecord> images = persistenceInterface.query(new SQuery<>(ImageRecord.META));
        images.forEach(i -> {
            i.deleteRecord();
            persistenceInterface.save(i);
        });

        // INSERTION
        File[] files = directory.listFiles();
        uploadFiles(files != null ? files : new File[0]);
    }

    protected static void uploadFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                uploadFolder(file, "");
            } else {
                uploadFile(file, "");
            }
        }
    }


    protected static void uploadFile(File f, String path) {
        String lName = f.getName().toLowerCase();
        if (lName.endsWith(".png") || lName
                .endsWith(".gif") || lName
                .endsWith(".jpg") || lName
                .endsWith(".jpeg") || lName
                .endsWith(".svg"))
            try {
                String ext = lName.substring(lName.lastIndexOf(".") + 1).toUpperCase();
                byte[] bytes = Files.readAllBytes(f.toPath());
                int width = 0;
                int height = 0;
                Image img = Toolkit.getDefaultToolkit().createImage(bytes);
                if (PathIcon.waitForImage(img)) {
                    width = img.getWidth(null);
                    height = img.getHeight(null);
                }

                try {
                    context.getImageManager().insertImage(f.getName(), "", ImageFormat.valueOf(ext), path, bytes, width, height, bytes.length);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } catch (FileNotFoundException e) {
                logger.error("FileNotFound exception for file: '" + f.getPath() + "'");
            } catch (IOException e) {
                logger.error("IOException exception for file: '" + f.getPath() + "'");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }


    protected static void uploadFolder(File dir, String path) {
        try {
            context.getImageManager().insertImageFolder(dir.getName(), path.equals("") ? null : path);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        uploadFolder(file, path + dir.getName() + "/");
                    } else {
                        uploadFile(file, path + dir.getName() + "/");
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void exportImages(Path projectFolderPath) {
        Path imageFolderPath = projectFolderPath.resolve("images");
        clearDirectory(imageFolderPath);
        try {
            Files.createDirectories(imageFolderPath);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        saveFolderImage(imageFolderPath, "");
    }

    public static void saveFolderImage(Path folderPath, String directory) {
        ImageManager imageManager = context.getImageManager();
        for (ImageRecord imageRecord : imageManager.getImages(directory)) {
            String path = imageRecord.getString(ImageRecord.Path);
            if (imageRecord.isDirectory()) {
                try {
                    Files.createDirectories(folderPath.resolve(path));
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }

                saveFolderImage(folderPath, path);
            } else {
                byte[] data = imageManager.getImage(path).getBytes(ImageRecord.Data);

                try {
                    Files.write(folderPath.resolve(path), data);
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
            }
        }
    }
}

class PathIcon extends ImageIcon {
    protected static final Component COMP = new Component() {
    };
    private static MediaTracker tracker;
    private static int nextId;
    public static boolean waitForImage(Image image) {
        if (image == null) {
            return false;
        } else if (image instanceof BufferedImage) {
            return true;
        } else {
            int id;
            synchronized(COMP) {
                id = nextId++;
            }

            tracker.addImage(image, id);

            try {
                tracker.waitForID(id);
            } catch (InterruptedException var4) {
                System.err.println("Image loading interrupted!");
                return false;
            }

            boolean success = !tracker.isErrorID(id);
            tracker.removeImage(image, id);
            return success;
        }
    }

    static {
        tracker = new MediaTracker(COMP);
        nextId = 0;
    }
}
