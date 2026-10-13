package database.repository;

import javax.sql.DataSource;

public class SessionRepository {
    private final DataSource dataSource;

    public SessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
