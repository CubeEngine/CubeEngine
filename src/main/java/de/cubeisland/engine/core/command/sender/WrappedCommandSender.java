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
package de.cubeisland.engine.core.command.sender;

import java.util.Locale;
import java.util.UUID;
import de.cubeisland.engine.core.Core;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.util.formatter.MessageType;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;

public class WrappedCommandSender implements CommandSender
{
    private final Core core;
    private final CommandSource wrapped;

    public WrappedCommandSender(Core core, CommandSource sender)
    {
        this.core = core;
        this.wrapped = sender;
    }

    @Override
    public UUID getUniqueId()
    {
        if (wrapped instanceof Player)
        {
            return ((Player)wrapped).getUniqueId();
        }
        return NON_PLAYER_UUID;
    }

    @Override
    public Core getCore()
    {
        return this.core;
    }

    @Override
    public String getName()
    {
        return this.getWrappedSender().getName();
    }

    @Override
    public String getDisplayName()
    {
        return this.getName();
    }

    @Override
    public boolean isAuthorized(Permission perm)
    {
        return this.getWrappedSender().hasPermission(perm.getName());
    }

    @Override
    public Locale getLocale()
    {
        return Locale.getDefault();
    }

    @Override
    public Server getServer()
    {
        return this.getWrappedSender().getServer();
    }

    @Override
    public String getTranslation(MessageType type, String message, Object... params)
    {
        return this.getCore().getI18n().translate(this.getLocale(), type, message, params);
    }

    @Override
    public void sendTranslated(MessageType type, String message, Object... params)
    {
        this.sendMessage(Texts.of(this.getTranslation(type, message, params)));
    }


    @Override
    public String getTranslationN(MessageType type, int n, String singular, String plural, Object... params)
    {
        return this.getCore().getI18n().translateN(this.getLocale(), type, n, singular, plural, params);
    }


    @Override
    public void sendTranslatedN(MessageType type, int n, String singular, String plural, Object... params)
    {
        this.sendMessage(Texts.of(this.getTranslationN(type, n, singular, plural, params)));
    }

    @Override
    public boolean hasPermission(String name)
    {
        return this.getWrappedSender().hasPermission(name);
    }

    public CommandSource getWrappedSender()
    {
        return this.wrapped;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof WrappedCommandSender)
        {
            return ((WrappedCommandSender)o).getName().equals(this.getWrappedSender().getName());
        }
        else if (o instanceof CommandSource)
        {
            return ((CommandSource)o).getName().equals(this.getWrappedSender().getName());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return this.getWrappedSender().hashCode();
    }
}
