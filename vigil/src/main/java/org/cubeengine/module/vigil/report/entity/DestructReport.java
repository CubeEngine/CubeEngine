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
package org.cubeengine.module.vigil.report.entity;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.action.EntityChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.reporting.Recall;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.time.Duration;

/* TODO
death
-animal
-boss
-kill
-monster
-npc
-other
-pet
-player?

-hanging-break
-vehicle-break

 */
public class DestructReport extends BaseReport<DestructEntityEvent> {
    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.ROTTEN_FLESH);
        var tr = i18n.translate(audience, "Entity Deaths");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }

    @Override
    public void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine) {
        final var actions = reportLine.actions();
        Action action = actions.get(0);
        //Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get(0);

        Component cause = Recall.causeAsComponent(action);
        var entityType = Recall.entity(action);

        // TODO locatables are not grouped into reportline atm
        boolean isLiving = reportLine.locatables().stream().filter(EntityChange.class::isInstance)
                .map(EntityChange.class::cast).map(EntityChange::living).findFirst().orElse(false);

        if (EntityTypes.ITEM.get().equals(entityType)) {
            var item = Recall.entitySnapshot(action);
            final var stack = Recall.stackFromEntitySnapshot(item);
            // var stack = item.get(Keys.ITEM_STACK_SNAPSHOT).orElse(null); // TODO support keys for EntitySnapshot in Sponge API
            if (stack != null) {
                // TODO sum multiple item quantity
                var quantity = stack.quantity();
                var name = Recall.stack(stack.asImmutable(), quantity);

                var firstCause = action.causes.causes().getFirst().get(StoredCause.CauserReason.DEFAULT);
                switch (firstCause.type()) {
                    case PLAYER -> receiver.sendReport(this, actions, "{txt#cause} picked up {txt#item}", cause, name);
                    case BLOCK, DAMAGE, TNT -> receiver.sendReport(this, actions, "{txt#cause} destroyed {txt#item}", cause, name);
                    case ENTITY -> {
                        if (item.uniqueId().map(uuid -> uuid.equals(firstCause.uuid())).orElse(false)) {
                            receiver.sendReport(this, actions, "{txt#item} despawned", name);
                        } else {
                            receiver.sendReport(this, actions, "{txt#cause} picked up {txt#item}", cause, name);
                        }
                    }
                }
                return;
            }
        }
        if (isLiving) {
            receiver.sendReport(this, actions, actions.size(),
                    "{txt} killed {txt}",
                    "{txt} killed {txt} x{}",
                    cause, Recall.entityType(entityType), actions.size());
            return;
        }

        if (EntityTypes.EXPERIENCE_ORB.get().equals(entityType)) {
            int exp = 0;
            for (Action a : actions) {
                EntitySnapshot orb = Recall.entitySnapshot(a);
                exp += Recall.expFromEntitySnapshot(orb);
            }

            receiver.sendReport(this, actions, actions.size(),
                    "{txt} picked up an ExpOrb worth {2:amount} points",
                    "{txt} picked up {amount} ExpOrbs worth {amount} points",
                    cause, actions.size(), exp);
            return;
        }
        receiver.sendReport(this, actions, actions.size(),
                "{txt} destroyed {txt}",
                "{txt} destroyed {txt} x{}",
                cause, Recall.entityType(entityType), actions.size());
    }


    @Override
    public void apply(Action action, boolean noOp) {

    }

    @Override
    public void unapply(final Action action, final boolean noOp) {

    }

    @Override
    public Action observe(DestructEntityEvent event) {
        return newAction(event.cause()).addEntityDeath(event.entity());
    }

    @Listener
    public void onAttack(AttackEntityEvent event) {
        //System.out.print(event.getCause()+ "\n");
        //System.out.print(event.getTargetEntity() + "\n");
    }

    @Listener
    public void onDestruct(DestructEntityEvent event) {
    /* TODO    if (event.getCause().get("CombinedItem", Object.class).isPresent())
        {
            // Ignore CombinedItem
            return;
        }

     TODO     if (event.getCause().get("PickedUp", Object.class).isPresent())
        {
            // Ignore Pickup
            return;
        }
        */
        // TODO picked up by player entity has empty item stack inside :/

        if (!isActive(event.entity().serverLocation().world())) {
            return;
        }

        report(observe(event));
    }

    @Override
    public Duration maxDiff() {
        return Duration.ofMinutes(30);
    }
}
