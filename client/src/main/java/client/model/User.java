package client.model;

import java.io.File;
import java.nio.file.*;

/**
 * Модель данных о пользователе
 */
public class User {
    private Path currentDir;
    private final String login;
    private boolean isSign;

    public User(String login) {
        this.login = login;
        currentDir = getHomeDir();
    }

    private Path getHomeDir() {
        return Paths.get(login);
    }

    public boolean isSign() {
        return isSign;
    }

    public void setSign(boolean sign) {
        isSign = sign;
    }

    public void setCurrentDir(Path currentDir) {
        this.currentDir = currentDir;
    }

    public String getLogin() {
        return login;
    }

    public String getPrompt() {
        String curDir = currentDir.toString();
        curDir = curDir.replaceFirst(login, "~");
        if(curDir.equals("~")) {
            curDir += File.separator;
        }
        return curDir;
    }

    public Path getCurrentDir() {
        return currentDir;
    }
}
