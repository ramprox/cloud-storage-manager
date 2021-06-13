package client.utils;

import client.MainClientApp;
import javafx.scene.image.Image;

/**
 * Вспомогательный класс
 */
public class ApplicationUtil {
    public static final Image IMG_DIRECTORY =
            new Image(MainClientApp.class.getResource("/images/dir.png").toString());
    public static final Image IMG_FILE =
            new Image(MainClientApp.class.getResource("/images/file.png").toString());
    public static final Image IMG_PARENT_DIR =
            new Image(MainClientApp.class.getResource("/images/parentdir.png").toString());
    public static final String SERVER_USER_ROOT_SYMBOL = "~";
    public static final int LOAD_BUFFER_SIZE = 8192 * 4;
    public static final String START_DIR_FOR_CLIENT = System.getProperty("user.home");
    public static final String HOST = "localhost";
    public static final int PORT = 5678;
}
