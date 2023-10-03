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
package org.cubeengine.libcube.service.config;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.converter.ClassedConverter;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.spongepowered.api.data.persistence.DataQuery;

public class DataQueryConverter implements ClassedConverter<DataQuery>
{
    @Override
    public Node toNode(DataQuery object, ConverterManager manager) throws ConversionException
    {
        return new StringNode(object.asString('_'));
    }

    @Override
    public DataQuery fromNode(Node node, Class<? extends DataQuery> type, ConverterManager manager) throws ConversionException
    {
        return DataQuery.of('_', node.getValue().toString());
    }
}
