package client.network.handlers;

import client.ui.interfaces.Presentable;
import client.network.Client;
import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.FileInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

/**
 * Класс обработки ответов от сервера с результатом выполненных команд
 */
public class CommandHandler extends SimpleChannelInboundHandler<Message> {
    /**
     * Обработка входящих сообщений
     * @param ctx контекст канала
     * @param response ответ от сервера. Если сообщение содежит ответ от команд:
     *                 Command.CHANGE_DIR, Command.GET_DIR_SIZE, Command.CREATE_FILE,
     *                 Command.CREATE_DIR, Command.DELETE, Command.RENAME, Command.SEARCH
     *                 то происходит обработка, если нет - передача следующему Handler
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message response) {
        Command command = response.getCommand();
        if(command == Command.CHANGE_DIR) {
            Presentable presentable = Client.getPresentable();
            Object[] data = (Object[]) response.getData();
            String newPath = (String) data[0];
            List<FileInfo> fileInfoList = (List<FileInfo>) data[1];
            presentable.currentDirChanged(newPath, fileInfoList);
        } else if(command == Command.GET_DIR_SIZE) {
            Object[] data = (Object[]) response.getData();
            FileInfo fileInfo = (FileInfo) data[0];
            long dirSize = (long)data[1];
            Presentable presentable = Client.getPresentable();
            presentable.viewDirSizeOnServer(fileInfo, dirSize);
        } else if(command == Command.CREATE_FILE || command == Command.CREATE_DIR) {
            FileInfo fileInfo = (FileInfo) response.getData();
            Presentable presentable = Client.getPresentable();
            presentable.fileCreated(fileInfo);
        } else if(command == Command.DELETE) {
            Presentable presentable = Client.getPresentable();
            presentable.fileDeleted((String) response.getData());
        } else if(command == Command.RENAME) {
            Object[] data = (Object[]) response.getData();
            String oldFilePath = (String) data[0];
            String newFilePath = (String) data[1];
            Presentable presentable = Client.getPresentable();
            presentable.fileRenamed(oldFilePath, newFilePath);
        } else if(command == Command.SEARCH) {
            Presentable presentable = Client.getPresentable();
            presentable.foundedFilesReceived((List<FileInfo>) response.getData());
        } else {
            ctx.fireChannelRead(response);
        }
    }

    /**
     * Происходит при возникновении ошибок в канале
     * @param ctx контекст канала
     * @param cause исключение
     * @throws Exception происходит при возникновении ошибок в канале
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Presentable presentable = Client.getPresentable();
        presentable.exceptionCaught(cause);
    }
}
