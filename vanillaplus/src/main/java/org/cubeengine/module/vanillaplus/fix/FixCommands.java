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
package org.cubeengine.module.vanillaplus.fix;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

@Singleton
@Command(name = "fix", desc = "Commands to fix things")
public class FixCommands
{
    private final TaskManager taskManager;
    private final I18n i18n;

    @Inject
    public FixCommands(TaskManager taskManager, I18n i18n)
    {
        this.taskManager = taskManager;
        this.i18n = i18n;
    }

    @Command(desc = "Touches all chunks to fix lighting issues")
    public void chunks(CommandCause cause, @Default @Named("in") ServerWorld world) {
        taskManager.runTask(new ChunkGenerator(cause, world));
    }

    private final class ChunkGenerator implements Runnable
    {
        private final CommandCause cause;
        private final ServerWorld world;
        private final Iterator<Vector3i> chunkPositions;
        private int batchCounter = 0;
        private int iterationCounter = 0;

        private ChunkGenerator(CommandCause cause, ServerWorld world)
        {
            this.cause = cause;
            this.world = world;
            this.chunkPositions = world.chunkPositions().iterator();
        }

        @Override
        public void run()
        {
            batchCounter++;
            Duration max = Duration.ofMillis(30);
            Instant start = Instant.now();
            while (chunkPositions.hasNext() && Duration.between(start, Instant.now()).compareTo(max) < 0) {
                final Vector3i pos = chunkPositions.next();
                world.loadChunk(pos, true);
                iterationCounter++;
            }
            if (chunkPositions.hasNext()) {
                i18n.send(cause, MessageType.NEUTRAL, "Processed {amount} chunks in {amount} batches...", iterationCounter, batchCounter);
                taskManager.runTaskAsync(this);
            } else {
                i18n.send(cause, MessageType.POSITIVE, "Processing complete!");
            }
        }
    }
}
