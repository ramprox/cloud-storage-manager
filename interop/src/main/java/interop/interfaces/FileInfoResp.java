package interop.interfaces;

import interop.model.fileinfo.FileInfo;

/**
 * Объекты классов откликов (Responses), которые в ответе отправляют FileInfo
 */
public interface FileInfoResp {
    FileInfo getFile();
}
