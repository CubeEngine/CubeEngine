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
package org.cubeengine.module.spawn;

import com.google.inject.Singleton;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.service.permission.PermissionService;

// TODO integrate in teleport module reading subject option is not that advanced of a feature?
@Singleton
@Module
public class Spawn
{
    @ModuleCommand private SpawnCommands spawnCommands;
    @ModuleListener private SpawnListener listener;

    @InjectService private PermissionService ps;

    public PermissionService getPermissionService()
    {
        return ps;
    }
}
