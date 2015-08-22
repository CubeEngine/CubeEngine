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
package de.cubeisland.engine.module.log.action.player.item;

import org.cubeengine.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.ITEM;

/**
 * Represents a player crafting an item
 */
public class ItemCraft extends ActionItem
{
    public ItemCraft()
    {
        super("craft", ITEM);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof ItemCraft && this.player.equals(((ItemCraft)action).player)
            && ((ItemCraft)action).item.isSimilar(this.item);
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            return user.getTranslation(POSITIVE, "{user} crafted {name#item} x{amount}", this.player.name,
                                       this.item.getItem().getName(), this.getAttached().size() + 1);
        }
        return user.getTranslation(POSITIVE, "{user} crafted {name#item}", this.player.name,
                                   this.item.getItem().getName());
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.item.craft;
    }
}
