package me.f0reach.mathgo.track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TemplateLibrary {
    private final List<SegmentTemplate> all = new ArrayList<>();

    public void register(SegmentTemplate template) {
        all.add(template);
    }

    public SegmentTemplate pickFirst(SegmentRole role) {
        for (SegmentTemplate t : all) {
            if (t.role() == role) return t;
        }
        throw new IllegalStateException("No template registered for role: " + role);
    }

    public SegmentTemplate pickWeighted(SegmentRole role) {
        List<SegmentTemplate> candidates = new ArrayList<>();
        int total = 0;
        for (SegmentTemplate t : all) {
            if (t.role() == role) {
                candidates.add(t);
                total += Math.max(1, t.weight());
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No template registered for role: " + role);
        }
        int pick = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (SegmentTemplate t : candidates) {
            acc += Math.max(1, t.weight());
            if (pick < acc) return t;
        }
        return candidates.get(candidates.size() - 1);
    }
}
