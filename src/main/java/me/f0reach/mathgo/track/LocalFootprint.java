package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Axis-aligned bounding box in template-local cell coordinates.
 * Origin (0,0,0) = entry cell, +F = forward direction at entry, +S = right of forward, +Y = up.
 */
public record LocalFootprint(int minF, int maxF, int minS, int maxS, int minY, int maxY) {

    public WorldAABB toWorldAabb(Location entryWorld, Direction forward) {
        World world = entryWorld.getWorld();
        if (world == null) throw new IllegalStateException("entryWorld has no world");
        Direction right = forward.rotateRight();
        int ex = entryWorld.getBlockX();
        int ey = entryWorld.getBlockY();
        int ez = entryWorld.getBlockZ();
        int x1 = ex + forward.dx() * minF + right.dx() * minS;
        int z1 = ez + forward.dz() * minF + right.dz() * minS;
        int x2 = ex + forward.dx() * maxF + right.dx() * maxS;
        int z2 = ez + forward.dz() * maxF + right.dz() * maxS;
        // Also consider mixed extents — the rotated rectangle remains axis-aligned in world frame
        // because forward and right are cardinal, so min/max above already span the corners.
        int y1 = ey + minY;
        int y2 = ey + maxY;
        return WorldAABB.of(x1, y1, z1, x2, y2, z2);
    }
}
