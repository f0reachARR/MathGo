package me.f0reach.mathgo.track.template;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.util.BlockTransformer;
import org.jetbrains.annotations.NotNull;

/** Replaces marker glazed-terracotta blocks with AIR during structure placement. */
public final class MarkerStripper implements BlockTransformer {
    public static final MarkerStripper INSTANCE = new MarkerStripper();

    private MarkerStripper() {}

    @Override
    public @NotNull BlockState transform(@NotNull LimitedRegion region, int x, int y, int z,
                                          @NotNull BlockState current, @NotNull TransformationState state) {
        Material mat = current.getType();
        if (MarkerBlocks.isMarker(mat)) {
            return Material.AIR.createBlockData().createBlockState();
        }
        return current;
    }
}
