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
package org.cubeengine.module.travel.warp;

import java.util.ArrayList;
import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.module.travel.config.Warp;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@Singleton
public class WarpCompleter implements ValueCompleter
{
    private final WarpManager manager;

    @Inject
    public WarpCompleter(WarpManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        final List<CommandCompletion> list = new ArrayList<>();
        if (context.cause().audience() instanceof ServerPlayer)
        {
            for (Warp warp : manager.list(((ServerPlayer) context.cause().audience()).user()))
            {
                if (warp.name.startsWith(currentInput))
                {
                    list.add(CommandCompletion.of(warp.name));
                }
            }
        }
        return list;
    }
}
