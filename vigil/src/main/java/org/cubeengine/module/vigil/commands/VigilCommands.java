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
package org.cubeengine.module.vigil.commands;

import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.SpawnUtil;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.cubeengine.module.vigil.data.VigilData;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "vigil", alias = "log", desc = "Vigil-Module Commands")
public class VigilCommands extends DispatcherCommand
{
    public static final Component toolName = Component.text("Vigil Log-Tool", NamedTextColor.DARK_AQUA);

    private I18n i18n;

    @Inject
    public VigilCommands(VigilAdminCommands adminCommands, I18n i18n)
    {
        super(adminCommands);
        this.i18n = i18n;
    }
    // TODO rollback
    // TODO redo


    @Alias(value = "lt")
    @Command(desc = "Gives you an item to check logs with.")
    @Restricted(msg = "Why don't you check in your log-file? You won't need an item there!")
    public void tool(ServerPlayer context)
    {
        findLogTool(context);
    }

    private void findLogTool(ServerPlayer player)
    {
        ItemStack inHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (inHand.get(VigilData.CREATOR).isPresent()) {
            return;
        }

        var books = player.inventory().query(QueryTypes.ITEM_TYPE, ItemTypes.BOOK);
        for (final var slot : books.slots()) {
            if (slot.peek().get(VigilData.CREATOR).isPresent()) {
                player.setItemInHand(HandTypes.MAIN_HAND, slot.peek());
                slot.set(inHand);
                i18n.send(ACTION_BAR, player, POSITIVE, "Found Log-Tool!");
                return;
            }
        }
        ItemStack itemStack = ItemStack.of(ItemTypes.BOOK);
        itemStack.offer(Keys.CUSTOM_NAME, toolName);
        itemStack.offer(Keys.LORE, List.of(i18n.translate(player, "created by {name}", player.name())));
        itemStack.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
        itemStack.offer(VigilData.CREATOR, player.uniqueId());
        final var lookupSettings = new LookupSettings();
        lookupSettings.toggleReport(BlockReport.class.getName());
        itemStack.offer(VigilData.LOOKUP_DATA, lookupSettings);
        player.setItemInHand(HandTypes.MAIN_HAND, itemStack);
        if (!inHand.isEmpty())
        {
            if (player.inventory().offer(inHand).type() != InventoryTransactionResult.Type.SUCCESS)
            {
                SpawnUtil.spawnItem(inHand, player.serverLocation());
            }
        }
        i18n.send(ACTION_BAR, player, POSITIVE, "Received a new Log-Tool!");
    }
}
