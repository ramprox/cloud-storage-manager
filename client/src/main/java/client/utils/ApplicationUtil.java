package client.utils;

import client.MainClientApp;
import javafx.scene.image.Image;

/**
 * Вспомогательный класс
 */
public class ApplicationUtil {
    public static final Image IMG_DIRECTORY = new Image(MainClientApp.class.getResource("/images/dir.png").toString());
    public static final Image IMG_FILE = new Image(MainClientApp.class.getResource("/images/file.png").toString());
    public static final Image IMG_PARENT_DIR = new Image(MainClientApp.class.getResource("/images/parentdir.png").toString());
}
