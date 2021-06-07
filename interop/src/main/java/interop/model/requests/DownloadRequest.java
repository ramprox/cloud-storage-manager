package interop.model.requests;

import java.io.Serializable;

/**
 * Класс для запросов загрузки файлов из сервера
 */
public class DownloadRequest implements Serializable {

    private Object request;

    public DownloadRequest(Object path) {
        this.request = path;
    }

    public Object getRequest() {
        return request;
    }
}
