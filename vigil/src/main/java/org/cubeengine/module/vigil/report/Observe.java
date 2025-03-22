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
package org.cubeengine.module.vigil.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockChange;
import org.cubeengine.module.vigil.action.BlockData;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.StoredCause.CauserReason;
import org.cubeengine.module.vigil.action.EntityChange;
import org.cubeengine.module.vigil.action.InventoryChange;
import org.cubeengine.module.vigil.action.LocatableChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Piston;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.explosive.fused.PrimedTNT;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.module.vigil.action.Causer.Type.BLOCK;
import static org.cubeengine.module.vigil.action.Causer.Type.DAMAGE;
import static org.cubeengine.module.vigil.action.Causer.Type.PLAYER;
import static org.cubeengine.module.vigil.action.Causer.Type.TNT;
import static org.cubeengine.module.vigil.action.StoredCause.CauserReason.DETONATOR;
import static org.cubeengine.module.vigil.action.StoredCause.CauserReason.INDIRECT;
import static org.cubeengine.module.vigil.action.StoredCause.CauserReason.DEFAULT;

public class Observe {

    public static StoredCause causes(Cause causes) {
        var causeList = causes.all().stream().distinct()
                .map(Observe::causer)
                .filter(Objects::nonNull)
                .filter(m -> !m.isEmpty())
                .distinct() // removes duplicate causes
                .toList();


        var context = causes.context().asMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().key().asString(),
                        e -> Observe.causer(e.getValue())));

        return new StoredCause(context, causeList);
    }

    private static Map<CauserReason, Causer> causer(Object cause) {

        if (cause instanceof DamageSource dmg) {
            if (dmg.source().isPresent()) {
                Map<CauserReason, Causer> map = new HashMap<>(Observe.entityCauser(dmg.source().orElse(null), true, StoredCause.CauserReason.DEFAULT));
                if (dmg.indirectSource().isPresent() && dmg.source().get() != dmg.indirectSource().get()) {
                    map.putAll(Observe.entityCauser(dmg.indirectSource().get(), true, INDIRECT));
                }
                return map;
            }

            if (dmg.blockSnapshot().isPresent()) {
                return Map.of(DEFAULT, Observe.blockCauser(dmg.blockSnapshot().get().state()));
            }
            return Map.of(DEFAULT, Observe.damageCause(dmg));

        }
        if (cause instanceof UUID uuid) {
            return Map.of(DEFAULT, userCauser(uuid));
        }
        if (cause instanceof User user) {
            return Map.of(DEFAULT, userCauser(user));
        }
        if (cause instanceof BlockEntity be) {
            return Map.of(DEFAULT, blockCauser(be.block()));
        }
        if (cause instanceof LocatableBlock block) {
            return Map.of(DEFAULT, blockCauser(block.blockState()));
        }
        if (cause instanceof BlockSnapshot snap) {
            return Map.of(DEFAULT, blockCauser(snap.state()));
        }
        if (cause instanceof Entity) {
            return entityCauser(((Entity) cause), true, DEFAULT);
        }
        // TODO other causes that interest us
        return Map.of();
    }

    private static Causer damageCause(DamageSource cause) {
        return Causer.of(DAMAGE, cause.type().key(RegistryTypes.DAMAGE_TYPE));
    }

    private static Map<CauserReason, Causer> tntCauser(PrimedTNT cause) {
        Map<CauserReason, Causer> map = new HashMap<>();

        var eType = cause.type().key(RegistryTypes.ENTITY_TYPE);
        map.put(DEFAULT, Causer.of(TNT, eType));

        cause.get(Keys.DETONATOR).ifPresent(detonator -> {
            if (detonator instanceof ServerPlayer pd) {
                map.put(DETONATOR, playerCauser(pd));
            } else {
                map.putAll(entityCauser(detonator, false, DETONATOR));
            }
        });
        return map;
    }

    private static Map<CauserReason, Causer> entityCauser(Entity cause, boolean doRecursion, CauserReason reason) {
        switch (cause) {
            case null -> {
                return Map.of();
            }
            case ServerPlayer player -> {
                return Map.of(reason, playerCauser(player));
            }
            case PrimedTNT primedTNT when doRecursion -> {
                return tntCauser(primedTNT);
            }
            default -> {
                var map = new HashMap<CauserReason, Causer>();
                map.put(reason, Causer.ofEntity(cause));
                if (doRecursion && cause instanceof Agent agent) {
                    agent.targetEntity().ifPresent(agentTarget ->
                            map.putAll(Observe.entityCauser(agentTarget.get(), false, StoredCause.CauserReason.AGENT_TARGET)));
                }
                return map;
            }
        }
    }

    public static Causer blockCauser(BlockState block) {
        return Causer.of(BLOCK, block.type().key(RegistryTypes.BLOCK_TYPE));
    }

    public static Causer playerCauser(ServerPlayer player) {
        var causer = Causer.ofPlayer(PLAYER, player);
        // TODO configurable data.put("ip", player.getConnection().getAddress().getAddress().getHostAddress());
        return causer;
    }

    public static Causer userCauser(UUID player) {
        return new Causer(PLAYER, player, null, null);
    }

    public static Causer userCauser(User player) {
        var causer = Causer.ofUser(PLAYER, player);
        return causer;
    }

    public static BlockData blockSnapshot(BlockSnapshot block) {
        final var blockState = block.state().toContainer();
        var blockContainer = block.toContainer();
        return new BlockData(block.state().type().key(RegistryTypes.BLOCK_TYPE), blockState, blockContainer);
    }

    /**
     * Observes a SlotTransaction
     *
     * @param transactions the transaction to observe
     * @return the observed data
     */
    public static List<InventoryChange> transactions(List<SlotTransaction> transactions) {
        return transactions.stream().map(transaction ->
                new InventoryChange(transaction.original().toContainer(),
                        transaction.finalReplacement().toContainer(),
                        transaction.slot().get(Keys.SLOT_INDEX).orElse(-1))).toList();
    }

    public static EntityChange entityDeath(final Action action, Entity entity) {
        final var type = entity.type().key(RegistryTypes.ENTITY_TYPE);
        return new EntityChange(action, entity.serverLocation().position(), type, entity.createSnapshot().toContainer(), null, entity instanceof Living);
    }

    public static LocatableChange position(final ServerLocation location) {
        return new LocatableChange(location.position());
    }

    public static BlockChange blockChange(final Action action, final BlockTransactionReceipt receipt, final ServerLocation at) {
        var op = receipt.operation().key(RegistryTypes.OPERATION).asString();
        final var original = blockSnapshot(receipt.originalBlock());
        final var replacement = blockSnapshot(receipt.finalBlock());
        return new BlockChange(action, at.position(), op, original, replacement);
    }
}
