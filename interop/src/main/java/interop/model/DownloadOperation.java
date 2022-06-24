package interop.model;

import java.io.Serializable;
import java.nio.file.Path;

public class DownloadOperation implements Serializable {

    private String destination;

    private long currentSize;

    private long totalSize;

    private Path source;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public Path getSource() {
        return source;
    }

    public void setSource(Path source) {
        this.source = source;
    }
}
