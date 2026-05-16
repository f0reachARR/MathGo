package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.LocalAnchor;
import me.f0reach.mathgo.track.LocalFootprint;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Rail;

public final class CurveTemplate implements SegmentTemplate {
    public enum Turn { RIGHT, LEFT }

    private final Turn turn;

    public CurveTemplate(Turn turn) {
        this.turn = turn;
    }

    @Override public String id() { return turn == Turn.RIGHT ? "curve_right_01" : "curve_left_01"; }
    @Override public SegmentRole role() { return SegmentRole.MOVE; }
    @Override public int weight() { return 1; }

    @Override
    public LocalFootprint footprint() {
        // L-shape occupies (0..2, -sideSign..+sideSign) in local cells, plus the ceiling.
        // We approximate with a 3x3 bounding box in the F-S plane: F in [0,2], S in [-1,+1].
        return new LocalFootprint(0, 2, -1, +1, -1, +2);
    }

    @Override
    public LocalAnchor exit() {
        // After turning right, out direction is east in local frame; after left, west.
        // forwardCells=1, sideCells=±1 (next cell past the exit rail in outDir).
        if (turn == Turn.RIGHT) {
            // Exit cell = (1, +1), then step one more in outDir(+S=EAST) → (1, +2).
            return new LocalAnchor(1, 2, Direction.EAST);
        } else {
            return new LocalAnchor(1, -2, Direction.WEST);
        }
    }

    @Override
    public PlacedSegment place(Location entry, Direction forward) {
        World world = entry.getWorld();
        if (world == null) {
            return new PlacedSegment(this, entry, forward, entry.clone(), forward,
                    footprint().toWorldAabb(entry, forward), null);
        }

        Direction outDir = turn == Turn.RIGHT ? forward.rotateRight() : forward.rotateLeft();
        Direction right = forward.rotateRight();
        int sideSign = (turn == Turn.RIGHT) ? +1 : -1;

        int baseY = entry.getBlockY();
        Material floor = Material.STONE;
        Material wall = Material.DEEPSLATE_BRICKS;

        // Rail cells in local (f, s): (0, 0), (1, 0), (1, +sideSign)
        carveCell(world, entry, forward, right, 0, 0, baseY, floor);
        carveCell(world, entry, forward, right, 1, 0, baseY, floor);
        carveCell(world, entry, forward, right, 1, sideSign, baseY, floor);

        // Walls.
        wallCell(world, entry, forward, right, 0, -1, baseY, wall);
        wallCell(world, entry, forward, right, 0, +1, baseY, wall);
        wallCell(world, entry, forward, right, 1, -sideSign, baseY, wall);
        wallCell(world, entry, forward, right, 2, 0, baseY, wall);
        wallCell(world, entry, forward, right, 2, sideSign, baseY, wall);

        // Rails:
        placeRail(world, entry, forward, right, 0, 0, baseY,
                TunnelBuilder.railStraightShape(forward));
        placeRail(world, entry, forward, right, 1, 0, baseY,
                TunnelBuilder.railCornerShape(forward.opposite(), outDir));
        placeRail(world, entry, forward, right, 1, sideSign, baseY,
                TunnelBuilder.railStraightShape(outDir));

        // Next segment entry location.
        int exitX = entry.getBlockX() + forward.dx() + right.dx() * sideSign + outDir.dx();
        int exitZ = entry.getBlockZ() + forward.dz() + right.dz() * sideSign + outDir.dz();
        Location exit = new Location(world, exitX, baseY, exitZ);

        return new PlacedSegment(this, entry, forward, exit, outDir,
                footprint().toWorldAabb(entry, forward), null);
    }

    private static void carveCell(World world, Location origin, Direction forward, Direction right,
                                  int f, int s, int baseY, Material floor) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeCorridorCell(world, x, baseY, z, floor);
    }

    private static void wallCell(World world, Location origin, Direction forward, Direction right,
                                 int f, int s, int baseY, Material wall) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeWallColumn(world, x, baseY, z, wall);
    }

    private static void placeRail(World world, Location origin, Direction forward, Direction right,
                                  int f, int s, int baseY, Rail.Shape shape) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeRail(world, x, baseY, z, Material.RAIL, shape);
    }
}
