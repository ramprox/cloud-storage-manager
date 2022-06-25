package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import server.config.Config;
import server.network.Server;
import server.network.service.DBMigrationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void run() {
        printBanner();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        context.registerShutdownHook();
        DBMigrationService migrationService = context.getBean(DBMigrationService.class);
        migrationService.runScript(context.getEnvironment().getProperty("initScript"));
        Server server = context.getBean(Server.class);
        server.start();
    }

    private static void printBanner() {
        try(InputStream stream = Application.class.getResourceAsStream("/banner.txt")) {
            if(stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                reader.lines().forEach(System.out::println);
            }
        } catch (IOException ex) {
            logger.info("Баннер отсутствует");
        }
    }
}
