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
package org.cubeengine.module.kickban;

import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;

/**
 * Overrides and improves Vanilla Kick and Ban Commands:
 *
 * /ban 	Adds player to banlist.
 * /ban-ip 	Adds IP address to banlist.
 * /banlist Displays banlist.
 * /kick 	Kicks a player off a server.
 * /pardon 	Removes entries from the banlist.
 */
// fake ban thing
@Singleton
@Module
public class KickBan
{
    @ModuleConfig private KickBanConfig config;
    @ModuleCommand private KickBanCommands kickBanCommands;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        this.kickBanCommands.init();
    }

    public KickBanConfig getConfiguration()
    {
        return this.config;
    }
}
