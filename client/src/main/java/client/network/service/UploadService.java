package client.network.service;

import interop.dto.Message;

import java.util.List;

public interface UploadService {
    
    void uploadRequest(String sourcePath, List<String> relativePaths, String destination);

    void readyUpload(Message message) throws Exception;

    void percentUpload(Message message) throws Exception;

    void uploadDone(Message message) throws Exception;

}
