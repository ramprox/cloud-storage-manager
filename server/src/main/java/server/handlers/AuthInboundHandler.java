package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Server;
import server.model.User;
import server.service.DBConnection;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthInboundHandler extends ChannelInboundHandlerAdapter {

    private static final String SIGN_IN = "signIn ";                              // аутентификация клиента auth [login] [password]
    private static final String SIGN_UP = "signUp ";                              // регистрация клиента signIn [login] [password]
    private static final String SIGN_IN_QUERY = "SELECT SIGN_IN(?, ?)";           // запрос на аутентификацию
    private static final String SIGN_UP_QUERY = "SELECT SIGN_UP(?, ?)";           // запрос на регистрацию

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
        Server.unsubscribeUser(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
        Server.unsubscribeUser(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handleSignInSignUp(ctx, String.valueOf(msg));
    }

    private void handleSignInSignUp(ChannelHandlerContext ctx, String command) throws Exception {
        String[] formatCommand = command.split(" ");
        if(formatCommand.length == 3) {
            String login = formatCommand[1];
            String password = formatCommand[2];
            if(Server.getUserByLogin(login) != null) {
                ctx.channel().writeAndFlush("User with login " + login + " already in storage.");
                return;
            }
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement = command.startsWith(SIGN_IN) ?
                    connection.prepareStatement(SIGN_IN_QUERY) :
                    connection.prepareStatement(SIGN_UP_QUERY);
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet result = statement.executeQuery();
            String resultQuery = "";
            while(result.next()) {
                resultQuery = result.getString(1);
            }
            if(resultQuery.startsWith("OK")) {
                ctx.channel().pipeline().remove(this);
                ctx.channel().pipeline().addLast("commandInboundHandler", new CommandInboundHandler());
                User user = new User(login);
                Server.subscribeUser(ctx.channel(), new User(login));
                if(!Files.exists(user.getHomeDir())) {
                    Files.createDirectory(user.getHomeDir());
                }
                String message = command.startsWith(SIGN_IN) ? "signInOK" : "signUpOK";
                ctx.channel().writeAndFlush(message);
            } else {
                ctx.channel().writeAndFlush(resultQuery);
            }
        }
    }
}
