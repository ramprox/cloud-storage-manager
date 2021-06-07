package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.responses.UploadResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectEncoder;
import java.io.File;
import java.util.List;

/**
 * Класс обработчика, ответственный за обработку ответов по загрузке файлов на сервер
 */
public class UploadHandler extends SimpleChannelInboundHandler<UploadResponse> {

    private List<File> uploadingFiles;

    public void setUploadingFiles(List<File> uploadingFiles) {
        this.uploadingFiles = uploadingFiles;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UploadResponse uploadResponse) {
        if(uploadResponse.getResponce() instanceof Boolean) {
            SendFilesHandler sendFilesHandler = new SendFilesHandler();
            ctx.pipeline().replace(ObjectEncoder.class, "sender", sendFilesHandler);
            for(File file : uploadingFiles) {
                ctx.channel().writeAndFlush(file);
            }
        } else if(uploadResponse.getResponce() instanceof Double) {
            Presentable presentable = Client.getPresentable();
            double percentUpload = (double)uploadResponse.getResponce();
            if(percentUpload == 1.0 && uploadingFiles.size() != 0) {
                ctx.pipeline().replace(SendFilesHandler.class, "encoder", new ObjectEncoder());
            }
            presentable.progressUpload(percentUpload);
        }
    }
}
