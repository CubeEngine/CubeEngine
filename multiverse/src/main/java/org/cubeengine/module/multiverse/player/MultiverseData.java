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
package org.cubeengine.module.multiverse.player;

import org.cubeengine.module.multiverse.PluginMultiverse;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;

public interface MultiverseData
{
    Key<Value<String>> UNIVERSE = Key.builder()
                                     .key(ResourceKey.of(PluginMultiverse.MULTIVERSE_ID, "current-universe"))
                                     .elementType(String.class).build();

    Key<MapValue<String, DataView>> DATA = Key.builder()
                                              .key(ResourceKey.of(PluginMultiverse.MULTIVERSE_ID, "player-data"))
                                              .mapElementType(String.class, DataView.class).build();

    static void register(RegisterDataEvent event)
    {
        registerCurrentWorldData(event);
        registerPlayerData(event);
    }

    static void registerCurrentWorldData(RegisterDataEvent event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginMultiverse.MULTIVERSE_ID, "current-universe");
        @SuppressWarnings("unchecked")
        final DataStore dataStore = DataStore.builder().pluginData(rkey)
                                             .holder(ServerPlayer.class, User.class)
                                             .key(MultiverseData.UNIVERSE, "current-universe")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(MultiverseData.UNIVERSE)
                                                              .store(dataStore)
                                                              .build();
        event.register(registration);
    }

    static void registerPlayerData(RegisterDataEvent event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginMultiverse.MULTIVERSE_ID, "player-data");
        @SuppressWarnings("unchecked")
        final DataStore dataStore = DataStore.builder().pluginData(rkey)
                                             .holder(ServerPlayer.class, User.class)
                                             .key(MultiverseData.DATA, "player-data")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(MultiverseData.DATA)
                                                              .store(dataStore)
                                                              .build();
        event.register(registration);
    }
}
