package server.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;

public class User {
    private Path currentDir;
    private Path homeDir;
    private String login;

    public User(String login) {
        this.login = login;
        this.homeDir = Paths.get(login);
    }

    public void setCurrentDir(Path currentDir) {
        this.currentDir = currentDir;
    }

    public Path getCurrentDir() {
        return currentDir;
    }

    public Path getHomeDir() {
        return homeDir;
    }

    public String getLogin() {
        return login;
    }

    public String getPrompt() {
        String result = "~" + File.separator;
        if(!currentDir.equals(homeDir)) {
            result += homeDir.relativize(currentDir).toString();
        }
        return result;
    }
}
