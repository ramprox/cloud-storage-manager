package interop.interfaces;

/**
 * Объекты классов откликов (Responses), которые в ответе отправляют FileInfo и long
 */
public interface FileInfoSizeResp extends FileInfoResp {
    long getSize();
}
