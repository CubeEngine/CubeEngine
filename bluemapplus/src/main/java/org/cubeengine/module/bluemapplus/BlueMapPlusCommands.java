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
package org.cubeengine.module.bluemapplus;

import com.google.inject.Inject;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@Command(name = "bluemapplus", desc = "BlueMapPlus commands", alias = "bmp")
public class BlueMapPlusCommands
{
    @Inject private FileManager fm;

    @Command(desc = "Adds a marker around the current biome")
    @Restricted
    public void markBiome(ServerPlayer player, String name)
    {
        final var blueMapWorld = BlueMapAPI.getInstance().get().getWorld(player.world());
        if (blueMapWorld.isEmpty()) {
            return;
        }

        final var marker = BlueMapUtils.buildBiomeMarker(player.serverLocation());
        for (final BlueMapMap map : blueMapWorld.get().getMaps())
        {
            var markerSet = BlueMapUtils.updateMarker(map, BlueMapUtils.BIOMES, marker, "biome-outline-" + name);
            var path = this.fm.getModulePath(BlueMapPlus.class);
            BlueMapUtils.writeMarkerSet(map, markerSet, path);
        }
    }
}
