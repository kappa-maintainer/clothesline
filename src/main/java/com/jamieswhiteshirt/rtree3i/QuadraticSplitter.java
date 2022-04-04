package com.jamieswhiteshirt.rtree3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * An O(n^4) time splitter with desirable results.
 */
public final class QuadraticSplitter implements Splitter {

    @Override
    public <T> Groups<T> split(List<T> items, int minSize, Function<T, Box> boxMapper) {
        Preconditions.checkArgument(items.size() >= 2);

        // according to
        // http://en.wikipedia.org/wiki/R-tree#Splitting_an_overflowing_node

        // find the worst combination pairwise in the list and use them to start
        // the two groups
        final Pair<T> worstCombination = worstCombination(items, boxMapper);

        // worst combination to have in the same node is now e1,e2.

        // establish a group around e1 and another group around e2
        final List<T> group1 = Lists.newArrayList(worstCombination.getValue1());
        final List<T> group2 = Lists.newArrayList(worstCombination.getValue2());

        final List<T> remaining = new ArrayList<>(items);
        remaining.remove(worstCombination.getValue1());
        remaining.remove(worstCombination.getValue2());

        final int minGroupSize = items.size() / 2;

        // now add the remainder to the groups using least mbb area increase
        // except in the case where minimumSize would be contradicted
        while (remaining.size() > 0) {
            assignRemaining(group1, group2, remaining, minGroupSize, boxMapper);
        }
        return new Groups<>(Group.of(group1, boxMapper), Group.of(group2, boxMapper));
    }

    private <T> void assignRemaining(final List<T> group1,
            final List<T> group2, final List<T> remaining, final int minGroupSize, Function<T, Box> key) {
        final Box mbb1 = Util.mbb(group1.stream().map(key).collect(Collectors.toList()));
        final Box mbb2 = Util.mbb(group2.stream().map(key).collect(Collectors.toList()));
        final T item1 = getBestCandidateForGroup(remaining, mbb1, key);
        final T item2 = getBestCandidateForGroup(remaining, mbb2, key);
        final boolean volume1LessThanVolume2 = key.apply(item1).add(mbb1).getVolume() <= key.apply(item2)
                .add(mbb2).getVolume();

        if (volume1LessThanVolume2 && (group2.size() + remaining.size() - 1 >= minGroupSize)
                || !volume1LessThanVolume2 && (group1.size() + remaining.size() == minGroupSize)) {
            group1.add(item1);
            remaining.remove(item1);
        } else {
            group2.add(item2);
            remaining.remove(item2);
        }
    }

    static <T> T getBestCandidateForGroup(List<T> list, Box groupMbb, Function<T, Box> key) {
        T minEntry = null;
        int minVolume = Integer.MAX_VALUE;
        for (final T entry : list) {
            final int volume = groupMbb.add(key.apply(entry)).getVolume();
            if (volume < minVolume) {
                minVolume = volume;
                minEntry = entry;
            }
        }
        return minEntry;
    }

    static <T> Pair<T> worstCombination(List<T> items, Function<T, Box> key) {
        T e1 = null;
        T e2 = null;
        int maxVolume = Integer.MIN_VALUE;
        for (final T entry1 : items) {
            for (final T entry2 : items) {
                if (entry1 != entry2) {
                    final int volume = key.apply(entry1).add(key.apply(entry2)).getVolume();
                    if (volume > maxVolume) {
                        e1 = entry1;
                        e2 = entry2;
                        maxVolume = volume;
                    }
                }
            }
        }

        return e1 != null ? new Pair<>(e1, e2) : new Pair<>(items.get(0), items.get(1));
    }
}
