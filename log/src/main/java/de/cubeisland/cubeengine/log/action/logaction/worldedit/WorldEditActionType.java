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
package de.cubeisland.cubeengine.log.action.logaction.worldedit;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;

public class WorldEditActionType extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(PLAYER,BLOCK);
    }

    @Override
    public String getName()
    {
        return "worldedit";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.getNewBlock().material.equals(Material.AIR))
        {
            user.sendTranslated("%s&2%s &aused worldedit to remove &6%s%s",
                                time,
                                logEntry.getCauserUser().getDisplayName(),
                                logEntry.getOldBlock(), loc);
        }
        else if (logEntry.getOldBlock().material.equals(Material.AIR))
        {
            user.sendTranslated("%s&2%s &aused worldedit to place &6%s%s",
                                time, logEntry.getCauserUser().getDisplayName(),
                                logEntry.getNewBlock(), loc);
        }
        else
        {
            user.sendTranslated("%s&2%s &aused worldedit to replace &6%s&a with &6%s%s",
                                time, logEntry.getCauserUser().getDisplayName(),
                                logEntry.getOldBlock(),
                                logEntry.getNewBlock(), loc);
        }
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).WORLDEDIT_enable;
    }
}
