package com.jamieswhiteshirt.clothesline.common.impl;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.jamieswhiteshirt.clothesline.api.INetwork;
import com.jamieswhiteshirt.clothesline.api.INetworkCollection;
import com.jamieswhiteshirt.clothesline.api.PersistentNetwork;
import com.jamieswhiteshirt.clothesline.common.util.ISpanFunction;
import com.jamieswhiteshirt.clothesline.internal.INetworkProvider;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public final class NetworkProvider implements INetworkProvider {
    private final INetworkCollection networks;
    private final ISpanFunction spanFunction;
    private final BiPredicate<Integer, Integer> isChunkLoaded;
    private Map<UUID, NetworkProviderEntry> entryMap = new HashMap<>();
    private SetMultimap<Long, UUID> chunkMap = MultimapBuilder.hashKeys().linkedHashSetValues().build();
    private int nextNetworkId = 0;

    public NetworkProvider(INetworkCollection networks, ISpanFunction spanFunction, BiPredicate<Integer, Integer> isChunkLoaded) {
        this.networks = networks;
        this.spanFunction = spanFunction;
        this.isChunkLoaded = isChunkLoaded;
    }

    private void chunkLoaded(NetworkProviderEntry entry) {
        if (entry.incrementLoadCount()) {
            Network network = new Network(nextNetworkId++, entry.getPersistentNetwork());
            networks.add(network);
        }
    }

    @Override
    public void reset(Collection<PersistentNetwork> persistentNetworks) {
        for (UUID uuid : entryMap.keySet()) {
            INetwork network = networks.getByUuid(uuid);
            if (network != null) networks.remove(network);
        }

        nextNetworkId = 0;
        entryMap = new HashMap<>();
        chunkMap = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        for (PersistentNetwork persistentNetwork : persistentNetworks) {
            addNetwork(persistentNetwork);
        }
    }

    @Override
    public Collection<PersistentNetwork> getNetworks() {
        return entryMap.values().stream().map(NetworkProviderEntry::getPersistentNetwork).collect(Collectors.toList());
    }

    @Override
    public void addNetwork(PersistentNetwork persistentNetwork) {
        LongSet chunks = spanFunction.getChunkSpanOfNetwork(persistentNetwork.getState());
        NetworkProviderEntry entry = new NetworkProviderEntry(persistentNetwork, chunks);
        entryMap.put(persistentNetwork.getUuid(), entry);
        for (long position : entry.getChunkSpan()) {
            chunkMap.put(position, persistentNetwork.getUuid());
            // Increment load count if this network spans an already loaded chunk
            if (isChunkLoaded.test(ISpanFunction.chunkX(position), ISpanFunction.chunkZ(position))) {
                chunkLoaded(entry);
            }
        }
    }

    @Override
    public void removeNetwork(UUID uuid) {
        NetworkProviderEntry entry = entryMap.remove(uuid);
        for (long position : entry.getChunkSpan()) {
            chunkMap.remove(position, uuid);
        }

        // Remove network if it is loaded
        networks.removeByUuid(uuid);
    }

    @Override
    public void onChunkLoaded(int x, int z) {
        long position = ISpanFunction.chunkPosition(x, z);
        for (UUID uuid : chunkMap.get(position)) {
            NetworkProviderEntry entry = entryMap.get(uuid);
            chunkLoaded(entry);
        }
    }

    @Override
    public void onChunkUnloaded(int x, int z) {
        long position = ISpanFunction.chunkPosition(x, z);
        for (UUID uuid : chunkMap.get(position)) {
            NetworkProviderEntry entry = entryMap.get(uuid);
            if (entry.decrementLoadCount()) {
                networks.removeByUuid(uuid);
            }
        }
    }
}
