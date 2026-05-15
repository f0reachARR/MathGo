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
    public PlacedSegment place(Location entry, Direction forward) {
        List<Location> changed = new ArrayList<>();
        TunnelBuilder.carveStraight(entry, forward, LENGTH, Material.GOLD_BLOCK, Material.EMERALD_BLOCK,
                Material.RAIL, changed);
        World world = entry.getWorld();
        if (world != null) {
            int ex = entry.getBlockX() + forward.dx() * (LENGTH - 1);
            int ez = entry.getBlockZ() + forward.dz() * (LENGTH - 1);
            int by = entry.getBlockY();
            TunnelBuilder.setBlock(world, ex, by + 1, ez, Material.SEA_LANTERN.createBlockData(), changed);
        }
        Location exit = entry.clone().add(forward.dx() * LENGTH, 0, forward.dz() * LENGTH);
        return new PlacedSegment(this, entry, forward, exit, forward, changed, null);
    }
}
