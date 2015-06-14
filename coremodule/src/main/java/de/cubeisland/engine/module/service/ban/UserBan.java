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
package de.cubeisland.engine.module.service.ban;

import java.util.Date;

import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.service.command.CommandSender;
import org.spongepowered.api.entity.player.User;
import org.spongepowered.api.text.Text.Literal;
import org.spongepowered.api.util.command.CommandSource;

import static de.cubeisland.engine.module.core.contract.Contract.expectNotNull;

public class UserBan extends Ban<User>
{
    private final User target;

    public UserBan(User target, CommandSender source, Literal reason)
    {
        this(target, source, reason, new Date(System.currentTimeMillis()), null);
    }

    public UserBan(User target, CommandSender source, Literal reason, Date expires)
    {
        this(target, source, reason, new Date(System.currentTimeMillis()), expires);
    }

    public UserBan(User target, CommandSender source, Literal reason, Date created, Date expires)
    {
        super(source, reason, created, expires);
        expectNotNull(target, "The user must not be null!");
        this.target = target;
    }

    public UserBan(User target, CommandSource commandSource, Literal reason, Date created, Date expires)
    {
        super(commandSource, reason, created, expires);
        expectNotNull(target, "The user must not be null!");
        this.target = target;
    }

    @Override
    public User getTarget()
    {
        return this.target;
    }

    @Override
    public String toString()
    {
        return ChatFormat.DARK_GREEN + (target == null ? "Unknown" : target.getName()) + ChatFormat.YELLOW + "(" + ChatFormat.GOLD +  this.getTarget().getUniqueId() + ChatFormat.YELLOW + ")";
    }
}
