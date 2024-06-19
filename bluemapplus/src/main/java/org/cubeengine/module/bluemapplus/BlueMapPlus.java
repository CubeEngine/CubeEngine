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
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.apache.logging.log4j.Logger;
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
        BlueMapAPI.onEnable(this::updateAll);
        BlueMapAPI.onEnable(this::loadAll);
    }

    private void loadAll(BlueMapAPI blueMapAPI)
    {
        var path = this.fm.getModulePath(BlueMapPlus.class);
        BlueMapUtils.loadMarkerSets(path);
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

            try
            {
                BlueMapAPI.getInstance().get().getWorld(world).ifPresent(bmWorld -> {
                    var chunksByRegion = world.chunkPositions().collect(Collectors.groupingBy(v -> v.toDouble().div(32).toInt().toVector2(true)));
                    var min = world.chunkPositions().reduce(Vector3i::min);
                    var max = world.chunkPositions().reduce(Vector3i::max);
                    if (min.isPresent() && max.isPresent())
                    {
                        BlueMapUtils.buildChunkAndRegionGrid(bmWorld, min.get(), max.get().add(Vector3i.ONE), chunksByRegion);
                    }
                });
            }
            catch (Exception e)
            {
                logger.error("Error while building chunk and region grid", e);
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
        blueMapAPI.getWorlds().stream().filter(map -> map.getId().endsWith(world.key().toString())).flatMap(b -> b.getMaps().stream()).forEach(map -> {
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
                BlueMapUtils.updateMarker(map, BlueMapUtils.BORDER, marker, markerId);
            }, () -> BlueMapUtils.updateMarker(map, BlueMapUtils.BORDER, null, markerId));
        });
    }
}
