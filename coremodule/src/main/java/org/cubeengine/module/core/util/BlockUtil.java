/**
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
package org.cubeengine.module.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Optional;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.manipulator.mutable.block.AttachedData;
import org.spongepowered.api.data.manipulator.mutable.block.PortionData;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.data.type.Hinges;
import org.spongepowered.api.data.type.PortionTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.util.Direction.*;

/**
 * Provides Utils for blocks in Bukkit.
 */
public class BlockUtil
{
    public static final org.spongepowered.api.util.Direction[] BLOCK_FACES = {
        DOWN, UP, EAST, NORTH, WEST, SOUTH
    };

    public static final org.spongepowered.api.util.Direction[] DIRECTIONS = {
        DOWN, NORTH, WEST, EAST, SOUTH
    };

    public static final org.spongepowered.api.util.Direction[] CARDINAL_DIRECTIONS = {
        NORTH, WEST, EAST, SOUTH
    };

    /**
     * Searches for blocks that are attached onto given block.
     *
     * @return the attached blocks
     */
    public static Collection<Location> getAttachedBlocks(Location block)
    {
        Collection<Location> blocks = new HashSet<>();
        for (org.spongepowered.api.util.Direction bf : BLOCK_FACES)
        {
            Optional<AttachedData> attached = block.getRelative(bf).get(AttachedData.class);
            if (attached.isPresent())
            {
                // TODO
                /*
                if (attached.get().getFace().opposite().equals(bf))
                {
                    blocks.add(block.getRelative(bf));
                }
                */
            }
        }
        return blocks;
    }

    public static Set<BlockType> DETACHABLE_FROM_BELOW = new HashSet<>(Arrays.asList(BROWN_MUSHROOM, CARROTS, DEADBUSH,
                                                                                     DETECTOR_RAIL, POTATOES, WHEAT,
                                                                                     POWERED_REPEATER,
                                                                                     UNPOWERED_REPEATER, FLOWER_POT,
                                                                                     IRON_DOOR, LEVER, TALLGRASS,
                                                                                     MELON_STEM, NETHER_WART, PORTAL,
                                                                                     GOLDEN_RAIL, ACTIVATOR_RAIL,
                                                                                     POWERED_COMPARATOR,
                                                                                     UNPOWERED_COMPARATOR,
                                                                                     HEAVY_WEIGHTED_PRESSURE_PLATE,
                                                                                     LIGHT_WEIGHTED_PRESSURE_PLATE,
                                                                                     PUMPKIN_STEM, RAIL, RED_MUSHROOM,
                                                                                     RED_FLOWER, REDSTONE_WIRE,
                                                                                     REDSTONE_TORCH,
                                                                                     UNLIT_REDSTONE_TORCH, SAPLING,
                                                                                     STANDING_SIGN, WALL_SIGN, SKULL,
                                                                                     SNOW, STONE_PRESSURE_PLATE, TORCH,
                                                                                     TRIPWIRE, WATERLILY, WOODEN_DOOR,
                                                                                     WOODEN_PRESSURE_PLATE,
                                                                                     YELLOW_FLOWER, REEDS, CACTUS, SAND,
                                                                                     GRAVEL));

    public static boolean isDetachableFromBelow(BlockType mat)
    {
        return DETACHABLE_FROM_BELOW.contains(mat);
    }

    public static Collection<Location> getDetachableBlocksOnTop(Location block)
    {
        Collection<Location> blocks = new HashSet<>();
        Location onTop = block.getRelative(UP);
        while (isDetachableFromBelow(onTop.getBlockType()))
        {
            blocks.add(onTop);
            for (Location attachedBlock : getAttachedBlocks(onTop))
            {
                blocks.add(attachedBlock);
                blocks.addAll(getDetachableBlocksOnTop(attachedBlock));
            }
            onTop = onTop.getRelative(UP);
        }
        return blocks;
    }

    public static Collection<Location> getDetachableBlocks(Location<World> block)
    {
        Collection<Location> blocks = new HashSet<>();

        for (Location attachedBlock : getAttachedBlocks(block))
        {
            blocks.add(attachedBlock);
            blocks.addAll(getDetachableBlocksOnTop(attachedBlock));
        }
        blocks.addAll(getDetachableBlocksOnTop(block));
        return blocks;
    }

