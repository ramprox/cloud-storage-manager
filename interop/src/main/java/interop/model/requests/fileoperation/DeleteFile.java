package interop.model.requests.fileoperation;

import java.io.Serializable;

/**
 * Класс для запроса удаления файла
 */
public class DeleteFile extends FileOperation implements Serializable {
    public DeleteFile(String fileName) {
        super(fileName);
    }
}
