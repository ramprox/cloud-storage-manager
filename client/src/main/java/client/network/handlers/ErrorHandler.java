package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.responses.ErrorResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Класс обработчика, ответственный за обработку сообщений об ошибках от сервера
 */
public class ErrorHandler extends SimpleChannelInboundHandler<ErrorResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ErrorResponse errorResponse) {
        Presentable presentable = Client.getPresentable();
        presentable.errorReceived(errorResponse.getErrorMessage());
    }
}
