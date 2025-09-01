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
package org.cubeengine.module.vigil.reporting;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.JoinConfiguration.separator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockChange;
import org.cubeengine.module.vigil.action.BlockData;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.EntityChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Nameable;
import org.spongepowered.math.vector.Vector3i;

public class Recall {


    public static final TextComponent UNKNOWN = Component.text("?");

    public static Optional<BlockSnapshot> block(BlockData data, Vector3i pos, ResourceKey world) {
        // TODO does this work?
        return BlockSnapshot.builder().build(data.snapshot());
    }

    public static Optional<BlockState> blockState(BlockData data) {
        return BlockState.builder().build(data.state());
    }

    public static ItemStack itemStack(DataContainer data) {
        return ItemStack.builder().fromContainer(data).build();
    }

    public static Optional<BlockSnapshot> origSnapshot(Action action) {
        // TODO there may be multiple
        return action.locatables.stream().filter(BlockChange.class::isInstance)
                .map(BlockChange.class::cast)
                .flatMap(bc -> snapshotOf(action, bc.original(), bc.blockPos()).stream())
                .findFirst();
    }

    private static Optional<BlockSnapshot> snapshotOf(final Action action, final BlockData data, final Vector3i pos) {
        return Recall.block(data, pos, action.world);
    }

