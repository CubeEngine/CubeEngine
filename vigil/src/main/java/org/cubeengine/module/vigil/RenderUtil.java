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
package org.cubeengine.module.vigil;

import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class RenderUtil {

    public static void renderArea(final ServerPlayer player, Vector3i pos) {
        var effect = ParticleEffect.builder().type(ParticleTypes.TRAIL).option(ParticleOptions.COLOR, Color.PURPLE);
        var points = getEdgePoints(pos.toDouble().sub(Vector3d.ONE.mul(0.02)), pos.toDouble().add(Vector3d.ONE.mul(0.02)), 6,
                new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}});
        for (final var point : points) {
            player.spawnParticles(effect.option(ParticleOptions.TARGET, point).build(), point);
        }
    }

    public static void renderArea(final ServerPlayer player, AABB pos) {
        var effect = ParticleEffect.builder().type(ParticleTypes.TRAIL).option(ParticleOptions.COLOR, Color.PURPLE)
                .option(ParticleOptions.TRAVEL_TIME, Ticks.of(40));
        var effect2 = ParticleEffect.builder().type(ParticleTypes.TRAIL).option(ParticleOptions.COLOR, Color.RED)
                .option(ParticleOptions.TRAVEL_TIME, Ticks.of(10));
        var effect3 = ParticleEffect.builder().type(ParticleTypes.TRAIL).option(ParticleOptions.COLOR, Color.BLUE)
                .option(ParticleOptions.TRAVEL_TIME, Ticks.of(40))
                ;
//                .option(ParticleOptions.DESTINATION, )
//                .option(ParticleOptions.SCALE, 0.5d)
//                .velocity(Vector3d.ONE.mul(1))
//                .build();


        final var frameLines = new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        int[][] faceDiagonals = {{0, 3}, {1, 2}, {4, 7}, {5, 6}, {0, 5}, {1, 4}, {2, 7}, {3, 6}, {0, 6}, {1, 7}, {2, 4}, {3, 5}};
        int[][] areaDiagonals = {{0, 7}, {1, 6}, {2, 5}, {3, 4}};


        var framePoints = getEdgePoints(pos.min(), pos.max(), 6, frameLines);
        for (final var point : framePoints) {
            player.spawnParticles(effect.option(ParticleOptions.TARGET, point).build(), point);
        }

//        var center = pos.center().add(Vector3d.ONE.mul(0.5));
//        for (final var point : framePoints) {
//            player.spawnParticles(effect2.option(ParticleOptions.TARGET, point)
//                            .option(ParticleOptions.TRAVEL_TIME, Ticks.of( (int)(2f * center.distance(point))))
//                    .build(), center);
//        }
    }

    public static List<Vector3d> getEdgePoints(Vector3d lower, Vector3d upper, float density, final int[][] lines) {
        List<Vector3d> points = new ArrayList<>();

        double[][] vertices = {
                {lower.x(), lower.y(), lower.z()}, {upper.x() + 1, lower.y(), lower.z()}, {lower.x(), upper.y() + 1, lower.z()},
                {upper.x() + 1, upper.y() + 1, lower.z()},
                {lower.x(), lower.y(), upper.z() + 1}, {upper.x() + 1, lower.y(), upper.z() + 1}, {lower.x(), upper.y() + 1, upper.z() + 1},
                {upper.x() + 1, upper.y() + 1, upper.z() + 1}
        };
        for (double[] vertex : vertices) {
            points.add(new Vector3d(vertex[0], vertex[1], vertex[2]));
        }


        for (int[] edge : lines) {
            computeEdgePoints(density, edge, vertices, points);
        }
        return points;
    }

    private static void computeEdgePoints(final float density, final int[] edge, final double[][] vertices, final List<Vector3d> points) {
        Vector3d v1 = new Vector3d(vertices[edge[0]][0], vertices[edge[0]][1], vertices[edge[0]][2]);
        Vector3d v2 = new Vector3d(vertices[edge[1]][0], vertices[edge[1]][1], vertices[edge[1]][2]);
        var edgeLength = v1.distance(v2);

        double step = 1d / density;
        for (double t = step; t <= edgeLength - step; t += step) {
            points.add(new Vector3d(
                    v1.x() * (1 - t / edgeLength) + v2.x() * (t / edgeLength),
                    v1.y() * (1 - t / edgeLength) + v2.y() * (t / edgeLength),
                    v1.z() * (1 - t / edgeLength) + v2.z() * (t / edgeLength)
            ));
        }
    }
}
