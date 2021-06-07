package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.RegularFile;
import interop.model.requests.DownloadRequest;
import interop.model.requests.UploadRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс обработчика, ответственный за загрузку файлов от сервера
 */
public class DownloadHandler extends SimpleChannelInboundHandler<UploadRequest> {

    private Path currentClientDir;

    public void setCurrentClientDir(Path currentClientDir) {
        this.currentClientDir = currentClientDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UploadRequest uploadRequest) throws Exception {
        List<FileInfo> listFiles = uploadRequest.getUploadList();
        for(FileInfo fileInfo : listFiles) {
            if(fileInfo instanceof Directory) {
                File file = new File(currentClientDir.resolve(fileInfo.getFileName()).toString());
                file.mkdirs();
            } else if(fileInfo instanceof RegularFile) {
                RegularFile regFile = (RegularFile)fileInfo;
                if(regFile.getSize() == 0) {
                    Path path = currentClientDir.resolve(fileInfo.getFileName());
                    Files.deleteIfExists(path);
                    Files.createFile(path);
                }
            }
        }
        List<FileInfo> filteredFiles = listFiles.stream()
                .filter(fileInfo -> !(fileInfo instanceof Directory)
                        && ((RegularFile)fileInfo).getSize() != 0)
                .collect(Collectors.toList());
        if(filteredFiles.size() != 0) {
            RegularFile[] files = new RegularFile[filteredFiles.size()];
            filteredFiles.toArray(files);
            FileWriterHandler writerHandler = new FileWriterHandler(files, currentClientDir);
            ctx.pipeline().addFirst(writerHandler);
            DownloadRequest request = new DownloadRequest(true);
            ctx.writeAndFlush(request);
        } else {
            Presentable presentable = Client.getPresentable();
            presentable.progressDownload(1.0);
        }
    }
}
