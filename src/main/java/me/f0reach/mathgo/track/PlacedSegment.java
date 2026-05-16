package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public final class PlacedSegment {
    private final SegmentTemplate template;
    private final Location entryLocation;
    private final Direction entryDirection;
    private final Location exitLocation;
    private final Direction exitDirection;
    private final WorldAABB worldFootprint;
    @Nullable private final QuestionAnchors questionAnchors;

    public PlacedSegment(SegmentTemplate template,
                         Location entryLocation, Direction entryDirection,
                         Location exitLocation, Direction exitDirection,
                         WorldAABB worldFootprint,
                         @Nullable QuestionAnchors questionAnchors) {
        this.template = template;
        this.entryLocation = entryLocation;
        this.entryDirection = entryDirection;
        this.exitLocation = exitLocation;
        this.exitDirection = exitDirection;
        this.worldFootprint = worldFootprint;
        this.questionAnchors = questionAnchors;
    }

    public SegmentTemplate template() { return template; }
    public Location entryLocation() { return entryLocation; }
    public Direction entryDirection() { return entryDirection; }
    public Location exitLocation() { return exitLocation; }
    public Direction exitDirection() { return exitDirection; }
    public WorldAABB worldFootprint() { return worldFootprint; }
    @Nullable public QuestionAnchors questionAnchors() { return questionAnchors; }
}
