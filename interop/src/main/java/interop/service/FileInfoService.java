package interop.service;

import interop.model.LoadingFiles;
import interop.dto.fileinfo.FileInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileInfoService {

    List<FileInfo> getFileInfos(Path dirPath) throws IOException;

    FileInfo getFileInfo(Path path);

    long getDirSize(Path path) throws IOException;

    LoadingFiles formFilesLists(String source, List<String> relativePaths);
}
