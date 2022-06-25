package client.network.service;

import interop.dto.Message;

import java.util.List;

public interface DownloadService {

    void downloadRequest(String sourcePath, List<String> files, String destination);

    void downloadResponse(Message message) throws Exception;

    void downloading(Message message) throws Exception;

}
