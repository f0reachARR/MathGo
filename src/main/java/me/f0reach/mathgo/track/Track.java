package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Track {
    private final World world;
    private final Direction forward;
    private final Location startBoardLocation;
    @Nullable private final Location goalLocation;
    private final List<PlacedSegment> segments;
    private final List<PlacedSegment> questionSegments;

    public Track(World world, Direction forward, Location startBoardLocation, @Nullable Location goalLocation,
                 List<PlacedSegment> segments) {
        this.world = world;
        this.forward = forward;
        this.startBoardLocation = startBoardLocation;
        this.goalLocation = goalLocation;
        this.segments = segments;
        this.questionSegments = new ArrayList<>();
        for (PlacedSegment seg : segments) {
            if (seg.template().role() == SegmentRole.QUESTION) {
                questionSegments.add(seg);
            }
        }
    }

    public World world() { return world; }
    public Direction forward() { return forward; }
    public Location startBoardLocation() { return startBoardLocation; }
    @Nullable public Location goalLocation() { return goalLocation; }
    public List<PlacedSegment> segments() { return segments; }
    public List<PlacedSegment> questionSegments() { return questionSegments; }

    public void cleanup() {
        for (PlacedSegment seg : segments) {
            for (Location loc : seg.changedBlocks()) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
    }
}
