package me.f0reach.mathgo.track;

import me.f0reach.mathgo.area.Area;
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

    public Track buildStageClear(World world, Area area, Location areaOrigin, Direction forward, int checkpoints,
                                 boolean weightedRandom) {
        List<PlacedSegment> placed = new ArrayList<>();
        Cursor cursor = new Cursor(areaOrigin.clone(), forward);

        // START — must fit; if not, the area is unusably small.
        if (!placeRoleIfFits(placed, cursor, area, SegmentRole.START, weightedRandom)) {
            // Force-place START even though it might overflow (the area is just too small);
            // this is a configuration error rather than a recoverable case.
            placeRoleForced(placed, cursor, SegmentRole.START, weightedRandom);
        }

        int placedCheckpoints = 0;
        for (int i = 0; i < checkpoints; i++) {
            // Try MOVE + QUESTION pair within area; if not, break early.
            if (!placeMoveStretchIfFits(placed, cursor, area, weightedRandom)) break;
            if (!placeRoleIfFits(placed, cursor, area, SegmentRole.QUESTION, weightedRandom)) break;
            placedCheckpoints++;
        }

        // Trailing buffer move + GOAL.
        placeMoveStretchIfFits(placed, cursor, area, weightedRandom);
        PlacedSegment goal = null;
        if (fits(library.pickFirst(SegmentRole.GOAL), cursor, area)) {
            goal = placeRoleForced(placed, cursor, SegmentRole.GOAL, weightedRandom);
        } else {
            // Last resort: force-place GOAL anyway (overflow at the very end is preferable to no goal).
            goal = placeRoleForced(placed, cursor, SegmentRole.GOAL, weightedRandom);
        }

        Location startBoarding = computeStartBoarding(placed.get(0));
        Location goalCenter = goal != null ? computeGoalCenter(goal) : null;
        return new Track(world, forward, startBoarding, goalCenter, placed);
    }

    /**
     * Builds a minimal survival starting track: just the START segment. The {@link
     * me.f0reach.mathgo.game.SurvivalDirector} takes over and extends with the perimeter loop.
     */
    public Track buildSurvivalStartOnly(World world, Area area, Location areaOrigin, Direction forward,
                                         boolean weightedRandom) {
        List<PlacedSegment> placed = new ArrayList<>();
        Cursor cursor = new Cursor(areaOrigin.clone(), forward);
        if (!placeRoleIfFits(placed, cursor, area, SegmentRole.START, weightedRandom)) {
            placeRoleForced(placed, cursor, SegmentRole.START, weightedRandom);
        }
        Location startBoarding = computeStartBoarding(placed.get(0));
        return new Track(world, forward, startBoarding, null, placed);
    }

    public Track buildSurvival(World world, Area area, Location areaOrigin, Direction forward, int questionsLimit,
                               boolean weightedRandom) {
        List<PlacedSegment> placed = new ArrayList<>();
        Cursor cursor = new Cursor(areaOrigin.clone(), forward);

        if (!placeRoleIfFits(placed, cursor, area, SegmentRole.START, weightedRandom)) {
            placeRoleForced(placed, cursor, SegmentRole.START, weightedRandom);
        }
        for (int i = 0; i < questionsLimit; i++) {
            if (!placeMoveStretchIfFits(placed, cursor, area, weightedRandom)) break;
            if (!placeRoleIfFits(placed, cursor, area, SegmentRole.QUESTION, weightedRandom)) break;
        }
        // Trailing buffer.
        placeMoveStretchIfFits(placed, cursor, area, weightedRandom);

        Location startBoarding = computeStartBoarding(placed.get(0));
        return new Track(world, forward, startBoarding, null, placed);
    }

    /**
     * Try to place a MOVE stretch (optional curve + mandatory straight), all within the area.
     * Returns false if even the mandatory straight does not fit.
     */
    private boolean placeMoveStretchIfFits(List<PlacedSegment> placed, Cursor cursor, Area area,
                                            boolean weightedRandom) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        SegmentTemplate move = weightedRandom ? library.pickWeighted(SegmentRole.MOVE)
                : library.pickFirst(SegmentRole.MOVE);
        if (rng.nextDouble() < CURVE_PROBABILITY) {
            CurveTemplate.Turn turn = cursor.budget.chooseTurn(rng);
            if (turn != null) {
                SegmentTemplate curve = new CurveTemplate(turn);
                if (fits(curve, cursor, area)
                        && fitsAfterCurve(curve, cursor, move, area)) {
                    PlacedSegment seg = curve.place(cursor.location.clone(), cursor.direction);
                    placed.add(seg);
                    cursor.location = seg.exitLocation();
                    cursor.direction = seg.exitDirection();
                    cursor.budget.record(turn);
                }
                // If the curve (or the straight that follows it) does not fit, skip the curve
                // and place a straight only at the original cursor.
            }
        }
        if (!fits(move, cursor, area)) {
            return false;
        }
        PlacedSegment moveSeg = move.place(cursor.location.clone(), cursor.direction);
        placed.add(moveSeg);
        cursor.location = moveSeg.exitLocation();
        cursor.direction = moveSeg.exitDirection();
        return true;
    }

    /** Dry-run check: does {@code after} fit at the cursor that would result from placing {@code curve}? */
    private static boolean fitsAfterCurve(SegmentTemplate curve, Cursor cursor, SegmentTemplate after, Area area) {
        LocalAnchor anchor = curve.exit();
        Location virtualEntry = anchor.toWorld(cursor.location, cursor.direction);
        Direction virtualDir = anchor.worldOutDir(cursor.direction);
        WorldAABB box = after.footprint().toWorldAabb(virtualEntry, virtualDir);
        return area.contains(box);
    }

    private boolean placeRoleIfFits(List<PlacedSegment> placed, Cursor cursor, Area area, SegmentRole role,
                                     boolean weightedRandom) {
        SegmentTemplate template = weightedRandom ? library.pickWeighted(role) : library.pickFirst(role);
        if (!fits(template, cursor, area)) return false;
        PlacedSegment seg = template.place(cursor.location.clone(), cursor.direction);
        placed.add(seg);
        cursor.location = seg.exitLocation();
        cursor.direction = seg.exitDirection();
        return true;
    }

    private PlacedSegment placeRoleForced(List<PlacedSegment> placed, Cursor cursor, SegmentRole role,
                                          boolean weightedRandom) {
        SegmentTemplate template = weightedRandom ? library.pickWeighted(role) : library.pickFirst(role);
        PlacedSegment seg = template.place(cursor.location.clone(), cursor.direction);
        placed.add(seg);
        cursor.location = seg.exitLocation();
        cursor.direction = seg.exitDirection();
        return seg;
    }

    private static boolean fits(SegmentTemplate template, Cursor cursor, Area area) {
        WorldAABB box = template.footprint().toWorldAabb(cursor.location, cursor.direction);
        return area.contains(box);
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
