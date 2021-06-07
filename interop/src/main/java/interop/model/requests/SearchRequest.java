package interop.model.requests;

import java.io.Serializable;

/**
 * Класс для запросов поиска
 */
public class SearchRequest implements Serializable {
    private final String fileName;                       // имя искомого файла

    public SearchRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
