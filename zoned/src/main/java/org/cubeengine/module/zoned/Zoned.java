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
package org.cubeengine.module.zoned;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.selector.CuboidSelector;
import org.cubeengine.module.selector.SelectorCommand;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

@Singleton
@Module
public class Zoned extends CubeEngineModule
{
    @Inject private PermissionManager pm;
    @Inject private CommandManager cm;
    @Inject private Game game;
    @Inject private I18n i18n;
    private Permission selectPerm;

    @InjectService private Selector selector;

    @Inject private ModuleManager mm;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        // selectPerm = pm.register(Zoned.class, "use-wand", "Allows using the selector wand",null);
    }

}
