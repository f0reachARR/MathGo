package me.f0reach.mathgo;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.f0reach.mathgo.command.MathGoCommand;
import me.f0reach.mathgo.config.MathGoConfig;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.listener.ChatListener;
import me.f0reach.mathgo.listener.TemplateWandListener;
import me.f0reach.mathgo.listener.VehicleListener;
import me.f0reach.mathgo.track.template.NbtTemplateLoader;
import me.f0reach.mathgo.track.template.TemplateAuthoringService;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MathGoPlugin extends JavaPlugin {
    private MathGoConfig mathGoConfig;
    private GameManager gameManager;
    private TemplateAuthoringService templateAuthoringService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);
        this.templateAuthoringService = new TemplateAuthoringService(this);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new VehicleListener(this), this);
        getServer().getPluginManager().registerEvents(new TemplateWandListener(this), this);

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

    public void reloadMathGo() {
        reloadConfig();
        if (gameManager != null) {
            gameManager.shutdown();
        }
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);
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
}
