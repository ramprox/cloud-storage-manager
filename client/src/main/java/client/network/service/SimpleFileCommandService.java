package client.network.service;

import interop.dto.Message;

public interface SimpleFileCommandService {

    void changeDirRequest(String path);

    void changeDirResponse(Message message);

    void getDirSizeRequest(Object... args);

    void getDirSizeResponse(Message message);

    void createFileRequest(String filePath);

    void createFileResponse(Message message);

    void createDirRequest(String filePath);

    void createDirResponse(Message message);

    void deleteFileRequest(String filePath);

    void deleteFileResponse(Message message);

    void renameFileRequest(String oldFilePath, String newFilePath);

    void renameFileResponse(Message message);

    void searchFileRequest(String startPath, String fileName);

    void searchFileResponse(Message message);

}
