package me.f0reach.mathgo.track;

public record WorldAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static WorldAABB of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new WorldAABB(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(WorldAABB other) {
        return other.minX >= minX && other.maxX <= maxX
                && other.minY >= minY && other.maxY <= maxY
                && other.minZ >= minZ && other.maxZ <= maxZ;
    }

    public boolean intersects(WorldAABB other) {
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }
}
