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
package de.cubeisland.engine.service.ban;

import java.util.Date;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.command.sender.WrappedCommandSender;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.text.Text.Literal;
import org.spongepowered.api.util.command.CommandSource;

import static de.cubeisland.engine.module.core.contract.Contract.expectNotNull;

public abstract class Ban<T>
{
    private CommandSource source;
    private Literal reason;
    private Date created;
    private Date expires;

    protected Ban(CommandSender source, Literal reason, Date expires)
    {
        this(source, reason, new Date(System.currentTimeMillis()), expires);
    }

    protected Ban(CommandSender source, Literal reason, Date created, Date expires)
    {
        this(getSource(source), reason, created, expires);
    }

    protected Ban(CommandSource source, Literal reason, Date created, Date expires)
    {
        expectNotNull(source, "The source must not be null");
        expectNotNull(reason, "The reason must not be null");
        expectNotNull(created, "The created must not be null");
        this.source = source;
        this.reason = reason;
        this.created = created;
        this.expires = expires;
    }

    private static CommandSource getSource(CommandSender source)
    {
        if (source instanceof User)
        {
            return ((User)source).getPlayer().get();
        }
        else if (source instanceof WrappedCommandSender)
        {
            return ((WrappedCommandSender)source).getWrappedSender();
        }
        else
        {
            throw new IllegalArgumentException("Source is not a wrapped CommandSender neither a User");
        }
    }

    /**
     * This method returns the source of this ban.
     * The source ban is usually a user or the console.
     *
     * @return the source as a string representation. never null!
     */
    public CommandSource getSource()
    {
        return source;
    }

    /**
     * This method sets the source of this command
     *
     * @param source the source as a string representation. may not be null!
     */
    public void setSource(CommandSource source)
    {
        expectNotNull(source, "The source must not be null");
        this.source = source;
    }

    /**
     * Returns the target of this ban as a string
     *
     * @return the target, never null
     */
    public abstract T getTarget();

    /**
     * This method returns the ban reason
     *
     * @return the ban reason. never null!
     */
    public Literal getReason()
    {
        return reason;
    }

    /**
     * This method sets the ban reason.
     *
     * @param reason the ban reason. may not be null!
     */
    public void setReason(Literal reason)
    {
        expectNotNull(reason, "The reason must not be null");
        this.reason = reason;
    }

    /**
     * This method returns the creation date of this ban
     *
     * @return the date of creation. never null!
     */
    public Date getCreated()
    {
        return created;
    }

    /**
     * This method set the date of creation.
     *
     * @param created the date. may not be null!
     */
    public void setCreated(Date created)
    {
        expectNotNull(created, "The created must not be null");
        this.created = created;
    }

    /**
     * This method returns the expire date.
     * A null value represents no expire date (known as permanent bans)
     *
     * @return the expire date or null
     */
    public Date getExpires()
    {
        return expires;
    }

    /**
     * This method sets the expire date.
     * A null value represents no expire date (known as permanent bans)
     *
     * @param expires the expire date or null
     */
    public void setExpires(Date expires)
    {
        this.expires = expires;
    }

    /**
     * This method checks whether this ban is expired.
     *
     * @return true if the the ban is expired
     */
    public boolean isExpired()
    {
        Date expires = this.getExpires();
        return expires != null && expires.getTime() <= System.currentTimeMillis();
    }

    /**
     * This method returns the string representation of the ban target
     *
     * @return the ban target as a string
     */
    @Override
    public abstract String toString();
}