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
package org.cubeengine.module.portals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.MovementType;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class PortalListener
{
    private final Portals module;
    private I18n i18n;

    @Inject
    public PortalListener(Portals module,I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Listener
    public void onMove(MoveEntityEvent event)
    {
        final Optional<MovementType> movementType = event.getContext().get(EventContextKeys.MOVEMENT_TYPE);
        final Entity entity = event.getEntity();
        ServerWorld destWorld = entity.getServerLocation().getWorld();
        ServerWorld origWorld = entity.getServerLocation().getWorld();
        if (event instanceof ChangeEntityWorldEvent)
        {
            destWorld = ((ChangeEntityWorldEvent)event).getDestinationWorld();
            origWorld = ((ChangeEntityWorldEvent)event).getOriginalWorld();
            if ((this.module.getConfig().disableVanillaPortals
                || this.module.getConfig().disabledVanillaPortalsInWorlds.getOrDefault(new ConfigWorld(origWorld), false))
                && movementType.map(t -> t == MovementTypes.PORTAL.get()).orElse(false))
            {
                event.setCancelled(true);
                return;
            }
            if (entity instanceof ServerPlayer)
            {
                final ServerLocation dest = ((ChangeEntityWorldEvent)event).getDestinationWorld().getLocation(event.getDestinationPosition());
                onTeleport(dest, ((ServerPlayer)entity));
            }
        }
        if (entity instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer)entity;
            if (!origWorld.getKey().equals(destWorld.getKey())
                || (event.getOriginalPosition().getFloorX() == event.getDestinationPosition().getFloorX()
                && event.getOriginalPosition().getFloorY() == event.getDestinationPosition().getFloorY()
                && event.getOriginalPosition().getFloorZ() == event.getDestinationPosition().getFloorZ())
            )
            {
                return;
            }

            final PortalsAttachment attachment = module.getPortalsAttachment(player.getUniqueId());
            for (Portal portal : module.getPortals())
            {
                if (portal.has(destWorld.getLocation(event.getDestinationPosition())))
                {
                    if (attachment.isDebug())
                    {
                        if (attachment.isInPortal())
                        {
                            i18n.send(player, POSITIVE, "{text:[Portals] Debug} Move in portal: {name}", portal.getName());
                        }
                        else
                        {
                            i18n.send(player, POSITIVE, "{text:[Portals] Debug} Entered portal: {name}", portal.getName());
                            portal.showInfo(player);
                            attachment.setInPortal(true);
                        }
                    }
                    else if (!attachment.isInPortal())
                    {
                        portal.teleport(player);
                        onTeleport(player.getServerLocation(), player);
                    }
                    return;
                }
                // else ignore
            }
            attachment.setInPortal(false);
            return;
        }

        // For non-players
        for (Portal portal : module.getPortals())
        {
            // Get list of entities known to be in the portal
            List<UUID> entities = module.getEntitiesInPortal(portal);
            if (portal.has(destWorld.getLocation(event.getDestinationPosition())))
            {
                entities.add(entity.getUniqueId());
                return;
            }
            entities.remove(entity.getUniqueId());
        }
    }

    private void onTeleport(ServerLocation target, Player player)
    {
        for (Portal portal : module.getPortals())
        {
            if (portal.has(target))
            {
                PortalsAttachment attachment = module.getPortalsAttachment(player.getUniqueId());
                attachment.setInPortal(true);
                if (attachment.isDebug())
                {
                    i18n.send(player, POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Teleported into portal: {name}", portal.getName());
                }
                return;
            }
            // else ignore
        }
    }
}
