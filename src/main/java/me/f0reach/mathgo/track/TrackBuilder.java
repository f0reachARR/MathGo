package me.f0reach.mathgo.track;

import me.f0reach.mathgo.track.codegen.CurveTemplate;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TrackBuilder {
    private static final double CURVE_PROBABILITY = 0.35;
    /** Max same-direction curves in a row before forcing the opposite. */
    private static final int MAX_SAME_TURN_STREAK = 2;
    /** Max cumulative net rotation (in 90° units). ±2 = ±180°; never reaches ±360° loop closure. */
    private static final int MAX_NET_ROTATION = 2;

    private final TemplateLibrary library;

    public TrackBuilder(TemplateLibrary library) {
        this.library = library;
    }

    public Track buildStageClear(World world, Location areaOrigin, Direction forward, int checkpoints,
                                 boolean weightedRandom) {
        List<PlacedSegment> placed = new ArrayList<>();
        Cursor cursor = new Cursor(areaOrigin.clone(), forward);

        // START
        placeRole(placed, cursor, SegmentRole.START, weightedRandom);
        // (MOVE, QUESTION) * checkpoints
        for (int i = 0; i < checkpoints; i++) {
            placeMoveStretch(placed, cursor, weightedRandom);
            placeRole(placed, cursor, SegmentRole.QUESTION, weightedRandom);
        }
        // Final stretch and GOAL
        placeMoveStretch(placed, cursor, weightedRandom);
        PlacedSegment goal = placeRole(placed, cursor, SegmentRole.GOAL, weightedRandom);

        Location startBoarding = computeStartBoarding(placed.get(0));
        Location goalCenter = computeGoalCenter(goal);
        return new Track(world, forward, startBoarding, goalCenter, placed);
    }

    public Track buildSurvival(World world, Location areaOrigin, Direction forward, int questionsLimit,
                               boolean weightedRandom) {
        List<PlacedSegment> placed = new ArrayList<>();
        Cursor cursor = new Cursor(areaOrigin.clone(), forward);

        placeRole(placed, cursor, SegmentRole.START, weightedRandom);
        for (int i = 0; i < questionsLimit; i++) {
            placeMoveStretch(placed, cursor, weightedRandom);
            placeRole(placed, cursor, SegmentRole.QUESTION, weightedRandom);
        }
        // No goal; survival ends by life loss. Add a final move stretch as a buffer.
        placeMoveStretch(placed, cursor, weightedRandom);

        Location startBoarding = computeStartBoarding(placed.get(0));
        return new Track(world, forward, startBoarding, null, placed);
    }

    /** Places a stretch of MOVE segments. May insert one curve, then at least one straight to avoid back-to-back curves. */
    private void placeMoveStretch(List<PlacedSegment> placed, Cursor cursor, boolean weightedRandom) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < CURVE_PROBABILITY) {
            CurveTemplate.Turn turn = cursor.budget.chooseTurn(rng);
            if (turn != null) {
                SegmentTemplate curve = new CurveTemplate(turn);
                PlacedSegment seg = curve.place(cursor.location.clone(), cursor.direction);
                placed.add(seg);
                cursor.location = seg.exitLocation();
                cursor.direction = seg.exitDirection();
                cursor.budget.record(turn);
            }
        }
        // Always at least one straight MOVE after a curve to give space and break "curve-then-curve" patterns.
        SegmentTemplate move = weightedRandom ? library.pickWeighted(SegmentRole.MOVE) : library.pickFirst(SegmentRole.MOVE);
        PlacedSegment moveSeg = move.place(cursor.location.clone(), cursor.direction);
        placed.add(moveSeg);
        cursor.location = moveSeg.exitLocation();
        cursor.direction = moveSeg.exitDirection();
    }

    private PlacedSegment placeRole(List<PlacedSegment> placed, Cursor cursor, SegmentRole role,
                                    boolean weightedRandom) {
        SegmentTemplate template = weightedRandom ? library.pickWeighted(role) : library.pickFirst(role);
        PlacedSegment seg = template.place(cursor.location.clone(), cursor.direction);
        placed.add(seg);
        cursor.location = seg.exitLocation();
        cursor.direction = seg.exitDirection();
        return seg;
    }

    private static Location computeStartBoarding(PlacedSegment start) {
        Direction f = start.entryDirection();
        return start.entryLocation().clone().add(f.dx() + 0.5, 0.1, f.dz() + 0.5);
    }

    private static Location computeGoalCenter(PlacedSegment goal) {
        Direction f = goal.entryDirection();
        return goal.entryLocation().clone().add(f.dx() * 2 + 0.5, 0.1, f.dz() * 2 + 0.5);
    }

    private static final class Cursor {
        Location location;
        Direction direction;
        final CurveBudget budget = new CurveBudget();
        Cursor(Location l, Direction d) { this.location = l; this.direction = d; }
    }

    /**
     * Constrains curve selection so the track cannot close into a loop.
     * Rule 1: at most {@link #MAX_SAME_TURN_STREAK} same-direction curves in a row.
     * Rule 2: cumulative net rotation never exceeds ±{@link #MAX_NET_ROTATION} (90° units),
     * keeping the path from reaching ±360° (= full loop).
     */
    private static final class CurveBudget {
        int netRotation = 0;
        int consecutiveSame = 0;
        @Nullable CurveTemplate.Turn lastTurn = null;

        @Nullable CurveTemplate.Turn chooseTurn(ThreadLocalRandom rng) {
            EnumSet<CurveTemplate.Turn> allowed = EnumSet.allOf(CurveTemplate.Turn.class);
            if (netRotation >= MAX_NET_ROTATION) allowed.remove(CurveTemplate.Turn.RIGHT);
            if (netRotation <= -MAX_NET_ROTATION) allowed.remove(CurveTemplate.Turn.LEFT);
            if (consecutiveSame >= MAX_SAME_TURN_STREAK && lastTurn != null) {
                allowed.remove(lastTurn);
            }
            if (allowed.isEmpty()) return null;
            if (allowed.size() == 1) return allowed.iterator().next();
            return rng.nextBoolean() ? CurveTemplate.Turn.RIGHT : CurveTemplate.Turn.LEFT;
        }

        void record(CurveTemplate.Turn turn) {
            consecutiveSame = (turn == lastTurn) ? consecutiveSame + 1 : 1;
            lastTurn = turn;
            netRotation += (turn == CurveTemplate.Turn.RIGHT) ? +1 : -1;
        }
    }
}
