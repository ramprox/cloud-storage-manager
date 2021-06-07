package interop.model.requests.fileoperation;

import interop.model.fileinfo.FileType;
import java.io.Serializable;

/**
 * Класс для запроса создания файла
 */
public class CreateFile extends FileOperation implements Serializable {

    private FileType type;

    public CreateFile(String fileName, FileType type) {
        super(fileName);
        this.type = type;
    }

    public FileType getType() {
        return type;
    }
}
