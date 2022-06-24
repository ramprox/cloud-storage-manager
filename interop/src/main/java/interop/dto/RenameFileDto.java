package interop.dto;

import java.io.Serializable;

public class RenameFileDto implements Serializable {

    private final String oldFileName;

    private final String newFileName;

    public RenameFileDto(String oldFileName, String newFileName) {
        this.oldFileName = oldFileName;
        this.newFileName = newFileName;
    }

    public String getOldFileName() {
        return oldFileName;
    }

    public String getNewFileName() {
        return newFileName;
    }
}
