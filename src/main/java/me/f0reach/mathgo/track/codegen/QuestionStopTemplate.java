package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.LocalAnchor;
import me.f0reach.mathgo.track.LocalFootprint;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.QuestionAnchors;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;

public final class QuestionStopTemplate implements SegmentTemplate {
    public static final int LENGTH = 6;
    public static final int WIDTH = 3;
    public static final int STOP_OFFSET = 3;

    @Override public String id() { return "question_stop_01"; }
    @Override public SegmentRole role() { return SegmentRole.QUESTION; }
    @Override public int weight() { return 1; }
    @Override public LocalFootprint footprint() { return CodegenFootprints.straight(LENGTH, WIDTH); }
    @Override public LocalAnchor exit() { return new LocalAnchor(LENGTH, 0, Direction.NORTH); }

    @Override
    public PlacedSegment place(Location entry, Direction forward) {
        TunnelBuilder.carveStraight(entry, forward, LENGTH, WIDTH, Material.POLISHED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.RAIL);
        Location stop = entry.clone().add(
                forward.dx() * STOP_OFFSET + 0.5,
                0.0625,
                forward.dz() * STOP_OFFSET + 0.5);
        Location display = stop.clone().add(0, 2.0, 0);
        QuestionAnchors anchors = new QuestionAnchors(stop, display);
        Location exit = entry.clone().add(forward.dx() * LENGTH, 0, forward.dz() * LENGTH);
        return new PlacedSegment(this, entry, forward, exit, forward,
                footprint().toWorldAabb(entry, forward), anchors);
    }
}
