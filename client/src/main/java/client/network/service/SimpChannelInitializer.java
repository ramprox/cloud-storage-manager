package client.network.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.util.List;

public abstract class SimpChannelInitializer extends ChannelInitializer<SocketChannel> {

    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        channel = socketChannel;
        getChannelHandlers().forEach(channelHandler -> channel.pipeline().addLast(channelHandler));
    }

    protected abstract List<ChannelHandler> getChannelHandlers();
}
