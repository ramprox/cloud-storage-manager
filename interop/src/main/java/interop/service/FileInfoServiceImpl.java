package interop.service;

import interop.ThrowableConsumer;
import interop.model.LoadingFiles;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileInfoServiceImpl implements FileInfoService {

    @Override
    public List<FileInfo> getFileInfos(Path dirPath) throws IOException {
        try(Stream<Path> pathStream = Files.list(dirPath)) {
            List<Path> paths = pathStream.collect(Collectors.toList());
            return paths.stream()
                    .map(this::getFileInfo)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public FileInfo getFileInfo(Path path) {
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            String fileName = path.toString();
            FileType type = attr.isDirectory() ? FileType.DIR : FileType.FILE;
            long size = type == FileType.DIR ? -1L : attr.size();
            long lastModified = attr.lastModifiedTime().toMillis();
            long createDate = attr.creationTime().toMillis();
            return new FileInfo(type, fileName, size, lastModified, createDate);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long getDirSize(Path path) throws IOException {
        final long[] result = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                result[0] += file.toFile().length();
                return FileVisitResult.CONTINUE;
            }
        });
        return result[0];
    }

    @Override
    public LoadingFiles formFilesLists(String source, List<String> relativePaths) {
        LoadingFiles files = new LoadingFiles();
        Path sourcePath = Paths.get(source);
        relativePaths
                .forEach(wrapper(path -> {
                    Path fullPath = Paths.get(source, path);
                    if(Files.isDirectory(fullPath)) {
                        formFilesForDir(files, sourcePath, fullPath);
                    } else {
                        formFilesForFile(files, sourcePath, fullPath);
                    }
                }));
        return files;
    }

    private Consumer<String> wrapper(ThrowableConsumer<String> throwableConsumer) {
        return files -> {
            try {
                throwableConsumer.accept(files);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private void formFilesForDir(LoadingFiles files, Path source, Path fullPath) throws IOException {
        try (Stream<Path> paths = Files.walk(fullPath)) {
            paths.map(Path::toString)
                    .forEach(wrapper(sub -> {
                        Path subPath = Paths.get(sub);
                        Path relativePath = source.relativize(subPath);
                        if(!Files.isDirectory(subPath) && Files.size(subPath) > 0) {
                            files.getSenderPaths().add(relativePath.toString());
                        }
                        FileInfo fileInfo = formRecipientFileInfo(relativePath, subPath);
                        files.getRecipientFiles().add(fileInfo);
                    }));
        }
    }

    private void formFilesForFile(LoadingFiles files, Path source, Path fullPath) throws IOException {
        Path relativePath = source.relativize(fullPath);
        if (Files.size(fullPath) > 0) {
            files.getSenderPaths().add(relativePath.toString());
        }
        FileInfo fileInfo = formRecipientFileInfo(relativePath, fullPath);
        files.getRecipientFiles().add(fileInfo);
    }

    private FileInfo formRecipientFileInfo(Path relativePath, Path fullPath) {
        FileInfo fileInfo = getFileInfo(fullPath);
        fileInfo.setFileName(relativePath.toString());
        return fileInfo;
    }
}
