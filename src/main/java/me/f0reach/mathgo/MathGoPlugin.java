package me.f0reach.mathgo;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.f0reach.mathgo.command.MathGoCommand;
import me.f0reach.mathgo.config.MathGoConfig;
import me.f0reach.mathgo.db.Database;
import me.f0reach.mathgo.db.ScoreRepository;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.integration.WorldEditBridge;
import me.f0reach.mathgo.listener.ChatListener;
import me.f0reach.mathgo.listener.MarkerPlaceListener;
import me.f0reach.mathgo.listener.TemplateWandListener;
import me.f0reach.mathgo.listener.VehicleListener;
import me.f0reach.mathgo.placeholder.MathGoPlaceholders;
import me.f0reach.mathgo.track.template.NbtTemplateLoader;
import me.f0reach.mathgo.track.template.TemplateAuthoringService;
import me.f0reach.mathgo.ui.Messages;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class MathGoPlugin extends JavaPlugin {
    private MathGoConfig mathGoConfig;
    private GameManager gameManager;
    private TemplateAuthoringService templateAuthoringService;
    @Nullable private Database database;
    @Nullable private ScoreRepository scoreRepository;
    @Nullable private MathGoPlaceholders placeholders;
    private boolean worldEditAvailable;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages.load(this);
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);
        this.templateAuthoringService = new TemplateAuthoringService(this);

        openDatabaseIfEnabled();
        registerPlaceholdersIfAvailable();
        initializeWorldEditIfAvailable();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new VehicleListener(this), this);
        getServer().getPluginManager().registerEvents(new TemplateWandListener(this), this);
        getServer().getPluginManager().registerEvents(new MarkerPlaceListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        MathGoCommand.build(this),
                        "MathGo クイズアトラクション",
                        java.util.List.of("mg")));

        World world = getServer().getWorld(mathGoConfig.worldName());
        if (world == null) {
            getLogger().warning("World '" + mathGoConfig.worldName()
                    + "' is not loaded. MathGo will not be playable until this world exists.");
        } else {
            loadNbtTemplates(world);
        }
        getLogger().info("MathGo enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        if (database != null) {
            database.close();
            database = null;
            scoreRepository = null;
        }
    }

    public MathGoConfig mathGoConfig() {
        return mathGoConfig;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public TemplateAuthoringService templateAuthoringService() {
        return templateAuthoringService;
    }

    @Nullable
    public ScoreRepository scoreRepository() {
        return scoreRepository;
    }

    public boolean worldEditAvailable() {
        return worldEditAvailable;
    }

    public void reloadMathGo() {
        reloadConfig();
        Messages.load(this);
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        if (database != null) {
            database.close();
            database = null;
            scoreRepository = null;
        }
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);
        openDatabaseIfEnabled();
        registerPlaceholdersIfAvailable();
        initializeWorldEditIfAvailable();
        World world = getServer().getWorld(mathGoConfig.worldName());
        if (world != null) loadNbtTemplates(world);
    }

    /** Rebuilds the template library: clears, re-registers codegen templates, then loads NBT files. */
    public void reloadTemplates() {
        if (gameManager == null) return;
        gameManager.resetLibraryToBuiltIns();
        World world = getServer().getWorld(mathGoConfig.worldName());
        if (world != null) loadNbtTemplates(world);
    }

    private void loadNbtTemplates(World world) {
        File templatesRoot = new File(getDataFolder(), "templates");
        NbtTemplateLoader loader = new NbtTemplateLoader(templatesRoot, world,
                mathGoConfig.scratchX(), mathGoConfig.scratchY(), mathGoConfig.scratchZ(),
                getLogger());
        loader.loadAll(gameManager.library());
    }

    private void openDatabaseIfEnabled() {
        if (!mathGoConfig.databaseEnabled()) {
            getLogger().info("MathGo: database is disabled (no scoreboard persistence).");
            return;
        }
        try {
            this.database = Database.open(mathGoConfig, getLogger());
            this.scoreRepository = new ScoreRepository(this, database);
        } catch (Exception e) {
            getLogger().severe("MathGo: failed to open database — scores will not be saved: " + e.getMessage());
            this.database = null;
            this.scoreRepository = null;
        }
    }

    private void initializeWorldEditIfAvailable() {
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            this.worldEditAvailable = false;
            return;
        }
        try {
            WorldEditBridge.initialize(this);
            this.worldEditAvailable = true;
        } catch (Throwable t) {
            getLogger().warning("MathGo: WorldEdit present but bridge init failed: " + t.getMessage());
            this.worldEditAvailable = false;
        }
    }

    private void registerPlaceholdersIfAvailable() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            this.placeholders = new MathGoPlaceholders(this);
            placeholders.register();
            getLogger().info("MathGo: registered PlaceholderAPI expansion (%mathgo_...%).");
        } catch (Throwable t) {
            getLogger().warning("MathGo: failed to register PlaceholderAPI expansion: " + t.getMessage());
            this.placeholders = null;
        }
    }
}
