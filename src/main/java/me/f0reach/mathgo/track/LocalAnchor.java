package me.f0reach.mathgo.track;

import org.bukkit.Location;

/**
 * Exit anchor of a template, expressed in template-local cell coordinates relative to the entry cell.
 * <p>
 * {@code forwardCells} = forward offset (entry-to-exit run along the original forward axis).
 * {@code sideCells}    = right-side offset (positive = right of forward).
 * {@code outDir}       = direction the cart leaves the exit in (world after rotation).
 */
public record LocalAnchor(int forwardCells, int sideCells, Direction outDir) {

    /** Compute the world-space exit location given the entry world location and entry forward direction. */
    public Location toWorld(Location entryWorld, Direction forward) {
        Direction right = forward.rotateRight();
        int ex = entryWorld.getBlockX() + forward.dx() * forwardCells + right.dx() * sideCells;
        int ez = entryWorld.getBlockZ() + forward.dz() * forwardCells + right.dz() * sideCells;
        int ey = entryWorld.getBlockY();
        return new Location(entryWorld.getWorld(), ex, ey, ez);
    }

    /** Compute the world direction the cart faces leaving the exit, given the entry forward. */
    public Direction worldOutDir(Direction entryForward) {
        // outDir is expressed in template-local frame assuming entry forward = NORTH (template author's frame).
        // Rotate it by (entryForward - NORTH). Because Direction has rotateRight(), we can compose:
        int rights = switch (entryForward) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
        };
        Direction d = outDir;
        for (int i = 0; i < rights; i++) d = d.rotateRight();
        return d;
    }
}
