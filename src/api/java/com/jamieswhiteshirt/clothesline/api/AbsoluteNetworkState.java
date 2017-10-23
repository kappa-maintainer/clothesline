package com.jamieswhiteshirt.clothesline.api;

import com.jamieswhiteshirt.clothesline.api.util.MutableSortedIntMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * State container for a network.
 * The structure of the network is immutable, while the attachments and their offsets are mutable.
 * Performant for manipulation of attachments on the network.
 */
public final class AbsoluteNetworkState {
    private static final int MAX_MOMENTUM = 40;

    private int previousOffset;
    private int offset;
    private int momentum;

    private final AbsoluteTree tree;
    private final Map<BlockPos, AbsoluteTree> posLookup;
    private final MutableSortedIntMap<ItemStack> attachments;

    public static AbsoluteNetworkState createInitial(AbsoluteTree tree) {
        return new AbsoluteNetworkState(
                0,
                0,
                0,
                tree,
                MutableSortedIntMap.empty(tree.getMaxOffset())
        );
    }

    public AbsoluteNetworkState(int previousOffset, int offset, int momentum, AbsoluteTree tree, MutableSortedIntMap<ItemStack> attachments) {
        this.tree = tree;
        this.posLookup = tree.createPositionLookup();
        this.attachments = attachments;
        this.previousOffset = previousOffset;
        this.offset = offset;
        this.momentum = momentum;
    }

    public AbsoluteTree getTree() {
        return tree;
    }

    public AbsoluteTree getSubTree(BlockPos pos) {
        return posLookup.get(pos);
    }

    public MutableSortedIntMap<ItemStack> getAttachments() {
        return attachments;
    }

    public ItemStack getAttachment(int offset) {
        ItemStack result = attachments.get(offset);
        if (result != null) {
            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void setAttachment(int offset, ItemStack stack) {
        if (stack.isEmpty()) {
            attachments.remove(offset);
        } else {
            attachments.put(offset, stack);
        }
    }

    public void update() {
        if (momentum > 0) {
            momentum -= 1;
        } else if (momentum < 0) {
            momentum += 1;
        }

        previousOffset = offset;
        offset += momentum;
    }

    public void addMomentum(int momentum) {
        this.momentum = Math.min(Math.max(this.momentum + momentum, -MAX_MOMENTUM), MAX_MOMENTUM);
    }

    public int getOffset() {
        return offset;
    }

    public int getPreviousOffset() {
        return previousOffset;
    }

    public int getMomentum() {
        return momentum;
    }

    public int getLoopLength() {
        return tree.getLoopLength();
    }
}
