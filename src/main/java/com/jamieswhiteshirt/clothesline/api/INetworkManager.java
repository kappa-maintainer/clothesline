package com.jamieswhiteshirt.clothesline.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface INetworkManager {
    Collection<Network> getNetworks();

    @Nullable
    Network getNetworkByUUID(UUID uuid);

    @Nullable
    Network getNetworkByBlockPos(BlockPos pos);

    void addNetwork(Network network);

    void removeNetwork(UUID networkUuid);

    void setNetworks(Map<UUID, Network> networks);

    void update();

    boolean connect(BlockPos from, BlockPos to);

    void destroy(BlockPos pos);

    ItemStack insertItem(Network network, int offset, ItemStack stack, boolean simulate);

    ItemStack extractItem(Network network, int offset, boolean simulate);

    void setItem(Network network, int offset, ItemStack stack);

    void removeItem(Network network, int offset);

    void addMomentum(Network network, int momentum);
}
