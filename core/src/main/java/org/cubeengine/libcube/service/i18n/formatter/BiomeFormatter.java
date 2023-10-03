/*
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
package org.cubeengine.libcube.service.i18n.formatter;

import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.dirigent.context.Arguments;
import org.cubeengine.dirigent.context.Context;
import org.cubeengine.dirigent.formatter.AbstractFormatter;
import org.cubeengine.dirigent.parser.component.Component;
import org.cubeengine.libcube.service.i18n.formatter.component.StyledComponent;
import org.spongepowered.api.world.biome.Biome;

public class BiomeFormatter extends AbstractFormatter<Biome>
{
    public BiomeFormatter()
    {
        super("biome");
    }

    @Override
    protected Component format(Biome arg, Context context, Arguments args)
    {
        return new StyledComponent(NamedTextColor.DARK_AQUA, arg.toString()); // TODO translation
    }
}
