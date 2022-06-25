package interop.dto;

import java.io.Serializable;

public class SearchFileDto implements Serializable {

    private final String startPath;

    private final String fileName;

    public SearchFileDto(String startPath, String fileName) {
        this.startPath = startPath;
        this.fileName = fileName;
    }

    public String getStartPath() {
        return startPath;
    }

    public String getFileName() {
        return fileName;
    }
}
