package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.List;

public final class CurveTemplate implements SegmentTemplate {
    public enum Turn { RIGHT, LEFT }

    private final Turn turn;

    public CurveTemplate(Turn turn) {
        this.turn = turn;
    }

    @Override public String id() { return turn == Turn.RIGHT ? "curve_right_01" : "curve_left_01"; }
    @Override public SegmentRole role() { return SegmentRole.MOVE; }
    @Override public int length() { return 2; }
    @Override public int weight() { return 1; }

    @Override
    public PlacedSegment place(Location entry, Direction forward) {
        List<Location> changed = new ArrayList<>();
        World world = entry.getWorld();
        if (world == null) {
            return new PlacedSegment(this, entry, forward, entry.clone(), forward, changed, null);
        }

        Direction outDir = turn == Turn.RIGHT ? forward.rotateRight() : forward.rotateLeft();
        // Side basis: for RIGHT turn, side = right of forward. For LEFT, side = left of forward.
        // We'll keep `right` as forward.rotateRight() and use a multiplier to flip for LEFT.
        Direction right = forward.rotateRight();
        int sideSign = (turn == Turn.RIGHT) ? +1 : -1;

        int baseY = entry.getBlockY();
        Material floor = Material.STONE;
        Material wall = Material.DEEPSLATE_BRICKS;

        // Rail cells in local (f, s): (0, 0), (1, 0), (1, +sideSign)
        carveCell(world, entry, forward, right, 0, 0, baseY, floor, changed);
        carveCell(world, entry, forward, right, 1, 0, baseY, floor, changed);
        carveCell(world, entry, forward, right, 1, sideSign, baseY, floor, changed);

        // Walls.
        // Common walls around the entry cell: (0, -1) and (0, +1) — but skip if cell is a rail cell.
        wallCell(world, entry, forward, right, 0, -1, baseY, wall, changed);
        wallCell(world, entry, forward, right, 0, +1, baseY, wall, changed);
        // Outer wall of corner: (1, -sideSign)
        wallCell(world, entry, forward, right, 1, -sideSign, baseY, wall, changed);
        // Wall blocking forward past the corner: (2, 0)
        wallCell(world, entry, forward, right, 2, 0, baseY, wall, changed);
        // Wall outside of exit rail (forward-side, exit-side): (2, +sideSign)
        wallCell(world, entry, forward, right, 2, sideSign, baseY, wall, changed);

        // Rails:
        // (0,0) straight in forward direction
        placeRail(world, entry, forward, right, 0, 0, baseY,
                TunnelBuilder.railStraightShape(forward), changed);
        // (1,0) corner connecting back=forward.opposite() and out=outDir
        placeRail(world, entry, forward, right, 1, 0, baseY,
                TunnelBuilder.railCornerShape(forward.opposite(), outDir), changed);
        // (1, sideSign) straight in outDir
        placeRail(world, entry, forward, right, 1, sideSign, baseY,
                TunnelBuilder.railStraightShape(outDir), changed);

        // Next segment entry location: one cell past the exit rail in outDir.
        int exitX = entry.getBlockX() + forward.dx() * 1 + right.dx() * sideSign + outDir.dx();
        int exitZ = entry.getBlockZ() + forward.dz() * 1 + right.dz() * sideSign + outDir.dz();
        Location exit = new Location(world, exitX, baseY, exitZ);

        return new PlacedSegment(this, entry, forward, exit, outDir, changed, null);
    }

    private static void carveCell(World world, Location origin, Direction forward, Direction right,
                                  int f, int s, int baseY, Material floor, List<Location> changed) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeCorridorCell(world, x, baseY, z, floor, changed);
    }

    private static void wallCell(World world, Location origin, Direction forward, Direction right,
                                 int f, int s, int baseY, Material wall, List<Location> changed) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeWallColumn(world, x, baseY, z, wall, changed);
    }

    private static void placeRail(World world, Location origin, Direction forward, Direction right,
                                  int f, int s, int baseY, Rail.Shape shape, List<Location> changed) {
        int x = origin.getBlockX() + forward.dx() * f + right.dx() * s;
        int z = origin.getBlockZ() + forward.dz() * f + right.dz() * s;
        TunnelBuilder.placeRail(world, x, baseY, z, Material.RAIL, shape, changed);
    }
}
