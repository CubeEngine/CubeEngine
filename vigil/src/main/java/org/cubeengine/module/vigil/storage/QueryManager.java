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
package org.cubeengine.module.vigil.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.plugin.PluginContainer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;

@Singleton
public class QueryManager
{
    private final BlockingQueue<Action> actions = new LinkedBlockingQueue<>();
    private final ExecutorService storeExecuter;
    private PluginContainer plugin;
    private final Vigil vigil;
    private TaskExecutorService queryShowExecutor;
    private Map<UUID, CompletableFuture<Void>> queryFuture = new HashMap<>();

    private ReportManager reportManager;
    private I18n i18n;

    private Map<UUID, Lookup> lastLookups = new HashMap<>();

    // TODO add statistics

    private AtomicLong actionsProcessed = new AtomicLong();

    @Inject
    public QueryManager(ThreadFactory tf, ReportManager reportManager, I18n i18n, PluginContainer plugin, Vigil vigil)
    {
        this.reportManager = reportManager;
        this.i18n = i18n;
        this.plugin = plugin;
        this.vigil = vigil;
        this.storeExecuter = newSingleThreadExecutor(tf);
        this.storeExecuter.submit(this::store);
    }

    public void initQueryShowExecutor()
    {
        if (this.queryShowExecutor == null)
        {
            this.queryShowExecutor = Sponge.server().scheduler().executor(plugin);
        }
    }

    private VigilRepository repo;
    public VigilRepository repository()
    {
        if (this.repo == null)
        {
            this.repo = new VigilRepository(this.plugin.logger()).init(vigil.getConfig());
        }
        return this.repo;
    }

    /**
     * Queues in an action to be persisted
     *
     * @param action the action to be persisted
     */
    public void report(Action action)
    {
        if (!this.storeExecuter.isShutdown())
        {
            actions.add(action);
        }
    }

    private void store()
    {
        while (true)
        {
            Action toStore = null;
            try
            {
                toStore = actions.take();
                if (toStore == Action.SHUTDOWN_SERVER) {
                    return;
                }
                repository().store(toStore);
                actionsProcessed.incrementAndGet();
            }
            catch (InterruptedException e)
            {
                plugin.logger().error("Error taking next action", e);
            }
            catch (Exception e)
            {
                plugin.logger().error("Error storing action {}", toStore, e);
            }
        }

    }

    public void queryAndShow(Lookup lookup, Player player)
    {
        this.initQueryShowExecutor();
        // TODO check if last lookup is the exact same
        this.lastLookups.put(player.uniqueId(), lookup);

        // TODO lookup cancel previous?
        CompletableFuture<Void> future = queryFuture.get(player.uniqueId());
        if (future != null && !future.isDone())
        {
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "There is another lookup active!");
            return;
        }


        var receiver = new Receiver(player, i18n, lookup);
        future = CompletableFuture.supplyAsync(() -> doLookup(lookup)) // Async MongoDB Lookup
                   .thenApply(result -> new PreparedReport(receiver, reportManager, result)) // Still Async Prepare Reports
                   .thenAcceptAsync(PreparedReport::doShow, queryShowExecutor)// Resync to show information
            .exceptionally(t -> {
                receiver.getI18n().send(receiver.getSender(), CRITICAL, "An error occurred in a report: {input}", "?"); // TODO
                for (final var action : actions) {
                    System.out.println("ACTION ID: " + action.id);
                    System.out.println(action);
                }
                plugin.logger().error("Error showing reports", t);
                return null;
            });
        queryFuture.put(player.uniqueId(), future);
    }

    private List<Action> doLookup(Lookup lookup)
    {

        lookup.time(Lookup.LookupTiming.LOOKUP);
        var actions = repository().find(lookup);
        actions.stream().filter(a -> a.causes.causes().isEmpty()).forEach(a -> a.causes.causes().add(Map.of(StoredCause.CauserReason.DEFAULT,
                Causer.unknown())));
        lookup.time(Lookup.LookupTiming.LOOKUP);
        return actions;
    }


    public void purge()
    {
        this.repository().purge();
    }

    public Optional<Lookup> getLast(Player player)
    {
        return Optional.ofNullable(this.lastLookups.get(player.uniqueId()));
    }

    public void shutdown()
    {
        if (this.queryShowExecutor != null)
        {
            this.queryShowExecutor.shutdown();
        }
        try
        {
            if (!this.actions.isEmpty()) {
                plugin.logger().info("Waiting for {} actions to be stored", this.actions.size());
            }
            this.actions.add(Action.SHUTDOWN_SERVER);
            this.storeExecuter.shutdown();
            this.storeExecuter.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
