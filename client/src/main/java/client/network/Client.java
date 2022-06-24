package client.network;

import client.events.ConnectionState;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Главный класс, ответственный за взаимодействие с сервером
 */
public abstract class Client implements ResponseHandler {

    private final Map<Command, Method> requestHandlers;

    private final Map<Command, Method> responseHandlers;

    private final ServerEventsListener serverEventsListener;

    private final ApplicationEventPublisher publisher;

    private Thread thread;

    public Client(Map<Command, Method> requestHandlers,
                  Map<Command, Method> responseHandlers,
                  ServerEventsListener serverEventsListener,
                  ApplicationEventPublisher publisher) {
        this.requestHandlers = requestHandlers;
        this.responseHandlers = responseHandlers;
        this.serverEventsListener = serverEventsListener;
        this.publisher = publisher;
    }

    protected abstract Bootstrap getBootstrap();

    protected abstract Object getObject(Method method);

    /**
     * Соединение с сервером
     */
    public void connect() {
        thread = new Thread(() -> {
            try {
                start();
            } catch (InterruptedException | IOException e) {
                System.out.println("Соединение закрыто");
            }
        });
        thread.start();
    }

    /**
     * Запуск соединения с сервером
     *
     * @throws InterruptedException ошибки прерывания
     */
    private void start() throws InterruptedException, IOException {
        Bootstrap bootstrap = getBootstrap();
        try {
            ChannelFuture channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bootstrap.config().group().shutdownGracefully().sync();
        }
    }

    @Override
    public void channelActivated() {
        serverEventsListener.channelActivated();
        publisher.publishEvent(new ConnectionState(this, true));
    }

    @Override
    public void channelInactivated() {
        publisher.publishEvent(new ConnectionState(this, false));
    }

    public void sendCommand(Command command, Object... args) {
        try {
            Method method = requestHandlers.get(command);
            Object object = getObject(method);
            method.invoke(object, args);
        } catch (Exception ex) {
            serverEventsListener.exceptionCaught(ex);
        }
    }

    @Override
    public void handleResponse(Message message) {
        try {
            Method method = responseHandlers.get(message.getCommand());
            Object object = getObject(method);
            method.invoke(object, message);
        } catch (Exception e) {
            serverEventsListener.exceptionCaught(e);
        }
    }

    public void disconnect() {
        thread.interrupt();
    }
}
