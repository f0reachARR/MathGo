package me.f0reach.mathgo.area;

import me.f0reach.mathgo.track.WorldAABB;
import org.bukkit.Location;
import org.bukkit.World;

public record Area(int gridX, int gridZ, int size) {
    public Location originAt(World world, int y) {
        return new Location(world, gridX * size, y, gridZ * size);
    }

    /** Horizontal AABB of this area in world block coordinates (Y unrestricted: use {@link Integer#MIN_VALUE/MAX_VALUE}). */
    public WorldAABB worldAabb() {
        int minX = gridX * size;
        int minZ = gridZ * size;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        return new WorldAABB(minX, Integer.MIN_VALUE, minZ, maxX, Integer.MAX_VALUE, maxZ);
    }

    /** Does the AABB (horizontally) fit entirely inside this area? Y is ignored. */
    public boolean contains(WorldAABB box) {
        int minX = gridX * size;
        int minZ = gridZ * size;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        return box.minX() >= minX && box.maxX() <= maxX
                && box.minZ() >= minZ && box.maxZ() <= maxZ;
    }
}
