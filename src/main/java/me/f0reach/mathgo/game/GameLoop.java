package me.f0reach.mathgo.game;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.config.MathGoConfig;
import me.f0reach.mathgo.effect.CartShake;
import me.f0reach.mathgo.effect.Effects;
import me.f0reach.mathgo.quiz.Difficulty;
import me.f0reach.mathgo.quiz.QuestionProvider;
import me.f0reach.mathgo.quiz.QuizQuestion;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.QuestionAnchors;
import me.f0reach.mathgo.track.Track;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;

public final class GameLoop extends BukkitRunnable {
    private static final double FORWARD_SPEED = 0.4;
    private static final double STOP_DETECTION_RADIUS_SQ = 0.6 * 0.6;

    private final MathGoPlugin plugin;
    private final GameManager manager;
    private final GameSession session;
    private final MathGoConfig config;
    private final QuestionProvider questionProvider;

    public GameLoop(MathGoPlugin plugin, GameManager manager, GameSession session, MathGoConfig config,
                    QuestionProvider questionProvider) {
        this.plugin = plugin;
        this.manager = manager;
        this.session = session;
        this.config = config;
        this.questionProvider = questionProvider;
    }

    @Override
    public void run() {
        try {
            tick();
        } catch (Throwable t) {
            plugin.getLogger().severe("GameLoop error: " + t);
            t.printStackTrace();
            terminate();
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        switch (session.state()) {
            case COUNTDOWN -> tickCountdown(now);
            case MOVING -> tickMoving();
            case ANSWERING -> tickAnswering(now);
            case RESULT -> tickResult(now);
            case GAMEOVER -> tickGameOver(now);
            case FINISHED -> terminate();
            default -> {
            }
        }
    }

    private void tickCountdown(long now) {
        long remaining = session.countdownEndMillis() - now;
        if (remaining <= 0) {
            session.player().showTitle(Title.title(
                    Component.text("スタート！", NamedTextColor.GREEN),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))));
            Track track = session.track();
            if (track != null) session.setLastMovingDirection(track.forward());
            session.setState(GameState.MOVING);
            return;
        }
        int seconds = (int) Math.ceil(remaining / 1000.0);
        session.player().showTitle(Title.title(
                Component.text(String.valueOf(seconds), NamedTextColor.YELLOW),
                Component.text("もうすぐスタート…", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
    }

    private void tickMoving() {
        Minecart cart = session.minecart();
        if (cart == null || cart.isDead()) {
            terminate();
            return;
        }
        Track track = session.track();
        if (track == null) {
            terminate();
            return;
        }
        // Update the known direction only from actual movement. A stationary cart's facing/yaw
        // is unreliable right after spawn (it can resolve to the opposite cardinal), so we never
        // trust facing as a direction source — that would reverse the launch direction.
        Direction dir = directionFromVelocity(cart);
        if (dir != null) session.setLastMovingDirection(dir);
        Direction effective = session.lastMovingDirection();
        if (effective == null) effective = track.forward();
        Vector v = effective.unitVector().multiply(FORWARD_SPEED);
        cart.setMaxSpeed(0.6);
        cart.setVelocity(v);

        // Detect arrival at next question stop.
        if (session.nextQuestionIndex() < track.questionSegments().size()) {
            PlacedSegment next = track.questionSegments().get(session.nextQuestionIndex());
            QuestionAnchors anchors = next.questionAnchors();
            if (anchors != null) {
                Location stop = anchors.stopLoc();
                Location loc = cart.getLocation();
                if (sameWorld(stop, loc) && loc.distanceSquared(stop) <= STOP_DETECTION_RADIUS_SQ) {
                    enterAnswering(next);
                    return;
                }
            }
            return;
        }
        // No more questions: check for goal (stage clear) or end of track (survival).
        Location goal = track.goalLocation();
        if (goal != null) {
            Location loc = cart.getLocation();
            if (sameWorld(goal, loc) && loc.distanceSquared(goal) <= 1.2 * 1.2) {
                finishClear();
            }
        } else {
            // Survival ran past the buffer: end as completion.
            finishSurvivalCompletion();
        }
    }

    private Direction directionFromVelocity(Minecart cart) {
        Vector v = cart.getVelocity();
        if (v.lengthSquared() <= 0.0025) return null;
        if (Math.abs(v.getX()) > Math.abs(v.getZ())) {
            return v.getX() > 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(v.getZ()) > 1e-4) {
            return v.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    private void enterAnswering(PlacedSegment segment) {
        Minecart cart = session.minecart();
        if (cart == null) return;
        cart.setMaxSpeed(0);
        cart.setVelocity(new Vector(0, 0, 0));
        // Remember the direction we were moving so we can resume cleanly.
        session.setLastMovingDirection(segment.entryDirection());

        Difficulty difficulty = effectiveDifficulty();
        QuizQuestion question = questionProvider.next(difficulty);
        session.setCurrentQuestion(question);
        session.setCurrentQuestionSegment(segment);
        SurvivalDirector director = session.director();
        if (director != null) {
            director.onQuestionEntered(segment);
        }
        long now = System.currentTimeMillis();
        long deadline = now + (long) question.timeLimitSeconds() * 1000L;
        session.setAnswerDeadlineMillis(deadline);

        Player player = session.player();
        player.showTitle(Title.title(
                Component.text(question.displayText(), NamedTextColor.AQUA),
                Component.text("チャットで答えを入力！", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(question.timeLimitSeconds() + 1L), Duration.ZERO)));
        BossBar bar = BossBar.bossBar(
                Component.text("のこり " + question.timeLimitSeconds() + " 秒", NamedTextColor.GOLD),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS);
        session.setBossBar(bar);
        player.showBossBar(bar);
        player.sendMessage(Component.text("もんだい: " + question.displayText(), NamedTextColor.AQUA));

        session.setState(GameState.ANSWERING);
    }

    private Difficulty effectiveDifficulty() {
        if (session.rule() != GameRule.SURVIVAL) {
            return config.difficulty();
        }
        int progress = session.nextQuestionIndex();
        if (progress < 5) return Difficulty.EASY;
        if (progress < 15) return Difficulty.NORMAL;
        return Difficulty.HARD;
    }

    private void tickAnswering(long now) {
        Minecart cart = session.minecart();
        if (cart != null) CartShake.apply(cart);
        QuizQuestion q = session.currentQuestion();
        BossBar bar = session.bossBar();
        if (q == null) return;
        long remainingMs = session.answerDeadlineMillis() - now;
        long totalMs = (long) q.timeLimitSeconds() * 1000L;
        if (bar != null) {
            float progress = Math.max(0f, Math.min(1f, remainingMs / (float) totalMs));
            bar.progress(progress);
            int secsLeft = (int) Math.max(0, Math.ceil(remainingMs / 1000.0));
            bar.name(Component.text("のこり " + secsLeft + " 秒",
                    secsLeft <= 1 ? NamedTextColor.RED : NamedTextColor.GOLD));
        }
        if (remainingMs <= 0) {
            timeout();
        }
    }

    public void submitAnswer(long value) {
        if (session.state() != GameState.ANSWERING) return;
        QuizQuestion q = session.currentQuestion();
        if (q == null) return;
        Player player = session.player();
        Location anchor = currentAnchor();
        if (q.answer() == value) {
            Effects.correct(player, anchor);
            session.incrementCombo();
            session.incrementCorrectCount();
            session.addScore(100 + session.combo() * 10);
            scheduleResume();
        } else {
            Effects.wrong(player, anchor);
            session.resetCombo();
            session.decrementLife();
            if (session.lives() <= 0) {
                enterGameOver();
            } else {
                scheduleResume();
            }
        }
    }

    private void timeout() {
        Player player = session.player();
        Location anchor = currentAnchor();
        Effects.timeout(player, anchor);
        session.resetCombo();
        session.decrementLife();
        if (session.lives() <= 0) {
            enterGameOver();
        } else {
            scheduleResume();
        }
    }

    private void scheduleResume() {
        clearBossBar();
        long until = System.currentTimeMillis() + (long) (config.answerResultEffectSeconds() * 1000L);
        session.setResultUntilMillis(until);
        session.setState(GameState.RESULT);
    }

    private void tickResult(long now) {
        if (now < session.resultUntilMillis()) {
            Minecart cart = session.minecart();
            if (cart != null) CartShake.apply(cart);
            return;
        }
        session.advanceQuestionIndex();
        session.setCurrentQuestion(null);
        session.setCurrentQuestionSegment(null);
        Minecart cart = session.minecart();
        if (cart != null) {
            cart.setMaxSpeed(0.6);
            // Kick the cart in the saved direction so it leaves the stop.
            Direction d = session.lastMovingDirection();
            if (d != null) {
                cart.setVelocity(d.unitVector().multiply(FORWARD_SPEED));
            }
        }
        session.setState(GameState.MOVING);
    }

    private void enterGameOver() {
        clearBossBar();
        Player player = session.player();
        Location anchor = currentAnchor();
        Effects.gameOver(plugin, player, anchor);
        long until = System.currentTimeMillis() + 5000L;
        session.setResultUntilMillis(until);
        session.setState(GameState.GAMEOVER);
    }

    private void tickGameOver(long now) {
        if (now >= session.resultUntilMillis()) {
            session.setState(GameState.FINISHED);
        }
    }

    private void finishClear() {
        Minecart cart = session.minecart();
        if (cart != null) {
            cart.setMaxSpeed(0);
            cart.setVelocity(new Vector(0, 0, 0));
        }
        Track track = session.track();
        Location anchor = (track != null && track.goalLocation() != null)
                ? track.goalLocation() : session.player().getLocation();
        Effects.goalReached(session.player(), anchor);
        session.player().showTitle(Title.title(
                Component.text("クリア！", NamedTextColor.GOLD),
                Component.text("せいかい " + session.correctCount() + " / スコア " + session.score(),
                        NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))));
        long until = System.currentTimeMillis() + 3000L;
        session.setResultUntilMillis(until);
        session.setState(GameState.GAMEOVER);
    }

    private void finishSurvivalCompletion() {
        Minecart cart = session.minecart();
        if (cart != null) {
            cart.setMaxSpeed(0);
            cart.setVelocity(new Vector(0, 0, 0));
        }
        Effects.goalReached(session.player(), session.player().getLocation());
        session.player().showTitle(Title.title(
                Component.text("完走！", NamedTextColor.GOLD),
                Component.text("せいかい " + session.correctCount() + " / スコア " + session.score(),
                        NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))));
        long until = System.currentTimeMillis() + 3000L;
        session.setResultUntilMillis(until);
        session.setState(GameState.GAMEOVER);
    }

    private Location currentAnchor() {
        QuestionAnchors anchors = session.currentQuestionSegment() != null ? session.currentQuestionSegment().questionAnchors() : null;
        if (anchors != null) return anchors.displayLoc();
        Minecart cart = session.minecart();
        if (cart != null) return cart.getLocation();
        return session.player().getLocation();
    }

    private void clearBossBar() {
        BossBar bar = session.bossBar();
        if (bar != null) {
            session.player().hideBossBar(bar);
            session.setBossBar(null);
        }
    }

    private void terminate() {
        clearBossBar();
        cancel();
        manager.endSession(session);
    }

    private static boolean sameWorld(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }
}
