/**
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
package de.cubeisland.engine.module.core.command.completer;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.completer.Completer;
import de.cubeisland.engine.module.core.CubeEngine;
import de.cubeisland.engine.module.core.command.CommandSender;
import de.cubeisland.engine.module.core.sponge.SpongeCore;
import de.cubeisland.engine.module.core.user.User;
import de.cubeisland.engine.module.core.user.UserManager;

import static de.cubeisland.engine.module.core.util.StringUtils.startsWithIgnoreCase;

/**
 * A PlayerCompleter for the other online users but not the user sending the command
 */
public class PlayerCompleter implements Completer
{
    private SpongeCore core;

    public PlayerCompleter(SpongeCore core)
    {

        this.core = core;
    }

    private static boolean canSee(CommandSender sender, User user)
    {
        return !(sender instanceof User) || ((User)sender).canSee(user.getPlayer().orNull());
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        List<String> playerNames = new ArrayList<>();
        final CommandSender sender = (CommandSender)invocation.getCommandSource(); // TODO prevent class cast exceptions
        for (User player : core.getModularity().start(UserManager.class).getOnlineUsers())
        {
            String name = player.getName();
            if (canSee(sender,  player) && startsWithIgnoreCase(name, invocation.currentToken()))
            {
                playerNames.add(name);
            }
        }
        playerNames.remove(sender.getName());
        return playerNames;
    }
}
