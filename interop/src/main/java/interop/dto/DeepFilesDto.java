package interop.dto;

import interop.dto.fileinfo.FileInfo;

import java.io.Serializable;
import java.util.List;

public class DeepFilesDto implements Serializable {

    private final String destination;

    private final List<FileInfo> files;

    public DeepFilesDto(String destination, List<FileInfo> files) {
        this.destination = destination;
        this.files = files;
    }

    public String getDestination() {
        return destination;
    }

    public List<FileInfo> getFiles() {
        return files;
    }
}
