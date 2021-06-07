package server.util;

import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.RegularFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

public class Conversations {

    /**
     * Преобразование из File[] в List<FileInfo>
     * @param files преобразуемый список
     * @return преобразованный список типа List<FileInfo>
     * @throws IOException
     */
    public static List<FileInfo> getFileInfos(File[] files) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        if(files != null) {
            for(File file : files) {
                result.add(getFileInfo(file));
            }
        }
        return result;
    }

    /**
     * Преобразование из File в FileInfo
     * @param file преобразуемый объект типа File
     * @return преобразованный объект типа FileInfo
     * @throws IOException
     */
    public static FileInfo getFileInfo(File file) throws IOException {
        FileInfo fileInfo;
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        long createDate = attr.creationTime().toMillis();
        if(file.isDirectory()) {
            fileInfo = new Directory(file.getName(), file.lastModified(), createDate);
        } else {
            fileInfo = new RegularFile(file.getName(), file.lastModified(), file.length(), createDate);
        }
        return fileInfo;
    }
}
