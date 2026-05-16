package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.track.Direction;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

/** Per-player editing state for an in-progress NBT template. */
public final class TemplateDraft {
    @Nullable private Location pos1;
    @Nullable private Location pos2;
    @Nullable private BlockVector entry;
    @Nullable private Direction entryFacing;
    @Nullable private BlockVector exit;
    @Nullable private Direction exitFacing;
    @Nullable private BlockVector stop;
    @Nullable private BlockVector display;

    public @Nullable Location pos1() { return pos1; }
    public @Nullable Location pos2() { return pos2; }
    public @Nullable BlockVector entry() { return entry; }
    public @Nullable Direction entryFacing() { return entryFacing; }
    public @Nullable BlockVector exit() { return exit; }
    public @Nullable Direction exitFacing() { return exitFacing; }
    public @Nullable BlockVector stop() { return stop; }
    public @Nullable BlockVector display() { return display; }

    public void setPos1(Location loc) { this.pos1 = loc; }
    public void setPos2(Location loc) { this.pos2 = loc; }
    public void setEntry(BlockVector v, Direction d) { this.entry = v; this.entryFacing = d; }
    public void setExit(BlockVector v, Direction d) { this.exit = v; this.exitFacing = d; }
    public void setStop(BlockVector v) { this.stop = v; }
    public void setDisplay(BlockVector v) { this.display = v; }

    public void clear() {
        pos1 = pos2 = null;
        entry = exit = stop = display = null;
        entryFacing = exitFacing = null;
    }
}
