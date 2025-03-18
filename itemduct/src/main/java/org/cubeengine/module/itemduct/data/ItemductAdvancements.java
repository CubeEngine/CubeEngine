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
package org.cubeengine.module.itemduct.data;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementTypes;
import org.spongepowered.api.advancement.DisplayInfo;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.ScoreAdvancementCriterion;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.plugin.PluginContainer;

public class ItemductAdvancements {

    public static Advancement ROOT;
    public static Advancement ACTIVATE_NETWORK;
    public static Advancement USE_FILTERS;
    public static Advancement USE_NETWORK;
    public static ScoreAdvancementCriterion USE_NETWORK_CRITERION;

    public static void init(final RegisterRegistryValueEvent.RegistryStep<Advancement> event, PluginContainer plugin)
    {
        final var rootKey = ResourceKey.of(plugin, "itemduct-start");
        final var activateNetworkkey = ResourceKey.of(plugin, "itemduct-activate");
        final var itemDuctsFilterKey = ResourceKey.of(plugin, "itemduct-filter");


        {
            final var info = DisplayInfo.builder()
                    .icon(ItemTypes.HOPPER)
                    .title(Component.text("Item Logistics"))
                    .description(Component.text("Craft an ItemDuct Activator"))
                    .build();
            ROOT = Advancement.builder().root().background(ResourceKey.minecraft("textures/gui/advancements/backgrounds/stone.png"))
                    .criterion(AdvancementCriterion.dummy())
                    .displayInfo(info).build();
            event.register(rootKey, ROOT);
        }

        {
            final var di = DisplayInfo.builder()
                    .icon(ItemTypes.HOPPER)
                    .title(Component.text("First Activation"))
                    .description(Component.text("Activate a Piston"))
                    .build();
            ACTIVATE_NETWORK = Advancement.builder()
                    .parent(rootKey)
                    .displayInfo(di)
                    .criterion(AdvancementCriterion.dummy())
                    .build();
            event.register(activateNetworkkey, ACTIVATE_NETWORK);

        }

        {
            final var di = DisplayInfo.builder()
                    .icon(ItemTypes.PAPER)
                    .title(Component.text("Filters"))
                    .description(Component.text("Open a Filter"))
                    .build();
            USE_FILTERS = Advancement.builder()
                    .parent(activateNetworkkey)
                    .displayInfo(di)
                    .criterion(AdvancementCriterion.dummy())
                    .build();
            event.register(itemDuctsFilterKey, USE_FILTERS);

        }

        {
            final var masterKey = ResourceKey.of(plugin, "itemduct-master");

            USE_NETWORK_CRITERION = ScoreAdvancementCriterion.builder()
                    .goal(100)
                    .name("itemduct-prompt")
                    .build();
            final var di = DisplayInfo.builder()
                    .icon(ItemTypes.NETHER_STAR)
                    .title(Component.text("Mastered"))
                    .description(Component.text("Use ItemDuct sorting over 100 times"))
                    .type(AdvancementTypes.CHALLENGE)
                    .hidden(false)
                    .build();
            USE_NETWORK = Advancement.builder()
                    .parent(activateNetworkkey)
                    .displayInfo(di)
                    .criterion(USE_NETWORK_CRITERION)
                    .build();
            event.register(masterKey, USE_NETWORK);
        }

    }
}
