package me.f0reach.mathgo.area;

import org.bukkit.Location;
import org.bukkit.World;

public record Area(int gridX, int gridZ, int size) {
    public Location originAt(World world, int y) {
        return new Location(world, gridX * size, y, gridZ * size);
    }
}
