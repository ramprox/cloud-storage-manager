package server.handlers;

import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.RegularFile;
import interop.model.requests.DownloadRequest;
import interop.model.requests.UploadRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectEncoder;
import server.Server;
import server.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс обработчика, ответственный за обработку запросов по загрузке файлов из сервера
 */
public class DownloadHandler extends SimpleChannelInboundHandler<DownloadRequest> {

    private List<File> uploadingRegularFiles;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DownloadRequest downloadRequest) throws Exception {
        if(downloadRequest.getRequest() instanceof String) {
            User user = Server.getUserByChannel(ctx.channel());
            String path = String.valueOf(downloadRequest.getRequest());
            Path filePath = Paths.get(Server.SERVER_FOLDER, path);

            List<FileInfo> fullListForClient = getFullListForUpload(filePath);

            uploadingRegularFiles = fullListForClient.stream()
                    .filter(fileInfo -> !(fileInfo instanceof Directory)
                            && ((RegularFile)fileInfo).getSize() != 0)
                    .map(fileInfo -> {
                        String serverPath = user.getCurrentDir().resolve(fileInfo.getFileName()).toString();
                        return new File(serverPath);
                    }).collect(Collectors.toList());
            UploadRequest request = new UploadRequest(fullListForClient);
            ctx.writeAndFlush(request);
        } else if(downloadRequest.getRequest() instanceof Boolean) {
            if(uploadingRegularFiles.size() != 0) {
                SendFilesHandler sendFilesHandler = new SendFilesHandler();
                ctx.pipeline().replace(ObjectEncoder.class, "writer", sendFilesHandler);
                File[] files = new File[uploadingRegularFiles.size()];
                uploadingRegularFiles.toArray(files);
                for(int i = 0; i < files.length; i++) {
                    if(i == files.length - 1) {
                        ctx.writeAndFlush(files[i]).addListener(future -> {
                            ctx.pipeline().replace(sendFilesHandler, "encoder", new ObjectEncoder());
                        });
                    } else {
                        ctx.writeAndFlush(files[i]);
                    }
                }
            }
        }
    }

    /**
     * Получение полного списка файлов для отправки клиенту
     * @param path путь к файлу. Файл может быть директорией или простым файлом
     * @return полный список файлов для отправки клиенту
     */
    private List<FileInfo> getFullListForUpload(Path path) {
        List<FileInfo> result = new LinkedList<>();
        try {
            if(Files.isDirectory(path)) {
                addToListForDirectory(path.toAbsolutePath(), result);
            } else {
                File file = path.toFile();
                BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                result.add(new RegularFile(file.getName(), file.lastModified(), file.length(), createDate));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Добавление к полному списку если путь к запрошенному файлу является директорией
     * @param path путь к файлу
     * @param list список для добавления
     * @throws IOException
     */
    private void addToListForDirectory(Path path, List<FileInfo> list) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                File file = dir.toFile();
                BasicFileAttributes attr = Files.readAttributes(dir, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                FileInfo fileInfo = new Directory(path.getFileName().resolve(path.relativize(dir)).toString(),
                        file.lastModified(), createDate);
                list.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                FileInfo fileInfo = new RegularFile(path.getFileName().resolve(path.relativize(file)).toString(),
                        file.toFile().lastModified(), file.toFile().length(), createDate);
                list.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
