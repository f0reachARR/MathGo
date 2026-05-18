package me.f0reach.mathgo.game;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.area.Area;
import me.f0reach.mathgo.area.AreaGrid;
import me.f0reach.mathgo.config.MathGoConfig;
import me.f0reach.mathgo.quiz.GeneratedQuestionProvider;
import me.f0reach.mathgo.quiz.QuestionProvider;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.TemplateLibrary;
import me.f0reach.mathgo.track.Track;
import me.f0reach.mathgo.track.TrackBuilder;
import me.f0reach.mathgo.track.codegen.GoalTemplate;
import me.f0reach.mathgo.track.codegen.MoveStraightTemplate;
import me.f0reach.mathgo.track.codegen.QuestionStopTemplate;
import me.f0reach.mathgo.track.codegen.StartTemplate;
import me.f0reach.mathgo.ui.Messages;
import static me.f0reach.mathgo.ui.Messages.get;
import static me.f0reach.mathgo.ui.Messages.u;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GameManager {
    private final MathGoPlugin plugin;
    private final MathGoConfig config;
    private final AreaGrid areaGrid;
    private final TemplateLibrary library;
    private final QuestionProvider questionProvider;
    private final Map<UUID, GameSession> sessionsByPlayer = new HashMap<>();
    private final Map<UUID, GameLoop> loopsBySession = new HashMap<>();
    private final Map<UUID, GameRule> preferredRules = new HashMap<>();

    public GameManager(MathGoPlugin plugin, MathGoConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.areaGrid = new AreaGrid(config.areaSize());
        this.library = new TemplateLibrary();
        registerBuiltInTemplates();
        this.questionProvider = new GeneratedQuestionProvider(
                config.enabledTypes(),
                config.timeLimitFor(me.f0reach.mathgo.quiz.Difficulty.EASY),
                config.timeLimitFor(me.f0reach.mathgo.quiz.Difficulty.NORMAL),
                config.timeLimitFor(me.f0reach.mathgo.quiz.Difficulty.HARD));
    }

    public TemplateLibrary library() { return library; }

    public void resetLibraryToBuiltIns() {
        library.clear();
        registerBuiltInTemplates();
    }

    private void registerBuiltInTemplates() {
        library.register(new StartTemplate());
        library.register(new MoveStraightTemplate());
        library.register(new QuestionStopTemplate());
        library.register(new GoalTemplate());
    }

    @Nullable
    public GameSession sessionOf(Player player) {
        return sessionsByPlayer.get(player.getUniqueId());
    }

    public boolean join(Player player) {
        if (sessionsByPlayer.containsKey(player.getUniqueId())) {
            player.sendMessage(get("game.join.already"));
            return false;
        }
        World world = plugin.getServer().getWorld(config.worldName());
        if (world == null) {
            player.sendMessage(get("game.join.world_missing", u("world", config.worldName())));
            return false;
        }
        Area area = areaGrid.reserveNext();
        GameRule rule = preferredRules.getOrDefault(player.getUniqueId(), GameRule.STAGE_CLEAR);
        int lives = rule == GameRule.SURVIVAL ? config.survivalInitialLives() : config.initialLives();
        GameSession session = new GameSession(player, PlayMode.SOLO, rule, area, lives);
        sessionsByPlayer.put(player.getUniqueId(), session);
        player.sendMessage(get("game.join.success", u("rule", ruleLabel(rule))));
        return true;
    }

    public boolean setRule(Player player, GameRule rule) {
        if (sessionsByPlayer.containsKey(player.getUniqueId())) {
            player.sendMessage(get("game.rule.cannot_change_in_session"));
            return false;
        }
        preferredRules.put(player.getUniqueId(), rule);
        player.sendMessage(get("game.rule.set", u("rule", ruleLabel(rule))));
        return true;
    }

    public boolean start(Player player) {
        GameSession session = sessionOf(player);
        if (session == null) {
            player.sendMessage(get("game.start.not_joined"));
            return false;
        }
        if (session.state() != GameState.PREPARING) {
            player.sendMessage(get("game.start.already_running"));
            return false;
        }
        World world = plugin.getServer().getWorld(config.worldName());
        if (world == null) {
            player.sendMessage(get("game.start.world_missing"));
            return false;
        }
        // Build track within the reserved area.
        int baseY = (int) Math.round(config.lobbyY());
        Location areaOrigin = session.area().originAt(world, baseY).add(2, 0, 2);
        TrackBuilder builder = new TrackBuilder(library);
        // Survival builds only the START segment; the director extends the perimeter loop from there.
        // The legacy config.survivalQuestions value is no longer used.
        Track track = (session.rule() == GameRule.SURVIVAL)
                ? builder.buildSurvivalStartOnly(world, session.area(), areaOrigin, Direction.EAST,
                        config.weightedRandom())
                : builder.buildStageClear(world, session.area(), areaOrigin, Direction.EAST,
                        config.checkpoints(), config.weightedRandom());
        session.setTrack(track);

        if (session.rule() == GameRule.SURVIVAL) {
            // Resume the sliding-window generator from the end of the initial track.
            var segs = track.segments();
            var last = segs.get(segs.size() - 1);
            SurvivalDirector director = new SurvivalDirector(plugin, session.area(), library, track,
                    last.exitLocation(), last.exitDirection(), config.weightedRandom());
            director.primeInitialLookahead();
            session.setDirector(director);
        }

        Location board = track.startBoardLocation();
        board.setYaw(Direction.EAST.yaw());
        board.setPitch(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(board);

        Minecart cart = world.spawn(board, Minecart.class, m -> {
            m.setMaxSpeed(0);
            m.setSlowWhenEmpty(false);
        });
        cart.addPassenger(player);
        session.setMinecart(cart);
        session.setCountdownEndMillis(System.currentTimeMillis() + (long) config.countdownSeconds() * 1000L);
        session.setStartTimeMillis(System.currentTimeMillis());
        session.setState(GameState.COUNTDOWN);

        GameLoop loop = new GameLoop(plugin, this, session, config, questionProvider);
        loop.runTaskTimer(plugin, 1L, 1L);
        loopsBySession.put(session.sessionId(), loop);

        player.sendMessage(get("game.start.success"));
        return true;
    }

    public boolean leave(Player player) {
        GameSession session = sessionOf(player);
        if (session == null) {
            player.sendMessage(get("game.leave.not_joined"));
            return false;
        }
        stopInternal(session, true);
        player.sendMessage(get("game.leave.success"));
        return true;
    }

    private static String ruleLabel(GameRule rule) {
        return Messages.getRaw(
                rule == GameRule.SURVIVAL ? "game.rule_label.survival" : "game.rule_label.stage_clear",
                rule == GameRule.SURVIVAL ? "サバイバル" : "ステージクリア");
    }

    public boolean stop(Player player) {
        return leave(player);
    }

    public void endSession(GameSession session) {
        stopInternal(session, false);
    }

    private void stopInternal(GameSession session, boolean cancelLoop) {
        // Record score (async) before any state mutation. Skip empty no-op sessions.
        var repo = plugin.scoreRepository();
        if (repo != null && (session.score() > 0 || session.correctCount() > 0)) {
            Player p = session.player();
            repo.insertAsync(me.f0reach.mathgo.db.ScoreRecord.forInsert(
                    p.getUniqueId(),
                    p.getName(),
                    session.rule(),
                    session.score(),
                    session.correctCount(),
                    session.maxCombo(),
                    session.durationSeconds()));
        }
        if (cancelLoop) {
            GameLoop loop = loopsBySession.remove(session.sessionId());
            if (loop != null) {
                try { loop.cancel(); } catch (IllegalStateException ignored) {}
            }
        } else {
            loopsBySession.remove(session.sessionId());
        }
        Player player = session.player();
        Minecart cart = session.minecart();
        if (cart != null) {
            cart.eject();
            cart.remove();
            session.setMinecart(null);
        }
        Track track = session.track();
        if (track != null) {
            track.cleanup();
            session.setTrack(null);
        }
        // Teleport player to lobby (world spawn or configured lobby in the mathgo world).
        World world = plugin.getServer().getWorld(config.worldName());
        if (world != null && player.isOnline()) {
            Location lobby = new Location(world, config.lobbyX(), config.lobbyY(), config.lobbyZ());
            player.teleport(lobby);
        }
        areaGrid.release(session.area());
        sessionsByPlayer.remove(player.getUniqueId());
        session.setState(GameState.FINISHED);
    }

    public void shutdown() {
        for (GameSession session : new java.util.ArrayList<>(sessionsByPlayer.values())) {
            stopInternal(session, true);
        }
    }

    public void submitAnswer(Player player, long value) {
        GameSession session = sessionOf(player);
        if (session == null) return;
        GameLoop loop = loopsBySession.get(session.sessionId());
        if (loop == null) return;
        loop.submitAnswer(value);
    }
}
