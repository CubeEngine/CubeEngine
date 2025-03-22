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
package org.cubeengine.module.vigil.report.inventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.reporting.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerLocation;

import static org.spongepowered.api.item.inventory.ItemStackComparators.ITEM_DATA;
import static org.spongepowered.api.item.inventory.ItemStackComparators.TYPE;

/* TODO
inventory
-insert
-remove
-move
-item-pickup
 */
public class ChangeInventoryReport extends BaseReport<ClickContainerEvent> implements Report.Readonly
{
    private static final Comparator<ItemStack> COMPARATOR = TYPE.get().thenComparing(ITEM_DATA.get());


    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.BARREL);
        var tr = i18n.translate(audience, "Inventory Changes");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }

    @Override
    public void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine)
    {
        final var actions = reportLine.actions();
        Component cause = Recall.causeAsComponent(actions.get(0));

        LinkedList<Transaction<ItemStack>> transactions = new LinkedList<>();

        for (Action action : actions)
        {
            var changes = action.inventoryChanges;
            for (var change : changes)
            {
                var originStack = Recall.itemStack(change.originalStack());
                var finalStack = Recall.itemStack(change.replacementStack());

                if (COMPARATOR.compare(originStack, finalStack) == 0)
                {
                    if (originStack.quantity() > finalStack.quantity())
                    {
                        ItemStack stack = originStack;
                        stack.setQuantity(originStack.quantity() - finalStack.quantity());
                        originStack = stack;
                        finalStack = ItemStack.empty();
                    }
                    else
                    {
                        ItemStack stack = finalStack;
                        stack.setQuantity(finalStack.quantity() - originStack.quantity());
                        finalStack = stack;
                        originStack = ItemStack.empty();
                    }
                }

                boolean added = false;
                for (Transaction<ItemStack> trans : transactions)
                {
                    if (originStack.isEmpty())
                    {
                        if (COMPARATOR.compare(trans.finalReplacement(), finalStack) == 0 && trans.original().isEmpty())
                        {
                            trans.finalReplacement().setQuantity(trans.finalReplacement().quantity() + finalStack.quantity());
                            added = true;
                            break;
                        }
                        else if (trans.finalReplacement().isEmpty() && COMPARATOR.compare(trans.original(), finalStack) == 0)
                        {
                            trans.original().setQuantity(trans.original().quantity() - finalStack.quantity());
                            added = true;
                            break;
                        }
                        else if (COMPARATOR.compare(trans.original(), finalStack) == 0)
                        {
                            break;
                        }
                    }
                    if (finalStack.isEmpty())
                    {
                        if (COMPARATOR.compare(trans.original(), originStack) == 0)
                        {
                            trans.original().setQuantity(trans.original().quantity() + originStack.quantity());
                            added = true;
                            break;
                        }
                        else if (trans.original().isEmpty() && COMPARATOR.compare(trans.finalReplacement(), originStack) == 0)
                        {
                            trans.finalReplacement().setQuantity(trans.finalReplacement().quantity() - originStack.quantity());
                            added = true;
                            break;
                        }
                        else if (COMPARATOR.compare(trans.finalReplacement(), originStack) == 0)
                        {
                            break;
                        }
                    }
                }
                if (!added)
                {
                    transactions.addFirst(new Transaction<>(originStack, finalStack));
                }

            }
        }

        Collections.reverse(transactions);

        for (Transaction<ItemStack> trans : transactions)
        {
            ItemStack stack1 = trans.original();
            ItemStack stack2 = trans.finalReplacement();
            if (stack1.isEmpty() && stack2.isEmpty())
            {
                continue;
            }
            if (stack1.type().isAnyOf(ItemTypes.AIR))
            {
                receiver.sendReport(this, actions, "{txt} inserted {txt}", cause, Recall.stack(stack2.asImmutable(), stack2.quantity()));
            }
            else if (stack2.type().isAnyOf(ItemTypes.AIR))
            {
                receiver.sendReport(this, actions, "{txt} took {txt}", cause, Recall.stack(stack1.asImmutable(), stack1.quantity()));
            }
            else
            {
                receiver.sendReport(this, actions, "{txt} swapped {txt} with {txt}", cause, Recall.stack(stack1.asImmutable(), stack1.quantity()), Recall.stack(stack2.asImmutable(), stack2.quantity()));
            }
        }
    }



    @Override
    public void apply(Action action, boolean noOp)
    {

    }

    @Listener
    public void listen(ClickContainerEvent event)
    {
        report(observe(event));
    }

    private static List<SlotTransaction> collectTransactions(final ClickContainerEvent event) {
        List<SlotTransaction> upperTransactions = new ArrayList<>();
        int upperSize = event.inventory().viewed().getFirst().capacity();
        for (SlotTransaction transaction : event.transactions())
        {
            // TODO slot parent is not player check instead?
            int affectedSlot = transaction.slot().get(Keys.SLOT_INDEX).orElse(-1);
            boolean upper = affectedSlot != -1 && affectedSlot < upperSize;
            if (upper)
            {
                upperTransactions.add(transaction);
            }
        }
        return upperTransactions;
    }

    @Override
    public Action observe(ClickContainerEvent event)
    {
        final var container = event.container();
        var location = containerLocation(container);
        if (location == null) {
            return null;
        }
        var transactions = collectTransactions(event);
        if (transactions.isEmpty()) {
            return null;
        }
        return newActionAt(event.cause(), location).withInventoryChanges(transactions);
    }

    public static ServerLocation containerLocation(final Container inventory) {
        if (inventory instanceof CarriedInventory<?> carried) {
            return carried.carrier().map(carrier -> {
                if (carrier instanceof Locatable locatableCarrier) {
                    return locatableCarrier.serverLocation();
                }
                return null;
            }).orElse(null);
        }
        return null;
    }

    @Override
    public Duration maxDiff() {
        return Duration.ofMinutes(1);
    }
}
