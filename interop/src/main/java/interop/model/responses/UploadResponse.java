package interop.model.responses;

import java.io.Serializable;

/**
 * Класс для откликов клиенту по загрузке файлов на сервер
 */
public class UploadResponse implements Serializable {
    private Object responce;

    public UploadResponse(Object responce) {
        this.responce = responce;
    }

    public Object getResponce() {
        return responce;
    }
}
