package me.f0reach.mathgo.game;

import me.f0reach.mathgo.area.Area;
import me.f0reach.mathgo.quiz.QuizQuestion;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.Track;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class GameSession {
    private final UUID sessionId = UUID.randomUUID();
    private final Player player;
    private final PlayMode mode;
    private final GameRule rule;
    private final Area area;

    private GameState state = GameState.PREPARING;
    private int lives;
    private int score;
    private int combo;
    private int maxCombo;
    private int correctCount;
    private long startTimeMillis;
    @Nullable private Track track;
    @Nullable private Minecart minecart;
    @Nullable private QuizQuestion currentQuestion;
    @Nullable private PlacedSegment currentQuestionSegment;
    private int nextQuestionIndex;
    private long answerDeadlineMillis;
    private long resultUntilMillis;
    private long countdownEndMillis;
    @Nullable private BossBar bossBar;
    @Nullable private Direction lastMovingDirection;
    @Nullable private SurvivalDirector director;

    public GameSession(Player player, PlayMode mode, GameRule rule, Area area, int initialLives) {
        this.player = player;
        this.mode = mode;
        this.rule = rule;
        this.area = area;
        this.lives = initialLives;
    }

    public UUID sessionId() { return sessionId; }
    public Player player() { return player; }
    public PlayMode mode() { return mode; }
    public GameRule rule() { return rule; }
    public Area area() { return area; }

    public GameState state() { return state; }
    public void setState(GameState state) { this.state = state; }

    public int lives() { return lives; }
    public void decrementLife() { this.lives--; }
    public int score() { return score; }
    public void addScore(int delta) { this.score += delta; }
    public int combo() { return combo; }
    public void incrementCombo() {
        this.combo++;
        if (this.combo > this.maxCombo) this.maxCombo = this.combo;
    }
    public void resetCombo() { this.combo = 0; }
    public int maxCombo() { return maxCombo; }

    public long startTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long t) { this.startTimeMillis = t; }
    public int durationSeconds() {
        if (startTimeMillis == 0L) return 0;
        return (int) Math.max(0, (System.currentTimeMillis() - startTimeMillis) / 1000L);
    }
    public int correctCount() { return correctCount; }
    public void incrementCorrectCount() { this.correctCount++; }

    @Nullable public Track track() { return track; }
    public void setTrack(Track track) { this.track = track; }

    @Nullable public Minecart minecart() { return minecart; }
    public void setMinecart(@Nullable Minecart minecart) { this.minecart = minecart; }

    @Nullable public QuizQuestion currentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(@Nullable QuizQuestion q) { this.currentQuestion = q; }

    @Nullable public PlacedSegment currentQuestionSegment() { return currentQuestionSegment; }
    public void setCurrentQuestionSegment(@Nullable PlacedSegment seg) { this.currentQuestionSegment = seg; }

    public int nextQuestionIndex() { return nextQuestionIndex; }
    public void advanceQuestionIndex() { this.nextQuestionIndex++; }

    public long answerDeadlineMillis() { return answerDeadlineMillis; }
    public void setAnswerDeadlineMillis(long t) { this.answerDeadlineMillis = t; }

    public long resultUntilMillis() { return resultUntilMillis; }
    public void setResultUntilMillis(long t) { this.resultUntilMillis = t; }

    public long countdownEndMillis() { return countdownEndMillis; }
    public void setCountdownEndMillis(long t) { this.countdownEndMillis = t; }

    @Nullable public BossBar bossBar() { return bossBar; }
    public void setBossBar(@Nullable BossBar bar) { this.bossBar = bar; }

    @Nullable public Direction lastMovingDirection() { return lastMovingDirection; }
    public void setLastMovingDirection(@Nullable Direction d) { this.lastMovingDirection = d; }

    @Nullable public SurvivalDirector director() { return director; }
    public void setDirector(@Nullable SurvivalDirector director) { this.director = director; }
}
