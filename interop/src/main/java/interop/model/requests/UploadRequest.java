package interop.model.requests;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Класс для запросов загрузки на сервер
 */
public class UploadRequest implements Serializable {
    private List<FileInfo> uploadList;                     // список загружаемых файлов

    public UploadRequest(List<FileInfo> uploadList) {
        this.uploadList = uploadList;
    }

    public List<FileInfo> getUploadList() {
        return uploadList;
    }
}
