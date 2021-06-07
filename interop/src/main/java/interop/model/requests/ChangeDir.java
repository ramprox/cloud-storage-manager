package interop.model.requests;

import java.io.Serializable;

/**
 * Класс для запросов изменения текущей директории
 */
public class ChangeDir implements Serializable {
    private String path;                            // путь к новой директории

    public ChangeDir(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
