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
package de.cubeisland.engine.service.command.readers;

import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.service.i18n.I18n;
import de.cubeisland.engine.module.core.util.formatter.MessageType;

public class ShortReader implements ArgumentReader<Short>
{
    private I18n i18n;

    public ShortReader(I18n i18n)
    {

        this.i18n = i18n;
    }

    @Override
    public Short read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String arg = invocation.consume(1);
        try
        {
            return Short.parseShort(arg);
        }
        catch (NumberFormatException e)
        {
            throw new ReaderException(i18n.translate(invocation.getLocale(), MessageType.NEGATIVE,
                                                     "Could not parse {input} to short!", arg));
        }
    }
}