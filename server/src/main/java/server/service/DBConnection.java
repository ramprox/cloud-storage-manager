package server.service;

import java.sql.*;

public class DBConnection {
    private static final String DB = "jdbc:mysql://localhost:3306/cloud_storage";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static Connection dbConnection;
    
    public static Connection getConnection() throws SQLException {
        if(dbConnection == null) {
            dbConnection = DriverManager.getConnection(DB, USER, PASSWORD);
            System.out.println("Database connection established");
        }
        return dbConnection;
    }
    
    public static void closeConnection() {
        try {
            if(dbConnection != null) {
                dbConnection.close();
            }
            System.out.println("Database connection closed");
        } catch(SQLException ex) {
            System.out.println("Error closing database connection: " + ex.getMessage());
        } finally {
            dbConnection = null;
        }
    }
}
