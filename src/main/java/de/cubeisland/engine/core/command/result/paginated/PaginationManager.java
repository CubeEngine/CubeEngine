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
package de.cubeisland.engine.core.command.result.paginated;

import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.core.sponge.SpongeCore;
import de.cubeisland.engine.core.command.CommandSender;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;

public class PaginationManager
{
    public static final String HEADER =          "--------- page {integer}/{integer} ---------";
    public static final String FOOTER =          "- /prev - page {integer}/{integer} - /next -";
    public static final String FIRST_FOOTER =    "--------- page {integer}/{integer} - /next -";
    public static final String LAST_FOOTER =     "- /prev - page {integer}/{integer} ---------";
    public static final String ONE_PAGE_FOOTER = "--------- page {integer}/{integer} ---------";
    public static final int LINES_PER_PAGE = 5;

    private Map<CommandSender, PaginatedResult> userCommandMap = new HashMap<>();
    private SpongeCore core;

    public PaginationManager(SpongeCore core)
    {
        this.core = core;
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        userCommandMap.remove(core.getUserManager().getExactUser(event.getPlayer().getUniqueId()));
    }

    public void registerResult(CommandSender sender, PaginatedResult result)
    {
        userCommandMap.put(sender, result);
    }

    public PaginatedResult getResult(CommandSender sender)
    {
        return userCommandMap.get(sender);
    }

    public boolean hasResult(CommandSender sender)
    {
        return userCommandMap.containsKey(sender);
    }
}
