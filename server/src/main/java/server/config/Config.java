package server.config;

import interop.Command;
import interop.handler.UploadHandler;
import interop.service.FileInfoService;
import interop.service.FileInfoServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import server.annotations.RequestHandler;
import server.network.handlers.MainHandler;
import server.network.service.UserService;

import java.lang.reflect.Method;
import java.util.*;

@Configuration
@ComponentScan("server")
@PropertySource("classpath:application.properties")
public class Config {

    @Autowired
    ApplicationContext context;

    @Bean
    public FileInfoService fileInfoService() {
        return new FileInfoServiceImpl();
    }

    @Bean
    public ServerBootstrap serverBootstrap(@Value("${server.port}") int port,
                                           @Value("${eventLoop.auth.count}") int authCount,
                                           @Value("${eventLoop.worker.count}") int workerCount,
                                           UserService userService) {
        EventLoopGroup auth = new NioEventLoopGroup(authCount);
        EventLoopGroup worker = new NioEventLoopGroup(workerCount);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(auth, worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(port)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(new ObjectEncoder());
                        ObjectDecoder decoder = new ObjectDecoder(ClassResolvers.cacheDisabled(ClassLoader.getSystemClassLoader()));
                        channel.pipeline().addLast(decoder);
                        channel.pipeline().addLast(mainHandler(userService));
                        channel.pipeline().addLast(new UploadHandler());
                    }
                });
        return bootstrap;
    }

    @Bean
    @Scope("prototype")
    public MainHandler mainHandler(UserService userService) {
        return new MainHandler(getRequestHandlers(), userService) {
            @Override
            protected Object getObject(Method method) {
                Class<?> type = method.getDeclaringClass();
                return context.getBean(type);
            }
        };
    }

    @Bean
    public Map<Command, Method> getRequestHandlers() {
        Map<Command, Method> requestHandlers = new HashMap<>();
        Reflections reflections = new Reflections("server", new MethodAnnotationsScanner());
        Set<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(RequestHandler.class);
        methodsAnnotatedWith.forEach(method -> {
            RequestHandler annotation = method.getAnnotation(RequestHandler.class);
            requestHandlers.put(annotation.command(), method);
        });
        return requestHandlers;
    }

}
