/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.stats;

import com.google.common.collect.Iterables;
import io.prometheus.client.Collector;
import org.cubeengine.module.stats.PullGauge.Label;
import org.cubeengine.module.stats.PullGauge.LabeledValue;
import org.spongepowered.api.Server;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.DefaultedRegistryValue;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.cubeengine.module.stats.PullGauge.Label.label;
import static org.cubeengine.module.stats.PullGauge.LabeledValue.value;

public class SpongeCollector extends Collector {
    private final PullGaugeCollector<Server> server;

    private static final PullGauge<Server> PLAYERS = PullGauge.build("sponge_server_online_player_count", (Server s) -> s.getOnlinePlayers().size())
            .help("Total online players")
            .build();

    private static final PullGauge<Server> MAX_PLAYERS = PullGauge.build("sponge_server_max_player_count", Server::getMaxPlayers)
            .help("Maximum online players")
            .build();

    private static final PullGauge<Server> TPS = PullGauge.build("sponge_server_tps", Server::getTicksPerSecond)
            .unit("tps")
            .help("Server tick rate")
            .build();

    private static final PullGauge<ServerWorld> LOADED_CHUNKS = PullGauge.build("sponge_world_loaded_chunk_count", SpongeCollector::loadedChunkCount)
            .help("Chunks loaded per world")
            .build();

    private static final PullGauge<ServerWorld> ENTITIES = PullGauge.build("sponge_world_entity_count", SpongeCollector::entityCount)
            .help("Entities loaded per world")
            .build();

    private static final PullGauge<ServerWorld> BLOCK_ENTITIES = PullGauge.build("sponge_world_block_entity_count", SpongeCollector::blockEntityCount)
            .help("Block entities loaded per world")
            .build();

    private static final PullGauge<ServerWorld> WORLD_PLAYERS = PullGauge.build("sponge_world_player_count", SpongeCollector::playerCount)
            .help("Players online per world")
            .build();

    public SpongeCollector(Server server) {
        this.server = PullGaugeCollector.build(server)
                .withGauge(PLAYERS)
                .withGauge(MAX_PLAYERS)
                .withGauge(TPS)
                .withMultiGauge(LOADED_CHUNKS, SpongeCollector::worlds)
                .withMultiGauge(ENTITIES, SpongeCollector::worlds)
                .withMultiGauge(BLOCK_ENTITIES, SpongeCollector::worlds)
                .withMultiGauge(WORLD_PLAYERS, SpongeCollector::worlds)
                .build();
    }

    private static Iterable<ServerWorld> worlds(Server server) {
        return server.getWorldManager().worlds();
    }

    private static LabeledValue loadedChunkCount(ServerWorld world) {
        return value(Iterables.size(world.getLoadedChunks()), worldLabels(world));
    }

    private static LabeledValue playerCount(ServerWorld world) {
        return value(world.getPlayers().size(), worldLabels(world));
    }

    private static List<LabeledValue> blockEntityCount(ServerWorld world) {
        return countByType(world, world.getBlockEntities().stream(), BlockEntity::getType, RegistryTypes.BLOCK_ENTITY_TYPE);
    }

    private static List<LabeledValue> entityCount(ServerWorld world) {
        return countByType(world, world.getEntities().stream(), Entity::getType, RegistryTypes.BLOCK_TYPE);
    }

    private static <T> List<LabeledValue> countByType(ServerWorld world, Stream<T> objects, Function<T, DefaultedRegistryValue> getType, DefaultedRegistryType<?> registryType) {
        final Label worldLabel = worldLabel(world);
        final Map<String, Long> counts = objects.collect(groupingBy(e -> getType.apply(e).key(registryType).asString(), counting()));
        List<LabeledValue> values = new ArrayList<>(counts.size());
        for (Map.Entry<String, Long> type : counts.entrySet()) {
            values.add(value(type.getValue(), worldLabel, label("type", type.getKey())));
        }
        return values;
    }

    private static List<Label> worldLabels(ServerWorld world) {
        return Collections.singletonList(worldLabel(world));
    }

    private static Label worldLabel(ServerWorld world) {
        return label("key", world.getProperties().getKey().asString());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return server.collect();
    }
}
