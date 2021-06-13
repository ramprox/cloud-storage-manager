package server.model;

import server.util.ApplicationUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;

public class User {
    private Path currentDir;
    private final String login;

    public User(String login) {
        this.login = login;
        currentDir = getHomeDir();
    }

    public void setCurrentDir(Path currentDir) {
        this.currentDir = currentDir;
    }

    public Path getCurrentDir() {
        return currentDir;
    }

    public Path getHomeDir() {
        return Paths.get(ApplicationUtil.SERVER_FOLDER, login);
    }

    public String getLogin() {
        return login;
    }

    public String getPrompt() {
        return "~" + currentDir.toString().substring(
                (ApplicationUtil.SERVER_FOLDER + File.separator + login).length());
    }
}
