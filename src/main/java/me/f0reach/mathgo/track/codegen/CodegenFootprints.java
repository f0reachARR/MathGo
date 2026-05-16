package me.f0reach.mathgo.track.codegen;

import me.f0reach.mathgo.track.LocalFootprint;

final class CodegenFootprints {
    private CodegenFootprints() {}

    /**
     * Footprint of a straight corridor of the given length (cells along forward) and width (cross-section, odd).
     * F range = [0, length-1], S range = [-(width-1)/2, +(width-1)/2], Y range = [-1, +2] (floor to ceiling).
     */
    static LocalFootprint straight(int length, int width) {
        int half = (width - 1) / 2;
        return new LocalFootprint(0, length - 1, -half, +half, -1, +2);
    }
}
