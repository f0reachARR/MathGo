package me.f0reach.mathgo.integration;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import me.f0reach.mathgo.MathGoPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Optional WorldEdit integration. When WorldEdit is installed, {@link #getSelection(Player)} returns
 * the player's current cuboid selection. This lets users reuse WorldEdit's familiar selection workflow
 * ({@code //wand}, {@code //pos1}, {@code //pos2}, {@code //sel cuboid}) for MathGo template authoring.
 *
 * <p>The integration is opt-in: if WorldEdit is absent at runtime, {@link #isAvailable()} stays false
 * and {@link #getSelection(Player)} returns {@code null}. The class references WorldEdit symbols only
 * inside its methods, so it must NOT be loaded when WE is missing — call {@link #initialize(MathGoPlugin)}
 * via reflection-safe wrapper {@link Holder}.
 */
public final class WorldEditBridge {
    private WorldEditBridge() {}

    public static void initialize(MathGoPlugin plugin) {
        // Touch a WorldEdit class to verify the API is actually loadable (paper-plugin.yml join-classpath
        // should make it available, but be defensive). Throwing here flips Holder.AVAILABLE to false.
        WorldEdit we = WorldEdit.getInstance();
        if (we == null) throw new IllegalStateException("WorldEdit instance not available");
        plugin.getLogger().info("MathGo: WorldEdit integration enabled.");
    }

    /**
     * Returns the player's current WorldEdit cuboid selection, or {@code null} if the selection is
     * incomplete, in a different world than the player, or any other error occurs.
     */
    @Nullable
    public static Selection getSelection(Player player) {
        try {
            var wePlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            World w = session.getSelectionWorld();
            if (w == null) return null;
            Region region = session.getSelection(w);
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return new Selection(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        } catch (IncompleteRegionException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Minimal-corner result type independent of the WE API so callers don't need to import WE classes. */
    public record Selection(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    /**
     * Class-loaded only when WorldEdit is actually present on the classpath. Accessing this nested
     * type triggers JVM class loading; if any WE reference inside {@link WorldEditBridge} fails to
     * link, the static initializer trips and {@link #AVAILABLE} stays false.
     */
    public static final class Holder {
        public static final boolean AVAILABLE;
        static {
            boolean ok;
            try {
                Class.forName("com.sk89q.worldedit.WorldEdit");
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }
            AVAILABLE = ok;
        }
        private Holder() {}
    }
}
