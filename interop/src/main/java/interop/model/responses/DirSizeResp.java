package interop.model.responses;

import interop.interfaces.FileInfoSizeResp;
import interop.model.fileinfo.FileInfo;

import java.io.Serializable;

/**
 * Класс для откликов клиенту по вычислению размера директории
 */
public class DirSizeResp implements FileInfoSizeResp, Serializable {

    private final FileInfo fileInfo;
    private final long size;

    public DirSizeResp(FileInfo fileInfo, long size) {
        this.fileInfo = fileInfo;
        this.size = size;
    }

    @Override
    public FileInfo getFile() {
        return fileInfo;
    }

    @Override
    public long getSize() {
        return size;
    }
}