    public static boolean isSurroundedByWater(Location block)
    {
        for (final Direction face : DIRECTIONS)
        {
            BlockType type = block.getRelative(face).getBlockType();
            if (type == WATER || type == FLOWING_WATER)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isInvertedStep(Location location)
    {
        Optional<PortionData> data = location.get(PortionData.class);
        if (data.isPresent())
        {
            return data.get().type().get() == PortionTypes.TOP;
        }
        return false;
    }

    /**
     * All fluid blocks
     */
    private static final Set<BlockType> FLUID_BLOCKS = new HashSet<>(Arrays.asList(WATER, FLOWING_WATER, LAVA,
                                                                                   FLOWING_LAVA));

    public static boolean isFluidBlock(BlockType mat)
    {
        return FLUID_BLOCKS.contains(mat);
    }

    /**
     * Blocks that can get destroyed by fluids
     */
    private static final Set<BlockType> NON_FLUID_PROOF_BLOCKS = new HashSet<>(Arrays.asList(SAPLING, GOLDEN_RAIL,
                                                                                             DETECTOR_RAIL, WEB,
                                                                                             TALLGRASS, DEADBUSH,
                                                                                             YELLOW_FLOWER, RED_FLOWER,
                                                                                             BROWN_MUSHROOM,
                                                                                             RED_MUSHROOM, TORCH, FIRE,
                                                                                             REDSTONE_WIRE, WHEAT,
                                                                                             LEVER,
                                                                                             UNLIT_REDSTONE_TORCH,
                                                                                             REDSTONE_TORCH, SNOW,
                                                                                             UNPOWERED_REPEATER,
                                                                                             POWERED_REPEATER,
                                                                                             PUMPKIN_STEM, MELON_STEM,
                                                                                             VINE, WATERLILY,
                                                                                             NETHER_WART, COCOA,
                                                                                             TRIPWIRE_HOOK, TRIPWIRE,
                                                                                             FLOWER_POT, CARROTS,
                                                                                             POTATOES, SKULL,
                                                                                             ACTIVATOR_RAIL,
                                                                                             POWERED_COMPARATOR,
                                                                                             UNPOWERED_COMPARATOR));

    public static boolean isNonFluidProofBlock(BlockType mat)
    {
        return NON_FLUID_PROOF_BLOCKS.contains(mat);
    }

    private static final Set<BlockType> NON_OBSTRUCTING_SOLID_BLOCKS = new HashSet<>(Arrays.asList(STANDING_SIGN,
                                                                                                   WALL_SIGN,
                                                                                                   WOODEN_DOOR,
                                                                                                   IRON_DOOR,
                                                                                                   ACACIA_DOOR,
                                                                                                   BIRCH_DOOR,
                                                                                                   DARK_OAK_DOOR,
                                                                                                   JUNGLE_DOOR,
                                                                                                   SPRUCE_DOOR,
                                                                                                   STONE_PRESSURE_PLATE,
                                                                                                   WOODEN_PRESSURE_PLATE,
                                                                                                   LIGHT_WEIGHTED_PRESSURE_PLATE,
                                                                                                   HEAVY_WEIGHTED_PRESSURE_PLATE));

    public static boolean isNonObstructingSolidBlock(BlockType material)
    {
        return NON_OBSTRUCTING_SOLID_BLOCKS.contains(material);
    }

    /**
     * On BlockPlaceEvent a door will orientate its hinge according to the returned data for the top door-half
     *
     * @param placeLocation the location where the lower door half is placed
     * @param player        the player placing the door
     *
     * @return the top-data
     */
    /*
    // TODO remove if logic no longer needed
    public static byte getTopDoorDataOnPlace(BlockType doorType, Location placeLocation, Player player)
    {

        byte dir1 = 0;
        byte dir2 = 0;
        switch ((int)Math.floor(((player.getLocation().getYaw() + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3)
        {
            case 0:
                dir2 = 1;
                break;
            case 1:
                dir1 = -1;
                break;
            case 2:
                dir2 = -1;
                break;
            case 3:
                dir1 = 1;
                break;
        }
        BlockType negLocType = placeLocation.clone().add(-dir1, 0, -dir2).getBlock().getType();
        BlockType negLocUpType = placeLocation.clone().add(-dir1, 1, -dir2).getBlock().getType();
        BlockType posgLocType = placeLocation.clone().add(dir1, 0, dir2).getBlock().getType();
        BlockType posLocUpType = placeLocation.clone().add(dir1, 1, dir2).getBlock().getType();
        int hingeBlockSide1 = (isHingeBlock(negLocType) ? 1 : 0) + (isHingeBlock(negLocUpType) ? 1 : 0);
        int hingeBlockSide2 = (isHingeBlock(posgLocType) ? 1 : 0) + (isHingeBlock(posLocUpType) ? 1 : 0);
        boolean foundDoorSide1 = negLocType == doorType || negLocUpType == doorType;
        boolean foundDoorSide2 = posgLocType == doorType || posLocUpType == doorType;
        return (byte)(8 | (((foundDoorSide1 && !foundDoorSide2) || (hingeBlockSide2 > hingeBlockSide1)) ? 1 : 0));

    }


    private static boolean isHingeBlock(BlockType material)
    {
        net.minecraft.server.v1_8_R2.Block block = getBlockForId(material.getId());
        // called in ItemDoor.place(...)
        return block.isOccluding(); // return (this.material.k()) && (d()) && (!isPowerSource());
    }

    private static net.minecraft.server.v1_8_R2.Block getBlockForId(int id)
    {
        return (net.minecraft.server.v1_8_R2.Block)REGISTRY.a(id);
    }
*/

    public static Location<World> getHighestBlockAt(Location<World> loc)
    {
        return getHighestBlockAt(loc.getExtent(), loc.getBlockX(), loc.getBlockZ());
    }

    @SuppressWarnings("deprecation")
    public static Location<World> getHighestBlockAt(World world, final int x, final int z)
    {
        int y = world.getDimension().getBuildHeight() - 1;

        while (world.getBlockType(x, y, z) == AIR && y > 0)
        {
            --y;
        }

        return world.getLocation(x, y, z);
    }

    public static Chunk getChunk(Location block)
    {
        return ((World)block.getExtent()).getChunk(block.getBlockPosition()).get();
    }

    public static Direction getOtherDoorDirection(Direction direction, Hinge hinge)
    {
        if (direction == NORTH)
        {
            direction = EAST;
        }
        else if (direction == EAST)
        {
            direction = SOUTH;
        }
        else if (direction == SOUTH)
        {
            direction = WEST;
        }
        else if (direction == WEST)
        {
            direction = NORTH;
        }
        if (hinge == Hinges.RIGHT) // TODO check if this is right might be the inverse
        {
            direction = direction.getOpposite();
        }
        return direction;
    }
}