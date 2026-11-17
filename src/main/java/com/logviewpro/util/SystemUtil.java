package com.logviewpro.util;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class SystemUtil {
    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);
    private static TrayIcon trayIcon;

    static {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                // Create minimal 16x16 transparent image if no icon loaded yet
                Image image = Toolkit.getDefaultToolkit().createImage(new byte[]{});
                trayIcon = new TrayIcon(image, "LogView Pro");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            }
        } catch (Exception e) {
            logger.debug("SystemTray not available: {}", e.getMessage());
        }
    }

    public static void playNotificationSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            logger.debug("Beep failed: {}", e.getMessage());
        }
    }

    public static void showDesktopNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, messageType != null ? messageType : TrayIcon.MessageType.INFO);
                return;
            } catch (Exception e) {
                logger.debug("Desktop notification failed: {}", e.getMessage());
            }
        }
        logger.info("[NOTIFICATION] {} - {}", title, message);
    }

    public static void copyToClipboard(String text) {
        if (text == null) return;
        Platform.runLater(() -> {
            try {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            } catch (Exception e) {
                logger.error("Clipboard copy failed: {}", e.getMessage());
            }
        });
    }
}
