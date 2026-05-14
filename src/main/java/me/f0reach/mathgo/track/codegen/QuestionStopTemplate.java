package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.QuestionAnchors;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class QuestionStopTemplate implements SegmentTemplate {
    public static final int LENGTH = 6;
    public static final int STOP_OFFSET = 3;

    @Override public String id() { return "question_stop_01"; }
    @Override public SegmentRole role() { return SegmentRole.QUESTION; }
    @Override public int length() { return LENGTH; }
    @Override public int weight() { return 1; }

    @Override
    public PlacedSegment place(Location origin, Direction forward) {
        List<Location> changed = new ArrayList<>();
        TunnelBuilder.carve(origin, forward, LENGTH, Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
                Material.RAIL, changed);
        Location stop = origin.clone().add(
                forward.dx() * STOP_OFFSET + 0.5,
                0.0625,
                forward.dz() * STOP_OFFSET + 0.5);
        Location display = stop.clone().add(0, 2.0, 0);
        QuestionAnchors anchors = new QuestionAnchors(stop, display);
        return new PlacedSegment(this, origin, forward, LENGTH, changed, anchors);
    }
}
