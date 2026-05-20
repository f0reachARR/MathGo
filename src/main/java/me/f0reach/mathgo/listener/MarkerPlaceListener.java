package me.f0reach.mathgo.listener;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.template.MarkerBlocks;
import me.f0reach.mathgo.track.template.TemplateAuthoringService;
import me.f0reach.mathgo.track.template.TemplateDraft;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.BlockVector;

import static me.f0reach.mathgo.ui.Messages.get;
import static me.f0reach.mathgo.ui.Messages.n;
import static me.f0reach.mathgo.ui.Messages.u;

/**
 * Intercepts {@link BlockPlaceEvent}s for tagged marker items. The block is never actually placed in
 * the world; instead the click location + player facing is recorded as the corresponding anchor in
 * the player's {@link TemplateDraft}. The marker item stays in inventory so the author can place it
 * again (e.g., to correct a position).
 */
public final class MarkerPlaceListener implements Listener {
    private final MathGoPlugin plugin;

    public MarkerPlaceListener(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        TemplateAuthoringService svc = plugin.templateAuthoringService();
        if (svc == null) return;
        MarkerBlocks marker = svc.markerOf(event.getItemInHand());
        if (marker == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block placedBlock = event.getBlockPlaced();
        BlockVector pos = new BlockVector(
                placedBlock.getX(), placedBlock.getY(), placedBlock.getZ());
        Direction facing = yawToDirection(player.getLocation().getYaw());
        TemplateDraft draft = svc.draftOf(player);

        switch (marker) {
            case ENTRY -> {
                draft.setEntry(pos, facing);
                player.sendMessage(get("template.entry_exit_set",
                        u("label", "entry"),
                        n("x", pos.getBlockX()), n("y", pos.getBlockY()), n("z", pos.getBlockZ()),
                        u("facing", facing.name())));
            }
            case EXIT -> {
                draft.setExit(pos, facing);
                player.sendMessage(get("template.entry_exit_set",
                        u("label", "exit"),
                        n("x", pos.getBlockX()), n("y", pos.getBlockY()), n("z", pos.getBlockZ()),
                        u("facing", facing.name())));
            }
            case QUESTION_STOP -> {
                draft.setStop(pos);
                player.sendMessage(get("template.anchor_set",
                        u("label", "stop"),
                        n("x", pos.getBlockX()), n("y", pos.getBlockY()), n("z", pos.getBlockZ())));
            }
            case QUESTION_DISPLAY -> {
                draft.setDisplay(pos);
                player.sendMessage(get("template.anchor_set",
                        u("label", "display"),
                        n("x", pos.getBlockX()), n("y", pos.getBlockY()), n("z", pos.getBlockZ())));
            }
        }
    }

    private static Direction yawToDirection(float yaw) {
        // Minecraft yaw: 0 = south, 90 = west, 180 = north, -90 / 270 = east.
        float y = ((yaw % 360) + 360) % 360;
        if (y >= 45 && y < 135) return Direction.WEST;
        if (y >= 135 && y < 225) return Direction.NORTH;
        if (y >= 225 && y < 315) return Direction.EAST;
        return Direction.SOUTH;
    }
}
