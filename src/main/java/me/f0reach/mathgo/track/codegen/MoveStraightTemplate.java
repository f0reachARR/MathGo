package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.LocalAnchor;
import me.f0reach.mathgo.track.LocalFootprint;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;

public final class MoveStraightTemplate implements SegmentTemplate {
    public static final int LENGTH = 8;
    public static final int WIDTH = 3;

    @Override public String id() { return "move_straight_01"; }
    @Override public SegmentRole role() { return SegmentRole.MOVE; }
    @Override public int weight() { return 3; }
    @Override public LocalFootprint footprint() { return CodegenFootprints.straight(LENGTH, WIDTH); }
    @Override public LocalAnchor exit() { return new LocalAnchor(LENGTH, 0, Direction.NORTH); }

    @Override
    public PlacedSegment place(Location entry, Direction forward) {
        TunnelBuilder.carveStraight(entry, forward, LENGTH, WIDTH, Material.STONE, Material.DEEPSLATE_BRICKS,
                Material.RAIL);
        Location exit = entry.clone().add(forward.dx() * LENGTH, 0, forward.dz() * LENGTH);
        return new PlacedSegment(this, entry, forward, exit, forward,
                footprint().toWorldAabb(entry, forward), null);
    }
}
