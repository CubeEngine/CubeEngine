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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

@Singleton
@Command(name = "fix", desc = "Commands to fix things")
public class FixCommands
{
    private final TaskManager taskManager;
    private final I18n i18n;
    private final AtomicReference<ChunkFixer> activeFixer = new AtomicReference<>();

    @Inject
    public FixCommands(TaskManager taskManager, I18n i18n)
    {
        this.taskManager = taskManager;
        this.i18n = i18n;
    }

    @Command(desc = "Touches all chunks to fix lighting issues")
    public void chunks(CommandCause cause, @Default @Named("in") ServerWorld world) {
        final ChunkFixer newFixer = new ChunkFixer(world);
        final ChunkFixer existingFixer = activeFixer.compareAndExchange(null, newFixer);
        if (existingFixer == null) {
            newFixer.attachAudience(cause.audience());
            taskManager.runTask(newFixer);
        } else {
            i18n.send(cause, MessageType.NEUTRAL, "Chunk fixer is already running, see the progress...");
            existingFixer.attachAudience(cause.audience());
        }
    }

    private final class ChunkFixer implements Runnable
    {
        private final List<Audience> audiences;
        private final ServerWorld world;
        private final Iterator<Vector3i> chunkPositions;
        private int batchCounter = 0;
        private int iterationCounter = 0;
        private long nextReportIn = 0;

        private ChunkFixer(ServerWorld world)
        {
            this.audiences = new ArrayList<>();
            this.audiences.add(Sponge.systemSubject());
            this.world = world;
            this.chunkPositions = world.chunkPositions().iterator();
        }

        public void attachAudience(Audience audience) {
            audiences.removeIf(a -> a instanceof ServerPlayer p && !p.isOnline());
            if (audience instanceof SystemSubject) {
                return;
            }
            audiences.add(audience);
        }

        @Override
        public void run()
        {
            batchCounter++;
            long timeSpent = 0;
            long lastDuration = 0;
            while (chunkPositions.hasNext() && timeSpent < 30L) {
                final Vector3i pos = chunkPositions.next();
                long start = System.currentTimeMillis();
                world.loadChunk(pos, true);
                lastDuration = System.currentTimeMillis() - start;
                timeSpent += lastDuration;
                iterationCounter++;
            }
            final Audience audience = Audience.audience(audiences);
            if (chunkPositions.hasNext()) {
                if (nextReportIn <= 0)
                {
                    i18n.send(audience, MessageType.NEUTRAL,
                              "Processed {amount} chunks in {amount} batches, last chunk took {amount}ms...",
                              iterationCounter, batchCounter, lastDuration);
                    nextReportIn = 10000L;
                } else {
                    nextReportIn -= timeSpent;
                }
                taskManager.runTaskAsync(this);
            } else {
                i18n.send(audience, MessageType.POSITIVE, "Processing complete!");
                activeFixer.compareAndSet(this, null);
            }
        }
    }
}
