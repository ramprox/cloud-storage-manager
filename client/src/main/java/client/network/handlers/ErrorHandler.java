package client.network.handlers;

import client.ui.interfaces.Presentable;
import client.network.Client;
import interop.Command;
import interop.model.Message;
import io.netty.channel.*;

/**
 * Класс обработчика, ответственный за обработку сообщений об ошибках от сервера
 */
public class ErrorHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message response) {
        Command command = response.getCommand();
        if(command == Command.ERROR) {
            Presentable presentable = Client.getPresentable();
            presentable.errorReceived((String) response.getData());
        } else {
            ctx.fireChannelRead(response);
        }
    }
}
