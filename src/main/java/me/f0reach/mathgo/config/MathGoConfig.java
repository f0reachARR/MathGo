package me.f0reach.mathgo.config;

import me.f0reach.mathgo.quiz.Difficulty;
import me.f0reach.mathgo.quiz.QuestionType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class MathGoConfig {
    private final String worldName;
    private final int areaSize;
    private final double lobbyX;
    private final double lobbyY;
    private final double lobbyZ;

    private final int countdownSeconds;
    private final double answerResultEffectSeconds;

    private final int initialLives;
    private final int checkpoints;
    private final int survivalInitialLives;
    private final int survivalQuestions;

    private final Set<QuestionType> enabledTypes;
    private final int timeLimitEasy;
    private final int timeLimitNormal;
    private final int timeLimitHard;
    private final Difficulty difficulty;
    private final boolean allowNegative;
    private final boolean allowDecimal;

    private final String templateSet;
    private final boolean weightedRandom;

    private final int scratchX;
    private final int scratchY;
    private final int scratchZ;

    private final boolean databaseEnabled;
    private final String dbHost;
    private final int dbPort;
    private final String dbDatabase;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbTable;
    private final int dbPoolSize;

    private MathGoConfig(FileConfiguration c) {
        this.worldName = c.getString("world.name", "mathgo_world");
        this.areaSize = c.getInt("world.area_size", 256);
        this.lobbyX = c.getDouble("world.lobby_x", 0.5);
        this.lobbyY = c.getDouble("world.lobby_y", 64.0);
        this.lobbyZ = c.getDouble("world.lobby_z", 0.5);

        this.countdownSeconds = c.getInt("game.countdown_seconds", 5);
        this.answerResultEffectSeconds = c.getDouble("game.answer_result_effect_seconds", 1.5);

        this.initialLives = c.getInt("rules.stage_clear.initial_lives", 3);
        this.checkpoints = c.getInt("rules.stage_clear.checkpoints", 5);
        this.survivalInitialLives = c.getInt("rules.survival.initial_lives", 3);
        this.survivalQuestions = c.getInt("rules.survival.max_questions", 30);

        Set<QuestionType> types = EnumSet.noneOf(QuestionType.class);
        List<String> typeStrings = c.getStringList("quiz.enabled_types");
        if (typeStrings.isEmpty()) {
            types.addAll(EnumSet.allOf(QuestionType.class));
        } else {
            for (String s : typeStrings) {
                try {
                    types.add(QuestionType.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        this.enabledTypes = types;

        this.timeLimitEasy = c.getInt("quiz.default_time_limit.easy", 6);
        this.timeLimitNormal = c.getInt("quiz.default_time_limit.normal", 5);
        this.timeLimitHard = c.getInt("quiz.default_time_limit.hard", 4);

        Difficulty diff = Difficulty.NORMAL;
        try {
            diff = Difficulty.valueOf(c.getString("quiz.difficulty", "NORMAL").trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        this.difficulty = diff;

        this.allowNegative = c.getBoolean("quiz.allow_negative", false);
        this.allowDecimal = c.getBoolean("quiz.allow_decimal", false);

        this.templateSet = c.getString("track.template_set", "default");
        this.weightedRandom = c.getBoolean("track.weighted_random", true);

        this.scratchX = c.getInt("templates.scratch_x", -100000);
        this.scratchY = c.getInt("templates.scratch_y", 64);
        this.scratchZ = c.getInt("templates.scratch_z", -100000);

        this.databaseEnabled = c.getBoolean("database.enabled", false);
        this.dbHost = c.getString("database.host", "localhost");
        this.dbPort = c.getInt("database.port", 3306);
        this.dbDatabase = c.getString("database.database", "mathgo");
        this.dbUsername = c.getString("database.username", "mathgo");
        this.dbPassword = c.getString("database.password", "");
        this.dbTable = c.getString("database.table", "mathgo_scores");
        this.dbPoolSize = c.getInt("database.pool_size", 4);
    }

    public static MathGoConfig load(FileConfiguration configuration) {
        return new MathGoConfig(configuration);
    }

    public String worldName() { return worldName; }
    public int areaSize() { return areaSize; }
    public double lobbyX() { return lobbyX; }
    public double lobbyY() { return lobbyY; }
    public double lobbyZ() { return lobbyZ; }
    public int countdownSeconds() { return countdownSeconds; }
    public double answerResultEffectSeconds() { return answerResultEffectSeconds; }
    public int initialLives() { return initialLives; }
    public int checkpoints() { return checkpoints; }
    public int survivalInitialLives() { return survivalInitialLives; }
    public int survivalQuestions() { return survivalQuestions; }
    public Set<QuestionType> enabledTypes() { return enabledTypes; }
    public int timeLimitFor(Difficulty d) {
        return switch (d) {
            case EASY -> timeLimitEasy;
            case NORMAL -> timeLimitNormal;
            case HARD -> timeLimitHard;
        };
    }
    public Difficulty difficulty() { return difficulty; }
    public boolean allowNegative() { return allowNegative; }
    public boolean allowDecimal() { return allowDecimal; }
    public String templateSet() { return templateSet; }
    public boolean weightedRandom() { return weightedRandom; }
    public int scratchX() { return scratchX; }
    public int scratchY() { return scratchY; }
    public int scratchZ() { return scratchZ; }

    public boolean databaseEnabled() { return databaseEnabled; }
    public String dbHost() { return dbHost; }
    public int dbPort() { return dbPort; }
    public String dbDatabase() { return dbDatabase; }
    public String dbUsername() { return dbUsername; }
    public String dbPassword() { return dbPassword; }
    public String dbTable() { return dbTable; }
    public int dbPoolSize() { return dbPoolSize; }
}
