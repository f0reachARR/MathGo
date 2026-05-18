package me.f0reach.mathgo.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.db.ScoreRecord;
import me.f0reach.mathgo.db.ScoreRepository;
import me.f0reach.mathgo.game.GameRule;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes MathGo scoreboard data through PlaceholderAPI. Results are cached for
 * {@link #CACHE_TTL_MILLIS} to avoid DB queries on every placeholder evaluation.
 *
 * Supported placeholders:
 * <ul>
 *   <li>{@code %mathgo_top_<rule>_<rank>_<field>%} — top score data, rank 1..10,
 *       rule = {@code stage}/{@code stage_clear}/{@code survival},
 *       field = {@code name}/{@code score}/{@code correct}/{@code combo}/{@code duration}.</li>
 *   <li>{@code %mathgo_best_<rule>_<field>%} — the requesting player's personal-best record
 *       (same field set as above).</li>
 * </ul>
 */
public final class MathGoPlaceholders extends PlaceholderExpansion {
    private static final long CACHE_TTL_MILLIS = 30_000L;
    private static final int TOP_CACHE_LIMIT = 10;

    private final MathGoPlugin plugin;
    private final Map<GameRule, CacheEntry<List<ScoreRecord>>> topCache = new HashMap<>();
    private final Map<String, CacheEntry<ScoreRecord>> personalCache = new HashMap<>();

    public MathGoPlaceholders(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "mathgo"; }
    @Override public @NotNull String getAuthor() { return "f0reachARR"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.toLowerCase().split("_");
        if (parts.length < 1) return "";
        return switch (parts[0]) {
            case "top" -> handleTop(parts);
            case "best" -> handleBest(parts, player);
            default -> null;
        };
    }

    /** {@code top_<rule>_<rank>_<field>} */
    private @Nullable String handleTop(String[] parts) {
        if (parts.length < 4) return "";
        GameRule rule = parseRule(parts[1]);
        if (rule == null) return "";
        int rank;
        try { rank = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return ""; }
        if (rank < 1 || rank > TOP_CACHE_LIMIT) return "";
        String field = parts[3];
        List<ScoreRecord> top = getTopCached(rule);
        if (top == null || top.size() < rank) return "-";
        return formatField(top.get(rank - 1), field);
    }

    /** {@code best_<rule>_<field>} */
    private @Nullable String handleBest(String[] parts, OfflinePlayer player) {
        if (player == null) return "";
        if (parts.length < 3) return "";
        GameRule rule = parseRule(parts[1]);
        if (rule == null) return "";
        String field = parts[2];
        ScoreRecord best = getPersonalCached(player, rule);
        if (best == null) return "-";
        return formatField(best, field);
    }

    private static @Nullable GameRule parseRule(String s) {
        return switch (s) {
            case "stage", "stage_clear" -> GameRule.STAGE_CLEAR;
            case "survival" -> GameRule.SURVIVAL;
            default -> null;
        };
    }

    private static String formatField(ScoreRecord r, String field) {
        return switch (field) {
            case "name" -> r.playerName();
            case "score" -> String.valueOf(r.score());
            case "correct" -> String.valueOf(r.correctCount());
            case "combo" -> String.valueOf(r.maxCombo());
            case "duration" -> String.valueOf(r.durationSeconds());
            default -> "";
        };
    }

    private @Nullable List<ScoreRecord> getTopCached(GameRule rule) {
        long now = System.currentTimeMillis();
        CacheEntry<List<ScoreRecord>> entry = topCache.get(rule);
        if (entry != null && now - entry.fetchedAt < CACHE_TTL_MILLIS) {
            return entry.value;
        }
        ScoreRepository repo = plugin.scoreRepository();
        if (repo == null) return null;
        // Stale entry: serve old data while we refresh asynchronously.
        repo.topAsync(rule, TOP_CACHE_LIMIT).thenAccept(list ->
                topCache.put(rule, new CacheEntry<>(list, System.currentTimeMillis())));
        return entry != null ? entry.value : List.of();
    }

    private @Nullable ScoreRecord getPersonalCached(OfflinePlayer player, GameRule rule) {
        String key = player.getUniqueId() + ":" + rule.name();
        long now = System.currentTimeMillis();
        CacheEntry<ScoreRecord> entry = personalCache.get(key);
        if (entry != null && now - entry.fetchedAt < CACHE_TTL_MILLIS) {
            return entry.value;
        }
        ScoreRepository repo = plugin.scoreRepository();
        if (repo == null) return null;
        repo.personalBestAsync(player.getUniqueId(), rule).thenAccept(rec ->
                personalCache.put(key, new CacheEntry<>(rec, System.currentTimeMillis())));
        return entry != null ? entry.value : null;
    }

    private record CacheEntry<T>(T value, long fetchedAt) {}
}
