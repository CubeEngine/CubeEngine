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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.flowpowered.math.vector.Vector2d;
import com.google.inject.Singleton;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.world.ChangeWorldBorderEvent;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.server.ServerWorld;


@Singleton
@Module(dependencies = @Dependency("bluemap"))

public class BlueMapPlus
{
    private static final String MARKER_SET_ID = "cubeengine-bluemap-plus";

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        BlueMapAPI.onEnable(this::updateAll);
    }

    private void updateAll(final BlueMapAPI blueMapAPI)
    {
        for (final ServerWorld world : Sponge.server().worldManager().worlds())
        {
            WorldBorder border = world.border();
            if (border.diameter() >= 59000000d)
            {
                this.updateBlueMapBorder(blueMapAPI, world, Optional.empty());
            }
            else
            {
                this.updateBlueMapBorder(blueMapAPI, world, Optional.of(border));
            }
        }
    }

    @Listener
    public void onChangeBorder(ChangeWorldBorderEvent.World event)
    {
        this.updateBlueMapBorder(BlueMapAPI.getInstance().get(), event.world(), event.newBorder());
    }

    private void updateBlueMapBorder(BlueMapAPI blueMapAPI, ServerWorld world, Optional<WorldBorder> optBorder)
    {
        // TODO bluemap calculates the save-folder for end/nether incorrectly, attempt to match by directory from config file instead
        //        blueMapAPI.getWorld(world.properties().key()).map(BlueMapWorld::getMaps).ifPresent(maps -> {
        //        });

        final Map<Path, Collection<BlueMapMap>> mapsForWorlds = blueMapAPI.getWorlds().stream().collect(
            Collectors.toMap(BlueMapWorld::getSaveFolder, BlueMapWorld::getMaps));
        final Collection<BlueMapMap> maps = mapsForWorlds.get(world.directory().toAbsolutePath().normalize());
        if (maps != null)
        {
            maps.forEach(map -> {
                String markerId = world.properties().key() + "-border";
                optBorder.ifPresentOrElse(border -> {
                    final var centerX = border.center().x();
                    final var centerZ = border.center().y();
                    final var radius = border.diameter() / 2d;
                    final Shape shape = Shape.createRect(new Vector2d(centerX - radius, centerZ - radius),
                                                         new Vector2d(centerX + radius, centerZ + radius));
                    final ShapeMarker marker = ShapeMarker.builder().label("World border")
                                                          .shape(shape, world.seaLevel())
                                                          .lineColor( new Color(0xFF0000, 1f))
                                                          .fillColor( new Color(0))
                                                          .lineWidth(2)
                                                          .depthTestEnabled(false)
                                                          .build();
                    updateMarker(map, marker, markerId);
                }, () -> updateMarker(map, null, markerId));
            });
        }

    }

    private static void updateMarker(final BlueMapMap map, final @Nullable ShapeMarker marker, final String markerId)
    {
        MarkerSet markerSet = map.getMarkerSets().get(MARKER_SET_ID);
        if (marker == null)
        {
            if (markerSet != null)
            {
                markerSet.getMarkers().remove(markerId);
            }
        }
        else
        {
            if (markerSet == null)
            {
                markerSet = MarkerSet.builder().label("BlueMap Plus").build();
                map.getMarkerSets().put(MARKER_SET_ID, markerSet);
            }
            markerSet.getMarkers().put(markerId, marker);
        }
    }
}
