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
package org.cubeengine.module.locker.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.cubeengine.module.locker.data.LockerManager;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.vehicle.Vehicle;
import org.spongepowered.api.entity.vehicle.minecart.BlockOccupiedMinecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.TameEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.server.ServerLocation;

@Singleton
public class LockerAutoProtectListener
{
    private I18n i18n;
    private LockerManager lockerManager;
    private Locker module;

    @Inject
    public LockerAutoProtectListener(I18n i18n, LockerManager lockerManager, Locker module)
    {
        this.i18n = i18n;
        this.lockerManager = lockerManager;
        this.module = module;
    }


    @Listener
    public void onPlace(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        if (module.getConfig().block.enable)
        {
            event.transactions(Operations.PLACE.get()).forEach(transaction -> {
                for (BlockLockConfig cfg : module.getConfig().block.blocks)
                {
                    if (cfg.isAutoProtect() && cfg.getType().equals(transaction.finalReplacement().state().type()))
                    {
                        final ServerLocation loc = transaction.finalReplacement().location().get();
                        final short flags = cfg.getFlags();
                        if (lockerManager.createLock(loc, player, flags))
                        {
                            event.setCancelled(true);
                        }
                    }
                }
            });
        }
    }

    @Listener
    public void onTame(TameEntityEvent event, @First ServerPlayer player)
    {
        for (EntityLockConfig cfg : module.getConfig().entity.entities)
        {
            if (cfg.isAutoProtect() && cfg.getType().equals(event.entity().type()))
            {
                final short flags = cfg.getFlags();
                lockerManager.createLock(event.entity(), player, flags);
            }
        }
    }

    @Listener
    public void onSpawn(SpawnEntityEvent event, @First ServerPlayer player)
    {
        for (Entity entity : event.entities())
        {
            if (entity instanceof Vehicle && !(entity instanceof BlockOccupiedMinecart))
            {
                for (EntityLockConfig cfg : module.getConfig().entity.entities)
                {
                    if (cfg.isAutoProtect() && cfg.getType().equals(entity.type()))
                    {
                        final short flags = cfg.getFlags();
                        lockerManager.createLock(entity, player, flags);
                    }
                }
            }
        }
    }
}
