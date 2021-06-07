package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.fileinfo.FileInfo;
import interop.model.responses.SignResp;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;

/**
 * Класс обработчика, ответственный за обработку успешной аутентификации или авторизации клиента
 */
public class SignHandler extends SimpleChannelInboundHandler<SignResp> {

    /**
     * Происходит при успешной авторизации или регистрации клиента на сервере
     * @param ctx контекст канала
     * @param signResp ответ от сервера, содержащий список файлов из домашней директории
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SignResp signResp) {
        Presentable presentable = Client.getPresentable();
        String currentDir = signResp.getCurrentPath();
        List<FileInfo> files = signResp.getFiles();
        presentable.clientSigned(currentDir, files);
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
