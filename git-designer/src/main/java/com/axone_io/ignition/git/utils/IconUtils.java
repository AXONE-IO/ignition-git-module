package com.axone_io.ignition.git.utils;

import com.axone_io.ignition.git.DesignerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class IconUtils {

    private static final Logger logger = LoggerFactory.getLogger(IconUtils.class);

    public static Icon getIcon(String bundleKey){
        InputStream iconStream = DesignerHook.class.getResourceAsStream(bundleKey);
        BufferedImage buffer = null;
        try {
            buffer = ImageIO.read(iconStream);
        } catch (IOException e) {
            logger.warn(e.toString(), e);
        }
        return buffer != null ? new ImageIcon(buffer) : null;
    }
}
