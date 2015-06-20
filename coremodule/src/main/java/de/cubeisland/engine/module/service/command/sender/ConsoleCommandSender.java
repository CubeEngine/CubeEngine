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
package de.cubeisland.engine.module.service.command.sender;

import de.cubeisland.engine.module.core.i18n.I18n;
import de.cubeisland.engine.module.core.sponge.CoreModule;
import org.spongepowered.api.util.command.source.ConsoleSource;

public class ConsoleCommandSender extends WrappedCommandSender<ConsoleSource>
{
    public ConsoleCommandSender(I18n i18n, ConsoleSource console)
    {
        super(i18n, console);
    }
}
