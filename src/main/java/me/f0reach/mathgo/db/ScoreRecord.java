package me.f0reach.mathgo.db;

import me.f0reach.mathgo.game.GameRule;

import java.time.Instant;
import java.util.UUID;

public record ScoreRecord(
        long id,
        UUID playerUuid,
        String playerName,
        GameRule rule,
        int score,
        int correctCount,
        int maxCombo,
        int durationSeconds,
        Instant createdAt
) {
    public static ScoreRecord forInsert(UUID playerUuid, String playerName, GameRule rule,
                                         int score, int correctCount, int maxCombo, int durationSeconds) {
        return new ScoreRecord(-1, playerUuid, playerName, rule, score, correctCount, maxCombo,
                durationSeconds, Instant.now());
    }
}
