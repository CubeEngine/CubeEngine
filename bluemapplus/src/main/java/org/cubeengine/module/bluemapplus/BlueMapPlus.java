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

import java.util.Optional;
import java.util.stream.Collectors;
import com.flowpowered.math.vector.Vector2d;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.world.ChangeWorldBorderEvent;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;


@Singleton
@Module(dependencies = @Dependency("bluemap"))
public class BlueMapPlus
{
    @ModuleCommand
    private BlueMapPlusCommands commands;

    @Inject private FileManager fm;
    @Inject private Logger logger;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        BlueMapAPI.onEnable(this::updateBorderMarkers);
        BlueMapAPI.onEnable(this::updateChunkAndRegionMarkers);
        BlueMapAPI.onEnable(this::loadMarkers);
    }

    private void updateChunkAndRegionMarkers(BlueMapAPI blueMapAPI)
    {
        logger.info("Generating Chunk and Region Markers...");
        int i = 0;
        for (final ServerWorld world : Sponge.server().worldManager().worlds())
        {
            final var blueMapWorld = blueMapAPI.getWorld(world);
            if (blueMapWorld.isEmpty())
            {
                continue;
            }
            i++;
            blueMapWorld.ifPresent(bmWorld -> {
                var chunksByRegion = world.chunkPositions().collect(Collectors.groupingBy(v -> v.toDouble().div(32).toInt().toVector2(true)));
                var min = world.chunkPositions().reduce(Vector3i::min);
                var max = world.chunkPositions().reduce(Vector3i::max);
                if (min.isPresent() && max.isPresent())
                {
                    BlueMapUtils.buildChunkAndRegionGrid(bmWorld, min.get(), max.get().add(Vector3i.ONE), chunksByRegion);
                }
            });
        }
        logger.info("Done generating Chunk and Region Markers for {} worlds", i);
    }

    private void loadMarkers(BlueMapAPI blueMapAPI)
    {
        logger.info("Loading serialized markers...");
        var path = this.fm.getModulePath(BlueMapPlus.class);
        BlueMapUtils.loadMarkerSets(blueMapAPI, path);
        logger.info("Done loading serialized markers");
    }

    private void updateBorderMarkers(final BlueMapAPI blueMapAPI)
    {
        logger.info("Generating Border Markers...");
        int i = 0;
        for (final ServerWorld world : Sponge.server().worldManager().worlds())
        {
            final var blueMapWorld = blueMapAPI.getWorld(world);
            if (blueMapWorld.isEmpty())
            {
                continue;
            }
            i++;
            WorldBorder border = world.border();
            for (final BlueMapMap map : blueMapWorld.get().getMaps())
            {
                if (border.diameter() >= 59000000d)
                {
                    this.updateBlueMapBorder(map, world.key().toString(), null);
                }
                else
                {
                    this.updateBlueMapBorder(map, world.key().toString(), border);
                }
            }
        }
        logger.info("Done Generating Border Markers for {} worlds", i);
    }

    @Listener
    public void onChangeBorder(ChangeWorldBorderEvent.World event)
    {
        final var bmWorld = BlueMapAPI.getInstance().get().getWorld(event.world());
        if (bmWorld.isPresent())
        {
            for (final BlueMapMap map : bmWorld.get().getMaps())
            {
                this.updateBlueMapBorder(map, event.world().key().toString(), event.newBorder().orElse(null));

            }
        }
    }

    private void updateBlueMapBorder(BlueMapMap map, String key, @Nullable WorldBorder border)
    {
        String markerId = key + "-border";
        if (border == null)
        {
            BlueMapUtils.updateMarker(map, BlueMapUtils.BORDER, null, markerId);
            return;
        }
        final var centerX = border.center().x();
        final var centerZ = border.center().y();
        final var radius = border.diameter() / 2d;
        final Shape shape = Shape.createRect(new Vector2d(centerX - radius, centerZ - radius),
                                             new Vector2d(centerX + radius, centerZ + radius));
        final ShapeMarker marker = ShapeMarker.builder().label("World border")
                                              .shape(shape, 64)
                                              .lineColor( new Color(0xFF0000, 1f))
                                              .fillColor( new Color(0))
                                              .lineWidth(2)
                                              .depthTestEnabled(false)
                                              .build();
        BlueMapUtils.updateMarker(map, BlueMapUtils.BORDER, marker, markerId);
    }
}
