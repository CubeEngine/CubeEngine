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
package org.cubeengine.module.fly;

import java.time.Duration;
import java.util.HashMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.scheduler.ScheduledTask;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Module
public class Fly
{
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private FlyPermissions perms;

    private final HashMap<Player, ScheduledTask> tasks = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Listener
    public void playerInteract(final InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (!itemInHand.type().isAnyOf(ItemTypes.FEATHER))
        {
            return;
        }

        if (!player.hasPermission(perms.FLY_FEATHER.getId()))
        {
            i18n.send(player, NEGATIVE, "You dont have permission to use this!");
            player.offer(Keys.CAN_FLY, false); //Disable when player is flying
            player.offer(Keys.IS_FLYING, false);
            return;
        }

        //I Believe I Can Fly ...
        player.offer(Keys.CAN_FLY, !player.get(Keys.CAN_FLY).orElse(false));
        if (!player.get(Keys.CAN_FLY).orElse(false))
        {
            player.offer(Keys.FALL_DISTANCE, 0d);
            i18n.send(player, NEUTRAL, "You cannot fly anymore!");
            return;
        }
        final ItemStack feather = ItemStack.of(ItemTypes.FEATHER, 1);
        player.inventory().query(QueryTypes.ITEM_STACK_IGNORE_QUANTITY, feather).poll(1);
        player.transform(Keys.VELOCITY, v -> v.add(0, 1, 0));
        player.setPosition(player.position().add(0, 0.05, 0)); //make sure the player stays flying
        player.offer(Keys.IS_FLYING, true);
        i18n.send(player, POSITIVE, "You can now fly!");
        long feathersPerMinute = 2;
        Duration delay = Duration.ofMinutes(1).dividedBy(feathersPerMinute);
        ScheduledTask flyMoreTask = tm.runTimer(task -> {
            if (!player.get(Keys.IS_FLYING).get())
            {
                player.offer(Keys.CAN_FLY, false);
                task.cancel();
                return;
            }
            if (player.inventory().contains(ItemTypes.FEATHER.get()))
            {
                player.inventory().query(QueryTypes.ITEM_STACK_IGNORE_QUANTITY, feather).poll(1);
            }
            else
            {
                player.offer(Keys.CAN_FLY, false);
                task.cancel();
            }
        }, delay, delay);

        ScheduledTask oldTask = this.tasks.put(player, flyMoreTask);
        if (oldTask != null)
        {
            oldTask.cancel();
        }
    }

}
