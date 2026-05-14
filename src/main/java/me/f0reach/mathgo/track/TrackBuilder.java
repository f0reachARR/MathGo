package me.f0reach.mathgo.track;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public final class TrackBuilder {
    private final TemplateLibrary library;

    public TrackBuilder(TemplateLibrary library) {
        this.library = library;
    }

    public Track buildStageClear(World world, Location areaOrigin, Direction forward, int checkpoints,
                                 boolean weightedRandom) {
        List<SegmentTemplate> sequence = new ArrayList<>();
        sequence.add(pick(SegmentRole.START, weightedRandom));
        for (int i = 0; i < checkpoints; i++) {
            sequence.add(pick(SegmentRole.MOVE, weightedRandom));
            sequence.add(pick(SegmentRole.QUESTION, weightedRandom));
        }
        sequence.add(pick(SegmentRole.MOVE, weightedRandom));
        sequence.add(pick(SegmentRole.GOAL, weightedRandom));

        List<PlacedSegment> placed = new ArrayList<>();
        Location cursor = areaOrigin.clone();
        PlacedSegment startPlaced = null;
        PlacedSegment goalPlaced = null;
        for (SegmentTemplate template : sequence) {
            PlacedSegment seg = template.place(cursor.clone(), forward);
            placed.add(seg);
            if (template.role() == SegmentRole.START && startPlaced == null) {
                startPlaced = seg;
            }
            if (template.role() == SegmentRole.GOAL) {
                goalPlaced = seg;
            }
            cursor = seg.exitLocation();
        }

        Location startBoarding = startPlaced != null
                ? startPlaced.origin().clone().add(forward.dx() + 0.5, 0.1, forward.dz() + 0.5)
                : areaOrigin.clone().add(0.5, 0.1, 0.5);
        Location goalLoc = goalPlaced != null
                ? goalPlaced.origin().clone().add(forward.dx() * (goalPlaced.length() - 1) + 0.5, 0.1,
                forward.dz() * (goalPlaced.length() - 1) + 0.5)
                : cursor.clone();

        return new Track(world, forward, startBoarding, goalLoc, placed);
    }

    private SegmentTemplate pick(SegmentRole role, boolean weightedRandom) {
        return weightedRandom ? library.pickWeighted(role) : library.pickFirst(role);
    }
}