    public static Optional<BlockSnapshot> replSnapshot(Action action) {
        // TODO there may be multiple
        return action.locatables.stream().filter(BlockChange.class::isInstance)
                .map(BlockChange.class::cast)
                .flatMap(bc -> Recall.block(bc.replacement(), bc.blockPos(), action.world).stream())
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public static Component causeAsComponent(Action action) {
        var list = action.causes.causes();

        if (list.isEmpty()) {
            return Component.text("No Cause recorded");
        }

        if (list.size() > 6) {
            list = list.subList(0, 6);
        }
        final Builder builder = Component.text();
        final TextComponent hoverTitle = Component.text("Cause Stack", NamedTextColor.GRAY).append(newline());

        var firstCausers = list.getFirst();
        var firstCauser = firstCausers.get(StoredCause.CauserReason.DEFAULT);
        if (firstCauser != null) {
            builder.append(causersAsComponent(firstCausers, Component.text("Root cause", NamedTextColor.GRAY).append(newline())));
        } else {
            builder.append(UNKNOWN);
        }

        list.stream().skip(1).forEach(causers -> {
            if (causers.get(StoredCause.CauserReason.DEFAULT) != null) {
                final Component cause = causersAsComponent(causers, Component.empty());
                final var hover = builder.build().append(newline()).append(Component.text("└")).append(cause);
                builder.append(Component.text("…").hoverEvent(HoverEvent.showText(hoverTitle.append(hover))));
            }
        });

        var notifier = action.causes.context().getOrDefault(EventContextKeys.NOTIFIER.key().asString(), Map.of()).get(StoredCause.CauserReason.DEFAULT);
        if (notifier != null && notifier.type() == Causer.Type.PLAYER && notifier.uuid() != null &&
            firstCauser != null && !notifier.uuid().equals(firstCauser.uuid())) {
            var notifierText = Component.text("!←", NamedTextColor.GRAY);
            var causeTT = notifierText.append(Component.space()).append(Component.text("Notifier").append(newline()));
            builder.append(notifierText).append(causersAsComponent(Map.of(StoredCause.CauserReason.DEFAULT, notifier), causeTT)); // TODO translate
        }
        return builder.build();

    }

    private static Component causersAsComponent(Map<StoredCause.CauserReason, Causer> causers, Component causeType) {
        final var defaultCauser = causers.get(StoredCause.CauserReason.DEFAULT);
        var targetType = defaultCauser.type();
        return switch (targetType) {
            case PLAYER -> {
                var name = defaultCauser.name();
                if (name == null) {
                    var profile = Sponge.server().gameProfileManager().profile(defaultCauser.uuid()).join();
                    name = profile.name().orElse("Unknown Player");
                }
                final var hoverUUID =
                        HoverEvent.showText(causeType.append(Component.text(defaultCauser.uuid().toString(), NamedTextColor.YELLOW)));
                yield Component.text(name, NamedTextColor.DARK_GREEN).hoverEvent(hoverUUID);
            }
            case BLOCK -> {
                final var bType = RegistryTypes.BLOCK_TYPE.get().findValue(defaultCauser.resourceKey());
                if (bType.isEmpty()) {
                    yield Component.text(defaultCauser.resourceKey().asString(), NamedTextColor.GOLD);
                }

                var blockTypeComponent = Recall.blockTypeAsComponent(bType.get());
                if (bType.get().isAnyOf(BlockTypes.LAVA, BlockTypes.FIRE)) {
                    yield blockTypeComponent.color(NamedTextColor.RED);
                }
                yield blockTypeComponent.color(NamedTextColor.GOLD);
            }
            case TNT -> {
                var tntCause = Component.text(defaultCauser.resourceKey().asString(), NamedTextColor.GOLD);
                var detonator = causers.get(StoredCause.CauserReason.DETONATOR);
                if (detonator != null) {
                    if (detonator.type() == Causer.Type.PLAYER) {
                        final var hoverUUID = HoverEvent.showText(Component.text(detonator.uuid().toString(), NamedTextColor.YELLOW));
                        tntCause = Component.text(detonator.name(), NamedTextColor.DARK_GREEN).hoverEvent(hoverUUID);
                    } else {
                        tntCause = Component.text(detonator.resourceKey().asString(), NamedTextColor.GOLD);
                    }
                }
                // TNT (detonator)
                yield Component.text("TNT", NamedTextColor.RED) // TODO translatable
                        .append(Component.space()).append(Component.text("(")).append(tntCause).append(Component.text(")"));
            }
            case DAMAGE -> {
                // TODO check if translation of damage type available?
                var damageTypeName = RegistryTypes.DAMAGE_TYPE.get().findValue(defaultCauser.resourceKey()).map(Nameable::name)
                        .map(msgId -> (Component) Component.translatable(msgId).asComponent().append(Component.text(msgId)))
                        .orElse(Component.text(defaultCauser.resourceKey().asString()));

                yield damageTypeName.color(NamedTextColor.GOLD);
            }
            case ENTITY -> {
                Component entityCause = RegistryTypes.ENTITY_TYPE.get().findValue(defaultCauser.resourceKey()).map(Recall::entityType)
                        // TODO hover
                        .orElse(Component.text(defaultCauser.resourceKey().asString())).color(NamedTextColor.GOLD);

                var agentTarget = causers.get(StoredCause.CauserReason.AGENT_TARGET);
                if (agentTarget != null) {
                    var component = causersAsComponent(Map.of(StoredCause.CauserReason.DEFAULT, agentTarget), Component.empty());
                    final var targettingHover = HoverEvent.showText(Component.text("targeting")); // TODO translate
                    entityCause = entityCause.append(Component.text("◎", NamedTextColor.GRAY).hoverEvent(targettingHover)).append(component);
                }

                var indirect = causers.get(StoredCause.CauserReason.INDIRECT);
                if (indirect != null) {
                    var component = causersAsComponent(Map.of(StoredCause.CauserReason.DEFAULT, indirect), Component.empty());
                    entityCause = entityCause.append(Component.text("↶", NamedTextColor.GRAY)).append(component);
                }
                yield entityCause;
            }
            case UNKNOWN -> Component.text("???");
        };
    }

    public static EntitySnapshot entitySnapshot(Action action) {
        return action.locatables.stream().filter(EntityChange.class::isInstance)
                .map(EntityChange.class::cast)
                .findFirst()
                .flatMap(change -> EntitySnapshot.builder().build(change.original()))
                // TODO failed to build entity snapshot?
                .orElse(null);
    }


    public static Optional<EntityType<?>> entity(final ResourceKey resourceKey) {
        return EntityTypes.registry().findValue(resourceKey);
    }

    public static EntityType<? extends Entity> entity(Action action) {
        return action.locatables.stream().filter(EntityChange.class::isInstance)
                .map(EntityChange.class::cast)
                .findFirst()
                .map(EntityChange::entityType)
                .flatMap(Recall::entity)
                .orElse(null);
    }


    public static Component blockTypeAsComponent(ResourceKey key) {
        final var type = RegistryTypes.BLOCK_TYPE.get().findValue(key);
        return type.map(blockType -> blockType.asComponent().hoverEvent(HoverEvent.showText(Component.text(key.asString()))))
                .orElseGet(() -> Component.text(key.asString(), NamedTextColor.GOLD));
    }

    public static Optional<BlockType> blockType(ResourceKey key) {
        return RegistryTypes.BLOCK_TYPE.get().findValue(key);
    }

    public static Component blockTypeAsComponent(BlockType type) {
        return type.asComponent().hoverEvent(HoverEvent.showText(Component.text(type.key(RegistryTypes.BLOCK_TYPE).asString())));
    }

    public static Component blockSnapshot(BlockSnapshot snapshot, final boolean detailedInventory) {
        BlockType type = snapshot.state().type();
        final Builder builder = Component.text();
        builder.append(type.asComponent().color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text(type.key(RegistryTypes.BLOCK_TYPE).asString()))));
        // TODO get items from snapshot? this should be components now, to we have API access?
        Optional<List<DataView>> items = snapshot.toContainer().getViewList(DataQuery.of("UnsafeData", "Items"));
        if (items.isPresent() && !items.get().isEmpty()) {
            // TODO lookup config : detailed inventory? click on ∋ to activate/deactivate or using cmd
            TextComponent elementsHover = Component.text("Inventory Content", NamedTextColor.GRAY);
            if (!detailedInventory) {
                elementsHover = elementsHover.append(Component.space()).append(Component.text("(hidden)", NamedTextColor.DARK_GRAY));
                // TODO
                // elementsHover = elementsHover.append(Component.newline()).append(Component.text("Click to show"));
            }

            final TextComponent elements = Component.text("∋").hoverEvent(HoverEvent.showText(elementsHover)); // TODO translate
            builder.append(Component.space()).append(elements).append(Component.space()).append(Component.text("["));
            if (detailedInventory) {
                builder.append(Component.space());
                for (DataView dataView : items.get()) {
                    DataContainer itemData = DataContainer.createNew();
                    itemData.set(DataQuery.of("Count"), dataView.get(DataQuery.of("Count")).get());
                    itemData.set(DataQuery.of("ItemType"), dataView.get(DataQuery.of("id")).get());

                    Optional<DataView> tag = dataView.getView(DataQuery.of("tag"));
                    if (tag.isPresent()) {
                        itemData.set(DataQuery.of("UnsafeData"), tag.get().values(false));
                    }

                    //                    itemData.set(DataQuery.of("UnsafeDamage"), dataView.get(DataQuery.of("Damage")).get());

                    ItemStack item = ItemStack.builder().fromContainer(itemData).build();
                    builder.append(Component.text(dataView.getInt(DataQuery.of("Slot")).get()).toBuilder()
                            .hoverEvent(item.asImmutable().asHoverEvent()).build());
                    builder.append(Component.space());
                }
            } else {
                builder.append(Component.text("..."));
            }
            builder.append(Component.text("]"));
        }

