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
package org.cubeengine.module.squelch.data;

import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.squelch.PluginSquelch;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import java.util.UUID;

public interface SquelchData
{
    Key<ListValue<UUID>> IGNORED = Key.builder().key(ResourceKey.of(PluginSquelch.SQUELCH_ID, "ignored")).listElementType(UUID.class).build();

    Key<Value<Long>> MUTED = Key.builder().key(ResourceKey.of(PluginSquelch.SQUELCH_ID, "muted")).elementType(Long.class).build();


    static void register(RegisterDataEvent event)
    {
        final ResourceKey key = ResourceKey.of(PluginSquelch.SQUELCH_ID, "squelch");
        @SuppressWarnings("unchecked")
        final DataStore dataStore = DataStore.builder().pluginData(key)
                                             .holder(ServerPlayer.class, User.class)
                                             .key(IGNORED, "ignored")
                                             .key(MUTED, "muted").build();
        event.register(DataRegistration.builder().dataKey(IGNORED, MUTED).store(dataStore).build());
    }
}
