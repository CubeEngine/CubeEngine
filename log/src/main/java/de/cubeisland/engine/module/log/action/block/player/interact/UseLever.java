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
package de.cubeisland.engine.module.log.action.block.player.interact;

import org.cubeengine.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import de.cubeisland.engine.module.log.action.block.player.ActionPlayerBlock;
import org.bukkit.material.Lever;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.USE;

/**
 * Represents a player using a lever
 */
public class UseLever extends ActionPlayerBlock
{
    public UseLever()
    {
        super("lever", USE);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof UseLever && this.player.equals(((ActionPlayerBlock)action).player)
            && this.coord.equals(action.coord);
    }

    @Override
    public Text translateAction(User user)
    {
        // TODO plural
        if (this.newBlock.as(Lever.class).isPowered())
        {
            return user.getTranslation(POSITIVE, "{user} activated the lever", this.player.name);
        }
        return user.getTranslation(POSITIVE, "{user} deactivated the lever", this.player.name);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.use.lever;
    }
}
