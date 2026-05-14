package me.f0reach.mathgo.track;

import org.bukkit.Location;

public interface SegmentTemplate {
    String id();

    SegmentRole role();

    int length();

    int weight();

    PlacedSegment place(Location origin, Direction forward);
}
