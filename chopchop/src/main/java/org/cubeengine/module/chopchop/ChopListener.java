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
package org.cubeengine.module.chopchop;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import com.google.inject.Inject;
import net.kyori.adventure.sound.Sound;
import org.cubeengine.libcube.util.ItemUtil;
import org.cubeengine.module.chopchop.ChopchopConfig.Tree;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static java.util.Collections.emptyList;
import static org.spongepowered.api.item.ItemTypes.APPLE;
import static org.spongepowered.api.item.ItemTypes.DIAMOND_AXE;
import static org.spongepowered.api.util.Direction.*;

public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);
    private Chopchop module;

    @Inject
    public ChopListener(Chopchop module)
    {
        this.module = module;
    }

    private boolean isLeaf(ServerWorld world, Vector3i pos, Tree species)
    {
        final BlockState state = world.block(pos);
        final BlockType type = state.type();
        return species.leafType == type;
    }

    private boolean isLog(ServerWorld world, Vector3i pos, Tree species)
    {
        final BlockState state = world.block(pos);
        final BlockType type = state.type();
        return species.logType == type;
    }

    private boolean isSoil(BlockType belowType) {
        return module.getConfig().soilTypes.contains(belowType);
    }

    @SuppressWarnings("unchecked")
    @Listener
    public void onChop(final ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        if (event.transactions(Operations.BREAK.get()).count() != 1 ||
            player.itemInHand(HandTypes.MAIN_HAND).isEmpty() ||
            event.cause().context().containsKey(EventContextKeys.SIMULATED_PLAYER))
        {
            return;
        }

        ItemStack axe = player.itemInHand(HandTypes.MAIN_HAND);
        if (!axe.type().isAnyOf(DIAMOND_AXE) ||
            axe.get(Keys.ITEM_DURABILITY).orElse(0) <= 0 ||
            !axe.get(Keys.APPLIED_ENCHANTMENTS).orElse(emptyList()).contains(Enchantment.of(EnchantmentTypes.PUNCH, 5)))
        {
            return;
        }
        if (!module.usePerm.check(player))
        {
            return;
        }

        event.transactions(Operations.BREAK.get())
             .filter(trans -> isLog(trans.original().state()))
             .filter(trans -> isSoil(trans.original().location().get().relativeTo(DOWN).blockType()))
             .flatMap(trans -> findChopResult(player, trans.original()).stream())
             .findFirst().ifPresent(chopResult -> chopResult.chopTree(player, axe));
    }

    private record ChopResult(Tree tree, ServerLocation origin, Set<Vector3i> logs, Set<Vector3i> leaves, Set<Vector3i> replantSaplings, List<ItemStack> stacks)
    {
        public void chopTree(ServerPlayer player, ItemStack axe)
        {
            var silktouch = axe.get(Keys.APPLIED_ENCHANTMENTS).orElse(List.of()).contains(Enchantment.of(EnchantmentTypes.SILK_TOUCH, 1));


            var random = new Random();
            final var csm = Sponge.server().causeStackManager();
            csm.addContext(EventContextKeys.SIMULATED_PLAYER, player.profile());


            int logs = 0;
            int leaves = 0;
            for (final Vector3i pos : this.logs)
            {
                if (logs++ % 5 == 0) {
                    origin.world().playSound(Sound.sound(SoundTypes.BLOCK_WOOD_BREAK, Sound.Source.NEUTRAL, 0.5f, 0.8f), pos.toDouble());
                }
                origin.world().removeBlock(pos);
            }

            for (final Vector3i pos : this.leaves)
            {
                origin.world().removeBlock(pos);
                if (leaves++ % 6 == 1) {
                    origin.world().playSound(Sound.sound(SoundTypes.BLOCK_GRASS_BREAK, Sound.Source.NEUTRAL, 0.5f, 0.8f), pos.toDouble());
                }
            }

            this.stacks.add(ItemStack.of(tree.logType.item().get(), logs));
            int saplingDrops = leaves / 20;
            if (this.tree.leafType.isAnyOf(BlockTypes.JUNGLE_LEAVES))
            {
                saplingDrops = leaves / 40;
            }

            saplingDrops -= replantSaplings.size();
            if (silktouch)
            {
                this.stacks.add(ItemStack.of(tree.leafType.item().get(), leaves));
            }
            else
            {
                this.stacks.add(ItemStack.of(tree.saplingType.item().get(), Math.max(1, saplingDrops)));
            }

            if (this.tree.leafType.isAnyOf(BlockTypes.DARK_OAK_LEAVES, BlockTypes.OAK_LEAVES))
            {
                var apples = (int) IntStream.range(0, leaves).filter(i -> random.nextDouble() < 0.005).count();
                this.stacks.add(ItemStack.of(APPLE, apples));
            }

            final var sapState = tree.saplingType.defaultState();
            for (final Vector3i replantSapling : this.replantSaplings)
            {
                origin.world().setBlock(replantSapling, sapState);
            }

            final int uses = axe.require(Keys.ITEM_DURABILITY) - logs;
            axe.offer(Keys.ITEM_DURABILITY, uses);
            player.setItemInHand(HandTypes.MAIN_HAND, axe);

            csm.removeContext(EventContextKeys.SIMULATED_PLAYER);
            csm.pushCause(player);

            for (final ItemStack stack : this.stacks)
            {
                ItemUtil.spawnItem(origin, stack);
            }
        }
    }

    private Tree getTreeType(BlockType logType)
    {
        for (Tree tree : this.module.getConfig().trees)
        {
            if (tree.logType == logType)
            {
                return tree;
            }
        }
        return null;
    }

    private boolean isLog(BlockState state)
    {
        return this.isLog(state.type());
    }


    private boolean isLog(BlockType type)
    {
        return this.module.getConfig().trees.stream().anyMatch(tree -> tree.logType == type);
    }

    private Optional<ChopResult> findChopResult(ServerPlayer player, BlockSnapshot original)
    {
        var world = original.location().get().world();
        var pos = original.position();
        var species = getTreeType(original.state().type());

        Set<Vector3i> logs = new HashSet<>();
        Set<Vector3i> leaves = new HashSet<>();
        Set<Vector3i> saplings = new HashSet<>();

        logs.add(pos);
        findTrunk(world, pos, pos, species, logs);
        findLeaves(world, logs, leaves, species);

        if (leaves.isEmpty())
        {
            return Optional.empty();
        }

        if (this.module.autoplantPerm.check(player))
        {
            BlockType belowTyp = world.block(pos.add(DOWN.asBlockOffset())).type();
            if (isSoil(belowTyp))
            {
                saplings.add(pos);
            }
        }

        return Optional.of(new ChopResult(species, original.location().get(), logs, leaves, saplings, new ArrayList<>()));
    }

    private void findLeaves(ServerWorld world, Set<Vector3i> logs, Set<Vector3i> finalLeaves, Tree species)
    {
        Set<Vector3i> leaves = new HashSet<>();
        for (Vector3i log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        Vector3i relative = log.add(x, y, z);
                        if (isLeaf(world, relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<Vector3i> lastLayer = new HashSet<>(logs);
        do
        {
            Set<Vector3i> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Vector3i layer : curLayer)
            {
                for (Vector3i leaf : leaves)
                {
                    Vector3i diff = layer.sub(leaf).abs();
                    if (diff.x() + diff.y() + diff.z() == 1 // cardinal or upright
                    || (diff.x() + diff.y() == 2 && diff.x() == diff.y())) // ordinal
                    {
                        lastLayer.add(leaf);
                    }
                }
                leaves.removeAll(lastLayer);
            }
            finalLeaves.addAll(lastLayer);
        }
        while (!lastLayer.isEmpty());
    }

    private void findTrunk(ServerWorld world, Vector3i root, Vector3i base, Tree species, Set<Vector3i> trunk)
    {
        Set<Vector3i> blocks = new HashSet<>();
        for (Direction face : dir8)
        {
            Vector3i relative = base.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(world, relative, species))
            {
                if (base.distanceSquared(relative) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Vector3i up = base.add(UP.asBlockOffset());
        if (!trunk.contains(up) && isLog(world, up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            Vector3i relative = up.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(world, relative, species))
            {
                if (root.distanceSquared(relative) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (Vector3i block : blocks)
        {
            findTrunk(world, root, block, species, trunk);
        }
    }

}
