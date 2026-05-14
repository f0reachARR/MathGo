package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PlacedSegment {
    private final SegmentTemplate template;
    private final Location origin;
    private final Direction forward;
    private final int length;
    private final List<Location> changedBlocks;
    @Nullable private final QuestionAnchors questionAnchors;

    public PlacedSegment(SegmentTemplate template, Location origin, Direction forward, int length,
                         List<Location> changedBlocks, @Nullable QuestionAnchors questionAnchors) {
        this.template = template;
        this.origin = origin;
        this.forward = forward;
        this.length = length;
        this.changedBlocks = changedBlocks;
        this.questionAnchors = questionAnchors;
    }

    public SegmentTemplate template() { return template; }
    public Location origin() { return origin; }
    public Direction forward() { return forward; }
    public int length() { return length; }
    public List<Location> changedBlocks() { return changedBlocks; }
    @Nullable public QuestionAnchors questionAnchors() { return questionAnchors; }

    public Location exitLocation() {
        return origin.clone().add(forward.dx() * length, 0, forward.dz() * length);
    }
}
