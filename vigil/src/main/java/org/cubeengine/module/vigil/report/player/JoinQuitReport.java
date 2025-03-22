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
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.StringAction;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.reporting.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.time.Duration;

public class JoinQuitReport extends BaseReport<ServerSideConnectionEvent> implements Report.Readonly {

    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.ARMOR_STAND);
    var tr = i18n.translate(audience, "Joins/Quits");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }


    @Override
    protected Action observe(ServerSideConnectionEvent event) {
        if (event instanceof ServerSideConnectionEvent.Join joinEvent) {
            return newActionAt(Cause.of(EventContext.empty(), joinEvent.player()),
                    joinEvent.player().serverLocation())
                    .withStringAction(StringAction.CONNECTION, "join");

        } else if (event instanceof ServerSideConnectionEvent.Leave leaveEvent) {
            return newActionAt(Cause.of(EventContext.empty(), leaveEvent.player()), leaveEvent.player().serverLocation()).withStringAction(StringAction.CONNECTION, "leave");
        }
        return null;
    }


    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event)
    {
        report(observe(event));
    }
    
    @Listener
    public void onQuit(ServerSideConnectionEvent.Leave event)
    {
        report(observe(event));
    }

    @Override
    public void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine)
    {
        final var actions = reportLine.actions();
        Action action = actions.getFirst();
        final var causer = Recall.causeAsComponent(action);

        int join = 0;
        int quit = 0;
        for (Action a : actions)
        {
            if (a.stringAction.data().equals("join"))
            {
                join++;
            }
            else if (a.reportType.equals(JoinQuitReport.class.getName()))
            {
                quit++;
            }
        }

        if (quit == 0)
        {
            receiver.sendReport(this, actions, actions.size(), "{txt} joined the game", "{txt} joined the game x{}",
                    causer, actions.size());
        }
        else if (join == 0)
        {
            receiver.sendReport(this, actions, actions.size(), "{txt} quit the game", "{txt} quit the game x{}",
                    causer, actions.size());
        }
        else
        {
            if (join == quit)
            {
                receiver.sendReport(this, actions, join, "{txt} joined and quit the game", "{txt} joined and quit the game x{}",
                        causer, join);
            }
            else
            {
                receiver.sendReport(this, actions, "{txt} joined x{} and left x{}",
                        causer, join, quit);
            }

        }
    }


    @Override
    public Duration maxDiff() {
        return Duration.ofHours(5);
    }
}
