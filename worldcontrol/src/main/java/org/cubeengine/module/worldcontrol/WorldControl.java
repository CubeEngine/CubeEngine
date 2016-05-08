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
package org.cubeengine.module.worldcontrol;

import java.util.Map.Entry;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.worldcontrol.WorldControlConfig.WorldSection;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.SpawnEntityEvent;

@ModuleInfo(name = "WorldControl", description = "Control what happens in your world")
public class WorldControl extends Module
{
    // TODO implementation...
    @ModuleConfig private WorldControlConfig config;

    @Enable
    public void onEnable()
    {
        for (Entry<ConfigWorld, WorldSection> entry : config.worldSettings.entrySet())
        {
            entry.getKey().getWorld().getProperties().setPVPEnabled(entry.getValue().pvp);
        }
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event)
    {
        WorldSection section = config.worldSettings.get(new ConfigWorld(event.getTargetWorld()));
        if (section != null)
        {

        }
    }
}
