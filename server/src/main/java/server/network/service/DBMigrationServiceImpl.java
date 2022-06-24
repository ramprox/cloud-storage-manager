package server.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

@Service
public class DBMigrationServiceImpl implements DBMigrationService {

    private final DBConnectionService dbConnectionService;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    public DBMigrationServiceImpl(DBConnectionService dbConnectionService) {
        this.dbConnectionService = dbConnectionService;
    }

    @Override
    public void runScript(String sourcePath) {
        if(sourcePath != null) {
            try (InputStream stream = Application.class.getResourceAsStream(sourcePath)) {
                if (stream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String[] operations = reader.lines().collect(Collectors.joining("")).split(";");
                    Statement statement = dbConnectionService.getConnection().createStatement();
                    for (String operation : operations) {
                        statement.executeUpdate(operation);
                    }
                    logger.info("Скрипт {} выполнен", sourcePath);
                }
            } catch (IOException | SQLException e) {
                logger.error("Ошибка при выполнении скрипта {}", sourcePath);
            }
        }
    }
}
