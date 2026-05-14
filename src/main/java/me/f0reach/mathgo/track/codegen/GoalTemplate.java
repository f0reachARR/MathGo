package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public final class GoalTemplate implements SegmentTemplate {
    public static final int LENGTH = 4;

    @Override public String id() { return "goal_01"; }
    @Override public SegmentRole role() { return SegmentRole.GOAL; }
    @Override public int length() { return LENGTH; }
    @Override public int weight() { return 1; }

    @Override
    public PlacedSegment place(Location origin, Direction forward) {
        List<Location> changed = new ArrayList<>();
        TunnelBuilder.carve(origin, forward, LENGTH, Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.RAIL, changed);
        // End wall: place a barrier of emerald at the very end.
        World world = origin.getWorld();
        if (world != null) {
            int ex = origin.getBlockX() + forward.dx() * (LENGTH - 1);
            int ez = origin.getBlockZ() + forward.dz() * (LENGTH - 1);
            int by = origin.getBlockY();
            TunnelBuilder.setBlock(world, ex, by + 1, ez, Material.SEA_LANTERN.createBlockData(), changed);
        }
        return new PlacedSegment(this, origin, forward, LENGTH, changed, null);
    }
}
