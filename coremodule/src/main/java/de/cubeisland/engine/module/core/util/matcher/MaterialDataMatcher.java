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
package de.cubeisland.engine.module.core.util.matcher;

import java.util.HashMap;
import de.cubeisland.engine.module.core.sponge.CoreModule;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.types.DyeColor;

public class MaterialDataMatcher
{
    private CoreModule core;
    private final Game game;
    private HashMap<String, DyeColor> dyeColors;

    public MaterialDataMatcher(CoreModule core, Game game)
    {
        this.core = core;
        this.game = game;

        initDataValues(game);
    }

    private void initDataValues(Game game)
    {
        this.dyeColors = new HashMap<>();
        for (DyeColor dyeColor : game.getRegistry().getAllOf(DyeColor.class))
        {
            dyeColors.put(dyeColor.getName(), dyeColor);
        }
        // TODO map ItemType / BlockType -> Map<String, CatalogType> see datavalues.txt
    }

    /**
     * Matches a DyeColor
     *
     * @param data the data
     * @return the dye color
     */
    public DyeColor colorData(String data)
    {
        String match = core.getModularity().start(StringMatcher.class).matchString(data, dyeColors.keySet());
        if (match == null)
        {
            return null;
        }
        return dyeColors.get(match);
    }
}