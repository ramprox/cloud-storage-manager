package server.network.service;

import java.sql.Connection;

public interface DBConnectionService extends AutoCloseable {

    Connection getConnection();

}
