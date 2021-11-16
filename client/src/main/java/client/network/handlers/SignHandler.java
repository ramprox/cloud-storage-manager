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
 * Класс обработчика, ответственный за обработку успешной аутентификации или авторизации клиента
 */
public class SignHandler extends SimpleChannelInboundHandler<Message> {

    /**
     * Происходит при успешной авторизации или регистрации клиента на сервере
     * @param ctx контекст канала
     * @param response ответ от сервера, содержащий список файлов из домашней директории
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message response) {
        Command command = response.getCommand();
        if(command == Command.SIGN_IN || command == Command.SIGN_UP) {
            ctx.pipeline().addLast(new CommandHandler());
            ctx.pipeline().addLast(new UploadHandler());
            ctx.pipeline().addLast(new DownloadHandler());
            ctx.pipeline().remove(this);
            Presentable presentable = Client.getPresentable();
            Object[] data = (Object[]) response.getData();
            String currentDir = (String) data[0];
            List<FileInfo> files = (List<FileInfo>) data[1];
            presentable.clientSigned(currentDir, files);
            return;
        }
        ctx.fireChannelRead(response);
    }

    /**
     * Происходит при активации канала
     * @param ctx контекст канала
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Presentable presentable = Client.getPresentable();
        presentable.channelActivated();
    }

    /**
     * Происходит при возникновении ошибок в канале
     * @param ctx контекст канала
     * @param cause исключение
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Presentable presentable = Client.getPresentable();
        presentable.exceptionCaught(cause);
    }
}
