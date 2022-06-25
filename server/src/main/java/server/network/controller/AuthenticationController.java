package server.network.controller;

import interop.Command;
import interop.dto.AuthDto;
import interop.dto.DirFilesDto;
import interop.dto.Message;
import interop.dto.fileinfo.FileInfo;
import interop.service.FileInfoService;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import server.annotations.RequestHandler;
import server.exceptions.HandleException;
import server.model.User;
import server.network.service.DBConnectionServiceImpl;
import server.network.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthenticationController {

    private static final String SIGN_IN_QUERY = "SELECT login FROM users WHERE users.login = ? AND users.password = ?;";           // запрос на аутентификацию
    private static final String SIGN_UP_QUERY = "INSERT INTO users VALUES(?, ?);";                                                    // запрос на регистрацию
    private static final int codeExistUser = 1062;  // код ошибки при регистрации с уже существующим в базе данных логином

    private final FileInfoService fileInfoService;

    private final DBConnectionServiceImpl connection;

    private final UserService userService;

    @Autowired
    public AuthenticationController(FileInfoService fileInfoService,
                                    DBConnectionServiceImpl connection,
                                    UserService userService) {
        this.fileInfoService = fileInfoService;
        this.connection = connection;
        this.userService = userService;
    }

    @RequestHandler(command = Command.SIGN_IN)
    public Message handleSignIn(Message message, Channel channel) {
        AuthDto authDto = (AuthDto) message.getData();
        String login = authDto.getLogin();
        String password = authDto.getPassword();
        if (userService.getUserByLogin(login) != null) {
            throw new HandleException("Пользователь с логином " + login + " уже в хранилище.");
        }
        try {
            PreparedStatement statement = getPreparedStatement(Command.SIGN_IN, login, password);
            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                throw new HandleException("Логин или пароль некорректные");
            }
            Message response = handleSuccessSign(login, channel);
            response.setCommand(message.getCommand());
            return response;
        } catch(SQLException | IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    @RequestHandler(command = Command.SIGN_UP)
    public Message handleSignUp(Message message, Channel channel) {
        AuthDto authDto = (AuthDto) message.getData();
        String login = authDto.getLogin();
        String password = authDto.getPassword();
        if(login.isEmpty() || password.isEmpty()) {
            throw new HandleException("Логин и пароль не должны быть пустыми.");
        }
        try {
            PreparedStatement statement = getPreparedStatement(Command.SIGN_UP, login, password);
            int rowCount = statement.executeUpdate();
            if(rowCount == 0) {
                throw new HandleException("Логин или пароль некорректные");
            }
            Message response = handleSuccessSign(login, channel);
            response.setCommand(message.getCommand());
            return response;
        } catch (SQLException ex) {
            int errorCode = ex.getErrorCode();
            if(errorCode == codeExistUser) {
                throw new HandleException("Пользователь с логином " + login + " уже существует");
            }
            throw new HandleException(ex.getMessage());
        } catch (IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    private PreparedStatement getPreparedStatement(Command command, String login, String password) throws SQLException {
        PreparedStatement statement;
        Connection connection = this.connection.getConnection();
        if(command == Command.SIGN_IN) {
            statement = connection.prepareStatement(SIGN_IN_QUERY);
        } else {
            statement = connection.prepareStatement(SIGN_UP_QUERY);
        }
        statement.setString(1, login);
        statement.setString(2, password);
        return statement;
    }

    private Message handleSuccessSign(String login, Channel channel) throws IOException {
        User user = new User(login);
        userService.subscribeUser(channel, user);
        Path homeDir = userService.getHomeDir(channel);
        if (!Files.exists(homeDir)) {
            Files.createDirectory(homeDir);
        }
        List<FileInfo> fileInfos = fileInfoService.getFileInfos(homeDir)
                .stream()
                .peek(fileInfo -> {
                    String fileName = Paths.get(fileInfo.getFileName()).getFileName().toString();
                    fileInfo.setFileName(fileName);
                }).collect(Collectors.toList());
        Message message = new Message();
        String path = userService.convertLocalPathToClient(homeDir, homeDir);
        DirFilesDto authResponseData = new DirFilesDto(path, fileInfos);
        message.setData(authResponseData);
        return message;
    }
}
