package com.jamieswhiteshirt.clothesline.api;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface IServerNetworkManager extends INetworkManager<INetworkEdge, INetworkNode> {
    void reset(List<PersistentNetwork> networks);

    @Nullable
    INetwork getNetworkByUuid(UUID uuid);

    void disconnect(BlockPos posA, BlockPos posB);
}
