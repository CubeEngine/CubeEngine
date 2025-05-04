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
package org.cubeengine.module.vigil.data;

import java.util.UUID;
import org.cubeengine.module.vigil.PluginVigil;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;

public interface VigilData
{
    Key<Value<UUID>> CREATOR = Key.builder().elementType(UUID.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "creator")).build();
    Key<Value<LookupSettings>> LOOKUP_DATA = Key.builder().elementType(LookupSettings.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "lookup_data")).build();

    Key<Value<ConfigAction>> CONFIG_ACTION = Key.builder().elementType(ConfigAction.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_action")).build();
    Key<Value<ConfigSection>> CONFIG_SECTION = Key.builder().elementType(ConfigSection.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_section")).build();
    Key<Value<String>> CONFIG_REPORT = Key.builder().elementType(String.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_report")).build();
    Key<Value<LookupSettings.AreaMode>> CONFIG_AREAMODE = Key.builder().elementType(LookupSettings.AreaMode.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_areamode")).build();
    Key<Value<ConfigOp>> CONFIG_OPERATION = Key.builder().elementType(ConfigOp.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_operation")).build();
    Key<Value<Long>> CONFIG_LONG_VALUE = Key.builder().elementType(Long.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_long_value")).build();
    Key<Value<UUID>> PLAYER = Key.builder().elementType(UUID.class).key(ResourceKey.of(PluginVigil.VIGIL_ID, "config_player")).build();

    static void register(RegisterDataEvent event)
    {
        event.register(DataRegistration.of(CREATOR, ItemStack.class));
        event.register(DataRegistration.of(LOOKUP_DATA, ItemStack.class));

        event.register(DataRegistration.of(CONFIG_ACTION, ItemStack.class));
        event.register(DataRegistration.of(CONFIG_SECTION, ItemStack.class));
        event.register(DataRegistration.of(CONFIG_AREAMODE, ItemStack.class));
        event.register(DataRegistration.of(CONFIG_OPERATION, ItemStack.class));
        event.register(DataRegistration.of(CONFIG_LONG_VALUE, ItemStack.class));
        event.register(DataRegistration.of(CONFIG_REPORT, ItemStack.class));
        event.register(DataRegistration.of(PLAYER, ItemStack.class));

        Sponge.dataManager().registerBuilder(LookupSettings.class, new LookupSettings.LookupDataBuilder());
    }

}
