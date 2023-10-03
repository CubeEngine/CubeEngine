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
package org.cubeengine.module.terra.data;

import org.cubeengine.module.terra.PluginTerra;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import java.util.UUID;

public class TerraData
{
    public static final Key<Value<String>> WORLD_KEY = Key.builder().key(ResourceKey.of(PluginTerra.TERRA_ID, "worldkey")).elementType(String.class).build();

    public static final Key<Value<UUID>> WORLD_UUID = Key.builder().key(ResourceKey.of(PluginTerra.TERRA_ID, "worlduuid")).elementType(UUID.class).build();

    public static final Key<Value<UUID>> POTION_UUID = Key.builder().key(ResourceKey.of(PluginTerra.TERRA_ID, "potionuuid")).elementType(UUID.class).build();

    public static final Key<Value<Boolean>> TERRA_POTION = Key.builder().key(ResourceKey.of(PluginTerra.TERRA_ID, "terra")).elementType(Boolean.class).build();

    public static void register(RegisterDataEvent event)
    {
        final DataStore dataStore = DataStore.builder()
                             .pluginData(ResourceKey.of(PluginTerra.TERRA_ID, "terra"))
                             .holder(ItemStack.class, ItemStackSnapshot.class)
                             .keys(WORLD_KEY, WORLD_UUID, POTION_UUID, TERRA_POTION).build();
        event.register(DataRegistration.builder().dataKey(WORLD_KEY, WORLD_UUID, POTION_UUID, TERRA_POTION).store(dataStore).build());
    }
}
