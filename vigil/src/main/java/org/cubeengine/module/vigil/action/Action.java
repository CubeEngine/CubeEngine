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
package org.cubeengine.module.vigil.action;

import org.cubeengine.module.vigil.report.Observe;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.world.server.ServerLocation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Action {

    public static final Action SHUTDOWN_SERVER = new Action(-1, null, "shutdown", null);

    public long id;
    public final Timestamp timestamp;
    public final String reportType;

    public ResourceKey world;

    public List<LocatableChange> locatables = new ArrayList<>();

    public StringAction stringAction;


    public List<InventoryChange> inventoryChanges = new ArrayList<>();


    public StoredCause causes;

    private Action(long id, Timestamp timestamp, String reportType, ResourceKey world) {
        this.id = id;
        this.timestamp = timestamp;
        this.reportType = reportType;
        this.world = world;
        this.causes = new StoredCause();
    }

    public Action(String type, final Cause cause) {
        this.causes = Observe.causes(cause);
        this.timestamp = Timestamp.from(Instant.now());
        this.reportType = type;
    }

    public Action(String type, final Cause cause, ServerLocation location) {
        this(type, cause);
        this.world = location.worldKey();
        this.locatables = List.of(Observe.position(location));
    }

    public static Action fromDatabase(long id, Timestamp timestamp, String reportType, final ResourceKey world) {
        return new Action(id, timestamp, reportType, world);
    }

    public Action withStringAction(String type, String data)
    {
        this.stringAction = new StringAction(type, data);
        return this;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    public void addBlockChange(final BlockTransactionReceipt receipt, final ServerLocation loc) {
        this.world = loc.worldKey();
        if (this.locatables == null) {
            this.locatables = new ArrayList<>();
        }
        this.locatables.add(Observe.blockChange(this, receipt, loc));
    }

    public Action addEntityDeath(final Entity entity) {
        this.world = entity.serverLocation().worldKey();
        this.locatables.add(Observe.entityDeath(this, entity));
        return this;
    }

    public Action withInventoryChanges(final List<SlotTransaction> transactions) {
        this.inventoryChanges = Observe.transactions(transactions);
        return this;
    }

    public Causer firstCauser(final StoredCause.CauserReason causerReason) {
        return causes.causes().getFirst().get(causerReason);
    }
}
