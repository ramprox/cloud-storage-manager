package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.fileinfo.FileInfo;
import interop.model.responses.ChangeDirResp;
import io.netty.channel.*;
import java.util.List;

/**
 * Класс обработчика, ответственный за обработку ответа успешного изменения текущей директории на сервере
 */
public class ChangeDirHandler extends SimpleChannelInboundHandler<ChangeDirResp> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChangeDirResp changeDirResp) {
        Presentable presentable = Client.getPresentable();
        String currentDir = changeDirResp.getCurrentPath();
        List<FileInfo> files = changeDirResp.getFiles();
        presentable.currentDirChanged(currentDir, files);
    }
}
