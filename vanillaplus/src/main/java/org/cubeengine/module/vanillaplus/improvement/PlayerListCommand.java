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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class PlayerListCommand
{
    protected static final Comparator<Player> USER_COMPARATOR = new UserComparator();
    private I18n i18n;

    @Inject
    public PlayerListCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    protected SortedMap<String, Set<ServerPlayer>> groupUsers(Set<ServerPlayer> users)
    {
        SortedMap<String, Set<ServerPlayer>> grouped = new TreeMap<>();
        for (ServerPlayer player : users)
        {
            String listGroup = player.option("list-group").orElse("&6Players");
            grouped.computeIfAbsent(listGroup, k -> new LinkedHashSet<>()).add(player);
        }
        return grouped;
    }

    @Command(desc = "Displays all the online players.")
    public void list(CommandCause context)
    {
        final SortedSet<ServerPlayer> users = new TreeSet<>(USER_COMPARATOR);

        for (ServerPlayer user : Sponge.server().onlinePlayers())
        {
            if (context.subject() instanceof ServerPlayer && !((ServerPlayer)context.subject()).canSee(user))
            {
                continue;
            }
            users.add(user);
        }

        if (users.isEmpty())
        {
            i18n.send(context, NEGATIVE, "There are no players online at the moment!");
            return;
        }

        SortedMap<String, Set<ServerPlayer>> grouped = this.groupUsers(users);
        i18n.send(context, POSITIVE, "Players online: {amount#online}/{amount#max}", users.size(), Sponge.server().maxPlayers());

        for (Entry<String, Set<ServerPlayer>> entry : grouped.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            final Component playerList = Component.join(separator(text(", ", NamedTextColor.WHITE)), entry.getValue().stream().map(this::formatUser).collect(Collectors.toList()));
            final TextComponent msg = LegacyComponentSerializer.legacyAmpersand().deserialize(entry.getKey())
                                                               .append(text(": ", NamedTextColor.WHITE))
                                                               .append(playerList);
            context.sendMessage(Identity.nil(), msg);
        }
    }

    private Component formatUser(ServerPlayer user)
    {
        final TextComponent result = text(user.name(), NamedTextColor.DARK_GREEN);

        // TODO chat module pass info that player is afk
        /*
        if (user.attachOrGet(BasicsAttachment.class, module).isAfk())
        {
            result = result.builder().append(Texts.of(WHITE, "(", GRAY, "afk", ")")).build();
        }
        */
        return result;
    }

    private static final class UserComparator implements Comparator<Player>
    {
        @Override
        public int compare(Player user1, Player user2)
        {
            return String.CASE_INSENSITIVE_ORDER.compare(user1.name(), user2.name());
        }
    }
}
