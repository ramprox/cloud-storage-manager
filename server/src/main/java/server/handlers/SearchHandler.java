package server.handlers;

import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.RegularFile;
import interop.model.requests.SearchRequest;
import interop.model.responses.SearchResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.Server;
import server.model.User;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

/**
 * Класс обработчика, отвественный за обработку запросов поиска файла
 */
public class SearchHandler extends SimpleChannelInboundHandler<SearchRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SearchRequest searchRequest) throws Exception {
        User user = Server.getUserByChannel(ctx.channel());
        String fileName = searchRequest.getFileName();
        Path startSearchPath = user.getCurrentDir();
        List<FileInfo> foundedFiles = findFilesByName(startSearchPath, fileName, user);
        SearchResponse response = new SearchResponse(foundedFiles);
        ctx.writeAndFlush(response);
    }

    /**
     * Поиск файла. В результаты добавляются все файлы, в названии которых содержится имя искомого файла
     * @param path стартовый путь
     * @param fileName имя искомого файла
     * @param user пользователь
     * @return список найденных файлов
     * @throws IOException
     */
    private List<FileInfo> findFilesByName(Path path, String fileName, User user) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(dir.getFileName().toString().contains(fileName)) {
                    String formattedFileName = "~" + dir.toString().substring(user.getHomeDir().toString().length());
                    result.add(new Directory(formattedFileName, 0L, 0L));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(file.getFileName().toString().contains(fileName)) {
                    BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                    long createDate = attr.creationTime().toMillis();
                    String formattedFileName = "~" + file.toString().substring(user.getHomeDir().toString().length());
                    FileInfo fileInfo = new RegularFile(formattedFileName, file.toFile().lastModified(),
                            file.toFile().length(), createDate);
                    result.add(fileInfo);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }
}
