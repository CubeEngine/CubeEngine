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
package org.cubeengine.module.locker.data;

import java.util.UUID;
import org.cubeengine.module.locker.PluginLocker;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataHolder.Mutable;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;

public interface LockerData
{
    Key<Value<String>> MODE = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "mode")).elementType(String.class).build();

    Key<Value<String>> PASS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "pass")).elementType(String.class).build();

    Key<Value<UUID>> OWNER = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "owner")).elementType(UUID.class).build();

    /**
     * Bitmasks see {@link ProtectionFlag}
     */
    Key<Value<Integer>> FLAGS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "flags")).elementType(Integer.class).build();
    Key<MapValue<UUID, Integer>> ACCESS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "access")).mapElementType(UUID.class, Integer.class).build();

    Key<MapValue<UUID, Integer>> TRUST = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "trust")).mapElementType(UUID.class, Integer.class).build();

    Key<ListValue<UUID>> UNLOCKS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "unlocks")).listElementType(UUID.class).build();

    Key<Value<Long>> LAST_ACCESS = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "last_access")).elementType(Long.class).build();
    Key<Value<Long>> CREATED = Key.builder().key(ResourceKey.of(PluginLocker.LOCKER_ID, "created")).elementType(Long.class).build();

    @SuppressWarnings("unchecked")
    static void register(RegisterDataEvent event)
    {
        DataStore bookDataStore = DataStore.builder().pluginData(ResourceKey.of(PluginLocker.LOCKER_ID, "book"))
                                       .holder(ItemStack.class)
                                       .keys(MODE, PASS, OWNER, FLAGS, ACCESS)
                                       .build();
        event.register(DataRegistration.builder().dataKey(MODE, PASS, OWNER, FLAGS, ACCESS).store(bookDataStore).build());

        DataStore lockDataStore = DataStore.builder().pluginData(ResourceKey.of(PluginLocker.LOCKER_ID, "lock"))
                                .holder(BlockEntity.class, Entity.class, BlockSnapshot.class)
                                .keys(PASS, OWNER, FLAGS, ACCESS, LAST_ACCESS, CREATED)
                                .build();
        event.register(DataRegistration.builder().dataKey(PASS, OWNER, FLAGS, ACCESS, LAST_ACCESS, CREATED).store(lockDataStore).build());


        DataStore globalTrust = DataStore.builder().pluginData(ResourceKey.of(PluginLocker.LOCKER_ID, "trust"))
                                           .holder(ServerPlayer.class, User.class)
                                           .keys(TRUST)
                                           .build();
        event.register(DataRegistration.builder().dataKey(TRUST).store(globalTrust).build());
    }

    static void purge(Mutable dataHolder)
    {
        dataHolder.remove(PASS);
        dataHolder.remove(OWNER);
        dataHolder.remove(FLAGS);
        dataHolder.remove(ACCESS);
        dataHolder.remove(UNLOCKS);
        dataHolder.remove(LAST_ACCESS);
        dataHolder.remove(CREATED);
    }
}
