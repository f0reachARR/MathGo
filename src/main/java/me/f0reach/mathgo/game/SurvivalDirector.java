package me.f0reach.mathgo.game;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.area.Area;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import me.f0reach.mathgo.track.TemplateLibrary;
import me.f0reach.mathgo.track.Track;
import me.f0reach.mathgo.track.WorldAABB;
import me.f0reach.mathgo.track.codegen.CurveTemplate;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives survival mode: maintains a sliding-window track by extending the front and pruning the back
 * as the player passes QUESTION zones. Generation respects the reserved {@link Area} and avoids
 * geometric overlap with currently-alive segments; loops over previously-cleaned ground are allowed.
 */
public final class SurvivalDirector {
    /** Try to keep at least this many questions placed beyond the most recently entered one. */
    private static final int LOOKAHEAD_QUESTIONS = 1;
    private static final double CURVE_PROBABILITY = 0.35;
    private static final int MAX_SAME_TURN_STREAK = 2;

    private final MathGoPlugin plugin;
    private final Area area;
    private final TemplateLibrary library;
    private final Track track;
    private final boolean weightedRandom;

    private Location cursorLocation;
    private Direction cursorDirection;
    private final CurveBudget budget = new CurveBudget();

    @Nullable private PlacedSegment lastEnteredQuestion;
    private int placedQuestions;
    private int enteredQuestions;
    private boolean deadEnd;

    public SurvivalDirector(MathGoPlugin plugin, Area area, TemplateLibrary library, Track track,
                             Location cursorLocation, Direction cursorDirection, boolean weightedRandom) {
        this.plugin = plugin;
        this.area = area;
        this.library = library;
        this.track = track;
        this.cursorLocation = cursorLocation;
        this.cursorDirection = cursorDirection;
        this.weightedRandom = weightedRandom;
        this.placedQuestions = track.questionSegments().size();
    }

    public boolean isDeadEnd() { return deadEnd; }
    public int placedQuestions() { return placedQuestions; }
    public int enteredQuestions() { return enteredQuestions; }

    /**
     * Hook called by GameLoop when the player's cart enters QUESTION segment {@code justEntered}.
     * Prunes segments behind the previous question and tries to top up lookahead.
     */
    public void onQuestionEntered(PlacedSegment justEntered) {
        PlacedSegment prev = lastEnteredQuestion;
        lastEnteredQuestion = justEntered;
        enteredQuestions++;
        if (prev != null) {
            track.pruneBeforeSegment(prev);
        }
        while (!deadEnd && (placedQuestions - enteredQuestions) < LOOKAHEAD_QUESTIONS) {
            if (!tryExtendOneCheckpoint()) {
                deadEnd = true;
                break;
            }
        }
    }

    /** Pre-extend the initial track until lookahead is reached, called once after construction. */
    public void primeInitialLookahead() {
        while (!deadEnd && (placedQuestions - enteredQuestions) < LOOKAHEAD_QUESTIONS + 1) {
            if (!tryExtendOneCheckpoint()) {
                deadEnd = true;
                break;
            }
        }
    }

    /** Try to append MOVE-stretch + QUESTION. Returns false if either cannot fit. */
    private boolean tryExtendOneCheckpoint() {
        if (!tryAppendMoveStretch()) return false;
        if (!tryAppendRole(SegmentRole.QUESTION)) return false;
        placedQuestions++;
        return true;
    }

    private boolean tryAppendMoveStretch() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        SegmentTemplate move = weightedRandom ? library.pickWeighted(SegmentRole.MOVE)
                : library.pickFirst(SegmentRole.MOVE);
        // Save cursor in case we tentatively choose a curve that turns out not to fit.
        Location savedLoc = cursorLocation.clone();
        Direction savedDir = cursorDirection;
        if (rng.nextDouble() < CURVE_PROBABILITY) {
            CurveTemplate.Turn turn = budget.chooseTurn(rng);
            if (turn != null) {
                SegmentTemplate curve = new CurveTemplate(turn);
                if (fits(curve) && fitsAfterCurve(curve, move)) {
                    PlacedSegment seg = curve.place(cursorLocation.clone(), cursorDirection);
                    track.appendSegment(seg);
                    cursorLocation = seg.exitLocation();
                    cursorDirection = seg.exitDirection();
                    budget.record(turn);
                }
            }
        }
        if (!fits(move)) {
            // Rollback (no curve was placed in the rejected branch; only commit nothing).
            cursorLocation = savedLoc;
            cursorDirection = savedDir;
            return false;
        }
        PlacedSegment seg = move.place(cursorLocation.clone(), cursorDirection);
        track.appendSegment(seg);
        cursorLocation = seg.exitLocation();
        cursorDirection = seg.exitDirection();
        return true;
    }

    private boolean tryAppendRole(SegmentRole role) {
        SegmentTemplate template = weightedRandom ? library.pickWeighted(role) : library.pickFirst(role);
        if (!fits(template)) return false;
        PlacedSegment seg = template.place(cursorLocation.clone(), cursorDirection);
        track.appendSegment(seg);
        cursorLocation = seg.exitLocation();
        cursorDirection = seg.exitDirection();
        return true;
    }

    private boolean fits(SegmentTemplate template) {
        WorldAABB box = template.footprint().toWorldAabb(cursorLocation, cursorDirection);
        if (!area.contains(box)) return false;
        return !track.intersectsActive(box, lastActive());
    }

    private boolean fitsAfterCurve(SegmentTemplate curve, SegmentTemplate after) {
        Location virtualEntry = curve.exit().toWorld(cursorLocation, cursorDirection);
        Direction virtualDir = curve.exit().worldOutDir(cursorDirection);
        WorldAABB box = after.footprint().toWorldAabb(virtualEntry, virtualDir);
        if (!area.contains(box)) return false;
        // The candidate "after" sits past the curve we'd place, which itself doesn't exist yet —
        // collision check against currently-alive segments (excluding the most recent, which would
        // become the curve's predecessor) is sufficient because adjacent AABBs are non-overlapping.
        return !track.intersectsActive(box, lastActive());
    }

    @Nullable
    private PlacedSegment lastActive() {
        var segs = track.segments();
        if (segs.isEmpty()) return null;
        return segs.get(segs.size() - 1);
    }

    /** Survival curve budget: allows full rotation (loops are fine after cleanup) but caps same-direction streaks. */
    private static final class CurveBudget {
        int consecutiveSame = 0;
        @Nullable CurveTemplate.Turn lastTurn = null;

        @Nullable CurveTemplate.Turn chooseTurn(ThreadLocalRandom rng) {
            EnumSet<CurveTemplate.Turn> allowed = EnumSet.allOf(CurveTemplate.Turn.class);
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
        }
    }
}
