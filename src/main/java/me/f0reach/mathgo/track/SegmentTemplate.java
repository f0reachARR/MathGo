package me.f0reach.mathgo.track;

import org.bukkit.Location;

public interface SegmentTemplate {
    String id();

    SegmentRole role();

    int weight();

    /**
     * Bounding box of this template in template-local cell coordinates.
     * Local frame: entry cell at (0,0,0), entry forward = +F axis, right-of-forward = +S axis, up = +Y.
     */
    LocalFootprint footprint();

    /** Exit cell offset (relative to entry) and outgoing direction in template-local frame. */
    LocalAnchor exit();

    PlacedSegment place(Location origin, Direction forward);
}
