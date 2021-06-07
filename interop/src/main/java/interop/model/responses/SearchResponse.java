package interop.model.responses;

import interop.model.fileinfo.FileInfo;
import java.io.Serializable;
import java.util.List;

/**
 * Класс для откликов клиенту по поиску файлов
 */
public class SearchResponse implements Serializable {
    private final List<FileInfo> foundedFiles;

    public SearchResponse(List<FileInfo> foundedFiles) {
        this.foundedFiles = foundedFiles;
    }

    public List<FileInfo> getFoundedFiles() {
        return foundedFiles;
    }
}
