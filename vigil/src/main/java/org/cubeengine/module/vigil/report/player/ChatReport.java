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
package org.cubeengine.module.vigil.report.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.StringAction;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.reporting.Recall;
import org.cubeengine.module.vigil.report.Report.Readonly;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.time.Duration;

public class ChatReport extends BaseReport<PlayerChatEvent.Submit> implements Readonly
{
    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.WRITTEN_BOOK);
        icon.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, false);
        var tr = i18n.translate(audience, "Chat");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }

    @Override
    public void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine)
    {
        var actions = reportLine.actions();
        Action action = actions.getFirst();
        var chat = action.stringAction.data();
        receiver.sendReport(this, actions, actions.size(), "{txt} wrote {input}", "{txt} spammed {input} x{}",
                Recall.causeAsComponent(action), chat, actions.size());
    }

    @Override
    public Action observe(PlayerChatEvent.Submit event)
    {
        final var player = event.cause().first(ServerPlayer.class).get(); // event-filter ensures this is present
        final var plainMsg = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        return newActionAt(event.cause(), player.serverLocation()).withStringAction(StringAction.CHAT, plainMsg);
    }

    @Listener(order = Order.POST)
    public void onChat(PlayerChatEvent.Submit event, @First Player player)
    {
        report(observe(event));
    }

    @Override
    public Duration maxDiff() {
        return Duration.ofMinutes(10);
    }
}