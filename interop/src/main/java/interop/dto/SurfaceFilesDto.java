package interop.dto;

import java.io.Serializable;
import java.util.List;

public class SurfaceFilesDto implements Serializable {

    private final String sourcePath;

    private final List<String> fileNames;

    public SurfaceFilesDto(String sourcePath, List<String> fileNames) {
        this.sourcePath = sourcePath;
        this.fileNames = fileNames;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public List<String> getFileNames() {
        return fileNames;
    }
}
