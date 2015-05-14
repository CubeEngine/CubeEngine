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
import java.util.Locale;
import java.util.Map;
import de.cubeisland.engine.module.core.sponge.CoreModule;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.types.Profession;

public class ProfessionMatcher
{
    private final Map<String, Profession> professions = new HashMap<>();
    private CoreModule core;

    public ProfessionMatcher(CoreModule core, Game game)
    {
        this.core = core;
        for (Profession profession : game.getRegistry().getAllOf(Profession.class))
        {
            this.professions.put(profession.getName().toLowerCase(), profession);
        }
    }

    public Profession profession(String name)
    {
        return professions.get(core.getModularity().start(StringMatcher.class).matchString(name.toLowerCase(Locale.ENGLISH),
                                                                                     this.professions.keySet()));
    }

    public String[] professions()
    {
        return professions.keySet().toArray(new String[professions.size()]);
    }
}