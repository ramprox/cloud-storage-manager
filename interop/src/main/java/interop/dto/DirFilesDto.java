package interop.dto;

import interop.dto.fileinfo.FileInfo;

import java.io.Serializable;
import java.util.List;

public class DirFilesDto implements Serializable {

    private final String dirPath;

    private final List<FileInfo> fileInfos;

    public DirFilesDto(String dirPath, List<FileInfo> fileInfos) {
        this.dirPath = dirPath;
        this.fileInfos = fileInfos;
    }

    public String getDirPath() {
        return dirPath;
    }

    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

}
