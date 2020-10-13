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
package org.cubeengine.module.worlds;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Worlds extends CubeEngineModule
{
    @ModuleCommand private WorldsCommands wc;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        cm.getProviders().register(this, new WorldGeneratorModifierParser(), WorldGeneratorModifier.class);
        wc.addCommand(new WorldsModifyCommands(cm, i18n));
    }
}
