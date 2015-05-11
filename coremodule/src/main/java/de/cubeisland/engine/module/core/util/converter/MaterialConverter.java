/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 * <p/>
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.module.core.util.converter;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.module.core.sponge.SpongeCore;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import org.spongepowered.api.item.ItemType;

public class MaterialConverter extends SimpleConverter<ItemType>
{
    private SpongeCore core;

    public MaterialConverter(SpongeCore core)
    {

        this.core = core;
    }

    @Override
    public Node toNode(ItemType object) throws ConversionException
    {
        return StringNode.of(object.getName());
    }

    @Override
    public ItemType fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            return core.getModularity().start(MaterialMatcher.class).material(node.asText());
        }
        throw ConversionException.of(this, node, "Node is not a StringNode!");
    }
}
