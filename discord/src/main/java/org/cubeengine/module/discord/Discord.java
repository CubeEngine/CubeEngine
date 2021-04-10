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
package org.cubeengine.module.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
@Module
public class Discord {
    private final AtomicReference<GatewayDiscordClient> client = new AtomicReference<>(null);
    private final AtomicReference<Webhook> webhook = new AtomicReference<>(null);
    private final AtomicReference<Server> server = new AtomicReference<>(null);
    private final PluginContainer pluginContainer;

    @ModuleConfig
    DiscordConfig config;

    @Inject
    public Discord(PluginContainer pluginContainer) {
        this.pluginContainer = pluginContainer;
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        server.set(event.engine());
        Mono.justOrEmpty(config.botToken)
                .flatMap(token ->DiscordClient.create(token)
                        .gateway()
                        .setEnabledIntents(IntentSet.of(Intent.GUILDS,
                                Intent.GUILD_MEMBERS,
                                Intent.GUILD_VOICE_STATES,
                                Intent.GUILD_MESSAGES,
                                Intent.GUILD_MESSAGE_REACTIONS,
                                Intent.DIRECT_MESSAGES))
                        .setInitialPresence(info -> ClientPresence.online(ClientActivity.watching("You")))
                        .setMemberRequestFilter(MemberRequestFilter.none())
                        .login()
                )
                .subscribe(this::clientConnected);
    }

    private void clientConnected(GatewayDiscordClient client) {
        this.client.set(client);

        Mono.justOrEmpty(config.webhook)
                .flatMap(webhook -> Mono.justOrEmpty(webhook.id).flatMap(id -> Mono.justOrEmpty(webhook.token).flatMap(token ->
                    client.getWebhookByIdWithToken(Snowflake.of(id), token)
                ))).subscribe(this.webhook::set);

        client.getUsers()
                .flatMap(user -> client.on(MessageCreateEvent.class).filter(m -> m.getMessage().getAuthor().map(u -> !u.getId().equals(user.getId())).orElse(true)))
                .filter(m -> m.getMessage().getAuthor().map(u -> !u.isBot()).orElse(true))
                .flatMap(m -> m.getMessage().getChannel().ofType(TextChannel.class).filter(c -> c.getName().equals(config.channel)).map(c -> m))
                .subscribe(this::onDiscordChat);
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {
        server.set(null);
        GatewayDiscordClient client = this.client.getAndSet(null);
        if (client != null) {
            client.logout();
        }
    }

    @Listener(order = Order.LAST)
    public void onMinecraftChat(PlayerChatEvent event, @Root ServerPlayer player) {
        final Webhook w = webhook.get();
        if (w != null) {
            w.execute(spec -> spec.setContent(event.message().toString()).setUsername(player.displayName().get().examinableName()));
        }
    }

    private void onDiscordChat(MessageCreateEvent event) {
        Server s = server.get();
        if (s != null) {
            final Task task = Task.builder()
                    .execute(() -> s.sendMessage(Component.text(event.getMessage().getContent())))
                    .plugin(pluginContainer)
                    .build();
            s.scheduler().submit(task);
        }
    }

}
