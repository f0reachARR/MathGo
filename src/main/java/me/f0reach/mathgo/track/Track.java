package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Track {
    private final World world;
    private final Direction forward;
    private final Location startBoardLocation;
    @Nullable private final Location goalLocation;
    /** Currently-alive (visible-in-world) segments. Mutable: survival mode appends/prunes. */
    private final List<PlacedSegment> segments;
    /** Append-only history of every QUESTION segment ever placed, indexed by question number. */
    private final List<PlacedSegment> questionSegments;

    public Track(World world, Direction forward, Location startBoardLocation, @Nullable Location goalLocation,
                 List<PlacedSegment> segments) {
        this.world = world;
        this.forward = forward;
        this.startBoardLocation = startBoardLocation;
        this.goalLocation = goalLocation;
        this.segments = new ArrayList<>(segments);
        this.questionSegments = new ArrayList<>();
        for (PlacedSegment seg : this.segments) {
            if (seg.template().role() == SegmentRole.QUESTION) {
                questionSegments.add(seg);
            }
        }
    }

    public World world() { return world; }
    public Direction forward() { return forward; }
    public Location startBoardLocation() { return startBoardLocation; }
    @Nullable public Location goalLocation() { return goalLocation; }
    /** Unmodifiable view of the currently-alive segments. Use {@link #appendSegment} / {@link #pruneBeforeSegment} to mutate. */
    public List<PlacedSegment> segments() { return Collections.unmodifiableList(segments); }
    /** Unmodifiable view of all question segments ever placed (append-only, never shrinks). */
    public List<PlacedSegment> questionSegments() { return Collections.unmodifiableList(questionSegments); }

    public void appendSegment(PlacedSegment seg) {
        segments.add(seg);
        if (seg.template().role() == SegmentRole.QUESTION) {
            questionSegments.add(seg);
        }
    }

    /**
     * Remove all currently-alive segments that come before {@code marker}, clearing their world blocks to AIR.
     * {@code marker} itself is kept. If marker is not in the alive list, nothing is pruned.
     */
    public void pruneBeforeSegment(PlacedSegment marker) {
        Iterator<PlacedSegment> it = segments.iterator();
        while (it.hasNext()) {
            PlacedSegment seg = it.next();
            if (seg == marker) break;
            clearAabb(seg.worldFootprint());
            it.remove();
        }
    }

    /**
     * Does the given world AABB intersect any currently-alive segment, optionally excluding one
     * (typically the last placed segment, whose exit edge touches the candidate's entry)?
     */
    public boolean intersectsActive(WorldAABB box, @Nullable PlacedSegment exclude) {
        for (PlacedSegment seg : segments) {
            if (seg == exclude) continue;
            if (seg.worldFootprint().intersects(box)) return true;
        }
        return false;
    }

    public void cleanup() {
        for (PlacedSegment seg : segments) {
            clearAabb(seg.worldFootprint());
        }
        segments.clear();
    }

    private void clearAabb(WorldAABB box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }
}
