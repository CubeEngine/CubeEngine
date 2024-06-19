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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import de.bluecolored.bluemap.api.math.Shape;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BlueMapUtils
{

    public static final String BORDER = "cubeengine-bluemap-plus-border";
    public static final String BIOMES = "cubeengine-bluemap-plus-biomes";
    public static final String CHUNKS = "cubeengine-bluemap-plus-chunks";
    public static final String REGIONS = "cubeengine-bluemap-plus-regions";
    public static final Map<String, String> NAMES = Map.of(BORDER, "Border", BIOMES, "Biomes", CHUNKS, "Chunks", REGIONS, "Regions");
    public static final Map<String, String> IDS = Map.of("Border", BORDER, "Biomes", BIOMES, "Chunks", CHUNKS, "Regions", REGIONS);
    private static final Vector3i[] directions = {
        new Vector3i(1, 0, 0),
        new Vector3i(-1, 0, 0),
        new Vector3i(0, 0, 1),
        new Vector3i(0, 0, -1)
    };
    private static final List<Vector2i> NEIGHBOR_OFFSETS = List.of(
        new Vector2i(-1, -1), new Vector2i(0, -1), new Vector2i(1, -1),
        new Vector2i(-1, 0), /* Current position */ new Vector2i(1, 0),
        new Vector2i(-1, 1), new Vector2i(0, 1), new Vector2i(1, 1));

    public static MarkerSet updateMarker(final BlueMapMap map, final String markerSetId, final @Nullable Marker marker, final String markerId)
    {
        MarkerSet markerSet = map.getMarkerSets().get(markerSetId);
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
                markerSet = MarkerSet.builder().label(NAMES.get(markerSetId)).build();
                map.getMarkerSets().put(markerSetId, markerSet);
            }
            markerSet.getMarkers().put(markerId, marker);
        }
        return markerSet;
    }

    public static ExtrudeMarker buildBiomeMarker(ServerLocation loc) {
        Biome startingBiome = loc.biome();  // Get the biome of the starting location

        var allPositions = new HashSet<Vector2i>();  // Set to collect all positions in the biome
        var visited = new HashSet<Vector3i>();  // Set to keep track of visited positions
        var queue = new LinkedList<ServerLocation>();  // Queue for BFS

        queue.add(loc);  // Add the starting location to the queue
        visited.add(loc.blockPosition());  // Mark the starting position as visited
        allPositions.add(new Vector2i(loc.blockX(), loc.blockZ()));  // Add the starting position to all positions

        while (!queue.isEmpty()) {  // Continue until there are no more locations to process
            var currentLoc = queue.poll();  // Dequeue the next location

            for (var direction : directions) {  // Iterate over possible directions
                var neighbor = currentLoc.add(direction);  // Get the neighboring location
                var neighborPos = neighbor.blockPosition();
                if (!visited.contains(neighborPos) && neighbor.biome().equals(startingBiome)) {  // Check if the neighbor is unvisited and in the same biome
                    visited.add(neighborPos);  // Mark the neighbor as visited
                    queue.add(neighbor);  // Add the neighbor to the queue for further exploration
                    allPositions.add(new Vector2i(neighborPos.x(), neighborPos.z()));  // Add the neighbor position to all positions
                }
            }
        }
        var boundaryPositions = allPositions.stream()
                              .filter(pos -> NEIGHBOR_OFFSETS.stream().anyMatch(offset -> !allPositions.contains(pos.add(offset))))
                              .collect(Collectors.toSet());
        return buildBiomeOutline(boundaryPositions);
    }

    private static void dfs(Vector2i currentPosition, Set<Vector2i> allPositions,
                            Set<Vector2i> visited, Set<Vector2i> currentSet) {
        visited.add(currentPosition);
        currentSet.add(currentPosition);

        for (Vector2i dir : NEIGHBOR_OFFSETS) {
            var neighbor = currentPosition.add(dir);
            if (allPositions.contains(neighbor) && !visited.contains(neighbor)) {
                dfs(neighbor, allPositions, visited, currentSet);
            }
        }
    }

    public static ExtrudeMarker buildBiomeOutline(Set<Vector2i> allPositions) {

        if (allPositions.isEmpty()) {
            return null;
        }

        Set<Set<Vector2i>> connectedSets = new HashSet<>();
        Set<Vector2i> visited = new HashSet<>();

        for (Vector2i pos : allPositions) {
            if (!visited.contains(pos)) {
                Set<Vector2i> currentSet = new HashSet<>();
                dfs(pos, allPositions, visited, currentSet);
                connectedSets.add(currentSet);
            }
        }

        var outlines = connectedSets.stream().sorted(Comparator.comparingInt(Set::size)).map(
            BlueMapUtils::toShape).toList();

        final var builder = ExtrudeMarker.builder().label("Biome Outline").shape(outlines.get(0), 64, 64);
        for (int i = 1; i < outlines.size(); i++)
        {
            builder.holes(outlines.get(i));
        }
        return builder.lineColor( new Color(0x807BB3, 0.5f))
                                              .fillColor( new Color(0))
                                              .lineWidth(1)
                                              .depthTestEnabled(false)
                                              .build();
    }

    private static Shape toShape(final Set<Vector2i> positions)
    {
        Vector2i startPoint = positions.iterator().next();
        Vector2i currentPoint = startPoint;
        int i = 0;

        var builder = Shape.builder();
        do {
            if (i++ > 2) {
                positions.add(startPoint);
            }
            positions.remove(currentPoint);
            builder.addPoint(currentPoint.toDouble());
            currentPoint = findNearestNeighbor(currentPoint, positions);;
        } while (currentPoint != null && !positions.isEmpty() && !startPoint.equals(currentPoint));
        return builder.build();
    }

    private static Vector2i findNearestNeighbor(Vector2i currentPoint, Set<Vector2i> positions) {
        return positions.stream()
                        .filter(pos -> !pos.equals(currentPoint))
                        .min(Comparator.comparingDouble(pos -> currentPoint.distanceSquared(pos)))
                        .orElse(null);
    }

    static void writeMarkerSet(BlueMapMap map, final MarkerSet markerSet, final Path dir)
    {
        final var json = MarkerGson.INSTANCE.toJson(markerSet);

        try
        {
            Files.write(dir.resolve(map.getWorld().getId() + "." + markerSet.getLabel() + ".json"), json.getBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void loadMarkerSets(final BlueMapAPI blueMapAPI, final Path dir)
    {
        try
        {
            Files.list(dir).forEach(file -> {
                final var filename = file.getFileName().toString();
                if (filename.endsWith(".json"))
                {
                    try
                    {
                        var json = Files.readString(file);
                        var markerSet = MarkerGson.INSTANCE.fromJson(json, MarkerSet.class);
                        for (final BlueMapMap map : blueMapAPI.getMaps())
                        {
                            if (filename.startsWith(map.getWorld().getId() + "."))
                            {
                                map.getMarkerSets().put(IDS.get(markerSet.getLabel()), markerSet);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static List<LineMarker> generateGridLines(Vector3i minPos, Vector3i maxPos, int distance, int height, Color color, int width,
                                                     int viewDistance) {
        List<Line> lines = new ArrayList<>();
        int minX = minPos.x();
        int minZ = minPos.z();
        int maxX = maxPos.x();
        int maxZ = maxPos.z();

        // Generate vertical lines along the Z axis
        for (int x = minX; x <= maxX; x += distance) {
            Line.Builder builder = Line.builder();
            builder.addPoint(new Vector3d(x, height, minZ));
            builder.addPoint(new Vector3d(x, height, maxZ));
            lines.add(builder.build());
        }

        // Generate horizontal lines along the X axis
        for (int z = minZ; z <= maxZ; z += distance) {
            Line.Builder builder = Line.builder();
            builder.addPoint(new Vector3d(minX, height, z));
            builder.addPoint(new Vector3d(maxX, height, z));
            lines.add(builder.build());
        }

        return lines.stream().map(line ->
            LineMarker.builder().label("Line:" + line.getMin() + ":" + line.getMax())
                      .line(line)
                      .lineColor(color)
                      .lineWidth(width)
                      .depthTestEnabled(false)
                      .maxDistance(viewDistance)
                      .build()
        ).toList();
    }

    static void buildChunkAndRegionGrid(final BlueMapWorld bmWorld, final Vector3i min, final Vector3i max,
                                        Map<org.spongepowered.math.vector.Vector2i, List<Vector3i>> chunksByRegion)
    {
        if (chunksByRegion.size() < 1000)
        {
            final var minPos = min.mul(16);
            final var maxPos = max.mul(16);
            final var chunkLineMarkers = generateGridLines(minPos, maxPos, 16, 64,
                                                           new Color(0xADD8E6, 0.2f), 1, 400);
            for (final BlueMapMap map : bmWorld.getMaps())
            {
                for (final LineMarker marker : chunkLineMarkers)
                {
                    updateMarker(map, CHUNKS, marker, marker.getLabel());
                }
            }
        }

        final var minRPos = min.toDouble().div(32).toInt().mul(32 * 16);
        final var maxRPos = max.toDouble().div(32).toInt().mul(32 * 16);
        final var regionLineMarkers = generateGridLines(minRPos, maxRPos, 16*32, 65,
                                                        new Color(0x90EE90, 0.4f), 2, Integer.MAX_VALUE);

        List<HtmlMarker> regionLabels = new ArrayList<>();
        for (int x = minRPos.x(); x <= maxRPos.x(); x += 16 * 32)
        {
            for (int z = minRPos.z(); z <= maxRPos.z(); z += 16 * 32)
            {
                final var pos = new Vector3d(x + 16 * 16, 64, z + 16 * 16);
                final var rPos = new org.spongepowered.math.vector.Vector2i(x, z).div(16*32);
                final var chunksInRegion = chunksByRegion.getOrDefault(rPos, Collections.emptyList());
                if (chunksInRegion.isEmpty())
                {
                    continue;
                }
                final var label = "%d : %d".formatted(x / (16 * 32), z / (16 * 32));
                final var marker = HtmlMarker.builder().position(pos)
                                             .label(label)
                                             .html("""
                        <div style="background-color: #AAAAAAAA; padding: 2px; border-radius: 5px; color: #000000FF; font-size: 12px; font-weight: bold; text-align: center;">
                            <div>%s</div>
                            <div style="font-size: 8px">%d/1024</div>
                        </div>
                        """.formatted(label, chunksInRegion.size()))
                                             .minDistance(1000)
                                             .maxDistance(9000)
                                             .build();
                regionLabels.add(marker);
            }
        }


        for (final BlueMapMap map : bmWorld.getMaps())
        {

            for (final LineMarker marker : regionLineMarkers)
            {
                updateMarker(map, REGIONS, marker, marker.getLabel());
            }
            for (final HtmlMarker marker : regionLabels)
            {
                updateMarker(map, REGIONS, marker, marker.getLabel());
            }
        }
    }

}
