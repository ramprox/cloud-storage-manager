package client.interfaces;

import interop.model.fileinfo.FileInfo;
import java.util.List;

/**
 * Объекты классов, реализующие данный интефейс способны реагировать на события приходящие от сервера
 */
public interface Presentable {
    void clientSigned(String currentDir, List<FileInfo> files);
    void channelActivated();
    void currentDirChanged(String newDirName, List<FileInfo> files);
    void fileCreated(FileInfo fileInfo);
    void fileRenamed(FileInfo oldFileInfo, FileInfo newFileInfo);
    void fileDeleted(FileInfo deletedFileInfo);
    void progressUpload(double percent);
    void progressDownload(double percent);
    void foundedFilesReceived(List<FileInfo> foundedFiles);
    void viewDirSizeOnServer(FileInfo fileInfo, long size);
    void errorReceived(String errorMessage);
    void exceptionCaught(Throwable cause);
}
