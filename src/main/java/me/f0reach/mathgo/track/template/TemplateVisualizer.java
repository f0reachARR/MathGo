package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.track.Direction;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

/**
 * Periodically renders particle outlines for each online player's in-progress {@link TemplateDraft}:
 * the selection cuboid plus the four marker anchors (entry/exit/stop/display). Each anchor is
 * coloured to match its marker block; entry/exit also get a short flame line indicating facing.
 *
 * <p>Particles are spawned per-player so only the author sees them.
 */
public final class TemplateVisualizer {
    private static final long PERIOD_TICKS = 10L;
    private static final double STEP = 0.5;
    private static final float DUST_SIZE = 1.0f;

    private static final Color SELECTION_COLOR = Color.fromRGB(255, 200, 0);
    private static final Color ENTRY_COLOR = Color.fromRGB(255, 64, 220);
    private static final Color EXIT_COLOR = Color.fromRGB(80, 230, 80);
    private static final Color STOP_COLOR = Color.fromRGB(255, 235, 60);
    private static final Color DISPLAY_COLOR = Color.fromRGB(120, 200, 255);

    private final MathGoPlugin plugin;
    @Nullable private BukkitTask task;

    public TemplateVisualizer(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        TemplateAuthoringService svc = plugin.templateAuthoringService();
        if (svc == null) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            TemplateDraft draft = svc.peekDraft(p);
            if (draft == null || draft.isEmpty()) continue;

            TemplateAuthoringService.Selection sel = svc.selectionFor(p, draft);
            if (sel != null && sel.world() == p.getWorld()) {
                drawCuboid(p,
                        sel.minX(), sel.minY(), sel.minZ(),
                        sel.maxX() + 1, sel.maxY() + 1, sel.maxZ() + 1,
                        SELECTION_COLOR);
            }
            drawAnchor(p, draft.entry(), ENTRY_COLOR);
            drawAnchor(p, draft.exit(), EXIT_COLOR);
            drawAnchor(p, draft.stop(), STOP_COLOR);
            drawAnchor(p, draft.display(), DISPLAY_COLOR);
            drawFacing(p, draft.entry(), draft.entryFacing());
            drawFacing(p, draft.exit(), draft.exitFacing());
        }
    }

    private static void drawAnchor(Player p, @Nullable BlockVector v, Color color) {
        if (v == null) return;
        drawCuboid(p,
                v.getBlockX(), v.getBlockY(), v.getBlockZ(),
                v.getBlockX() + 1, v.getBlockY() + 1, v.getBlockZ() + 1,
                color);
    }

    private static void drawFacing(Player p,
                                   @Nullable BlockVector v, @Nullable Direction dir) {
        if (v == null || dir == null) return;
        double cx = v.getBlockX() + 0.5;
        double cy = v.getBlockY() + 0.5;
        double cz = v.getBlockZ() + 0.5;
        double dx = 0, dz = 0;
        switch (dir) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case EAST -> dx = 1;
            case WEST -> dx = -1;
        }
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps * 1.5;
            p.spawnParticle(Particle.FLAME,
                    cx + dx * t, cy, cz + dz * t,
                    1, 0, 0, 0, 0);
        }
    }

    private static void drawCuboid(Player p,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, DUST_SIZE);
        // 4 bottom edges
        drawLine(p, x1, y1, z1, x2, y1, z1, dust);
        drawLine(p, x1, y1, z2, x2, y1, z2, dust);
        drawLine(p, x1, y1, z1, x1, y1, z2, dust);
        drawLine(p, x2, y1, z1, x2, y1, z2, dust);
        // 4 top edges
        drawLine(p, x1, y2, z1, x2, y2, z1, dust);
        drawLine(p, x1, y2, z2, x2, y2, z2, dust);
        drawLine(p, x1, y2, z1, x1, y2, z2, dust);
        drawLine(p, x2, y2, z1, x2, y2, z2, dust);
        // 4 vertical edges
        drawLine(p, x1, y1, z1, x1, y2, z1, dust);
        drawLine(p, x2, y1, z1, x2, y2, z1, dust);
        drawLine(p, x1, y1, z2, x1, y2, z2, dust);
        drawLine(p, x2, y1, z2, x2, y2, z2, dust);
    }

    private static void drawLine(Player p,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 Particle.DustOptions dust) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) {
            p.spawnParticle(Particle.DUST, x1, y1, z1, 1, 0, 0, 0, 0, dust);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(len / STEP));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            p.spawnParticle(Particle.DUST,
                    x1 + dx * t, y1 + dy * t, z1 + dz * t,
                    1, 0, 0, 0, 0, dust);
        }
    }
}
