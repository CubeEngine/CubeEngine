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

import java.util.List;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.module.vigil.report.ReportUtil.name;

/* TODO explosions
creeper
enderdrag
entity?
fireball
tnt
wither
 */
// TODO wait for Sponge implementation
public class ExplosionReport extends BaseBlockReport<ExplosionEvent> implements Report.Readonly
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        // TODO test
        Action action = actions.get(0);
        BlockSnapshot snap = action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get();
        receiver.sendReport(this, actions, actions.size(),
                            "{txt} made boom {txt}",
                            "{txt} made boom {txt} x{}",
                            Recall.cause(action), name(snap, receiver), actions.size());
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        return false;
    }

//    @Listener(order = Order.POST)
//    public void listen(ExplosionEvent.Post event, @First Player player)
//    {
//        // TODO cause filtering
//        report(event);
//    }

//    protected void report(ExplosionEvent.Post event)
//    {
//        for (BlockTransactionReceipt receipt : event.receipts())
//        {
//            final ServerLocation loc = receipt.originalBlock().location().get();
//            if (!isActive(loc.world()))
//            {
//                continue;
//            }
//
//            final Action action = observe(event);
//            action.addData(BLOCK_CHANGES, Observe.transactions(receipt));
//            action.addData(LOCATION, Observe.location(loc));
//            action.addData(OPERATION, receipt.operation().key(RegistryTypes.OPERATION).asString());
//
//            report(action);
//        }
//    }

}
