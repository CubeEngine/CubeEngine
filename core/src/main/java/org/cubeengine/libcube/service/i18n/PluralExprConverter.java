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
package org.cubeengine.libcube.service.i18n;

import java.util.HashMap;
import java.util.Map;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.converter.SimpleConverter;
import org.cubeengine.converter.node.Node;
import org.cubeengine.i18n.plural.ComplexExpr;
import org.cubeengine.i18n.plural.GreaterThanOneExpr;
import org.cubeengine.i18n.plural.NotOneExpr;
import org.cubeengine.i18n.plural.PluralExpr;
import org.cubeengine.i18n.plural.ZeroExpr;

public class PluralExprConverter extends SimpleConverter<PluralExpr>
{
    private final Map<String,PluralExpr> pluralExpressions = new HashMap<>();

    public PluralExprConverter()
    {
        this.pluralExpressions.put("0", new ZeroExpr());
        this.pluralExpressions.put("n!=1", new NotOneExpr());
        this.pluralExpressions.put("n>1", new GreaterThanOneExpr());
    }

    @Override
    public Node toNode(PluralExpr object) throws ConversionException
    {
        throw new UnsupportedOperationException("Cannot convert PluralExpr back into String!");
    }

    @Override
    public PluralExpr fromNode(Node node) throws ConversionException
    {
        String expr = node.asText().replaceAll("\\s", "").replaceAll("[a-zA-Z]", "n");
        PluralExpr pluralExpr = this.pluralExpressions.get(expr);
        if (pluralExpr == null)
        {
            return new ComplexExpr(expr);
        }
        return pluralExpr;
    }
}
