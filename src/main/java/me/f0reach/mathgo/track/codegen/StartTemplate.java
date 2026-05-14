package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class StartTemplate implements SegmentTemplate {
    public static final int LENGTH = 4;

    @Override public String id() { return "start_01"; }
    @Override public SegmentRole role() { return SegmentRole.START; }
    @Override public int length() { return LENGTH; }
    @Override public int weight() { return 1; }

    @Override
    public PlacedSegment place(Location origin, Direction forward) {
        List<Location> changed = new ArrayList<>();
        TunnelBuilder.carve(origin, forward, LENGTH, Material.SMOOTH_STONE, Material.STONE_BRICKS, Material.RAIL, changed);
        return new PlacedSegment(this, origin, forward, LENGTH, changed, null);
    }
}
