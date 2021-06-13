package server.util;

import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.FileType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

public class ApplicationUtil {
    public static final String DB = "jdbc:mysql://localhost:3306/cloud_storage";
    public static final String USER = "root";
    public static final String PASSWORD = "root";
    public static final int LOAD_BUFFER_SIZE = 8192 * 4;
    public static final int PORT = 5678;
    public static final String SERVER_FOLDER = "storage";
    public static final String USER_ROOT_SYMBOL = "~";

    /**
     * Преобразование из List<Path> в List<FileInfo>
     * @param paths преобразуемый список
     * @return преобразованный список типа List<FileInfo>
     * @throws IOException
     */
    public static List<FileInfo> getFileInfos(List<Path> paths) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        if(paths != null) {
            for(Path path : paths) {
                result.add(getFileInfo(path));
            }
        }
        return result;
    }

    /**
     * Преобразование из Path в FileInfo
     * @param path путь к файлу
     * @return преобразованный объект типа FileInfo
     * @throws IOException
     */
    public static FileInfo getFileInfo(Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        String fileName = path.getFileName().toString();
        FileType type = attr.isDirectory() ? FileType.DIR : FileType.FILE;
        long size = type == FileType.DIR ? -1L : attr.size();
        long lastModified = attr.lastModifiedTime().toMillis();
        long createDate = attr.creationTime().toMillis();
        return new FileInfo(type, fileName, size, lastModified, createDate);
    }
}
