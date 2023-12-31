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
package org.cubeengine.module.vigil.report.entity.player;

import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report.Readonly;
import org.cubeengine.module.vigil.report.Report.SimpleGrouping;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.PlayerChatEvent;

import static java.util.Collections.singletonList;

public class ChatReport extends BaseReport<PlayerChatEvent> implements Readonly, SimpleGrouping
{
    private static final String CHAT = "chat";

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        receiver.sendReport(this, actions, actions.size(), "{txt} wrote {input}", "{txt} spammed {input} x{}",
                Recall.cause(action), action.getData(CHAT), actions.size());
    }

    @Override
    public Action observe(PlayerChatEvent event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.cause()));
        action.addData(CHAT, PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
        final ServerPlayer serverPlayer = event.cause().first(ServerPlayer.class).get(); // event-filter ensures this is present
        action.addData(LOCATION, Observe.location(serverPlayer.serverLocation()));
        return action;
    }

    @Override
    public List<String> groupBy()
    {
        return singletonList(CHAT);
    }

    @Listener(order = Order.POST)
    public void onChat(PlayerChatEvent event, @First Player player)
    {
        report(observe(event));
    }
}