package client.network.handlers;

import client.interfaces.Callback;
import client.interfaces.Callback0;
import client.network.Client;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AuthInboundHandler extends ChannelInboundHandlerAdapter {

    private Callback0 authOKCallback;
    private Callback<String> errorCallback;
    private Callback0 channelActive;

    public void setChannelActive(Callback0 channelActive) {
        this.channelActive = channelActive;
    }

    public void setAuthOKCallback(Callback0 authOKCallback) {
        this.authOKCallback = authOKCallback;
    }

    public void setErrorCallback(Callback<String> errorCallback) {
        this.errorCallback = errorCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(channelActive != null) {
            channelActive.call();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handleAuthMessage(ctx, String.valueOf(msg));
    }

    private void handleAuthMessage(ChannelHandlerContext ctx, String message) {
        if(message.equals("signInOK") || message.equals("signUpOK")) {
            if(authOKCallback != null) {
                authOKCallback.call();
            }
        } else {
            if(errorCallback != null) {
                errorCallback.call(message);
            }
        }
    }
}
