package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.AppConfig;

import javax.sql.DataSource;

public class DataSourceFactory {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    public static DataSource create(AppConfig appConfig) {
        config.setJdbcUrl(appConfig.databaseUrl);
        config.setUsername(appConfig.databaseUser);
        config.setPassword(appConfig.databasePassword);
        config.addDataSourceProperty("cachePrepStmts" , appConfig.cachePrepStmts.toString());
        config.addDataSourceProperty("prepStmtCacheSize" , appConfig.prepStmtCacheSize.toString());
        config.addDataSourceProperty("prepStmtCacheSqlLimit" , appConfig.prepStmtCacheSqlLimit.toString());
        config.addDataSourceProperty("maximumPoolSize" , appConfig.maximumPoolSize.toString());

        ds = new HikariDataSource(config);
        return ds;
    }

}
