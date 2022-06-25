package client.config;

import client.network.Client;
import client.network.annotations.RequestHandler;
import client.network.annotations.ResponseHandler;
import client.network.handlers.MainHandler;
import client.network.service.*;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.handler.UploadHandler;
import interop.service.FileInfoService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;

@Configuration
public class NetworkConfig {

    @Autowired
    ApplicationContext context;

    @Value("${host}")
    private String host;

    @Value("${port}")
    private int port;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Bean
    public AuthenticationService authenticationService(ServerEventsListener serverEventsListener) {
        return new AuthenticationServiceImpl(serverEventsListener) {
            @Override
            protected Channel getChannel() {
                return ((SimpChannelInitializer)channelInitializer()).getChannel();
            }
        };
    }

    @Bean
    public SimpleFileCommandService simpleFileCommandService(ServerEventsListener serverEventsListener) {
        return new SimpleFileCommandServiceImpl(serverEventsListener) {
            @Override
            protected Channel getChannel() {
                return ((SimpChannelInitializer)channelInitializer()).getChannel();
            }
        };
    }

    @Bean
    public UploadService uploadService(FileInfoService fileInfoService,
                                       ServerEventsListener serverEventsListener,
                                       @Value("${loadBufferSize}") int loadBufferSize) {
        return new UploadServiceImpl(fileInfoService, serverEventsListener, loadBufferSize) {
            @Override
            protected Channel getChannel() {
                return ((SimpChannelInitializer)channelInitializer()).getChannel();
            }
        };
    }

    @Bean
    public DownloadService downloadService(ServerEventsListener serverEventsListener,
                                           ApplicationEventPublisher publisher) {
        return new DownloadServiceImpl(serverEventsListener, publisher) {
            @Override
            protected Channel getChannel() {
                return ((SimpChannelInitializer)channelInitializer()).getChannel();
            }
        };
    }

    @Bean
    public Map<Command, Method> requestHandlers() {
        Map<Command, Method> requestHandlers = new HashMap<>();
        Reflections reflections = new Reflections("client", new MethodAnnotationsScanner());
        Set<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(RequestHandler.class);
        methodsAnnotatedWith.forEach(method -> {
            RequestHandler annotation = method.getAnnotation(RequestHandler.class);
            requestHandlers.put(annotation.command(), method);
        });
        return requestHandlers;
    }

    @Bean
    public Map<Command, Method> responseHandlers() {
        Map<Command, Method> responseHandlers = new HashMap<>();
        Reflections reflections = new Reflections("client", new MethodAnnotationsScanner());
        Set<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(ResponseHandler.class);
        methodsAnnotatedWith.forEach(method -> {
            ResponseHandler annotation = method.getAnnotation(ResponseHandler.class);
            responseHandlers.put(annotation.command(), method);
        });
        return responseHandlers;
    }

    @Bean
    public Client client(ServerEventsListener serverEventsListener) {
        Map<Command, Method> requestHandlers = requestHandlers();
        Map<Command, Method> responseHandlers = responseHandlers();
        return new Client(requestHandlers, responseHandlers, serverEventsListener, publisher) {

            @Override
            protected Bootstrap getBootstrap() {
                return bootstrap();
            }

            @Override
            protected Object getObject(Method method) {
                Class<?> interfaceType = method.getDeclaringClass().getInterfaces()[0];
                return context.getBean(interfaceType);
            }
        };
    }

    @Bean
    @Scope("prototype")
    public Bootstrap bootstrap() {
        EventLoopGroup group = eventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port))
                .handler(channelInitializer());
        return bootstrap;
    }

    @Bean
    public ChannelInitializer<SocketChannel> channelInitializer() {
        return new SimpChannelInitializer() {
            @Override
            protected List<ChannelHandler> getChannelHandlers() {
                return channelHandlers();
            }
        };
    }

    public EventLoopGroup eventLoopGroup() {
        return new NioEventLoopGroup();
    }

    private List<ChannelHandler> channelHandlers() {
        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ObjectEncoder());
        handlers.add(new ObjectDecoder(ClassResolvers.cacheDisabled(ClassLoader.getSystemClassLoader())));
        handlers.add(new MainHandler(context.getBean(Client.class)));
        handlers.add(new UploadHandler());
        return handlers;
    }
}
