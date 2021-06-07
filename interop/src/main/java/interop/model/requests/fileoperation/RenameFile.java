package interop.model.requests.fileoperation;

import java.io.Serializable;

/**
 * Класс для запроса удаления файла
 */
public class RenameFile extends FileOperation implements Serializable {

    private String newFileName;

    public RenameFile(String oldFileName, String newFileName) {
        super(oldFileName);
        this.newFileName = newFileName;
    }

    public String getNewFileName() {
        return newFileName;
    }
}
