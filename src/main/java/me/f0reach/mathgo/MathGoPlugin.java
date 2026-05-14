package me.f0reach.mathgo;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.f0reach.mathgo.command.MathGoCommand;
import me.f0reach.mathgo.config.MathGoConfig;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.listener.ChatListener;
import me.f0reach.mathgo.listener.VehicleListener;
import org.bukkit.plugin.java.JavaPlugin;

public class MathGoPlugin extends JavaPlugin {
    private MathGoConfig mathGoConfig;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new VehicleListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        MathGoCommand.build(this),
                        "MathGo クイズアトラクション",
                        java.util.List.of("mg")));

        if (getServer().getWorld(mathGoConfig.worldName()) == null) {
            getLogger().warning("World '" + mathGoConfig.worldName()
                    + "' is not loaded. MathGo will not be playable until this world exists.");
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

    public void reloadMathGo() {
        reloadConfig();
        if (gameManager != null) {
            gameManager.shutdown();
        }
        this.mathGoConfig = MathGoConfig.load(getConfig());
        this.gameManager = new GameManager(this, mathGoConfig);
    }
}
