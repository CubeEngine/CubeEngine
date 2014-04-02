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
package de.cubeisland.engine.log.action.newaction.block.entity;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.log.action.ActionCategory;
import de.cubeisland.engine.log.action.newaction.BaseAction;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.log.action.ActionCategory.ENTITY_ENDERMAN;

/**
 * Represents an Enderman placing a block
 */
public class EndermanPlace extends ActionEntityBlock<ListenerEntityBlock>
{
    // return this.lm.getConfig(world).block.enderman.ENDERMAN_PLACE_enable;

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof EndermanPlace && ((EndermanPlace)action).newBlock == this.newBlock;
    }

    @Override
    public String translateAction(User user)
    {
        if (this.hasAttached())
        {
            int endermanCount = this.countUniqueEntities();
            return user.getTranslationN(POSITIVE, endermanCount, "{text:One Enderman} placed {name#block} x{amount}!",
                                        "{2:amount} {text:Enderman} placed {name#block} x{amount}!",
                                        this.oldBlock.name(), this.getAttached().size() + 1, endermanCount);
        }
        return user.getTranslation(POSITIVE, "An {text:Enderman} placed {name#block}", this.oldBlock.name());
    }

    @Override
    public ActionCategory getCategory()
    {
        return ENTITY_ENDERMAN;
    }

    @Override
    public String getName()
    {
        return "place";
    }
}
