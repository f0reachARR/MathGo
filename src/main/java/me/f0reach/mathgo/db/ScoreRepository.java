package me.f0reach.mathgo.db;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.game.GameRule;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Async repository over {@link Database}. All public methods are non-blocking: writes use
 * {@link CompletableFuture#runAsync} on Bukkit's async scheduler, reads return a future that
 * resolves on a worker thread (callers should hop back to the main thread for any Bukkit API
 * usage via {@link #onMain(MathGoPlugin, Object, Consumer)}).
 */
public final class ScoreRepository {
    private final MathGoPlugin plugin;
    private final Database db;

    public ScoreRepository(MathGoPlugin plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    /** Insert asynchronously. Errors are logged. */
    public void insertAsync(ScoreRecord record) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO " + db.tableName()
                         + " (player_uuid, player_name, rule, score, correct_count, max_combo,"
                         + " duration_seconds, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, record.playerUuid().toString());
                ps.setString(2, record.playerName());
                ps.setString(3, record.rule().name());
                ps.setInt(4, record.score());
                ps.setInt(5, record.correctCount());
                ps.setInt(6, record.maxCombo());
                ps.setInt(7, record.durationSeconds());
                ps.setTimestamp(8, Timestamp.from(record.createdAt()));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("MathGo: failed to insert score: " + e.getMessage());
            }
        });
    }

    /** Fetch the top-N scores for a given rule. Returns the future on an async worker. */
    public CompletableFuture<List<ScoreRecord>> topAsync(GameRule rule, int limit) {
        CompletableFuture<List<ScoreRecord>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id, player_uuid, player_name,"
                         + " rule, score, correct_count, max_combo, duration_seconds, created_at"
                         + " FROM " + db.tableName() + " WHERE rule = ? ORDER BY score DESC, created_at ASC LIMIT ?")) {
                ps.setString(1, rule.name());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ScoreRecord> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapRow(rs));
                    }
                    future.complete(out);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("MathGo: failed to query top scores: " + e.getMessage());
                future.complete(List.of());
            }
        });
        return future;
    }

    /** Fetch a single player's personal best for a rule (highest score, then earliest timestamp). */
    public CompletableFuture<ScoreRecord> personalBestAsync(UUID playerUuid, GameRule rule) {
        CompletableFuture<ScoreRecord> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id, player_uuid, player_name,"
                         + " rule, score, correct_count, max_combo, duration_seconds, created_at"
                         + " FROM " + db.tableName() + " WHERE player_uuid = ? AND rule = ?"
                         + " ORDER BY score DESC, created_at ASC LIMIT 1")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, rule.name());
                try (ResultSet rs = ps.executeQuery()) {
                    future.complete(rs.next() ? mapRow(rs) : null);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("MathGo: failed to query personal best: " + e.getMessage());
                future.complete(null);
            }
        });
        return future;
    }

    private static ScoreRecord mapRow(ResultSet rs) throws SQLException {
        return new ScoreRecord(
                rs.getLong("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                GameRule.valueOf(rs.getString("rule")),
                rs.getInt("score"),
                rs.getInt("correct_count"),
                rs.getInt("max_combo"),
                rs.getInt("duration_seconds"),
                Optional(rs.getTimestamp("created_at")));
    }

    private static Instant Optional(Timestamp ts) {
        return ts != null ? ts.toInstant() : Instant.EPOCH;
    }

    /** Hop a future's result back onto the main thread. */
    public static <T> void onMain(MathGoPlugin plugin, CompletableFuture<T> future, Consumer<T> consumer) {
        future.thenAccept(value -> Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(value)));
    }
}
