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

    /** Carves a straight tunnel of `length` cells along `forward`, placing rails on the centerline and walls on both sides. */
    static void carveStraight(Location origin, Direction forward, int length, Material floor, Material wall,
                              Material rail, List<Location> changed) {
        World world = origin.getWorld();
        if (world == null) return;
        Direction right = forward.rotateRight();
        int baseY = origin.getBlockY();
        Rail.Shape straightShape = railStraightShape(forward);
        for (int f = 0; f < length; f++) {
            int cx = origin.getBlockX() + forward.dx() * f;
            int cz = origin.getBlockZ() + forward.dz() * f;
            placeCorridorCell(world, cx, baseY, cz, floor, changed);
            if (rail != null) {
                placeRail(world, cx, baseY, cz, rail, straightShape, changed);
            }
            // Walls on both sides.
            placeWallColumn(world, cx + right.dx(), baseY, cz + right.dz(), wall, changed);
            placeWallColumn(world, cx - right.dx(), baseY, cz - right.dz(), wall, changed);
        }
    }

    /** Carves an air corridor cell (floor + 3 air blocks). */
    static void placeCorridorCell(World world, int x, int y, int z, Material floor, List<Location> changed) {
        setBlock(world, x, y - 1, z, floor.createBlockData(), changed);
        setBlock(world, x, y, z, Material.AIR.createBlockData(), changed);
        setBlock(world, x, y + 1, z, Material.AIR.createBlockData(), changed);
        setBlock(world, x, y + 2, z, Material.AIR.createBlockData(), changed);
    }

    /** Places a wall column (rail level + head height). */
    static void placeWallColumn(World world, int x, int y, int z, Material wall, List<Location> changed) {
        setBlock(world, x, y - 1, z, wall.createBlockData(), changed);
        setBlock(world, x, y, z, wall.createBlockData(), changed);
        setBlock(world, x, y + 1, z, wall.createBlockData(), changed);
    }

    static void placeRail(World world, int x, int y, int z, Material railMaterial, Rail.Shape shape,
                          List<Location> changed) {
        BlockData rd = railMaterial.createBlockData();
        if (rd instanceof Rail r) {
            r.setShape(shape);
            rd = r;
        }
        setBlock(world, x, y, z, rd, changed);
    }

    static Rail.Shape railStraightShape(Direction forward) {
        return (forward.dx() != 0) ? Rail.Shape.EAST_WEST : Rail.Shape.NORTH_SOUTH;
    }

    /** Returns the curved rail shape connecting two adjacent cardinal sides. */
    static Rail.Shape railCornerShape(Direction a, Direction b) {
        boolean hasNorth = a == Direction.NORTH || b == Direction.NORTH;
        boolean hasSouth = a == Direction.SOUTH || b == Direction.SOUTH;
        boolean hasEast = a == Direction.EAST || b == Direction.EAST;
        boolean hasWest = a == Direction.WEST || b == Direction.WEST;
        if (hasNorth && hasEast) return Rail.Shape.NORTH_EAST;
        if (hasNorth && hasWest) return Rail.Shape.NORTH_WEST;
        if (hasSouth && hasEast) return Rail.Shape.SOUTH_EAST;
        if (hasSouth && hasWest) return Rail.Shape.SOUTH_WEST;
        throw new IllegalArgumentException("Cannot form corner from " + a + " and " + b);
    }

    static void setBlock(World world, int x, int y, int z, BlockData data, List<Location> changed) {
        Block block = world.getBlockAt(x, y, z);
        Location loc = new Location(world, x, y, z);
        changed.add(loc);
        block.setBlockData(data, false);
    }
}
