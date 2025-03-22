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
package org.cubeengine.module.vigil.reporting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.LocatableChange;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.pagination.PaginationList.Builder;
import org.spongepowered.api.util.locale.LocaleSource;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class Receiver
{
    private final Audience cmdSource;
    private final I18n i18n;
    private Lookup lookup;

    private List<Component> lines = new ArrayList<>();

    public Receiver(Audience cmdSource, I18n i18n, Lookup lookup)
    {
        this.cmdSource = cmdSource;
        this.i18n = i18n;
        this.lookup = lookup;
    }

    public void sendReport(Report report, List<Action> actions, String msg, Object... args)
    {
        Component reports = text(report.getClass().getSimpleName());
        Component trans = i18n.translate(cmdSource, NEUTRAL, msg, args).hoverEvent(HoverEvent.showText(reports));
        sendReport(actions, trans);
    }

    public void sendReportX(Report report, List<Action> actions, int x, String msg, Object... args)
    {
        Component reports = text(report.getClass().getSimpleName());
        Component trans = i18n.translate(cmdSource, NEUTRAL, msg, args);
        var withTimes = trans.append(Component.text(" x")).append(Component.text(x).color(NamedTextColor.GOLD));
        var withHover = (x == 1 ? trans : withTimes).hoverEvent(HoverEvent.showText(reports));
        sendReport(actions, withHover);
    }

    public void sendReport(Report report, List<Action> actions, int size, String msgSingular, String msgPlural, Object... args)
    {
        Component reports = text(report.getClass().getSimpleName());
        Component trans = i18n.translateN(cmdSource, NEUTRAL, size, msgSingular, msgPlural, args).hoverEvent(HoverEvent.showText(reports));
        sendReport(actions, trans);
    }

    private static final SimpleDateFormat dateShort = new SimpleDateFormat("yy-MM-dd");
    private static final SimpleDateFormat dateLong = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timeLong = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat timeShort = new SimpleDateFormat("HH:mm");

    private static final Component RED_SEPARATOR = text(" - ", NamedTextColor.RED);

    private void sendReport(List<Action> actions, Component trans)
    {
        Action firstAction = actions.get(0);
        Action lastAction = actions.get(actions.size() - 1);

        Component date = lookup.settings().isNoDate() ? null : getDatePrefix(firstAction, lastAction);
        Component loc = lookup.settings().isShowLocation() ? null : getLocation(actions);
        if (date != null && loc != null)
        {
            lines.add(
                date.append(Component.space()).append(i18n.translate(cmdSource, "at"))
                    .append(Component.space()).append(loc)
//                        .append(Component.newline())
                        .append(text("  ")).append(trans));
        }
        else
        {
            Component prefix = Component.empty();
            if (date != null)
            {
                prefix = date.append(RED_SEPARATOR);
            }
            else if (loc != null)
            {
                prefix = loc.append(RED_SEPARATOR);
            }
            lines.add(prefix.append(trans));
        }
    }

    private Component getLocation(List<Action> actions)
    {
        var worlds = actions.stream().map(a -> a.world).distinct().toList();
        if (worlds.size() > 1) {
            // TODO
            return Component.text("multiple worlds");
        }
        var worldKey = worlds.getFirst();
        var worldExists = Sponge.server().worldManager().worldExists(worldKey);
        var world = Sponge.server().worldManager().world(worldKey);
        Component worldName = Component.text(worldKey.asString());
        if (worldExists) {
            worldName = world.flatMap(w -> w.properties().displayName()).orElse(worldName);
        }
        var positions = actions.stream().flatMap(action -> action.locatables.stream()).map(LocatableChange::blockPos).distinct().toList();
        if (positions.size() == 1) {
            var pos = positions.getFirst();
            Component text = Component.join(separator(text(":", NamedTextColor.WHITE)), text(pos.x()), text(pos.y()), text(pos.z()));
            if (worldExists) {
                if (cmdSource instanceof ServerPlayer player && world.isPresent()) {
                    var hoverEvent = HoverEvent.showText(i18n.translate(cmdSource, NEUTRAL, "Click to teleport to the location in {txt#world}", worldName));
                    var clickEvent = SpongeComponents.executeCallback(c -> player.setLocation(world.get().location(pos.toDouble().add(0.5, 0.5, 0.5))));
                    text = text.hoverEvent(hoverEvent).clickEvent(clickEvent);
                } else {
                    var hoverEvent = HoverEvent.showText(i18n.translate(cmdSource, NEGATIVE, "Cannot teleport to unloaded world {txt#world}", worldName));
                    text = text.hoverEvent(hoverEvent);
                }
            } else {
                text = text.append(Component.space()).append(i18n.translate(cmdSource, NEGATIVE, "World {txt#world} does not exist!", worldName));
            }

            if (lookup.settings().isFullLocation())
            {
                return Component.space().append(i18n.translate(cmdSource, "in")).append(Component.space()).append(worldName.color(NamedTextColor.GRAY));
            }
            return text;
        }
        return Component.text("range"); // TODO
    }

    private Component getDatePrefix(Action firstAction, Action lastAction)
    {
        if (firstAction == lastAction)
        {
            var date = firstAction.timestamp;
            String dLong = dateLong.format(date);
            boolean sameDay = dateLong.format(new Date()).equals(dLong);
            String tLong = timeLong.format(date);
            Component full = text(dLong, NamedTextColor.GRAY).append(Component.space()).append(text(tLong));
            if (lookup.settings().isFullDate())
            {
                return full;
            }
            String tShort = timeShort.format(date);
            if (sameDay) // Today?
            {
                return text(tShort, NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(full));
            }
            else
            {
                return text(dateShort.format(date), NamedTextColor.GRAY).append(Component.space()).append(
                    text(tShort)).hoverEvent(HoverEvent.showText(full));
            }
        }
        else
        {
            var firstDate = firstAction.timestamp;
            var lastDate = lastAction.timestamp;

            String fdLong = dateLong.format(firstDate);
            String ldLong = dateLong.format(lastDate);
            boolean isSameDay = fdLong.equals(ldLong);
            boolean isToday = dateLong.format(new Date()).equals(fdLong);
            final TextComponent ftLong = text(timeLong.format(firstDate));
            final TextComponent ltLong = text(timeLong.format(lastDate));
            final Component fFull = text(fdLong, NamedTextColor.GRAY).append(Component.space()).append(ftLong);
            final Component lFull = text(ldLong, NamedTextColor.GRAY).append(Component.space()).append(ltLong);
            final TextComponent dash = text(" - ", NamedTextColor.WHITE);
            if (lookup.settings().isFullDate())
            {
                return fFull.append(dash).append(lFull);
            }
            final Component fdShort = text(dateShort.format(firstDate), NamedTextColor.GRAY);
            final Component ftShort = text(timeShort.format(firstDate), NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(fFull));
            final Component ltShort = text(timeShort.format(lastDate), NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(lFull));
            if (isSameDay)
            {
                if (isToday)
                {
                    return ftShort.append(dash).append(ltShort);
                }
                return fdShort.append(Component.space()).append(ftShort).append(dash).append(ltShort);
            }
            else
            {
                final Component ldShort = text(dateShort.format(lastDate), NamedTextColor.GRAY);
                return fdShort.append(Component.space()).append(ftShort).append(dash).append(ldShort).append(Component.space()).append(ltShort);
            }
        }
    }

    public void sendReports(List<PreparedReport.ReportLine> reportLines)
    {
        if (reportLines.isEmpty())
        {
            if (cmdSource instanceof Player)
            {
                i18n.send(ChatType.ACTION_BAR, ((Player) cmdSource), NEGATIVE, "Nothing logged here");
                return;
            }
            i18n.send(cmdSource, NEGATIVE, "Nothing logged here");
            return;
        }

        cmdSource.sendMessage(text(StringUtils.repeat("-", 53), NamedTextColor.GOLD));
        for (PreparedReport.ReportLine reportAction : reportLines)
        {
            reportAction.showReport(this);
        }
        final Builder builder = Sponge.game().serviceProvider().paginationService().builder();
        Component titleLineAmount = i18n.translate(cmdSource, POSITIVE, "Showing {amount} Logs", lines.size());
        String titleLineSort = i18n.getTranslation(cmdSource, "(newest first)");
        Component titleTimings = i18n.translate(cmdSource, NEUTRAL, "Query: {input#time} Report: {input#time}",
                                                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.LOOKUP)),
                                                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.REPORT)));
        final Component titleLine = titleLineAmount.append(Component.space()).append(
            text(titleLineSort, NamedTextColor.YELLOW)).hoverEvent(HoverEvent.showText(titleTimings));
        builder.title(titleLine).padding(text("-"))
               // TODO reverse order setting
               .contents(lines).linesPerPage(6 + Math.min(lines.size() * 2, 14)).sendTo(cmdSource);
        // TODO remove linesPerPage when Sponge puts the lines to the bottom
    }

    public Locale getLocale()
    {
        if (cmdSource instanceof LocaleSource)
        {
            return ((LocaleSource)cmdSource).locale();
        }
        return Locale.getDefault();
    }

    public Lookup getLookup()
    {
        return lookup;
    }

    public Audience getSender()
    {
        return cmdSource;
    }

    public I18n getI18n()
    {
        return i18n;
    }
}
