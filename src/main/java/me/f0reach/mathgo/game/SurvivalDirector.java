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

/**
 * Drives survival mode with a circular (rectangular-loop) layout: the cart turns in a single fixed
 * direction every {@code straightsPerSide} MOVE placements, tracing a perimeter that fits inside the
 * reserved {@link Area}. As the player passes QUESTION zones, segments behind are pruned and new
 * ones are appended at the front, so the cart can loop indefinitely.
 */
public final class SurvivalDirector {
    /**
     * Target lookahead: how many questions to keep placed beyond the most recently entered one.
     * Sized so the player always sees a visible stretch of corridor ahead of the cart (each MOVE+QUESTION
     * pair is ~14 cells, so 3 → ~40+ cells of corridor ahead).
     */
    private static final int LOOKAHEAD_QUESTIONS = 3;
    /**
     * Empirical estimate of forward cells consumed per (MOVE + QUESTION) pair, used to size the
     * loop side so the rectangle fits inside the area.
     */
    private static final int PAIR_FORWARD_CELLS = 14;
    /** Extra margin reserved at the edges of the area to accommodate corner curves and walls. */
    private static final int LOOP_MARGIN = 8;

    private final MathGoPlugin plugin;
    private final Area area;
    private final TemplateLibrary library;
    private final Track track;
    private final boolean weightedRandom;

    private Location cursorLocation;
    private Direction cursorDirection;

    private final CurveTemplate.Turn loopTurn;
    private final int straightsPerSide;
    private int straightsSinceCurve;

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
        // RIGHT is the correct direction given that TrackBuilder starts at the NW corner facing EAST:
        // right turns route the cart inward (E→S→W→N→E), tracing the area's perimeter.
        this.loopTurn = CurveTemplate.Turn.RIGHT;
        this.straightsPerSide = Math.max(1, (area.size() - LOOP_MARGIN) / PAIR_FORWARD_CELLS);
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

    /**
     * Places a corner curve (in the fixed {@link #loopTurn} direction) when the side quota is reached,
     * followed by exactly one MOVE template. When the quota is not yet reached, places only a MOVE.
     */
    private boolean tryAppendMoveStretch() {
        SegmentTemplate move = weightedRandom ? library.pickWeighted(SegmentRole.MOVE)
                : library.pickFirst(SegmentRole.MOVE);
        if (straightsSinceCurve >= straightsPerSide) {
            SegmentTemplate curve = new CurveTemplate(loopTurn);
            if (fits(curve) && fitsAfterCurve(curve, move)) {
                PlacedSegment curveSeg = curve.place(cursorLocation.clone(), cursorDirection);
                track.appendSegment(curveSeg);
                cursorLocation = curveSeg.exitLocation();
                cursorDirection = curveSeg.exitDirection();
                straightsSinceCurve = 0;
            }
            // If the curve doesn't fit (geometry mismatch with custom templates / NBT segments),
            // fall through and attempt a straight; the next call may succeed at the curve.
        }
        if (!fits(move)) return false;
        PlacedSegment moveSeg = move.place(cursorLocation.clone(), cursorDirection);
        track.appendSegment(moveSeg);
        cursorLocation = moveSeg.exitLocation();
        cursorDirection = moveSeg.exitDirection();
        straightsSinceCurve++;
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
        return !track.intersectsActive(box, lastActive());
    }

    @Nullable
    private PlacedSegment lastActive() {
        var segs = track.segments();
        if (segs.isEmpty()) return null;
        return segs.get(segs.size() - 1);
    }
}
