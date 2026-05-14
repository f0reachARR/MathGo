package me.f0reach.mathgo.track;

import org.bukkit.util.Vector;

public enum Direction {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0);

    private final int dx;
    private final int dy;
    private final int dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int dx() { return dx; }
    public int dy() { return dy; }
    public int dz() { return dz; }

    public Vector unitVector() {
        return new Vector(dx, dy, dz);
    }

    public float yaw() {
        return switch (this) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> -90f;
        };
    }
}
