package me.f0reach.mathgo.area;

import java.util.HashSet;
import java.util.Set;

public final class AreaGrid {
    private final int size;
    private final Set<Long> reserved = new HashSet<>();

    public AreaGrid(int size) {
        this.size = size;
    }

    public synchronized Area reserveNext() {
        for (int radius = 0; radius < 64; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius && radius != 0) continue;
                    long key = key(dx, dz);
                    if (!reserved.contains(key)) {
                        reserved.add(key);
                        return new Area(dx, dz, size);
                    }
                }
            }
        }
        throw new IllegalStateException("AreaGrid exhausted");
    }

    public synchronized void release(Area area) {
        reserved.remove(key(area.gridX(), area.gridZ()));
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }
}
