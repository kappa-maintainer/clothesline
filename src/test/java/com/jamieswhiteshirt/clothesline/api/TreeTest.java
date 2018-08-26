package com.jamieswhiteshirt.clothesline.api;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

class TreeTest {
    @Test
    void buildsSimplePath() {
        BlockPos posA = new BlockPos(0, 0, 0);
        BlockPos posB = new BlockPos(1, 0, 0);
        Tree tree = new Tree(
            posA, Collections.singletonList(new Tree.Edge(
                posB.subtract(posA),
                AttachmentUnit.lengthBetween(posA, posB),
                0,
                Tree.empty(posB, AttachmentUnit.UNITS_PER_BLOCK, 0)
            )), 0, AttachmentUnit.UNITS_PER_BLOCK * 2, 0
        );

        Path path = tree.buildPath();

        Path.Edge a_b = new Path.Edge(posB.subtract(posA), new Line(posA, posB), AttachmentUnit.UNITS_PER_BLOCK * 0, AttachmentUnit.UNITS_PER_BLOCK * 1);
        Path.Edge b_a = new Path.Edge(posA.subtract(posB), new Line(posB, posA), AttachmentUnit.UNITS_PER_BLOCK * 1, AttachmentUnit.UNITS_PER_BLOCK * 2);
        Path.Node a = new Path.Node(posA, Collections.singletonList(a_b), 0);
        Path.Node b = new Path.Node(posB, Collections.singletonList(b_a), 0);

        Assertions.assertEquals(path.getNodes().keySet(), new HashSet<>(Arrays.asList(posA, posB)));
        Assertions.assertEquals(path.getNodes().get(posA), a);
        Assertions.assertEquals(path.getNodes().get(posB), b);
        Assertions.assertEquals(path.getEdges(), Arrays.asList(a_b, b_a));
    }
}
