package server.handlers;

import interop.model.fileinfo.*;
import interop.model.requests.sign.*;
import interop.model.responses.*;
import io.netty.channel.*;
import server.Server;
import server.model.User;
import server.util.DBConnection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;

/**
 * Класс обработчика ответственный за аутентификацию и регистрацию клиента на сервере
 */
public class SignHandler extends SimpleChannelInboundHandler<Sign> {

    private static final String SIGN_IN_QUERY = "SELECT COUNT(*) FROM users WHERE users.login = ? AND users.password = ?;";           // запрос на аутентификацию
    private static final String SIGN_UP_QUERY = "INSERT INTO users VALUES(?, ?);";                                                    // запрос на регистрацию
    private static final int codeExistUser = 1062;                    // код ошибки при регистрации с уже существующим в базе данных логином

    /**
     * Обработка аутентификации клиента по логину и паролю. Если клиент проходит эту процедуру ему в ответ
     * отправляется список файлов текущей директории (по умолчанию домашней - корневой)
     * @param ctx контекст канала
     * @param signData запрос на аутентификацию или регистрацию
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Sign signData) {
        String login = signData.getLogin();
        String password = signData.getPassword();
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
            PreparedStatement statement = signData instanceof SignIn ?
                    connection.prepareStatement(SIGN_IN_QUERY) :
                    connection.prepareStatement(SIGN_UP_QUERY);
            statement.setString(1, login);
            statement.setString(2, password);
            int count = 0;
            if(signData instanceof SignIn) {
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    count = result.getInt(1);
                }
            } else {
                count = statement.executeUpdate();
            }
            if (count == 1) {
                handleSuccessSigning(ctx, login);
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
    private void handleSuccessSigning(ChannelHandlerContext ctx, String login) throws IOException {
        User user = new User(login);
        Server.subscribeUser(ctx.channel(), user);
        if (!Files.exists(user.getHomeDir())) {
            Files.createDirectory(user.getHomeDir());
        }
        File[] files = user.getHomeDir().toFile().listFiles();
        SignResp response = new SignResp(user.getPrompt(), getFileInfos(files));
        ctx.writeAndFlush(response);
    }

    /**
     * Отправка клиенту сообщения с ошибкой
     * @param ctx контекст канала
     * @param message сообщение
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String message) {
        ErrorResponse response = new ErrorResponse(message);
        ctx.writeAndFlush(response);
    }

    /**
     * Получение списка файлов из домашней директории клиента и преобразование в тип FileInfo
     * для последующей передачи клиенту
     * @param files список преобразуемых файлов
     * @return список преобразованных файлов типа FileInfo
     */
    private List<FileInfo> getFileInfos(File[] files) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        if(files != null) {
            for(File file : files) {
                FileInfo fileInfo;
                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                if(file.isDirectory()) {
                    fileInfo = new Directory(file.getName(), file.lastModified(), createDate);
                } else {
                    fileInfo = new RegularFile(file.getName(), file.lastModified(), file.length(), createDate);
                }
                result.add(fileInfo);
            }
        }
        return result;
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
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Server.unsubscribeUser(ctx.channel());
    }
}
