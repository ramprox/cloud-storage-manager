package client.ui.interfaces;

import interop.model.fileinfo.FileInfo;

import java.util.List;

/**
 * Объекты классов, реализующие данный интефейс способны реагировать на события приходящие от сервера
 * Реализуется MainWindowController
 */
public interface Presentable {
    void clientSigned(String currentDir, List<FileInfo> files);
    void channelActivated();
    void currentDirChanged(String newDirName, List<FileInfo> files);
    void fileCreated(FileInfo fileInfo);
    void fileRenamed(String oldFilePath, String newFilePath);
    void fileDeleted(String deletedFilePath);
    void progressUpload(double percent, String fileName);
    void uploadDone(String currentDir, List<FileInfo> fileInfoList);
    void progressDownload(double percent, String fileName);
    void downloadDone(String downloadingPath);
    void foundedFilesReceived(List<FileInfo> foundedFiles);
    void viewDirSizeOnServer(FileInfo fileInfo, long size);
    void errorReceived(String errorMessage);
    void exceptionCaught(Throwable cause);
}
