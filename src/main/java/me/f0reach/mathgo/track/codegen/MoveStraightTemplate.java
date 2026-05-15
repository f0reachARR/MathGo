package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class MoveStraightTemplate implements SegmentTemplate {
    public static final int LENGTH = 8;

    @Override public String id() { return "move_straight_01"; }
    @Override public SegmentRole role() { return SegmentRole.MOVE; }
    @Override public int length() { return LENGTH; }
    @Override public int weight() { return 3; }

    @Override
    public PlacedSegment place(Location entry, Direction forward) {
        List<Location> changed = new ArrayList<>();
        TunnelBuilder.carveStraight(entry, forward, LENGTH, Material.STONE, Material.DEEPSLATE_BRICKS,
                Material.RAIL, changed);
        Location exit = entry.clone().add(forward.dx() * LENGTH, 0, forward.dz() * LENGTH);
        return new PlacedSegment(this, entry, forward, exit, forward, changed, null);
    }
}
