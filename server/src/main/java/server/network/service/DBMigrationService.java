package server.network.service;

public interface DBMigrationService {

    void runScript(String sourcePath);

}
