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
package de.cubeisland.cubeengine.log.action.logaction.block;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENVIRONEMENT;

/**
 * Blocks fading
 * <p>Events: {@link BlockFadeEvent}</p>
 */
public class BlockFade extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(BLOCK, ENVIRONEMENT);
    }

    @Override
    public String getName()
    {
        return "block-fade";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event)
    {
        if (this.isActive(event.getBlock().getWorld()))
        {
            if (!this.lm.getConfig(event.getBlock().getWorld()).BLOCK_FADE_ignore.contains(event.getBlock().getType()))
            {
                this.logBlockChange(null,
                                    event.getBlock().getState(),
                                    event.getNewState(),null);
            }
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&6%s &afaded away%s!",
                            time,logEntry.getOldBlock(),loc);
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).BLOCK_FADE_enable;
    }
}