package server.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.exceptions.HandleException;

import javax.annotation.PreDestroy;
import java.sql.*;

/**
 * Класс ответственный за соединение с базой данных
 */
@Service
public class DBConnectionServiceImpl implements DBConnectionService {

    private Connection dbConnection;

    private static final Logger logger = LoggerFactory.getLogger(DBConnectionServiceImpl.class);

    @Autowired
    public DBConnectionServiceImpl(@Value("${db.url}") String url,
                                   @Value("${db.user}") String user,
                                   @Value("${db.password}") String password) {
        try {
            dbConnection = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            String message = "Не могу соединиться с базой данных.";
            logger.error(message + " {}", ex.getMessage());
            throw new HandleException(ex.getMessage());
        }
        logger.info("Соединение с базой данных установлено");
    }

    @Override
    public Connection getConnection() {
        return dbConnection;
    }

    /**
     * Закрытие соединения
     */
    @Override
    @PreDestroy
    public void close() {
        try {
            if(dbConnection != null) {
                dbConnection.close();
                logger.info("Соединение с базой данных закрыто");
            }
        } catch(SQLException ex) {
            logger.error("Ошибка при закрытии соединения с базой данных: {}", ex.getMessage());
        } finally {
            dbConnection = null;
        }
    }
}
