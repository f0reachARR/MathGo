package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.LocalAnchor;
import me.f0reach.mathgo.track.LocalFootprint;
import me.f0reach.mathgo.track.PlacedSegment;
import me.f0reach.mathgo.track.QuestionAnchors;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.SegmentTemplate;
import me.f0reach.mathgo.track.WorldAABB;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SegmentTemplate backed by a Paper {@link Structure} (.nbt). Metadata (entry/exit/anchor positions and
 * facings) was extracted at load time by scanning marker blocks; markers are stripped from the placement
 * by {@link MarkerStripper}.
 */
public final class NbtTemplate implements SegmentTemplate {
    private final String id;
    private final SegmentRole role;
    private final int weight;
    private final Structure structure;
    private final BlockVector structureSize;
    private final BlockVector entryLocal;
    private final Direction entryLocalForward;
    private final BlockVector exitLocal;
    private final Direction exitLocalForward;
    @Nullable private final BlockVector stopLocal;
    @Nullable private final BlockVector displayLocal;
    private final LocalFootprint footprint;
    private final LocalAnchor exitAnchor;

    public NbtTemplate(String id, SegmentRole role, int weight,
                       Structure structure, BlockVector structureSize,
                       BlockVector entryLocal, Direction entryLocalForward,
                       BlockVector exitLocal, Direction exitLocalForward,
                       @Nullable BlockVector stopLocal,
                       @Nullable BlockVector displayLocal) {
        this.id = id;
        this.role = role;
        this.weight = weight;
        this.structure = structure;
        this.structureSize = structureSize;
        this.entryLocal = entryLocal;
        this.entryLocalForward = entryLocalForward;
        this.exitLocal = exitLocal;
        this.exitLocalForward = exitLocalForward;
        this.stopLocal = stopLocal;
        this.displayLocal = displayLocal;
        this.footprint = computeFootprint();
        this.exitAnchor = computeExitAnchor();
    }

    @Override public String id() { return id; }
    @Override public SegmentRole role() { return role; }
    @Override public int weight() { return weight; }
    @Override public LocalFootprint footprint() { return footprint; }
    @Override public LocalAnchor exit() { return exitAnchor; }

    @Override
    public PlacedSegment place(Location entryWorld, Direction forward) {
        World world = entryWorld.getWorld();
        StructureRotation rotation = rotationFor(entryLocalForward, forward);
        // Origin so that the entry marker's rotated world offset lands on entryWorld.
        int[] rotatedEntry = rotateLocal(entryLocal.getBlockX(), entryLocal.getBlockY(), entryLocal.getBlockZ(),
                rotation, structureSize.getBlockX(), structureSize.getBlockY(), structureSize.getBlockZ());
        Location origin = new Location(world,
                entryWorld.getBlockX() - rotatedEntry[0],
                entryWorld.getBlockY() - rotatedEntry[1],
                entryWorld.getBlockZ() - rotatedEntry[2]);

        structure.place(origin, false, rotation, Mirror.NONE, -1, 1.0f,
                ThreadLocalRandom.current(),
                List.of(MarkerStripper.INSTANCE),
                List.of());

        Location exitWorld = worldOfLocalCell(origin, exitLocal, rotation);
        Direction exitDir = rotateFacing(exitLocalForward, rotation);

        QuestionAnchors anchors = null;
        if (stopLocal != null) {
            Location stopBlock = worldOfLocalCell(origin, stopLocal, rotation);
            Location stop = stopBlock.clone().add(0.5, 0.0625, 0.5);
            Location display = (displayLocal != null)
                    ? worldOfLocalCell(origin, displayLocal, rotation).clone().add(0.5, 0.0625, 0.5)
                    : stop.clone().add(0, 2.0, 0);
            anchors = new QuestionAnchors(stop, display);
        }

        WorldAABB worldFootprint = computeWorldAabb(origin, structureSize, rotation);
        return new PlacedSegment(this, entryWorld, forward, exitWorld, exitDir, worldFootprint, anchors);
    }

