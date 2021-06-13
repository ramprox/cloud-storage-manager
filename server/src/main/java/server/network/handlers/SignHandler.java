package server.network.handlers;

import interop.Command;
import interop.model.Message;
import io.netty.channel.*;
import server.network.Server;
import server.model.User;
import server.util.ApplicationUtil;
import server.util.DBConnection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс обработчика ответственный за аутентификацию и регистрацию клиента на сервере
 */
public class SignHandler extends SimpleChannelInboundHandler<Message> {

    private static final String SIGN_IN_QUERY = "SELECT login FROM users WHERE users.login = ? AND users.password = ?;";           // запрос на аутентификацию
    private static final String SIGN_UP_QUERY = "INSERT INTO users VALUES(?, ?);";                                                    // запрос на регистрацию
    private static final int codeExistUser = 1062;                    // код ошибки при регистрации с уже существующим в базе данных логином

    /**
     * Обработка аутентификации клиента по логину и паролю. Если клиент проходит эту процедуру ему в ответ
     * отправляется список файлов текущей директории (по умолчанию домашней - корневой)
     * @param ctx контекст канала
     * @param request запрос на аутентификацию или регистрацию
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message request) {
        Command command = request.getCommand();
        if(command != Command.SIGN_IN && command != Command.SIGN_UP) {
            return;
        }
        String[] data = (String[]) request.getData();
        String login = data[0];
        String password = data[1];
        if(login.isEmpty() || password.isEmpty()) {
            sendErrorMessage(ctx,"Логин и пароль не должны быть пустыми.");
            return;
        }
        if (Server.getUserByLogin(login) != null) {
            sendErrorMessage(ctx,"Пользователь с логином " + login + " уже в хранилище.");
            return;
        }
        try {
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement = command == Command.SIGN_IN ?
                    connection.prepareStatement(SIGN_IN_QUERY) :
                    connection.prepareStatement(SIGN_UP_QUERY);
            statement.setString(1, login);
            statement.setString(2, password);
            int count = 0;
            if(command == Command.SIGN_IN) {
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    count = 1;
                }
            } else {
                count = statement.executeUpdate();
            }
            if (count == 1) {
                handleSuccessSigning(ctx, command, login);
            } else {
                sendErrorMessage(ctx, "Логин или пароль некорректные");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch(SQLException ex) {
            int errorCode = ex.getErrorCode();
            if(errorCode == codeExistUser) {
                sendErrorMessage(ctx, "Пользователь с логином " + login + " уже существует");
            }
        }
    }

    /**
     * Выполняется при успешном прохождении аутентификации или регистрации
     * @param ctx контекст канала
     * @param login логин пользователя
     * @throws IOException может возникнуть при создании директории для пользователя или получении списка
     *                     файлов из текущей директории
     */
    private void handleSuccessSigning(ChannelHandlerContext ctx, Command command, String login) throws IOException {
        User user = new User(login);
        Server.subscribeUser(ctx.channel(), user);
        if (!Files.exists(user.getHomeDir())) {
            Files.createDirectory(user.getHomeDir());
        }
        List<Path> paths = Arrays.stream(user.getHomeDir().toFile().listFiles())
                .map(file -> user.getHomeDir().resolve(file.getName()))
                .collect(Collectors.toList());
        Object[] data = new Object[2];
        data[0] = user.getPrompt();
        data[1] = ApplicationUtil.getFileInfos(paths);
        ctx.pipeline().addLast(new CommandHandler());
        ctx.pipeline().addLast(new UploadHandler());
        ctx.pipeline().addLast(new DownloadHandler());
        ctx.pipeline().remove(this);
        Message response = new Message(command, data);
        ctx.writeAndFlush(response);
    }

    /**
     * Отправка клиенту сообщения с ошибкой
     * @param ctx контекст канала
     * @param message сообщение
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String message) {
        Message response = new Message(Command.ERROR, message);
        ctx.writeAndFlush(response);
    }

    /**
     * Происходит при активации канала
     * @param ctx контекст канала
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    /**
     * Происходит при возникновении исключения во время процедуры аутентификации
     * @param ctx контекст канала
     * @param cause исключение
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        Server.unsubscribeUser(ctx.channel());
    }

    /**
     * Происходит при дезактивации канала
     * @param ctx контекст канала
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
    }
}
