package interop.dto;

import java.io.Serializable;

public class FileChunkDto implements Serializable {

    private final String path;

    private final byte[] content;

    public FileChunkDto(String path, byte[] content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public byte[] getContent() {
        return content;
    }
}