    private Location worldOfLocalCell(Location origin, BlockVector localCell, StructureRotation r) {
        int[] rot = rotateLocal(localCell.getBlockX(), localCell.getBlockY(), localCell.getBlockZ(),
                r, structureSize.getBlockX(), structureSize.getBlockY(), structureSize.getBlockZ());
        return new Location(origin.getWorld(),
                origin.getBlockX() + rot[0],
                origin.getBlockY() + rot[1],
                origin.getBlockZ() + rot[2]);
    }

    private LocalFootprint computeFootprint() {
        Direction right = entryLocalForward.rotateRight();
        int sizeX = structureSize.getBlockX();
        int sizeY = structureSize.getBlockY();
        int sizeZ = structureSize.getBlockZ();
        int entryX = entryLocal.getBlockX();
        int entryY = entryLocal.getBlockY();
        int entryZ = entryLocal.getBlockZ();
        int minF = Integer.MAX_VALUE, maxF = Integer.MIN_VALUE;
        int minS = Integer.MAX_VALUE, maxS = Integer.MIN_VALUE;
        for (int lx : new int[]{0, sizeX - 1}) {
            for (int lz : new int[]{0, sizeZ - 1}) {
                int dx = lx - entryX, dz = lz - entryZ;
                int f = dx * entryLocalForward.dx() + dz * entryLocalForward.dz();
                int s = dx * right.dx() + dz * right.dz();
                if (f < minF) minF = f;
                if (f > maxF) maxF = f;
                if (s < minS) minS = s;
                if (s > maxS) maxS = s;
            }
        }
        int minY = -entryY;
        int maxY = sizeY - 1 - entryY;
        return new LocalFootprint(minF, maxF, minS, maxS, minY, maxY);
    }

    private LocalAnchor computeExitAnchor() {
        Direction right = entryLocalForward.rotateRight();
        int dx = exitLocal.getBlockX() - entryLocal.getBlockX();
        int dz = exitLocal.getBlockZ() - entryLocal.getBlockZ();
        int fcells = dx * entryLocalForward.dx() + dz * entryLocalForward.dz();
        int scells = dx * right.dx() + dz * right.dz();
        // outDir in local frame: rotate so that entryLocalForward maps to NORTH.
        int rights = (exitLocalForward.ordinal() - entryLocalForward.ordinal() + 4) % 4;
        Direction outLocal = Direction.NORTH;
        for (int i = 0; i < rights; i++) outLocal = outLocal.rotateRight();
        return new LocalAnchor(fcells, scells, outLocal);
    }

    private static StructureRotation rotationFor(Direction from, Direction to) {
        int diff = (to.ordinal() - from.ordinal() + 4) % 4;
        return switch (diff) {
            case 0 -> StructureRotation.NONE;
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            case 3 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }

    private static Direction rotateFacing(Direction localFacing, StructureRotation r) {
        return switch (r) {
            case NONE -> localFacing;
            case CLOCKWISE_90 -> localFacing.rotateRight();
            case CLOCKWISE_180 -> localFacing.opposite();
            case COUNTERCLOCKWISE_90 -> localFacing.rotateLeft();
        };
    }

    private static int[] rotateLocal(int lx, int ly, int lz, StructureRotation r,
                                      int sizeX, int sizeY, int sizeZ) {
        return switch (r) {
            case NONE -> new int[]{lx, ly, lz};
            case CLOCKWISE_90 -> new int[]{sizeZ - 1 - lz, ly, lx};
            case CLOCKWISE_180 -> new int[]{sizeX - 1 - lx, ly, sizeZ - 1 - lz};
            case COUNTERCLOCKWISE_90 -> new int[]{lz, ly, sizeX - 1 - lx};
        };
    }

    private static WorldAABB computeWorldAabb(Location origin, BlockVector size, StructureRotation r) {
        int sizeX = size.getBlockX();
        int sizeY = size.getBlockY();
        int sizeZ = size.getBlockZ();
        int rsx = (r == StructureRotation.CLOCKWISE_90 || r == StructureRotation.COUNTERCLOCKWISE_90)
                ? sizeZ : sizeX;
        int rsz = (r == StructureRotation.CLOCKWISE_90 || r == StructureRotation.COUNTERCLOCKWISE_90)
                ? sizeX : sizeZ;
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();
        return new WorldAABB(x, y, z, x + rsx - 1, y + sizeY - 1, z + rsz - 1);
    }
}
