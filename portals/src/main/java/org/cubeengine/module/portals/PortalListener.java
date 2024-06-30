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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.ItemUtil;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.portals.config.Destination;
import org.cubeengine.module.portals.config.PortalConfig;
import org.cubeengine.module.portals.config.PortalConfig.PortalRegion;
import org.cubeengine.module.zoned.ZonedData;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.MovementType;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.HarvestEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class PortalListener
{
    private final Portals module;
    private I18n i18n;
    private Reflector reflector;

    @Inject
    public PortalListener(Portals module, I18n i18n, Reflector reflector)
    {
        this.module = module;
        this.i18n = i18n;
        this.reflector = reflector;
    }

    @Listener
    public void onMove(MoveEntityEvent event)
    {
        final Optional<MovementType> movementType = event.context().get(EventContextKeys.MOVEMENT_TYPE);
        final Entity entity = event.entity();
        ServerWorld destWorld = entity.serverLocation().world();
        ServerWorld origWorld = entity.serverLocation().world();
        if (event instanceof ChangeEntityWorldEvent)
        {
            destWorld = ((ChangeEntityWorldEvent)event).destinationWorld();
            origWorld = ((ChangeEntityWorldEvent)event).originalWorld();
            if ((this.module.getConfig().disableVanillaPortals
                || this.module.getConfig().disabledVanillaPortalsInWorlds.getOrDefault(new ConfigWorld(origWorld), false))
                && movementType.map(t -> t == MovementTypes.PORTAL.get()).orElse(false))
            {
                event.setCancelled(true);
                return;
            }
            if (entity instanceof ServerPlayer)
            {
                final ServerLocation dest = ((ChangeEntityWorldEvent)event).destinationWorld().location(event.destinationPosition());
                onTeleport(dest, ((ServerPlayer)entity));
            }
        }
        if (entity instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer)entity;
            if (!origWorld.key().equals(destWorld.key())
                || (event.originalPosition().floorX() == event.destinationPosition().floorX()
                && event.originalPosition().floorY() == event.destinationPosition().floorY()
                && event.originalPosition().floorZ() == event.destinationPosition().floorZ())
            )
            {
                return;
            }

            final PortalsAttachment attachment = module.getPortalsAttachment(player.uniqueId());
            for (Portal portal : module.getPortals())
            {
                if (portal.has(destWorld.location(event.destinationPosition())))
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
                        onTeleport(player.serverLocation(), player);
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
            if (portal.has(destWorld.location(event.destinationPosition())))
            {
                entities.add(entity.uniqueId());
                return;
            }
            entities.remove(entity.uniqueId());
        }
    }

    @Listener
    public void onPlacePortalExit(SpawnEntityEvent.Pre event)
    {
        if (event.entities().size() == 1 && event.entities().get(0).type() == EntityTypes.ARMOR_STAND.get())
        {
            event.context().get(EventContextKeys.USED_ITEM).flatMap(item -> item.get(PortalsData.PORTAL)).ifPresent(p -> {

                final HashMap<EquipmentType, Boolean> noInteraction = new HashMap<>();
                noInteraction.put(EquipmentTypes.CHEST.get(), true);
                noInteraction.put(EquipmentTypes.FEET.get(), true);
                noInteraction.put(EquipmentTypes.HEAD.get(), true);
                noInteraction.put(EquipmentTypes.LEGS.get(), true);
                noInteraction.put(EquipmentTypes.MAINHAND.get(), true);
                noInteraction.put(EquipmentTypes.OFFHAND.get(), true);

                final ArmorStand entity = (ArmorStand) event.entities().get(0);
                entity.offer(PortalsData.PORTAL, p);
                entity.setHead(ItemStack.of(ItemTypes.PLAYER_HEAD));
                entity.offer(Keys.HAS_ARMS, true);
                entity.offer(Keys.IS_PLACING_DISABLED, noInteraction);
                entity.offer(Keys.IS_TAKING_DISABLED, noInteraction);
            });
        }
    }

    @Listener
    public void onHarvestPortalExit(HarvestEntityEvent event)
    {
        if (event.entity() instanceof ArmorStand armorStand)
        {
            if (armorStand.get(PortalsData.PORTAL).isPresent())
            {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onBreakPortalExit(DestructEntityEvent.Death event)
    {
        if (event.entity() instanceof ArmorStand armorStand)
        {
            if (armorStand.get(PortalsData.PORTAL).isPresent())
            {
                ItemUtil.spawnItem(armorStand.serverLocation(), PortalsItems.portalExit());
            }
        }
    }

    @Listener
    public void onInteractPortalExit(InteractEntityEvent.Primary event, @First ServerPlayer player)
    {
        if (event.entity() instanceof ArmorStand armorStand)
        {
            if (armorStand.get(PortalsData.PORTAL).isPresent())
            {
                ZonedData.getSavedZone(player.itemInHand(HandTypes.MAIN_HAND)).ifPresent(zone -> {
                    if (zone.shape instanceof Cuboid cuboid)
                    {

                        final PortalConfig config = reflector.create(PortalConfig.class);
                        config.owner = player.name();
                        config.destination = new Destination(armorStand.serverLocation(), armorStand.rotation(), i18n);
                        config.location = new PortalRegion();
                        config.world = new ConfigWorld(player.world());
                        config.location.from = cuboid.getMinimumPoint().toInt();
                        config.location.to = cuboid.getMaximumPoint().toInt();

                        // TODO name the portal?
                        final var portalName = "test";
                        config.setFile(module.getPortalFile(portalName));
                        config.save();

                        final var portal = new Portal(module, portalName, config, i18n);
                        this.module.addPortal(portal);
                        player.playSound(Sound.sound(SoundTypes.ENTITY_ENDERMAN_TELEPORT, Source.NEUTRAL, 5, 0.5f), armorStand.position());
                        armorStand.remove();
                        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Portal created!");
                    }
                    else
                    {
                        i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Saved selection is not a cuboid");
                    }
                });
            }
        }
    }

    private void onTeleport(ServerLocation target, Player player)
    {
        for (Portal portal : module.getPortals())
        {
            if (portal.has(target))
            {
                PortalsAttachment attachment = module.getPortalsAttachment(player.uniqueId());
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
