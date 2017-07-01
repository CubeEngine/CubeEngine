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
package org.cubeengine.module.squelch;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.squelch.storage.TableIgnorelist;
import org.cubeengine.module.squelch.storage.TableMuted;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
@ModuleTables({TableMuted.class, TableIgnorelist.class})
// TODO custom data for mute & ignore
public class Squelch extends CubeEngineModule
{
    @ModuleConfig private SquelchConfig config;
    @Inject private SquelchPerm perms;
    @ModuleListener private MuteListener muteListener;

    public SquelchConfig getConfig()
    {
        return config;
    }

    public SquelchPerm perms()
    {
        return this.perms;
    }
}
