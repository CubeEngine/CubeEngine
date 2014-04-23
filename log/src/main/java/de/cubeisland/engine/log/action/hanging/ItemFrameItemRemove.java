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
package de.cubeisland.engine.log.action.hanging;

import org.bukkit.inventory.ItemStack;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.log.LoggingConfiguration;
import de.cubeisland.engine.log.action.ActionCategory;
import de.cubeisland.engine.log.action.BaseAction;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.log.action.ActionCategory.ITEM;

/**
 * Represents a player removing an item from an item-frame
 */
public class ItemFrameItemRemove extends ActionHanging
{
    public ItemStack item;

    public ItemFrameItemRemove()
    {
        super("remove", ITEM);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof ItemFrameItemRemove && this.player.equals(
            ((ItemFrameItemRemove)action).player);
    }

    @Override
    public String translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} removed {name#item} from an itemframe",
                                    "{user} removed {2:amount} items from itemframes", this.player.name,
                                    this.item.getType().name(), count);
    }

    // TODO redo/rollback

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.hanging.item_remove;
    }
}
