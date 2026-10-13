import auth.AuthService;
import config.AppConfig;
import config.Env;
import database.DataSourceFactory;
import database.repository.SessionRepository;
import database.repository.UserRepository;
import logging.Debug;

import javax.sql.DataSource;
import java.io.IOException;

public class Main {
    private static AppConfig appConfig;
    public static AppConfig getAppConfig() { return appConfig; }

    private static DataSource dataSource;
    public static DataSource getDataSource() { return dataSource; }

    static void main(String[] args) {
        try {
            appConfig = new AppConfig(new Env());
        } catch (IOException ioEx) {
            Debug.log(ioEx.toString());
            return;
        }
        dataSource = DataSourceFactory.create(appConfig);

        UserRepository userRepo = new UserRepository(dataSource);
        SessionRepository sessionRepo = new SessionRepository(dataSource);
        AuthService authService = new AuthService(appConfig, userRepo, sessionRepo);

    }
}
