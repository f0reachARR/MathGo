package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;

final class TunnelBuilder {
    private TunnelBuilder() {}

    /**
     * Carves a straight tunnel of {@code length} cells along {@code forward}, with {@code width} cells of
     * cross-section perpendicular to forward (must be odd; e.g. 3 → 1 floor + 2 walls). Rails on centerline,
     * floor cells fill the interior, wall columns line both edges.
     */
    static void carveStraight(Location origin, Direction forward, int length, int width, Material floor,
                              Material wall, Material rail) {
        World world = origin.getWorld();
        if (world == null) return;
        if (width < 3 || width % 2 == 0) {
            throw new IllegalArgumentException("width must be odd and >= 3 (got " + width + ")");
        }
        Direction right = forward.rotateRight();
        int half = (width - 1) / 2;
        int baseY = origin.getBlockY();
        Rail.Shape straightShape = railStraightShape(forward);
        for (int f = 0; f < length; f++) {
            int fx = origin.getBlockX() + forward.dx() * f;
            int fz = origin.getBlockZ() + forward.dz() * f;
            for (int s = -half; s <= half; s++) {
                int cx = fx + right.dx() * s;
                int cz = fz + right.dz() * s;
                if (Math.abs(s) == half) {
                    placeWallColumn(world, cx, baseY, cz, wall);
                } else {
                    placeCorridorCell(world, cx, baseY, cz, floor);
                }
            }
            if (rail != null) {
                placeRail(world, fx, baseY, fz, rail, straightShape);
            }
        }
    }

    /** Carves an air corridor cell (floor + 3 air blocks). */
    static void placeCorridorCell(World world, int x, int y, int z, Material floor) {
        setBlock(world, x, y - 1, z, floor.createBlockData());
        setBlock(world, x, y, z, Material.AIR.createBlockData());
        setBlock(world, x, y + 1, z, Material.AIR.createBlockData());
        setBlock(world, x, y + 2, z, Material.AIR.createBlockData());
    }

    /** Places a wall column (rail level + head height). */
    static void placeWallColumn(World world, int x, int y, int z, Material wall) {
        setBlock(world, x, y - 1, z, wall.createBlockData());
        setBlock(world, x, y, z, wall.createBlockData());
        setBlock(world, x, y + 1, z, wall.createBlockData());
    }

    static void placeRail(World world, int x, int y, int z, Material railMaterial, Rail.Shape shape) {
        BlockData rd = railMaterial.createBlockData();
        if (rd instanceof Rail r) {
            r.setShape(shape);
            rd = r;
        }
        setBlock(world, x, y, z, rd);
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

    static void setBlock(World world, int x, int y, int z, BlockData data) {
        Block block = world.getBlockAt(x, y, z);
        block.setBlockData(data, false);
    }
}
