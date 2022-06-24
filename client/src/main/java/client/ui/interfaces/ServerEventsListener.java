package client.ui.interfaces;

import interop.dto.fileinfo.FileInfo;

import java.util.List;

/**
 * Объекты классов, реализующие данный интефейс реагируют на события приходящие от сервера
 */
public interface ServerEventsListener {

    void clientSigned(String currentDir, List<FileInfo> files);

    void channelActivated();

    void currentDirChanged(String newDirName, List<FileInfo> files);

    void fileCreated(FileInfo fileInfo);

    void fileRenamed(String oldFilePath, String newFilePath);

    void fileDeleted(String deletedFilePath);

    void progressUpload(double percent, String fileName);

    void uploadDone(String destination);

    void progressDownload(double percent, String fileName);

    void downloadDone(String downloadingPath);

    void foundedFilesReceived(List<FileInfo> foundedFiles);

    void viewDirSizeOnServer(FileInfo fileInfo);

    void errorReceived(String errorMessage);

    void exceptionCaught(Throwable cause);

}
