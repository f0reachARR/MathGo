package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.track.Direction;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.jetbrains.annotations.Nullable;

/**
 * Marker block schema for NBT templates. Markers carry semantic metadata (entry/exit/anchor) by their
 * material; facing direction is encoded in the block's {@link Directional#getFacing()}.
 */
public enum MarkerBlocks {
    ENTRY(Material.MAGENTA_GLAZED_TERRACOTTA),
    EXIT(Material.LIME_GLAZED_TERRACOTTA),
    QUESTION_STOP(Material.YELLOW_GLAZED_TERRACOTTA),
    QUESTION_DISPLAY(Material.LIGHT_BLUE_GLAZED_TERRACOTTA);

    private final Material material;

    MarkerBlocks(Material material) {
        this.material = material;
    }

    public Material material() { return material; }

    @Nullable
    public static MarkerBlocks of(Material material) {
        for (MarkerBlocks m : values()) {
            if (m.material == material) return m;
        }
        return null;
    }

    public static boolean isMarker(Material material) {
        return of(material) != null;
    }

    /** Create a glazed-terracotta block data with the given facing. */
    public BlockData dataFacing(Direction dir) {
        BlockData data = material.createBlockData();
        if (data instanceof Directional d) {
            d.setFacing(toBlockFace(dir));
            return d;
        }
        return data;
    }

    public static Direction directionFromFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static BlockFace toBlockFace(Direction dir) {
        return switch (dir) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
        };
    }
}
