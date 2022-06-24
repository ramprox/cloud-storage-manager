package client.network.model;

import java.util.List;

public class UploadOperation {

    private final String sourcePath;

    private final List<String> files;

    private final String destination;

    public UploadOperation(String sourcePath, List<String> files, String destination) {
        this.sourcePath = sourcePath;
        this.files = files;
        this.destination = destination;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getDestination() {
        return destination;
    }
}
