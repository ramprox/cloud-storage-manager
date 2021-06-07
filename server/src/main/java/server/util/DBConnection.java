package server.util;

import java.sql.*;

/**
 * Класс ответственный за соединение с базой данных
 */
public class DBConnection {
    private static Connection dbConnection;

    /**
     * Получение соединения с базой данных
     * @return соединение с БД
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        if(dbConnection == null) {
            dbConnection = DriverManager.getConnection(ApplicationUtil.DB,
                    ApplicationUtil.USER, ApplicationUtil.PASSWORD);
            System.out.println("Database connection established");
        }
        return dbConnection;
    }

    /**
     * Закрытие соединения
     */
    public static void closeConnection() {
        try {
            if(dbConnection != null) {
                dbConnection.close();
                System.out.println("Database connection closed");
            }
        } catch(SQLException ex) {
            System.out.println("Error closing database connection: " + ex.getMessage());
        } finally {
            dbConnection = null;
        }
    }
}
