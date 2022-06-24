package server.network.handlers;

import interop.Command;
import interop.dto.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.exceptions.HandleException;
import server.network.service.UserService;

import java.lang.reflect.Method;
import java.util.Map;

public abstract class MainHandler extends SimpleChannelInboundHandler<Message> {

    private final Map<Command, Method> requestHandlers;

    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

    public MainHandler(Map<Command, Method> requestHandlers,
                       UserService userService) {
        this.requestHandlers = requestHandlers;
        this.userService = userService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        Command command = message.getCommand();
        Method method = requestHandlers.get(command);
        Object handler = getObject(method);
        Object response = method.invoke(handler, message, ctx.channel());
        if(response != null) {
            ctx.writeAndFlush(response);
        }
    }

    protected abstract Object getObject(Method method);


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(cause.getCause() instanceof HandleException) {
            sendErrorMessage(ctx, cause.getCause().getMessage());
        } else {
            ctx.close();
            userService.unsubscribeUser(ctx.channel());
        }
    }

    /**
     * Отправка клиенту сообщения с ошибкой
     * @param ctx контекст канала
     * @param exceptionMessage сообщение
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String exceptionMessage) {
        Message message = new Message();
        message.setCommand(Command.ERROR);
        message.setData(exceptionMessage);
        ctx.writeAndFlush(message);
    }

    /**
     * Происходит при активации канала
     * @param ctx контекст канала
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Клиент присоединился: {}", ctx.channel().remoteAddress());
    }

    /**
     * Происходит при дезактивации канала
     * @param ctx контекст канала
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Клиент отсоединился: {}", ctx.channel().remoteAddress());
        userService.unsubscribeUser(ctx.channel());
    }
}
