package client.network.handlers;

import client.network.ResponseHandler;
import interop.dto.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class MainHandler extends SimpleChannelInboundHandler<Message> {

    private final ResponseHandler responseHandler;

    public MainHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) {
        responseHandler.handleResponse(message);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        responseHandler.channelActivated();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        responseHandler.channelInactivated();
    }
}
