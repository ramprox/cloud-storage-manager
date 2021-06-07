package interop.model.responses;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Класс для откликов клиенту по успешному изменению текущей директории
 */
public class ChangeDirResp implements Serializable {
    private String currentPath;
    private List<FileInfo> files;

    public ChangeDirResp(String currentPath, List<FileInfo> files) {
        this.currentPath = currentPath;
        this.files = files;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public List<FileInfo> getFiles() {
        return files;
    }
}
