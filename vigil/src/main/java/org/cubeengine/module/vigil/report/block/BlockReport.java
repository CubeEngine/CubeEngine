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
package org.cubeengine.module.vigil.report.block;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockChange;
import org.cubeengine.module.vigil.reporting.Recall;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.tag.BlockTypeTags;
import org.spongepowered.api.world.server.ServerLocation;

import static org.spongepowered.api.block.BlockTypes.AIR;

/* TODO Break
Sign
Trample ?
Bucket
Inventory
Jukebox item
tnt ignite?
decay
fade
fall

sheep eat

entitybreak
endermanpickup
 */
/* TODO Place
Bucket
grow

ignite (fire spreads)
-fireball
-lava
-lighter
-lightning
-other
-spread

flow
-lava
-water

form
grow

pistonmove
spread

entityform
entitychange
endermanplace
 */
/* TODO change
sign
noteblock
jukebox
repeater
plate
lever
door
comparatpr
cake
button

bonemeal?

TODO explosion
creeper
enderdrag
entity?
fireball
tnt
wither
 */
public class BlockReport extends BaseReport<ChangeBlockEvent.Post> {

    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.STONE);
        var tr = i18n.translate(audience, "Block Changes");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }

    private enum BlockOp {
        BREAK, FLUID_REMOVE, PLACE, MODIFY, REPLACE
    }

    @Override
    public void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine) {
        var actions = reportLine.actions();
        Action firstAction = actions.getFirst();
        var causer = firstAction.firstCauser(StoredCause.CauserReason.DEFAULT);
        var causeComponent = Recall.causeAsComponent(firstAction);

        Map<BlockOp, List<BlockChange>> changeByOp = new HashMap<>();
        List<BlockChange> allBlockChanges = new ArrayList<>();
        List<BlockOp> allOps = new ArrayList<>();

        BlockOp lastOp = null;
        for (final var locatable : reportLine.locatables()) {
            if (locatable instanceof BlockChange bc) {
                // TODO cache this
                var orig = Recall.blockState(bc.original()).orElse(AIR.get().defaultState());
                var repl = Recall.blockState(bc.replacement()).orElse(AIR.get().defaultState());

                if (repl.type().is(BlockTypeTags.AIR)) {
                    if (orig.fluidState().isEmpty()) {
                        lastOp = BlockOp.BREAK;
                    } else {
                        lastOp = BlockOp.FLUID_REMOVE;
                    }
                } else {
                    if (repl.type().equals(orig.type())) {
                        lastOp = BlockOp.MODIFY;
                    } else if (orig.type().is(BlockTypeTags.AIR)) {
                        lastOp = BlockOp.PLACE;
                    } else {
                        lastOp = BlockOp.REPLACE;
                    }
                }
                changeByOp.computeIfAbsent(lastOp, k -> new ArrayList<>()).add(bc);
                allBlockChanges.add(bc);
                allOps.add(lastOp);
            }
        }

        if (changeByOp.size() > 1) {
            if (reportLine.positions().size() == 1) {
                // more than one operation at the same location
                sendReportSingleChange(receiver, lastOp, actions, causeComponent, allBlockChanges, allOps, causer);
            } else {
                // TODO hover show details
                var stats = hoverStatsByOp(changeByOp);
                receiver.sendReport(this, actions, "{txt} changed {} blocks", causeComponent, allBlockChanges.size());
            }
            return;
        }
        for (final var entry : changeByOp.entrySet()) {
            final var op = entry.getKey();
            final var changes = entry.getValue();

            if (allBlockChanges.size() == 1) {
                sendReportSingleChange(receiver, op, actions, causeComponent, changes, allOps, causer);
            } else {
                // TODO single block type

                // TODO hover show details
                var hover = hoverStatsByBlock(op, changeByOp.get(op));
                switch (op) {
                    case BREAK -> receiver.sendReport(this, actions, "{txt} broke {} blocks", causeComponent, changes.size());
                    case FLUID_REMOVE -> receiver.sendReport(this, actions, "{txt} removed {} fluids", causeComponent, changes.size());
                    case PLACE -> {
                        if (changes.stream().map(bc -> bc.replacement().block()).distinct().count() == 1) {
                            var repl = Recall.blockTypeAsComponent(changes.getFirst().replacement().block());
                            receiver.sendReport(this, actions, "{txt} placed {txt#block} x {}", causeComponent, repl, changes.size());
                        } else {
                            receiver.sendReport(this, actions, "{txt} placed {} blocks", causeComponent, changes.size());
                        }
                    }
                    case MODIFY -> sendModifyReport(receiver, actions, causer, causeComponent, changes);
                    case REPLACE -> sendReplaceBlockReport(receiver, actions, causer, changes, causeComponent);
                }
            }


        }
    }

    private void sendModifyReport(final Receiver receiver, final List<Action> actions, final Causer causer, final Component causeComponent, final List<BlockChange> changes) {
        long originalTypes = changes.stream().map(bc -> bc.original().block()).distinct().count();
        if (originalTypes == 1) {
            var origKey = changes.getFirst().original().block();
            var origName = Recall.blockTypeAsComponent(origKey);
            var orig = Recall.blockType(origKey).orElse(AIR.get());
            var interactions = 0;
            // TODO more interactable blocks
            if (orig.is(BlockTypeTags.BUTTONS) || orig.is(BlockTypeTags.PRESSURE_PLATES) || orig.isAnyOf(BlockTypes.LEVER)) {
                interactions = changes.size();
            }
            if (orig.is(BlockTypeTags.DOORS) || orig.is(BlockTypeTags.BEDS)) {
                interactions = changes.size() / 2;
            }
            if (interactions > 0 && (causer.type().equals(Causer.Type.PLAYER) || causer.type().equals(Causer.Type.ENTITY))) {
                receiver.sendReportX(this, actions, interactions, "{txt} used {txt#block}", causeComponent, origName);
                return;
            }

            if (orig.is(BlockTypeTags.CROPS)) {
                if (causer.type().equals(Causer.Type.BLOCK) && causer.resourceKey().equals(origKey)) {
                    receiver.sendReportX(this, actions, changes.size(), "{txt} grew", causeComponent);
                } else {
                    receiver.sendReportX(this, actions, changes.size(), "{txt} grew {txt#block}", causeComponent, origName);
                }
                return;
            }

            // TODO plant grow
            receiver.sendReportX(this, actions, changes.size(), "{txt} modified {txt#block}", causeComponent, origName);
            return;
        }
        // TODO plant grow detection?
        receiver.sendReport(this, actions, "{txt} modified {} blocks", causeComponent, changes.size());
    }



    private void sendReplaceBlockReport(Receiver receiver, List<Action> actions, Causer causer, List<BlockChange> changes, Component causeComponent) {
        long replacementTypes = changes.stream().map(bc -> bc.replacement().block()).distinct().count();
        long originalTypes = changes.stream().map(bc -> bc.original().block()).distinct().count();
        if (replacementTypes == 1 && originalTypes == 1) {
            var orig = Recall.blockTypeAsComponent(changes.getFirst().original().block());
            var repl = Recall.blockTypeAsComponent(changes.getLast().replacement().block());
            // TODO trample detection
            // TODO fluid mix
            if (causer.type().equals(Causer.Type.BLOCK) && causer.resourceKey().equals(changes.getFirst().replacement().block())) {
                // single replacement type same as cause
                receiver.sendReportX(this, actions, changes.size(), "{txt#block} spread to {txt#block}", causeComponent, orig);
                return;
            }
            if (causer.type().equals(Causer.Type.ENTITY) && causer.resourceKey().equals(EntityTypes.SHEEP.location())) {
                receiver.sendReportX(this, actions, changes.size(), "{txt} ate {txt#block}", causeComponent, orig);
                return;
            }
            receiver.sendReport(this, actions, "{txt} replaced {txt#block} with {txt#block}", causeComponent, orig, repl);
            return;
        }
        // TODO hover show details
        receiver.sendReport(this, actions, "{txt} replaced {} blocks", causeComponent, changes.size());
    }



    private Component hoverStatsByBlock(final BlockOp op, final List<BlockChange> changes) {
        var builder = Component.empty().toBuilder();
        var byType = changes.stream().map(bc -> {
            var orig = Recall.blockState(bc.original()).map(BlockState::type).orElse(AIR.get());
            var repl = Recall.blockState(bc.replacement()).map(BlockState::type).orElse(AIR.get());
            return switch (op) {
                case BREAK, FLUID_REMOVE, MODIFY, REPLACE -> orig;
                case PLACE -> repl;
            };
        }).collect(Collectors.groupingBy(bs -> bs, Collectors.counting()));
        byType.forEach((type, count) ->
                builder.append(type).append(Component.space()).append(Component.text(count)).append(Component.newline()));
        return builder.build();
    }

    private Component hoverStatsByOp(final Map<BlockOp, List<BlockChange>> changeByOp) {
        var builder = Component.empty().toBuilder();
        changeByOp.forEach((op, blockChanges) -> {
            if (!blockChanges.isEmpty()) {
                builder.append(Component.text(op.name())).append(Component.space()).append(Component.text(blockChanges.size()))
                        .append(Component.newline());
            }
        });
        return builder.build();
    }

    private void sendReportSingleChange(final Receiver receiver, final BlockOp op, final List<Action> actions, final Component cause,
            final List<BlockChange> changes,
            final List<BlockOp> allOps, Causer causer) {

        if (changes.size() > 1) {
            // TODO hover show details
            var hover = hoverChanges(receiver, cause, changes, allOps);
        }
        var orig = Recall.blockTypeAsComponent(Recall.blockState(changes.getFirst().original()).map(BlockState::type).orElse(AIR.get()));
        var repl = Recall.blockTypeAsComponent(Recall.blockState(changes.getLast().replacement()).map(BlockState::type).orElse(AIR.get()));
        switch (op) {
            // TODO hover info for signs etc.
            // TODO fading/decay leaves/snow
            // TODO entity forming
            case BREAK -> receiver.sendReport(this, actions, "{txt} broke {txt#block}", cause, orig);
            case FLUID_REMOVE -> receiver.sendReport(this, actions, "{txt} removed {txt#fluid}", cause, orig);
            // TODO ignite/fire spread
            // TODO fluid flow
            // TODO tree grow
            case PLACE -> receiver.sendReport(this, actions, "{txt} placed {txt#block}", cause, repl);
            case MODIFY -> sendModifyReport(receiver, actions, causer, cause, changes);
            case REPLACE -> sendReplaceBlockReport(receiver, actions, causer, changes, cause);
        }
    }




    private Component hoverChanges(final Receiver receiver, final Component cause, final List<BlockChange> changes, final List<BlockOp> allOps) {
        var builder = Component.empty().toBuilder();
        for (var i = changes.size() - 1; i >= 0; i--) {
            var change = changes.get(i);
            var op = allOps.get(i);
            var orig = Recall.blockState(change.original()).map(BlockState::type).orElse(AIR.get()).asComponent();
            var repl = Recall.blockState(change.replacement()).map(BlockState::type).orElse(AIR.get()).asComponent();
            var i18n = receiver.getI18n();
            var sender = receiver.getSender();

            var changeText = switch (op) {
                case BREAK -> i18n.translate(sender, "{txt} broke {txt#block}", cause, orig);
                case FLUID_REMOVE -> i18n.translate(sender, "{txt} removed {txt#fluid}", cause, orig);
                case PLACE -> i18n.translate(sender, "{txt} placed {txt#block}", cause, repl);
                case MODIFY -> i18n.translate(sender, "{txt} modified {txt#block}", cause, orig);
                // TODO no need to repeat prev. in between
                case REPLACE -> i18n.translate(sender, "{txt} replaced {txt#block} with {txt#block}", cause, orig, repl);
            };
            builder.append(Component.text("↴").append(Component.space())).append(changeText).append(Component.newline());
        }
        return builder.build();
    }

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Post event) {
        report(observe(event));
    }

    @Override
    protected Action observe(ChangeBlockEvent.Post event) {
        final var action = newAction(event.cause());

        for (BlockTransactionReceipt receipt : event.receipts()) {
            if (receipt.originalBlock().equals(receipt.finalBlock())) {
                continue;
            }
            if (receipt.originalBlock().location().isEmpty()) {
                continue;
            }
            final ServerLocation loc = receipt.originalBlock().location().get();
            if (!isActive(loc.world())) {
                continue;
            }
            if (isRedstoneChange(receipt.originalBlock().state(), receipt.finalBlock().state())) {
                continue;
            }
            action.addBlockChange(receipt, loc);
        }

        return action;
    }


    private static boolean isRedstoneChange(BlockState origState, BlockState finalState) {
        if (!origState.type().equals(finalState.type())) {
            return false;
        }
        return origState.type().isAnyOf(BlockTypes.REDSTONE_WIRE, BlockTypes.REPEATER, BlockTypes.COMPARATOR,
                BlockTypes.REDSTONE_TORCH, BlockTypes.REDSTONE_WALL_TORCH,
                BlockTypes.DROPPER, BlockTypes.DISPENSER, BlockTypes.HOPPER);
    }


    @Override
    public void apply(Action action, boolean noOp) {
        // TODO noOp preview
        //        action.getCached(BLOCKS_REPL, Recall::replSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }

    @Override
    public void unapply(Action action, boolean noOp) {
        // TODO noOp preview
        //        action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }

    @Override
    public Duration maxDiff() {
        return Duration.ofMinutes(1);
    }
}
