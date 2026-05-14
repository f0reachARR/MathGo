package me.f0reach.mathgo.listener;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.game.GameSession;
import me.f0reach.mathgo.game.GameState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

public final class VehicleListener implements Listener {
    private final MathGoPlugin plugin;

    public VehicleListener(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Minecart cart)) return;
        Entity exited = event.getExited();
        if (!(exited instanceof Player player)) return;
        GameManager manager = plugin.gameManager();
        if (manager == null) return;
        GameSession session = manager.sessionOf(player);
        if (session == null) return;
        if (session.state() == GameState.FINISHED) return;
        if (session.minecart() != cart) return;
        event.setCancelled(true);
        // In case cancellation still drops the rider, re-add next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!cart.isDead() && !cart.getPassengers().contains(player)) {
                cart.addPassenger(player);
            }
        });
    }
}