        Optional<List<Component>> sign = snapshot.get(Keys.SIGN_LINES);
        if (sign.isPresent()) {
            builder.append(Component.space())
                    .append(Component.text("[I]").hoverEvent(HoverEvent.showText(Component.join(separator(newline()), sign.get()))));
        }

        return builder.build();

    }

    public static Component entityType(EntityType<?> entity) {
        return entity.asComponent().hoverEvent(HoverEvent.showText(Component.text(entity.key(RegistryTypes.ENTITY_TYPE).asString())));
    }

    public static Component name(EntitySnapshot entity) {
        return entity.type().asComponent().hoverEvent(HoverEvent.showText(Component.text(entity.type().key(RegistryTypes.ENTITY_TYPE).asString())));
    }

    public static Component stack(ItemStackSnapshot stack, int quantity) {
        final var key = Component.text(stack.type().key(RegistryTypes.ITEM_TYPE).asString());
        var component = stack.asComponent().hoverEvent(HoverEvent.showText(key)).color(NamedTextColor.GOLD);
        return component.append(Component.text(" x", NamedTextColor.YELLOW))
                .append(Component.text(quantity, NamedTextColor.GRAY));
    }

    public static ItemStack stackFromEntitySnapshot(final EntitySnapshot item) {
        var stackKey = item.toContainer().getString(DataQuery.of("UnsafeData", "Item", "id"))
                .map(ResourceKey::resolve)
                .flatMap(key -> (Optional<ItemType>) ItemTypes.registry().findValue(key))
                .orElse(null);
        var stackCnt = item.toContainer().getInt(DataQuery.of("UnsafeData", "Item", "count")).orElse(1);
        if (stackKey == null) {
            return null;
        }
        return ItemStack.of(stackKey, stackCnt);
    }

    public static int expFromEntitySnapshot(final EntitySnapshot item) {
        return item.toContainer().getInt(DataQuery.of("UnsafeData", "Value")).orElse(0);
    }

}
