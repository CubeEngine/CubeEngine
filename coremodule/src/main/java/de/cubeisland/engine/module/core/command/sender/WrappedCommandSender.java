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
package de.cubeisland.engine.module.core.command.sender;

import java.util.Locale;
import java.util.UUID;

import de.cubeisland.engine.module.core.command.CommandSender;
import de.cubeisland.engine.module.core.i18n.I18n;
import de.cubeisland.engine.module.core.sponge.CoreModule;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;

public class WrappedCommandSender<W extends CommandSource> implements CommandSender
{
    private final CoreModule core;
    private final W wrapped;

    public WrappedCommandSender(CoreModule core, W sender)
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
    public CoreModule getCore()
    {
        return this.core;
    }

    @Override
    public String getName()
    {
        return this.getWrappedSender().getIdentifier();
    }

    @Override
    public String getDisplayName()
    {
        return this.getWrappedSender().getName();
    }

    @Override
    public Locale getLocale()
    {
        return Locale.getDefault();
    }

    @Override
    public String getTranslation(MessageType type, String message, Object... params)
    {
        return getCore().getModularity().start(I18n.class).translate(this.getLocale(), type, message, params);
    }

    @Override
    public void sendTranslated(MessageType type, String message, Object... params)
    {
        this.sendMessage(this.getTranslation(type, message, params));
    }

    @Override
    public String getTranslationN(MessageType type, int n, String singular, String plural, Object... params)
    {
        return getCore().getModularity().start(I18n.class).translateN(this.getLocale(), type, n, singular, plural, params);
    }


    @Override
    public void sendTranslatedN(MessageType type, int n, String singular, String plural, Object... params)
    {
        this.sendMessage(this.getTranslationN(type, n, singular, plural, params));
    }

    @Override
    public boolean hasPermission(String name)
    {
        return this.getWrappedSender().hasPermission(name);
    }

    public W getWrappedSender()
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

    @Override
    public void sendMessage(String msg)
    {
        getWrappedSender().sendMessage(Texts.of(msg));
    }
}