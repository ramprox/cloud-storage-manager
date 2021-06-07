package server.handlers;

import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.RegularFile;
import interop.model.requests.UploadRequest;
import interop.model.responses.UploadResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectDecoder;
import server.Server;
import server.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс обработчика, ответственный за обарботку запросов по загрузке файлов на сервер
 */
public class UploadHandler extends SimpleChannelInboundHandler<UploadRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UploadRequest uploadRequest) throws Exception {
        User user = Server.getUserByChannel(ctx.channel());
        List<FileInfo> uploadingFiles = uploadRequest.getUploadList();
        for(FileInfo fileInfo : uploadingFiles) {
            if(fileInfo instanceof Directory) {
                File file = new File(user.getCurrentDir().resolve(fileInfo.getFileName()).toString());
                file.mkdirs();
            } else if(fileInfo instanceof RegularFile) {
                RegularFile regFile = (RegularFile)fileInfo;
                if(regFile.getSize() == 0) {
                    Path path = user.getCurrentDir().resolve(fileInfo.getFileName());
                    Files.deleteIfExists(path);
                    Files.createFile(path);
                }
            }
        }

        List<FileInfo> filteredFiles = uploadingFiles.stream()             // фильтрация
                .filter(fileInfo -> !(fileInfo instanceof Directory)       // остаются только файлы
                        && ((RegularFile) fileInfo).getSize() != 0)        // с ненулевым размером
                .collect(Collectors.toList());

        if(filteredFiles.size() == 0) {
            UploadResponse response = new UploadResponse(1.0);
            ctx.writeAndFlush(response);
            return;
        }
        RegularFile[] files = new RegularFile[filteredFiles.size()];
        filteredFiles.toArray(files);
        FileWriterHandler writerHandler = new FileWriterHandler(files);
        ctx.pipeline().addFirst(writerHandler);
        UploadResponse response = new UploadResponse(true);
        ctx.writeAndFlush(response);
    }
}
