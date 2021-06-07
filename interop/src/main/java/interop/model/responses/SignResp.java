package interop.model.responses;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Класс для откликов клиенту по умпешной авторизации или регистрации
 */
public class SignResp implements Serializable {
    private String currentPath;
    private List<FileInfo> files;

    public SignResp(String currentPath, List<FileInfo> files) {
        this.currentPath = currentPath;
        this.files = files;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public String getCurrentPath() {
        return currentPath;
    }
}
