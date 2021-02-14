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

import java.util.Map;
import java.util.Optional;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.world.BlockChangeFlags;

public abstract class BaseBlockReport<T extends Event> extends BaseReport<T>
{
    public static final Action.DataKey<Map<String, Object>> BLOCK_CHANGES = new Action.DataKey<>("block-changes");
    public static final Action.DataKey<Optional<BlockSnapshot>> BLOCKS_ORIG = new Action.DataKey<>(BLOCK_CHANGES.name + "-orig");
    public static final Action.DataKey<Optional<BlockSnapshot>> BLOCKS_REPL = new Action.DataKey<>(BLOCK_CHANGES.name + "-repl");
    public static final Action.DataKey<String> OPERATION = new Action.DataKey<>("operation");
    public static final DataQuery BLOCK_STATE = DataQuery.of("BlockState");
    public static final DataQuery BLOCK_DATA = DataQuery.of("TileEntityData");
    public static final DataQuery BLOCK_UNSAFE_DATA = DataQuery.of("UnsafeData");
    public static final DataQuery BLOCK_ITEMS = DataQuery.of("UnsafeData", "Items");
    public static final Action.DataKey<Map<String, Object>> ORIGINAL = new Action.DataKey<>("original");
    public static final Action.DataKey<Map<String, Object>> REPLACEMENT = new Action.DataKey<>("replacement");

    protected boolean group(Optional<BlockSnapshot> repl1, Optional<BlockSnapshot> repl2)
    {
        if ((repl1.isPresent() && !repl2.isPresent()) || (!repl1.isPresent() && repl2.isPresent()))
        {
            return false;
        }

        if (repl1.isPresent() && repl2.isPresent())
        {
            if (!repl1.get().getWorld().equals(repl2.get().getWorld()))
            {
                return false;
            }
            if (!repl1.get().getState().equals(repl2.get().getState()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }

        Optional<BlockSnapshot> orig1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> orig2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);

        if (!group(orig1, orig2))
        {
            return false;
        }

        Optional<BlockSnapshot> repl1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);

        if (!group(repl1, repl2))
        {
            return false;
        }

        if (!action.getData(CAUSE).equals(otherAction.getData(CAUSE)))
        {
            // TODO check same cause better
            return false;
        }

        // TODO in short timeframe (minutes? configurable)
        return true;
    }


    @Override
    public void apply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_REPL, Recall::replSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }

    @Override
    public void unapply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }

    @Override
    protected Action observe(T event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        return action;
    }
}
