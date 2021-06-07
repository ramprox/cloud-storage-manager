package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.interfaces.FileInfoResp;
import interop.model.responses.DirSizeResp;
import interop.model.responses.fileoperation.*;
import io.netty.channel.*;

/**
 * Класс обработчика, ответственный за обработку откликов по изменению файлов (создание, переименование, удаление)
 * и получение размера файлов
 */
public class FileChangeHandler extends SimpleChannelInboundHandler<FileInfoResp> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FileInfoResp response) {
        Presentable presentable = Client.getPresentable();
        if (response instanceof CreateFileResp) {
            presentable.fileCreated(response.getFile());
        } else if (response instanceof RenameFileResp) {
            RenameFileResp resp = (RenameFileResp) response;
            presentable.fileRenamed(resp.getFile(), resp.getNewFileInfo());
        } else if (response instanceof DeleteFileResp) {
            presentable.fileDeleted(response.getFile());
        } else if(response instanceof DirSizeResp) {
            DirSizeResp dirSizeResp = (DirSizeResp)response;
            presentable.viewDirSizeOnServer(dirSizeResp.getFile(), dirSizeResp.getSize());
        }
    }
}
