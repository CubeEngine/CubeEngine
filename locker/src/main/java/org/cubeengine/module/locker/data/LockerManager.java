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
package org.cubeengine.module.locker.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.LockerPerm;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataHolder.Mutable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.menu.handler.SlotChangeHandler;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class LockerManager
{

    private I18n i18n;
    private LockerPerm perms;
    private Locker module;
    private TaskManager tm;

    private Set<UUID> accessBookPunchers = new HashSet<>();
    private Set<UUID> trustBookPunchers = new HashSet<>();
    private Map<UUID, Map<UUID, Integer>> trustCache = new HashMap<>();

    @Inject
    public LockerManager(I18n i18n, LockerPerm perms, Locker module, TaskManager tm)
    {
        this.i18n = i18n;
        this.perms = perms;
        this.module = module;
        this.tm = tm;
    }

    public Set<UUID> getAccessBookPunchers()
    {
        return accessBookPunchers;
    }

    public Set<UUID> getTrustBookPunchers()
    {
        return trustBookPunchers;
    }

    private List<Mutable> getMultiBlocks(Mutable dataHolder)
    {
        if (dataHolder instanceof BlockEntity)
        {
            final Set<Direction> directions = ((BlockEntity)dataHolder).block().get(Keys.CONNECTED_DIRECTIONS).orElse(Collections.emptySet());
            return directions.stream().map(dir -> ((BlockEntity)dataHolder).serverLocation().relativeTo(dir)).map(Location::blockEntity)
                             .filter(Optional::isPresent).map(Optional::get).map(Mutable.class::cast).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void createLock(DataHolder.Mutable dataHolder, ServerPlayer owner, ItemStack lockerBook)
    {
        final int flags = lockerBook.get(LockerData.FLAGS).orElse(0); // TODO default for dataholder AND filter allowed flags
        final Map<UUID, Integer> accessMap = owner.get(LockerData.ACCESS).orElse(new HashMap<>());
        accessMap.putAll(lockerBook.get(LockerData.ACCESS).orElse(new HashMap<>())); // Override global access
        if (!accessMap.isEmpty())
        {
            dataHolder.offer(LockerData.ACCESS, accessMap);
        }

        this.createLockAt(dataHolder, owner, flags, accessMap);
        for (Mutable multiblock : getMultiBlocks(dataHolder))
        {
            this.createLockAt(multiblock, owner, flags, accessMap);
        }
        i18n.send(ChatType.ACTION_BAR, owner, POSITIVE, "Lock created.");
    }

    public boolean createLock(ServerLocation location, ServerPlayer owner, int flags)
    {
        final Mutable dataHolder = getDataHolderAtLoc(location);
        if (dataHolder.get(LockerData.OWNER).isPresent())
        {
            i18n.send(ChatType.ACTION_BAR, owner, NEUTRAL, "There is already a protection here!");
            return true;
        }
        // TODO check if canProtect
        if (false)
        {
            i18n.send(owner, NEGATIVE, "You cannot protect this block!");
            return false;
        }
        final List<Mutable> multiBlocks = getMultiBlocks(dataHolder);
        int blocks = 1;
        boolean expand = false;
        for (Mutable multiBlock : multiBlocks)
        {
            final Optional<UUID> otherOowner = multiBlock.get(LockerData.OWNER);
            final boolean isOwner = otherOowner.map(o -> o.equals(owner.uniqueId())).orElse(false);

            if (otherOowner.isPresent())
            {
                if (isOwner)
                {
                    expand = true;
                    blocks++;
                }
                else
                {
                    i18n.send(ChatType.ACTION_BAR, owner, NEGATIVE, "The nearby chest is protected by someone else!");
                    return true;
                }
            }
        }

        if (owner.get(Keys.IS_SNEAKING).orElse(false))
        {
            final Component click = i18n.translate(owner, NEUTRAL, "Click here to protect")
                                  .clickEvent(SpongeComponents.executeCallback(s -> createLock(location, owner, flags)));
            i18n.send(owner, NEUTRAL, "Autoprotect is disabled while sneaking. {txt}", click);
            return false;
        }

        if (expand)
        {
            i18n.send(ChatType.ACTION_BAR, owner, POSITIVE, "Protection expanded to {amount} blocks!", blocks);
            final Mutable otherMutable = multiBlocks.get(0);
            this.createLockAt(dataHolder, owner, otherMutable.get(LockerData.FLAGS).orElse(0),
                                                 otherMutable.get(LockerData.ACCESS).orElse(Collections.emptyMap()));
            return false;
        }

        this.createLockAt(dataHolder, owner, flags, Collections.emptyMap());
        for (Mutable multiBlock : multiBlocks)
        {
            this.createLockAt(multiBlock, owner, flags, Collections.emptyMap());
        }
        final int size = 1 + multiBlocks.size();
        i18n.sendN(ChatType.ACTION_BAR, owner, POSITIVE, size, "Protection created.", "Protection created ({n} blocks)", size);
        return false;
    }

    public void createLock(Entity entity, ServerPlayer owner, int flags)
    {
        if (entity.get(LockerData.OWNER).isPresent())
        {
            i18n.send(ChatType.ACTION_BAR, owner, NEUTRAL, "There is already a protection here!");
            return;
        }
        // TODO check if canProtect
        if (false)
        {
            i18n.send(owner, NEGATIVE, "You cannot protect this entity!");
            return;
        }
        this.createLockAt(entity, owner, flags, Collections.emptyMap());
    }

    public void createLockAt(DataHolder.Mutable dataHolder, ServerPlayer owner, int flags, Map<UUID, Integer> accessMap)
    {
        dataHolder.offer(LockerData.OWNER, owner.uniqueId());
        dataHolder.offer(LockerData.FLAGS, flags);
        if (!accessMap.isEmpty())
        {
            dataHolder.offer(LockerData.ACCESS, accessMap);
        }
        dataHolder.offer(LockerData.CREATED, System.currentTimeMillis());
    }

    public void transferLockOwner(DataHolder.Mutable dataHolder, ServerPlayer player, User newOwner)
    {
        final boolean isOwner = dataHolder.get(LockerData.OWNER).map(owner -> owner.equals(player.uniqueId())).orElse(false);
        final boolean hasPerm = perms.CMD_GIVE_OTHER.check(player);
        if (isOwner || hasPerm)
        {
            dataHolder.offer(LockerData.OWNER, newOwner.uniqueId());
            i18n.send(player, NEUTRAL, "{user} is now the owner of this protection.", newOwner);
            return;
        }
        i18n.send(player, NEGATIVE, "This is not your protection!");
    }

    public void createKeyBook(DataHolder.Mutable dataHolder, ServerPlayer player)
    {
        // TODO find protection later?
        i18n.send(player, NEGATIVE, "This is not your protection!");
        i18n.send(player, NEUTRAL, "This protection is public!");
    }

    public void updateLockFlags(Mutable dataHolder, ServerPlayer player, ItemStack lockerBook)
    {
        if (dataHolder == null || !dataHolder.get(LockerData.OWNER).isPresent())
        {
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEUTRAL, "There is no protection here");
            return;
        }
        final boolean isOwner = dataHolder.get(LockerData.OWNER).map(owner -> owner.equals(player.uniqueId())).orElse(false);
        final boolean hasAdmin = ProtectionFlag.ADMIN.isSet(dataHolder.get(LockerData.ACCESS).map(a -> a.get(player.uniqueId())).orElse(0));
        final boolean hasPerm = perms.CMD_MODIFY_OTHER_FLAGS.check(player);
        if (!isOwner && !hasAdmin && !hasPerm)
        {
            i18n.send(player, NEGATIVE, "You are not allowed to modify the flags for this protection!");
            return;
        }
        final Integer flags = lockerBook.get(LockerData.FLAGS).orElse(0);
        dataHolder.offer(LockerData.FLAGS, flags);
        final List<Mutable> multiBlocks = getMultiBlocks(dataHolder);
        for (Mutable multiBlock : multiBlocks)
        {
            multiBlock.offer(LockerData.FLAGS, flags);
        }
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Updated Protection Flags");
        // TODO delta message/effects
    }

    public void updateLockAccess(Mutable dataHolder, ServerPlayer player, ItemStack lockerBook)
    {
        if (dataHolder == null || !dataHolder.get(LockerData.OWNER).isPresent())
        {
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEUTRAL, "There is no protection here");
            return;
        }
        final boolean isOwner = dataHolder.get(LockerData.OWNER).map(owner -> owner.equals(player.uniqueId())).orElse(false);
        final boolean hasAdmin = ProtectionFlag.ADMIN.isSet(dataHolder.get(LockerData.ACCESS).map(a -> a.get(player.uniqueId())).orElse(0));
        final boolean hasPerm = perms.CMD_MODIFY_OTHER_ACCESS.check(player);

        if (!isOwner && !hasAdmin && !hasPerm)
        {
            i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to modify the access list of this protection!");
            return;
        }

        final Map<UUID, Integer> accessMap = lockerBook.get(LockerData.ACCESS).orElse(new HashMap<>());
        if (!accessMap.isEmpty())
        {
            dataHolder.offer(LockerData.ACCESS, accessMap);
        }
        else
        {
            dataHolder.remove(LockerData.ACCESS);
        }

        final List<Mutable> multiBlocks = getMultiBlocks(dataHolder);
        for (Mutable multiBlock : multiBlocks)
        {
            if (!accessMap.isEmpty())
            {
                multiBlock.offer(LockerData.ACCESS, accessMap);
            }
            else
            {
                multiBlock.remove(LockerData.ACCESS);
            }
        }

        // TODO delta message/effects
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Updated Protection Access");
    }

    public void showLock(Mutable dataHolder, ServerPlayer player)
    {
        final boolean isOwner = dataHolder.get(LockerData.OWNER).map(o -> o.equals(player.uniqueId())).orElse(false);
        final boolean hasAdmin = ProtectionFlag.ADMIN.isSet(dataHolder.get(LockerData.ACCESS).map(a -> a.get(player.uniqueId())).orElse(0));
        final boolean hasPerm = perms.CMD_INFO_OTHER.check(player);
        if (isOwner || hasAdmin || hasPerm)
        {
            showFullLock(dataHolder, player);
        }
        else
        {
            showReducedLock(dataHolder, player);
        }

    }

    public void showFullLock(Mutable dataHolder, ServerPlayer player)
    {
        final UUID ownerUUID = dataHolder.get(LockerData.OWNER).get();
        final String ownerName = uuidToName(ownerUUID, ownerUUID.toString());
        i18n.send(player, POSITIVE, "Protection by {user}", ownerName);
        final Optional<Long> created = dataHolder.get(LockerData.CREATED);
        if (created.isPresent())
        {
            i18n.send(player, POSITIVE, "protects since {input#time}", new Date(created.get()).toString());
        }
        final Optional<Long> lastAccess = dataHolder.get(LockerData.LAST_ACCESS);
        if (lastAccess.isPresent())
        {
            i18n.send(player, POSITIVE, "last access was {input#time}", new Date(lastAccess.get()).toString());
        }

        final List<UUID> unlocks = dataHolder.get(LockerData.UNLOCKS).orElse(Collections.emptyList());
        if (unlocks.contains(player.uniqueId()))
        {
            // TODO password protection?
            i18n.send(player, POSITIVE, "Has a password and is currently {text:unlocked:color=YELLOW}");
            i18n.send(player, POSITIVE, "Has a password and is currently {text:locked:color=RED}");
        }

        final int flags = dataHolder.get(LockerData.FLAGS).orElse(0);
        if (flags == 0)
        {
            i18n.send(player, POSITIVE, "No flags are set.");
        }
        else
        {
            i18n.send(player, POSITIVE, "The following flags are set:");
            for (ProtectionFlag flag : ProtectionFlag.values())
            {
                if (flag.isSet(flags))
                {
                    // TODO description on hover?
                    player.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                                                                .append(Component.text(flag.flagname, NamedTextColor.YELLOW)));
                }
            }
        }

        final Map<UUID, Integer> accessMap = dataHolder.get(LockerData.ACCESS).orElse(Collections.emptyMap());
        // TODO global access
        if (!accessMap.isEmpty())
        {
            i18n.send(player, POSITIVE, "The following users have access to this protection");
            for (Entry<UUID, Integer> entry : accessMap.entrySet())
            {
                final String userName = uuidToName(entry.getKey(), entry.getKey().toString());
                TextComponent text = Component.text(" - ", NamedTextColor.GRAY).append(Component.text(userName, NamedTextColor.GREEN));
                final Builder builder = Component.text();
                builder.append(i18n.translate(player, POSITIVE, "Bypassing the following flags"));
                for (ProtectionFlag flag : ProtectionFlag.values())
                {
                    if (flag.isSet(flags) && flag.isSet(entry.getValue()))
                    {
                        builder.append(Component.newline());
                        builder.append(Component.text(" - ", NamedTextColor.GRAY).append(Component.text(flag.flagname, NamedTextColor.YELLOW)));
                    }
                }
                final TextComponent flagsText = Component.text(" [Flags] ", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(builder.build()));
                text = text.append(flagsText);
                if (ProtectionFlag.ADMIN.isSet(entry.getValue()))
                {
                    text = text.append(Component.text(" [Admin]", NamedTextColor.GOLD));
                }
                if (unlocks.contains(entry.getKey()))
                {
                    text = text.append(Component.text(" [Unlocked]", NamedTextColor.GOLD));
                }
                player.sendMessage(text);
            }
        }

        final List<Mutable> multiBlocks = getMultiBlocks(dataHolder);
        if (!multiBlocks.isEmpty())
        {
            final int n = multiBlocks.size() + 1;
            i18n.sendN(player, POSITIVE, n, "This protection covers a single block!", "This protections covers {amount} blocks!", n);
        }
    }

    public void showReducedLock(Mutable dataHolder, ServerPlayer player)
    {
        if (perms.CMD_INFO_SHOW_OWNER.check(player))
        {
            final UUID ownerUUID = dataHolder.get(LockerData.OWNER).get();
            final String ownerName = uuidToName(ownerUUID, ownerUUID.toString());
            i18n.send(player, POSITIVE, "Protection by {user}", ownerName);
        }

        // pw unlocked
        i18n.send(player, POSITIVE, "As you memorize the pass phrase the magic aura protecting this allows you to interact");
        // pw locked
        i18n.send(player, POSITIVE, "You sense that the strong magic aura protecting this won't let you through without the right passphrase");

        // locked
        i18n.send(player, POSITIVE, "You sense a strong magic aura protecting this");

        // flags?
        i18n.send(player, POSITIVE, "but it does not hinder you when moving items");
    }


    public void showKeyBook(ServerPlayer player)
    {
//        i18n.send(player, POSITIVE, "The strong magic surrounding this KeyBook allows you to access the designated protection");
//        i18n.send(player, POSITIVE, "The protection corresponding to this book is located at {vector} in {world}", loc.getBlockPosition(), loc.getExtent());
//        i18n.send(player, POSITIVE, "The entity protection corresponding to this book is located at {vector} in {world}", loc.getBlockPosition(), loc.getExtent());
//        i18n.send(player, POSITIVE, "Your magic is not strong enough to locate the corresponding entity protection!");
//        i18n.send(player, NEUTRAL, "As you inspect the KeyBook closer you realize that its magic power has disappeared!");
    }

    public void removeLock(Mutable dataHolder, ServerPlayer player)
    {
        final boolean isOwner = dataHolder.get(LockerData.OWNER).map(o -> o.equals(player.uniqueId())).orElse(false);
        final boolean hasAdmin = ProtectionFlag.ADMIN.isSet(dataHolder.get(LockerData.ACCESS).map(a -> a.get(player.uniqueId())).orElse(0));
        final boolean hasPerm = perms.CMD_REMOVE_OTHER.check(player);
        if (isOwner || hasAdmin || hasPerm)
        {
            // TODO connected protections too
            LockerData.purge(dataHolder);
            for (Mutable multiBlock : getMultiBlocks(dataHolder))
            {
                LockerData.purge(multiBlock);
            }

            i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Removed Lock!");
            return;
        }
        i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "This protection is not yours!");

    }

    public void unlock(Mutable dataHolder, ServerPlayer player)
    {
        // TODO
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Upon hearing the right passphrase the magic gets thinner and lets you pass!");
//        KeyBook.playUnlockSound(user, soundLoc, module.getTaskManager());

        i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "You try to open the container with a passphrase but nothing changes!");
    }

    public void setGlobalAccess(ServerPlayer player, UUID user, int flags)
    {
        // TODO
        final String userName = uuidToName(user, user.toString());
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Global access for {user} set!", userName);
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Updated global access level for {user}!", userName);
        i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "{user} had no global access!", userName);
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Removed global access from {user}", userName);
    }

    public boolean canProtect(Mutable dataHolder)
    {
        if (dataHolder instanceof ServerLocation)
        {
            for (BlockLockConfig cfg : module.getConfig().block.blocks)
            {
                if (cfg.isType(((ServerLocation)dataHolder).blockType()))
                {
                    return true;
                }
            }
        }
        else if (dataHolder instanceof Entity)
        {
            for (EntityLockConfig cfg : module.getConfig().entity.entities)
            {
                if (cfg.isType(((Entity)dataHolder).type()))
                {
                    return true;
                }
            }
        }
        return false;
    }


    // return true when custom handling
    public boolean handleInventoryOpen(Mutable dataHolder, ServerPlayer player, Inventory inventory)
    {
        final Integer flags = dataHolder.get(LockerData.FLAGS).orElse(null);
        if (flags == null)
        {
            return false;
        }
        final UUID owner = dataHolder.get(LockerData.OWNER).get();
        final String ownerName = uuidToName(owner, owner.toString());
        if (perms.SHOW_OWNER.check(player))
        {
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "This inventory is protected by {name}", ownerName);
        }

        final int accessFlags = canAccess(dataHolder, player, owner, perms.ACCESS_OTHER);

        final boolean blockInteract = ProtectionFlag.BLOCK_INTERACT.isBlocked(flags, accessFlags);

        if (blockInteract)
        {
            if (perms.SHOW_OWNER.check(player))
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from accessing this inventory of {user}!", ownerName);
            }
            else
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from accessing this inventory!");
            }
            return true; // Cancel open inventory
        }

        final boolean blockTake = ProtectionFlag.INVENTORY_TAKE.isBlocked(flags, accessFlags);
        final boolean blockPut = ProtectionFlag.INVENTORY_PUT.isBlocked(flags, accessFlags);

        this.notify(owner, player, dataHolder, flags);

        // TODO update lastAccess

        if (!blockTake && !blockPut)
        {
            return false; // Allow vanilla open inventory
        }

        final ViewableInventory viewableInventory = inventory.asViewable().get();
        final InventoryMenu menu = viewableInventory.asMenu();

        if (blockTake && blockPut)
        {
            menu.setReadOnly(true);
        }
        else
        {
            menu.registerChange(new ProtectedInventory(blockTake, blockPut));
        }

        menu.open(player);
        return true;
    }

    public String uuidToName(UUID owner, String s)
    {
        return Sponge.server().gameProfileManager().basicProfile(owner).join().name().orElse(s);
    }

    private void notify(UUID owner, ServerPlayer player, Mutable dataHolder, int flags)
    {
        if (ProtectionFlag.NOTIFY_ACCESS.isSet(flags) && !perms.PREVENT_NOTIFY.check(player))
        {
            Sponge.server().player(owner).ifPresent(ownerPlayer -> {
                // TODO notify
                // TODO prevent spamming. ~60s
//                i18n.send(ownerPlayer, NEUTRAL, "{player} accessed your protected {type} at {vector} in {world}" , player, type, pos, world);
            });
        }
    }

    public int canAccess(DataHolder dataHolder, @Nullable ServerPlayer player, UUID owner, @Nullable Permission byPassPerm)
    {
        final boolean isOwner = player != null && player.uniqueId().equals(owner);
        if (isOwner)
        {
            return ProtectionFlag.ALL; // allowed by owner
        }
        final Map<UUID, Integer> trust = this.getTrust(owner);
        if (trust.containsKey(player.uniqueId()))
        {
            return trust.get(player.uniqueId());
        }
        final boolean hasValidKeyBook = player != null && isValidKeyBook(dataHolder, player);
        if (hasValidKeyBook)
        {
            ItemStack heldItem = player.itemInHand(HandTypes.MAIN_HAND);
            return heldItem.get(LockerData.ACCESS).get().getOrDefault(player.uniqueId(), 0);
        }
        final boolean canAccessOther = player != null && byPassPerm != null && byPassPerm.check(player);
        if (canAccessOther)
        {
            return ProtectionFlag.ALL; // allowed by permission
        }
        final boolean isUnlocked = player != null && dataHolder.get(LockerData.UNLOCKS).orElse(Collections.emptyList()).contains(player.uniqueId());
        if (isUnlocked)
        {
            return ProtectionFlag.FULL;
        }
        if (player != null)
        {
            return dataHolder.get(LockerData.ACCESS).orElse(Collections.emptyMap()).getOrDefault(player.uniqueId(), ProtectionFlag.NONE);
        }
        return ProtectionFlag.NONE;
    }

    private Map<UUID, Integer> getTrust(UUID owner)
    {
        final Map<UUID, Integer> trust = this.trustCache.get(owner);
        if (trust == null)
        {
            final Optional<Map<UUID, Integer>> onlineTrust = Sponge.server().player(owner).flatMap(p -> p.get(LockerData.TRUST));
            if (onlineTrust.isPresent())
            {
                this.trustCache.put(owner, onlineTrust.get());
            }
            else
            {
                final Map<UUID, Integer> offlineTrust = Sponge.server().userManager().load(owner).join().flatMap(p -> p.get(LockerData.TRUST)).orElse(Collections.emptyMap());
                this.trustCache.put(owner, offlineTrust);
            }
        }
        return this.trustCache.get(owner);
    }

    public boolean isValidKeyBook(DataHolder dataHolder, ServerPlayer player)
    {
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (true) {
            return false;
        }
        // TODO check
        i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "As you approach with your KeyBook the magic lock disappears!");
        return true;
    }

    public static void playUnlockSound(ServerPlayer player, final ServerLocation effectLocation, TaskManager tm)
    {
//        holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
//        holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, (float) 1.5);
//
//        tm.runTaskDelayed(Locker.class, () -> {
//            holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
//            holder.playSound(BLOCK_PISTON_EXTEND, effectLocation.getPosition(), 1, (float)1.5);
//        }, 3);
    }

    public boolean handleEntityInteract(Mutable dataHolder, ServerPlayer player)
    {
        final Integer flags = dataHolder.get(LockerData.FLAGS).orElse(null);
        if (flags == null)
        {
            return false;
        }
        final UUID owner = dataHolder.get(LockerData.OWNER).get();
        final String ownerName = uuidToName(owner, owner.toString());
        if (perms.SHOW_OWNER.check(player))
        {
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "This entity is protected by {name}", ownerName);
        }

        final int accessFlags = canAccess(dataHolder, player, owner, perms.ACCESS_OTHER);
        final boolean blockInteract = ProtectionFlag.ENTITY_INTERACT.isBlocked(flags, accessFlags);
        if (blockInteract)
        {
            if (perms.SHOW_OWNER.check(player))
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic from {user} repelled your attempts to reach this entity!", ownerName);
            }
            else
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE,  "Magic repelled your attempts to reach this entity!");
            }
            return true;
        }

        this.notify(owner, player, dataHolder, flags);
        // TODO update lastAccess

        return false;
    }

    public boolean handleRedstoneInteract(Mutable dataHolder)
    {
        final Integer flags = dataHolder.get(LockerData.FLAGS).orElse(null); // TODO performance?
        if (flags == null)
        {
            return false;
        }
        if (ProtectionFlag.BLOCK_REDSTONE.isSet(flags))
        {
            return true;
        }
        return false;
    }

    public boolean handleHopperInteract(Inventory source, Inventory target)
    {
        final Integer sourceFlags = source.get(LockerData.FLAGS).orElseGet(() -> getCarrierFlags(source));
        final Integer targetFlags = target.get(LockerData.FLAGS).orElseGet(() -> getCarrierFlags(source));
        return ProtectionFlag.INVENTORY_HOPPER_TAKE.isSet(sourceFlags) || ProtectionFlag.INVENTORY_HOPPER_PUT.isSet(targetFlags);
    }

    public int getCarrierFlags(Inventory source)
    {
        if (source instanceof CarriedInventory && ((CarriedInventory<?>)source).carrier().orElse(null) instanceof Mutable)
        {
            return ((Mutable)((CarriedInventory<?>)source).carrier().get()).get(LockerData.FLAGS).orElse(ProtectionFlag.NONE);
        }
        return ProtectionFlag.NONE;
    }

    public boolean handleEntityDamage(Mutable dataHolder, @Nullable ServerPlayer player)
    {
        final Integer flags = dataHolder.get(LockerData.FLAGS).orElse(null);
        if (flags == null)
        {
            return false;
        }
        final UUID owner = dataHolder.get(LockerData.OWNER).get();
        final int accessFlags = canAccess(dataHolder, player, owner, perms.BREAK_OTHER);
        if (player != null)
        {
            final boolean blockDamage = ProtectionFlag.ENTITY_DAMAGE.isBlocked(flags, accessFlags);
            if (blockDamage)
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic repelled your attempts to hit this entity!");
                return true;
            }
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "The magic surrounding this entity quivers as you hit it!");
            return false;
        }

        if (ProtectionFlag.ENTITY_DAMAGE_ENVIRONMENT.isSet(flags))
        {
            return true;
        }
        return false;
    }

    public boolean handleBlockInteract(DataHolder.Mutable dataHolder, ServerPlayer player)
    {
        final Integer flags = dataHolder.get(LockerData.FLAGS).orElse(null);
        if (flags == null)
        {
            return false;
        }
        final UUID owner = dataHolder.get(LockerData.OWNER).get();
        final String ownerName = uuidToName(owner, owner.toString());
        if (perms.SHOW_OWNER.check(player))
        {
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "This block is protected by {user}", ownerName);
        }

        final int accessFlags = canAccess(dataHolder, player, owner, null);

        final boolean blockInteract = ProtectionFlag.BLOCK_INTERACT.isBlocked(flags, accessFlags);
        if (blockInteract)
        {
            if (perms.SHOW_OWNER.check(player))
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from interacting with this block of {user}!", ownerName);
            }
            else
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from interacting with this block!");
            }
            return true;
        }
        this.notify(owner, player, dataHolder, flags);
        return false;
    }

    public Mutable getDataHolderAtLoc(ServerLocation serverLoc)
    {
        if (serverLoc == null)
        {
            return null;
        }
        return serverLoc.blockEntity().map(Mutable.class::cast).orElse(serverLoc);
    }

    public boolean handleBlockBreak(@Nullable BlockSnapshot snap, @Nullable ServerPlayer player, boolean withDetachable, Set<Vector3i> checkedPositions)
    {
        // TODO loc does not have the TileEntityData anymore
        if (snap == null || checkedPositions.contains(snap.position()))
        {
            return false;
        }
        final ServerLocation loc = snap.location().get();
        checkedPositions.add(loc.blockPosition());
        final Integer flags = snap.get(LockerData.FLAGS).orElse(null);

        if (flags == null)
        {
            return false;
        }

        final UUID owner = snap.get(LockerData.OWNER).orElse(null);
        final int accessFlags = this.canAccess(snap, player, owner, perms.BREAK_OTHER);
        if (ProtectionFlag.BLOCK_BREAK.isBlocked(flags, accessFlags))
        {
            if (player != null && perms.SHOW_OWNER.check(player))
            {
                final String ownerName = uuidToName(owner, owner.toString());
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from breaking this protection of {user}!", ownerName);
            }
            else if (player != null)
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Magic prevents you from breaking this protection!");
            }
            return true;
        }

        if (withDetachable)
        {
            // Detachable Blocks
            for (ServerLocation detachableBlock : BlockUtil.getDetachableBlocks(loc))
            {
                if (handleBlockBreak(detachableBlock.createSnapshot(), player, false, checkedPositions))
                {
                    return true;
                }
            }

            // Detachable Entities
            for (Hanging entity : loc.world().entities(Hanging.class, AABB.of(loc.blockPosition().sub(Vector3i.ONE), loc.blockPosition().add(Vector3i.ONE))))
            {
                if (entity.serverLocation().relativeTo(entity.get(Keys.DIRECTION).orElse(Direction.NONE)).blockPosition().equals(loc.blockPosition()))
                {
                    if (handleEntityDamage(entity, player))
                    {
                        return true;
                    }
                }
            }
        }

        tm.runTask(() -> removeLock(getDataHolderAtLoc(loc), player));
        return true;
    }

    public void openBook(ServerPlayer player)
    {
        final ItemStack lockerBook = player.itemInHand(HandTypes.MAIN_HAND);
        player.openBook(Book.book(Component.empty(), Component.empty(), buildPages(player, lockerBook)));
    }

    private List<Component> buildPages(ServerPlayer player, ItemStack lockerBook)
    {
        final List<Component> pages = new ArrayList<>();
        final LockerMode currentMode = lockerBook.get(LockerData.MODE).map(LockerMode::valueOf).orElse(LockerMode.INFO_CREATE);
        final Builder builder = Component.text();
        builder.append(Component.text("Mode Selection").append(Component.newline())).append(Component.newline());
        for (LockerMode mode : LockerMode.values())
        {
            if (currentMode.equals(mode))
            {
                builder.append(Component.text(mode.text, NamedTextColor.GOLD).clickEvent(ClickEvent.changePage(2))).append(Component.newline());
            }
            else
            {
                builder.append(Component.text(mode.text).clickEvent(SpongeComponents.executeCallback(c -> {
                    lockerBook.offer(LockerData.MODE, mode.name());
                    player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                    i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Changed mode to {name}", mode.name());
                    openBook(player);
                }))).append(Component.newline());
            }
        }

        pages.add(builder.build());
        pages.addAll(modeData(player, lockerBook, currentMode));
        return pages;
    }

    private List<Component> modeData(ServerPlayer player, ItemStack lockerBook, LockerMode currentMode)
    {
        final Style titleStyle = Style.style(TextDecoration.UNDERLINED).color(NamedTextColor.GRAY);

        final List<Component> pages = new ArrayList<>();
        final int flags = lockerBook.get(LockerData.FLAGS).orElse(0);
        final Map<UUID, Integer> accessMap = lockerBook.get(LockerData.ACCESS).orElse(Collections.emptyMap());
        switch (currentMode)
        {
            case INFO:
            case REMOVE:
                break;
            case INFO_CREATE:
            case UPDATE:
              {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Block Flags:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.BLOCK_INTERACT);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.BLOCK_BREAK);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.BLOCK_EXPLOSION);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.BLOCK_REDSTONE);
                    builder.append(Component.newline());
                    builder.append(Component.text("Presets:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildPresetLine(player, lockerBook, flags, i18n.translate(player, "private"), builder,
                                    Arrays.asList(ProtectionFlag.BLOCK_REDSTONE), ProtectionFlag.BLOCK_INTERACT, ProtectionFlag.BLOCK_BREAK, ProtectionFlag.BLOCK_EXPLOSION);
                    buildPresetLine(player, lockerBook, flags, i18n.translate(player, "public"), builder,
                                    Arrays.asList(ProtectionFlag.BLOCK_REDSTONE, ProtectionFlag.BLOCK_INTERACT, ProtectionFlag.BLOCK_BREAK, ProtectionFlag.BLOCK_EXPLOSION));
                    pages.add(builder.build());
                }
                {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Inventory Flags:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.INVENTORY_TAKE);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.INVENTORY_PUT);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.INVENTORY_HOPPER_TAKE);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.INVENTORY_HOPPER_PUT);

                    builder.append(Component.newline());
                    builder.append(Component.text("Presets:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildPresetLine(player, lockerBook, flags, i18n.translate(player, "protected"), builder,
                                    Arrays.asList(ProtectionFlag.BLOCK_INTERACT), ProtectionFlag.INVENTORY_TAKE, ProtectionFlag.INVENTORY_PUT);
                    buildPresetLine(player, lockerBook, flags, i18n.translate(player, "donation"), builder,
                                    Arrays.asList(ProtectionFlag.BLOCK_INTERACT, ProtectionFlag.INVENTORY_PUT), ProtectionFlag.INVENTORY_TAKE);
                    buildPresetLine(player, lockerBook, flags, i18n.translate(player, "free"), builder,
                                    Arrays.asList(ProtectionFlag.BLOCK_INTERACT, ProtectionFlag.INVENTORY_TAKE), ProtectionFlag.INVENTORY_PUT);

                    pages.add(builder.build());
                }
                {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Entity Flags:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.ENTITY_INTERACT);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.ENTITY_DAMAGE);
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.ENTITY_DAMAGE_ENVIRONMENT);
                    pages.add(builder.build());
                }
                {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Other:").style(titleStyle)).append(Component.newline()).append(Component.newline());
                    buildFlagToggleLine(player, lockerBook, flags, builder, ProtectionFlag.NOTIFY_ACCESS);
                    pages.add(builder.build());
                }
                {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Access "));
                    builder.append(i18n.translate(player, "(Add new)").color(NamedTextColor.GOLD).clickEvent(SpongeComponents.executeCallback(c -> {
                        i18n.send(player, POSITIVE, "Punch your friend");
                        accessBookPunchers.add(c.cause().first(ServerPlayer.class).get().uniqueId());
                    })));

                    builder.append(Component.newline()).append(Component.newline());
                    for (Entry<UUID, Integer> entry : accessMap.entrySet())
                    {
                        final String userName = uuidToName(entry.getKey(), entry.getKey().toString());
                        builder.append(Component.text(userName, NamedTextColor.DARK_GREEN));
                        builder.append(Component.text(" (-)", NamedTextColor.DARK_RED)
                                                .hoverEvent(HoverEvent.showText(i18n.translate(player, "revoke access")))
                                                .clickEvent(SpongeComponents.executeCallback(c -> {
                            accessMap.remove(entry.getKey());
                            lockerBook.offer(LockerData.ACCESS, accessMap);
                            player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                            openBook(player);
                            i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Removed access from {user}", userName);
                        })));
                        if (ProtectionFlag.ADMIN.isSet(entry.getValue()))
                        {
                            builder.append(Component.text(" @ ", NamedTextColor.GOLD).append(Component.text("(-)", NamedTextColor.DARK_RED)
                                                                                                      .hoverEvent(HoverEvent.showText(i18n.translate(player, "revoke admin access")))
                                                                                                      .clickEvent(SpongeComponents.executeCallback(c -> {
                                accessMap.put(entry.getKey(), entry.getValue() & ~ProtectionFlag.ADMIN.flagValue);
                                lockerBook.offer(LockerData.ACCESS, accessMap);
                                player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                                openBook(player);
                                i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Revoked admin access from {user}", userName);
                            }))));
                        }
                        else
                        {
                            builder.append(Component.text(" @ ").append(Component.text("(+)", NamedTextColor.DARK_GREEN)
                                                                                 .hoverEvent(HoverEvent.showText(i18n.translate(player, "grant admin access")))
                                                                                 .clickEvent(SpongeComponents.executeCallback(c -> {
                                accessMap.put(entry.getKey(), entry.getValue() | ProtectionFlag.ADMIN.flagValue);
                                lockerBook.offer(LockerData.ACCESS, accessMap);
                                player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                                openBook(player);
                                i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Granted admin access to {user}", userName);
                            }))));
                        }
                        builder.append(Component.newline());
                    }
                    // TODO more pages
                    pages.add(builder.build());
                }
                break;
            case TRUST:
                {
                    final Builder builder = Component.text();
                    builder.append(Component.text("Trust "));
                    builder.append(i18n.translate(player, "(Add new)").color(NamedTextColor.GOLD).clickEvent(SpongeComponents.executeCallback(c -> {
                        i18n.send(player, POSITIVE, "Punch your trusted friend");
                        trustBookPunchers.add(c.cause().first(ServerPlayer.class).get().uniqueId());
                    })));
                    builder.append(Component.newline()).append(Component.newline());
                    final Map<UUID, Integer> trustMap = player.get(LockerData.TRUST).orElse(new HashMap<>());
                    for (Entry<UUID, Integer> entry : trustMap.entrySet())
                    {
                        final String userName = uuidToName(entry.getKey(), entry.getKey().toString());
                        builder.append(Component.text(userName, NamedTextColor.DARK_GREEN));
                        builder.append(Component.text(" (-)", NamedTextColor.DARK_RED)
                                                .hoverEvent(HoverEvent.showText(i18n.translate(player, "revoke trust")))
                                                .clickEvent(SpongeComponents.executeCallback(c -> {
                                                    trustMap.remove(entry.getKey());
                                                    player.offer(LockerData.TRUST, trustMap);
                                                    this.invalidateTrustCache(player.uniqueId());
                                                    openBook(player);
                                                    i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Removed trust from {user}", userName);
                                                })));
                        if (ProtectionFlag.ADMIN.isSet(entry.getValue()))
                        {
                            builder.append(Component.text(" @ ", NamedTextColor.GOLD).append(Component.text("(-)", NamedTextColor.DARK_RED)
                                      .hoverEvent(HoverEvent.showText(i18n.translate(player, "revoke admin trust")))
                                      .clickEvent(SpongeComponents.executeCallback(c -> {
                                          trustMap.put(entry.getKey(), entry.getValue() & ~ProtectionFlag.ADMIN.flagValue);
                                          player.offer(LockerData.TRUST, trustMap);
                                          this.invalidateTrustCache(player.uniqueId());
                                          openBook(player);
                                          i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Revoked admin trust from {user}", userName);
                                      }))));
                        }
                        else
                        {
                            builder.append(Component.text(" @ ").append(Component.text("(+)", NamedTextColor.DARK_GREEN)
                                     .hoverEvent(HoverEvent.showText(i18n.translate(player, "grant admin trust")))
                                     .clickEvent(SpongeComponents.executeCallback(c -> {
                                         trustMap.put(entry.getKey(), entry.getValue() | ProtectionFlag.ADMIN.flagValue);
                                         player.offer(LockerData.TRUST, trustMap);
                                         this.invalidateTrustCache(player.uniqueId());
                                         openBook(player);
                                         i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Granted admin trust to {user}", userName);
                                     }))));
                        }
                        builder.append(Component.newline());
                    }
                    pages.add(builder.build());
                }
                break;
            case KEYBOOK:
                // TODO
                break;
        }
        return pages;
    }

    private void buildFlagToggleLine(ServerPlayer player, ItemStack lockerBook, int flags, Builder builder, ProtectionFlag flag)
    {
        final boolean isSet = flag.isSet(flags);
        builder.append(Component.text(flag.flagname).append(Component.text(": ")).append(Component.text(isSet, isSet ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED)
                                                                                        .hoverEvent(HoverEvent.showText(i18n.translate(player, "click to toggle"))))
               .clickEvent(SpongeComponents.executeCallback(c -> {
                   lockerBook.offer(LockerData.FLAGS, flags ^ flag.flagValue);
                   player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                   i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Toggled flag {name} to {txt#value}", flag.flagname, Component.text(!isSet, !isSet ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
                   openBook(player);
               })))
               .append(Component.newline());
    }

    private void buildPresetLine(ServerPlayer player, ItemStack lockerBook, int flags, Component presetName, Builder builder, List<ProtectionFlag> unsetFlags, ProtectionFlag... setFlags)
    {
        final Builder setFlagsText = Component.text();
        setFlagsText.append(i18n.translate(player, "Setting:"));
        for (ProtectionFlag setFlag : setFlags)
        {
            setFlagsText.append(Component.newline());
            setFlagsText.append(Component.text(" - " + setFlag.flagname).color(NamedTextColor.GREEN));
        }
        setFlagsText.append(Component.newline());
        setFlagsText.append(i18n.translate(player, "Unsetting:"));
        for (ProtectionFlag unsetFlag : unsetFlags)
        {
            setFlagsText.append(Component.newline());
            setFlagsText.append(Component.text(" - " + unsetFlag.flagname).color(NamedTextColor.RED));
        }
        builder.append(presetName.hoverEvent(HoverEvent.showText(setFlagsText))
                                .clickEvent(SpongeComponents.executeCallback(c -> {
                                    int newFlags = flags;
                                    for (ProtectionFlag unsetFlag : unsetFlags)
                                    {
                                        newFlags &= ~unsetFlag.flagValue;
                                    }
                                    for (ProtectionFlag unsetFlag : setFlags)
                                    {
                                        newFlags |= unsetFlag.flagValue;
                                    }
                                    lockerBook.offer(LockerData.FLAGS, newFlags);
                                    player.setItemInHand(HandTypes.MAIN_HAND, lockerBook);
                                    i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Using preset {txt}", presetName);
                                    openBook(player);
                                })))
               .append(Component.newline());
    }

    public void invalidateTrustCache(UUID owner)
    {
        this.trustCache.remove(owner);
    }

    private static class ProtectedInventory implements SlotChangeHandler
    {
        private boolean blockPut;
        private boolean blockTake;

        public ProtectedInventory(boolean blockTake, boolean blockPut)
        {

            this.blockPut = blockPut;
            this.blockTake = blockTake;
        }

        @Override
        public boolean handle(Cause cause, Container container, Slot slot, int slotIndex, ItemStackSnapshot oldStack, ItemStackSnapshot newStack)
        {
            if (slot.viewedSlot().parent() instanceof PlayerInventory)
            {
                return true; // Allow all changes in player inventory
            }

            final ItemStack stack1 = oldStack.createStack();
            final ItemStack stack2 = newStack.createStack();
            final int compare = ItemStackComparators.IGNORE_SIZE.get().compare(stack1, stack2);
            if (compare != 0) // Stack type changed
            {
                if (stack2.isEmpty() && !this.blockTake)
                {
                    return true; // Allow taking stack out
                }
                if (stack1.isEmpty() && !this.blockPut)
                {
                    return true; // Allow putting stack in
                }
                return false;
            }
            final int oldQuantity = stack1.quantity();
            final int newQuantity = stack2.quantity();
            if (oldQuantity > newQuantity && !this.blockTake)
            {
                return true; // Allow taking stack out
            }
            if (newQuantity > oldQuantity && !this.blockPut)
            {
                return true; // Allow putting stacks int
            }
            if (newQuantity == oldQuantity)
            {
                return true; // Nothing changed
            }
            return false; // Block this
        }
    }
}
