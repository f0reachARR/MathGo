package me.f0reach.mathgo.listener;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.track.template.TemplateAuthoringService;
import me.f0reach.mathgo.track.template.TemplateDraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class TemplateWandListener implements Listener {
    private final MathGoPlugin plugin;

    public TemplateWandListener(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        TemplateAuthoringService svc = plugin.templateAuthoringService();
        if (svc == null) return;
        if (!svc.isWand(event.getItem())) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        event.setCancelled(true);
        TemplateDraft draft = svc.draftOf(event.getPlayer());
        Location loc = clicked.getLocation();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            draft.setPos1(loc);
            event.getPlayer().sendMessage(Component.text(
                    "pos1: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")",
                    NamedTextColor.AQUA));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            draft.setPos2(loc);
            event.getPlayer().sendMessage(Component.text(
                    "pos2: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")",
                    NamedTextColor.AQUA));
        }
    }
}
