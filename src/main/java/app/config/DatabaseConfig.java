package app.config;

import app.database.DataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
public class DatabaseConfig {
    @Bean
    public AppConfig appConfig() throws IOException {
        return new AppConfig(new Env());
    }

    @Bean(destroyMethod = "close")
    public DataSource dataSource(AppConfig appConfig) {
        return DataSourceFactory.create(appConfig);
    }
}
