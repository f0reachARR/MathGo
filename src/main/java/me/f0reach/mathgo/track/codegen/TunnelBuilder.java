package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;

import java.util.List;

final class TunnelBuilder {
    private TunnelBuilder() {}

    static void carve(Location origin, Direction forward, int length, Material floor, Material wall, Material rail,
                      List<Location> changed) {
        World world = origin.getWorld();
        if (world == null) return;
        int baseY = origin.getBlockY();
        // left direction: 90 deg counterclockwise of forward (for east -> north)
        int leftDx;
        int leftDz;
        switch (forward) {
            case EAST -> { leftDx = 0; leftDz = -1; }
            case WEST -> { leftDx = 0; leftDz = 1; }
            case NORTH -> { leftDx = -1; leftDz = 0; }
            case SOUTH -> { leftDx = 1; leftDz = 0; }
            default -> { leftDx = 0; leftDz = 0; }
        }
        for (int i = 0; i < length; i++) {
            int cx = origin.getBlockX() + forward.dx() * i;
            int cz = origin.getBlockZ() + forward.dz() * i;
            // Floor under rail
            setBlock(world, cx, baseY - 1, cz, floor.createBlockData(), changed);
            // Air interior (rail level + 2 above)
            setBlock(world, cx, baseY, cz, Material.AIR.createBlockData(), changed);
            setBlock(world, cx, baseY + 1, cz, Material.AIR.createBlockData(), changed);
            setBlock(world, cx, baseY + 2, cz, Material.AIR.createBlockData(), changed);
            // Rail
            if (rail != null) {
                BlockData rd = rail.createBlockData();
                if (rd instanceof Rail r) {
                    r.setShape(forward.dx() != 0 ? Rail.Shape.EAST_WEST : Rail.Shape.NORTH_SOUTH);
                    rd = r;
                }
                setBlock(world, cx, baseY, cz, rd, changed);
            }
            // Walls on left and right at rail and head height
            int wlx = cx + leftDx;
            int wlz = cz + leftDz;
            int wrx = cx - leftDx;
            int wrz = cz - leftDz;
            setBlock(world, wlx, baseY, wlz, wall.createBlockData(), changed);
            setBlock(world, wlx, baseY + 1, wlz, wall.createBlockData(), changed);
            setBlock(world, wrx, baseY, wrz, wall.createBlockData(), changed);
            setBlock(world, wrx, baseY + 1, wrz, wall.createBlockData(), changed);
            // Ceiling (optional, keep open for now: skip)
        }
    }

    static void setBlock(World world, int x, int y, int z, BlockData data, List<Location> changed) {
        Block block = world.getBlockAt(x, y, z);
        Location loc = new Location(world, x, y, z);
        changed.add(loc);
        block.setBlockData(data, false);
    }
}
