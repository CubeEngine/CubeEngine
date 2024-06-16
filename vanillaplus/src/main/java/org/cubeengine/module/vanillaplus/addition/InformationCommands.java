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
package org.cubeengine.module.vanillaplus.addition;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.entity.EntityTypes.ITEM_FRAME;

@Singleton
public class InformationCommands extends PermissionContainer
{
    private VanillaPlus module;
    private I18n i18n;

    @Inject
    public InformationCommands(PermissionManager pm, VanillaPlus module, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Displays the biome type you are standing in.")
    public void biome(CommandCause context,
                      @Option ServerWorld world,
                      @Label ("<x> <z>") @Option Vector2i blockPos)
    {
        if (!(context.subject() instanceof ServerPlayer) && (world == null || blockPos == null))
        {
            i18n.send(context, NEGATIVE, "Please provide a world and x and z coordinates!");
            return;
        }
        if (world == null)
        {
            world = ((ServerPlayer)context.subject()).world();
        }
        if (blockPos == null)
        {
            final ServerLocation loc = ((ServerPlayer)context.subject()).serverLocation();
            if (loc.world() == world)
            {
                blockPos = new Vector2i(loc.blockX(), loc.blockZ());
            }
            else
            {
                blockPos = new Vector2i(world.properties().spawnPosition().x(), world.properties().spawnPosition().z());
            }
        }

        Biome biome = world.biome(blockPos.x(), 0, blockPos.y());
        i18n.send(context, NEUTRAL, "Biome at {vector:x\\=:z\\=} in {world}: {name}", blockPos, world, RegistryTypes.BIOME.keyFor(world, biome).asString());
    }

    @Command(desc = "Displays the seed of a world.")
    public void seed(CommandCause context, @Option ServerWorld world)
    {
        if (world == null)
        {
            if (!(context.subject() instanceof ServerPlayer))
            {
                i18n.send(context, CRITICAL,"Too few arguments!");
                return;
            }

            world = ((ServerPlayer)context.subject()).world();
        }
        final long seed = world.properties().worldGenerationConfig().seed();
        i18n.send(context, NEUTRAL, "Seed of {world} is {input#seed}", world, Component.text(seed)
                .clickEvent(ClickEvent.copyToClipboard(String.valueOf(seed)))
                .hoverEvent(HoverEvent.showText(i18n.translate(context, "click to copy"))));
    }

    @Command(desc = "Displays the direction in which you are looking.")
    @Restricted(msg = "{text:ProTip}: I assume you are looking right at your screen, right?")
    public void compass(ServerPlayer context)
    {
        Vector3d rotation = context.rotation();
        Vector3d direction = Quaterniond.fromAxesAnglesDeg(rotation.x(), -rotation.y(), rotation.z()).direction();
        i18n.send(context, NEUTRAL, "You are looking to {input#direction}!", Direction.closest(direction).name()); // TODO translation of direction
    }

    @Command(desc = "Displays your current depth.")
    @Restricted(msg = "You dug too deep!")
    public void depth(ServerPlayer context)
    {
        final int height = context.location().blockY();
        if (height > 62)
        {
            i18n.send(context, POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} above sealevel)", height, height - 62);
            return;
        }
        i18n.send(context, POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} below sealevel)", height, 62 - height);
    }

    @Command(desc = "Displays your current location.")
    @Restricted(msg = "Your position: {text:Right in front of your screen!:color=RED}")
    public void getPos(ServerPlayer context)
    {
        final ServerLocation loc = context.serverLocation();
        i18n.send(context, NEUTRAL, "Your position is {vector:x\\=:y\\=:z\\=}", new Vector3i(loc.blockX(), loc.blockY(), loc.blockZ()));
    }

    @Command(desc = "Displays near players(entities/mobs) to you.")
    public void near(CommandCause context, @Option Integer radius, @Default ServerPlayer player, @Flag boolean entity, @Flag boolean mob)
    {
        if (radius == null)
        {
            radius = this.module.getConfig().add.commandNearDefaultRadius;
        }

        LinkedList<Component> outputlist = new LinkedList<>();
        ServerLocation userLocation = player.serverLocation();
        TreeMap<Double, List<Entity>> sortedMap = new TreeMap<>();
        player.nearbyEntities(radius, e -> entity || mob && e instanceof Living || e instanceof ServerPlayer).forEach(e -> {
            if (e != player)
            {
                double distance = e.position().distance(userLocation.position());
                sortedMap.computeIfAbsent(distance, k -> new ArrayList<>()).add(e);
            }
        });

        int i = 0;
        LinkedHashMap<Component, Pair<Double, Integer>> groupedEntities = new LinkedHashMap<>();
        for (double dist : sortedMap.keySet())
        {
            i++;
            for (Entity e : sortedMap.get(dist))
            {
                if (i <= 10)
                {
                    this.addNearInformation(context, outputlist, e, Math.sqrt(dist));
                    continue;
                }
                Component key;
                if (e instanceof ServerPlayer)
                {
                    key = i18n.translate(context, Style.style(NamedTextColor.DARK_GREEN),"player");
                }
                else if (e instanceof Living)
                {
                    key = e.type().asComponent().color(NamedTextColor.DARK_AQUA);
                }
                else if (e instanceof Item)
                {
                    final ItemStack stack = e.get(Keys.ITEM_STACK_SNAPSHOT).get().createStack();
                    key = stack.type().asComponent().color(NamedTextColor.GRAY);
                }
                else
                {
                    key = e.type().asComponent().color(NamedTextColor.GRAY);
                }
                Pair<Double, Integer> pair = groupedEntities.get(key);
                if (pair == null)
                {
                    pair = new Pair<>(Math.sqrt(dist), 1);
                    groupedEntities.put(key, pair);
                }
                else
                {
                    pair.setRight(pair.getRight() + 1);
                }
            }
        }
        final Builder builder = Component.text();
        for (Component key : groupedEntities.keySet())
        {
            builder.append(Component.newline())
                   .append(Component.text(groupedEntities.get(key).getRight(), NamedTextColor.GOLD))
                   .append(Component.text("x ")).append(key)
                   .append(Component.text(" (", NamedTextColor.WHITE).append(
                       Component.text(groupedEntities.get(key).getLeft().intValue() + "m", NamedTextColor.GOLD))
                    .append(Component.text(")")));
        }
        if (outputlist.isEmpty())
        {
            i18n.send(context, NEGATIVE, "Nothing detected nearby!");
            return;
        }
        final var separator = JoinConfiguration.separator(Component.text(", ", NamedTextColor.WHITE));
        Component result = Component.join(separator, outputlist).append(builder.build());
        if (context.subject().equals(player))
        {
            i18n.send(context, NEUTRAL, "Found those nearby you:");
            context.sendMessage(Identity.nil(), result);
            return;
        }
        i18n.send(context, NEUTRAL, "Found those nearby {user}:", player);
        context.sendMessage(Identity.nil(), result);
    }

    private void addNearInformation(CommandCause context, List<Component> list, Entity entity, double distance)
    {
        Component s;
        if (entity instanceof ServerPlayer)
        {
            s = Component.text(((ServerPlayer)entity).name(), NamedTextColor.DARK_GREEN);
        }
        else if (entity instanceof Living)
        {
            s = entity.type().asComponent().color(NamedTextColor.DARK_AQUA);
        }
        else if (entity instanceof Item)
        {
            final ItemStack stack = entity.get(Keys.ITEM_STACK_SNAPSHOT).get().createStack();
            s = stack.get(Keys.DISPLAY_NAME).get().color(NamedTextColor.GRAY);
        }
        else
        {
            s = entity.type().asComponent().color(NamedTextColor.GRAY);
        }

        if (context.audience() instanceof ServerPlayer)
        {
            s.hoverEvent(HoverEvent.showText(i18n.translate(context, NEUTRAL, "Click here to teleport")));
            s.clickEvent(SpongeComponents.executeCallback(c -> {
                ((ServerPlayer)c).setLocation(entity.serverLocation());
            }));
        }
        s.append(Component.text(" (", NamedTextColor.WHITE)
                 .append(Component.text( (int)distance + "m", NamedTextColor.GOLD))
                 .append(Component.text(")")));
        list.add(s);
    }

    @Command(alias = "pong", desc = "Pong!")
    public void ping(CommandCause context)
    {
        boolean ping = context.context().get(EventContextKeys.COMMAND).get().toLowerCase().startsWith("/ping");
        if (context.subject() instanceof ServerPlayer)
        {
            final Optional<EngineConnectionState> state = ((ServerPlayer)context.subject()).connection().state();
            // TODO check is this working?
            state.ifPresent(s -> {
                if (s instanceof ServerConnectionState.Game gameState)
                {
                    i18n.send(context, MessageType.NEUTRAL, (ping ? "pong" : "ping") + "! Your latency: {integer#ping}",
                              gameState.latency());
                }
            });

            return;
        }
        i18n.send(context, NEUTRAL, (ping ? "ping" : "pong") + " in the console?");
    }

    @Command(desc = "Displays chunk, memory and world information.")
    public void lag(CommandCause context)
    {
        //Uptime:
//        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, context.getLocale());
        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT); // TODO locale
        Date start = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());

        final Locale locale = context.audience() instanceof ServerPlayer ? ((ServerPlayer)context.audience()).locale() : Locale.getDefault();
        String uptime = TimeUtil.format(locale, System.currentTimeMillis() - start.getTime());
        i18n.send(context, POSITIVE, "Server has been running since {input#uptime}", df.format(start));
        i18n.send(context, POSITIVE, "Uptime: {input#uptime}", uptime);
        //TPS:
        double tps = Sponge.server().ticksPerSecond();
        NamedTextColor color = tps == 20 ? NamedTextColor.DARK_GREEN :
                       tps > 17 ?  NamedTextColor.YELLOW :
                       tps > 10 ?  NamedTextColor.RED :
                       tps == 0 ?  NamedTextColor.YELLOW :
                                   NamedTextColor.DARK_RED;
        i18n.send(context, POSITIVE, "Current TPS: {txt}", Component.text((int)tps, color));
        //Memory
        long memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1048576;
        long memCom = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() / 1048576;
        long memMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / 1048576;
        long memUsePercent = 100 * memUse / memMax;
        color = memUsePercent > 90 ? memUsePercent > 95 ? NamedTextColor.DARK_RED : NamedTextColor.RED : memUsePercent > 60 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GREEN;
        i18n.send(context, POSITIVE, "Memory Usage: {txt#memused}/{integer#memcom}/{integer#memMax} MB", Component.text(memUse, color), memCom, memMax);
        //Worlds with loaded Chunks / Entities
        for (ServerWorld world : Sponge.server().worldManager().worlds())
        {
            final long loadedChunks = StreamSupport.stream(world.loadedChunks().spliterator(), false).count();
            int entities = world.entities().size();
            i18n.send(context, POSITIVE, "{world}: {amount} chunks {amount} entities", world, loadedChunks, entities);

            final Builder builder = Component.text();

            boolean foundAny = false;
            world.entities().stream().filter(e -> e.type() != ITEM_FRAME.get()).collect(Collectors.groupingBy(e -> e.location().chunkPosition()))
                    .entrySet().stream().filter(e -> e.getValue().size() > 50).forEach(e -> {
                final Component pos = Component.text(e.getKey().x(), NamedTextColor.GOLD).append(Component.text(":", NamedTextColor.GRAY)).append(Component.text(e.getKey().z(), NamedTextColor.GOLD));
                pos.hoverEvent(HoverEvent.showText(i18n.translate(context, NEUTRAL, "Click here to teleport")));
                pos.clickEvent(SpongeComponents.executeCallback(c -> ((ServerPlayer)c.subject()).setLocation(e.getValue().get(0).serverLocation())));
                builder.append(pos, Component.space());
            });
            final TextComponent builtText = builder.build();
            if (!builtText.children().isEmpty())
            {
                i18n.send(context, NEUTRAL, "High entity count in Chunks: {txt#list}", builtText);
            }
        }

    }

    // TODO only load when cubeengine-worlds is not present
//    @Command(desc = "Displays all loaded worlds", alias = {"worldlist","worlds"})
//    public void listWorlds(CommandCause context)
//    {
//        i18n.send(context, POSITIVE, "Loaded worlds:");
//        for (ServerWorld world : Sponge.server().worldManager().worlds())
//        {
//            Component text = Component.space().append(Component.text("- ", NamedTextColor.WHITE))
//                     .append(world.properties().displayName().orElse(Component.text(world.key().value())).color(NamedTextColor.GOLD))
//                     .append(Component.text(": ", NamedTextColor.WHITE))
//                     .append(Component.text(world.key().asString(), NamedTextColor.BLUE));
//            context.sendMessage(Identity.nil(), text);
//        }
//    }
}
