package me.f0reach.mathgo.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.f0reach.mathgo.config.MathGoConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Wraps a HikariCP pool against the configured MySQL server. Holds the table name in use so the
 * repository can build statements with the configured name.
 */
public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final String tableName;

    private Database(HikariDataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
    }

    public static Database open(MathGoConfig config, Logger logger) {
        HikariConfig hc = new HikariConfig();
        String jdbcUrl = "jdbc:mysql://" + config.dbHost() + ":" + config.dbPort()
                + "/" + config.dbDatabase() + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=UTC&autoReconnect=true";
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(config.dbUsername());
        hc.setPassword(config.dbPassword());
        hc.setMaximumPoolSize(Math.max(1, config.dbPoolSize()));
        hc.setPoolName("MathGo-Hikari");
        hc.setConnectionTimeout(5_000L);
        hc.setLeakDetectionThreshold(10_000L);
        HikariDataSource ds = new HikariDataSource(hc);
        Database db = new Database(ds, config.dbTable());
        db.runMigrations(logger);
        return db;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String tableName() { return tableName; }

    private void runMigrations(Logger logger) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "  id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "  player_uuid CHAR(36) NOT NULL,"
                + "  player_name VARCHAR(32) NOT NULL,"
                + "  rule VARCHAR(16) NOT NULL,"
                + "  score INT NOT NULL,"
                + "  correct_count INT NOT NULL,"
                + "  max_combo INT NOT NULL,"
                + "  duration_seconds INT NOT NULL,"
                + "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "  INDEX idx_rule_score (rule, score DESC),"
                + "  INDEX idx_player (player_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection c = getConnection();
             var stmt = c.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("MathGo: database schema ready (" + tableName + ").");
        } catch (SQLException e) {
            logger.warning("MathGo: failed to ensure schema: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
