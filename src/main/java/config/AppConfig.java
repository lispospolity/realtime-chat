package config;

public class AppConfig {
    public Integer minNameLength = 3;
    public Integer maxNameLength = 100;
    public Integer minPasswordLength = 8;
    public Integer maxPasswordLength = 20;
    public Long tokenExpiryInMillis = 86_400_000L;

    public String databaseUrl;
    public String databaseUser;
    public String databasePassword;
    public Boolean cachePrepStmts = true;
    public Integer prepStmtCacheSize = 250;
    public Integer prepStmtCacheSqlLimit = 2048;
    public Integer maximumPoolSize = 5;

    public Integer webSocketPort = 8080;


    public AppConfig(Env env) {
        minNameLength = Integer.parseInt(
                env.getOrDefault("MIN_NAME_LENGTH", minNameLength.toString())
        );
        maxNameLength = Integer.parseInt(
                env.getOrDefault("MAX_NAME_LENGTH", maxNameLength.toString())
        );
        minPasswordLength = Integer.parseInt(
                env.getOrDefault("MIN_PASSWORD_LENGTH", minPasswordLength.toString())
        );
        maxPasswordLength = Integer.parseInt(
                env.getOrDefault("MAX_PASSWORD_LENGTH", maxPasswordLength.toString())
        );
        tokenExpiryInMillis = Long.parseLong(
                env.getOrDefault("CLIENT_TOKENS_EXPIRY_IN_MILLIS", tokenExpiryInMillis.toString())
        );

        databaseUrl = env.getRequired("JDBC_URL");
        databaseUser = env.getRequired("DB_USER");
        databasePassword = env.getRequired("DB_PASSWORD");
        cachePrepStmts = parseBoolean(env.getOrDefault("CACHE_PREPARED_STATEMENTS", cachePrepStmts.toString()));
        prepStmtCacheSize = Integer.parseInt(
                env.getOrDefault("PREPARED_STATEMENTS_CACHE_SIZE", prepStmtCacheSize.toString())
        );
        prepStmtCacheSqlLimit = Integer.parseInt(
                env.getOrDefault("PREPARED_STATEMENTS_CACHE_LIMIT", prepStmtCacheSqlLimit.toString())
        );
        maximumPoolSize = Integer.parseInt(
                env.getOrDefault("MAX_POOL_SIZE", maximumPoolSize.toString())
        );

        webSocketPort = Integer.parseInt(
                env.getOrDefault("WEBSOCKET_PORT", webSocketPort.toString())
        );
    }

    private boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        throw new IllegalArgumentException(
                "Expected true or false, got: " + value
        );
    }
}
