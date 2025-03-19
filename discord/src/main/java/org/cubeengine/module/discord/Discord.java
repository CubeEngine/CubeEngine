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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.ComponentUtil;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.plugin.PluginContainer;

import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static org.cubeengine.libcube.util.ComponentUtil.autoLink;
import static org.cubeengine.libcube.util.ComponentUtil.stripLegacy;
import static org.cubeengine.libcube.util.StringUtils.replaceWithCallback;

@Singleton
@Module
public class Discord {
    public static final String DEFAULT_CHAT_FORMAT = "{NAME}: {MESSAGE}";
    private static final Pattern EMOJI_FROM_DISCORD = Pattern.compile("<(:[^: ]+:)\\d+>");
    private static final Pattern EMOJI_FROM_MINECRAFT = Pattern.compile(":([^: ]+):");
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@(\\d+)>");

    @ModuleConfig private DiscordConfig config;
    @ModuleCommand private DiscordCommands commands;
    @Inject private PluginContainer pluginContainer;
    @Inject private I18n i18n;
    @Inject private TaskManager taskManager;

    private MessageReceiveListener discordApi;

    private static class MessageReceiveListener extends ListenerAdapter
    {
        private final DiscordConfig config;
        private final TaskManager taskManager;
        private final I18n i18n;
        private IncomingWebhookClient hookClient;
        private JDA jda;

        public MessageReceiveListener(DiscordConfig config, final TaskManager taskManager, final I18n i18n) {
            this.config = config;
            this.taskManager = taskManager;
            this.i18n = i18n;
        }

        @Override
        public void onReady(final ReadyEvent event) {
            this.hookClient = WebhookClient.createClient(event.getJDA(),config.webhook.id, config.webhook.token);
            this.jda = event.getJDA();
        }

        @Override
        public void onMessageReceived(final MessageReceivedEvent event) {
            if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
                return;
            }
            if (event.getChannel().getName().equals(config.channel)) {
                this.onDiscordChat(event);
            }
        }
        
        public void shutdown()
        {
            if (this.jda != null) {
                this.jda.shutdownNow();
                this.jda = null;
            }
        }

        private void onDiscordChat(MessageReceivedEvent event)
        {
            if (!event.isFromGuild()) {
                return;
            }
            var author = event.getMember();
            if (author == null) {
                return;
            }
            final var message = event.getMessage();
            var content = message.getContentRaw();
            if (config.replaceEmoji)
            {
                content = replace(content, config.forwardEmojiReplacePattern, config.emojiMapping);
            }

            final Component attachmentStrings = message.getAttachments().stream().reduce((Component) Component.empty(), (component, attachment) ->
                            Component.empty()
                                    .append(Component.text("[")
                                            .append(Component.text(attachment.getFileName(), NamedTextColor.DARK_RED))
                                            .append(Component.text("]"))
                                            .clickEvent(openUrl(attachment.getUrl()))
                                            .hoverEvent(Component.text("Open Attachment").asHoverEvent()))
                                    .append(Component.space()),
                    Component::append);

            var mentions = message.getMentions().getMembers().stream().collect(Collectors.toMap(ISnowflake::getId, Discord::mentionAsComponent));
            
            var contentWithEmoji = EMOJI_FROM_DISCORD.matcher(content).replaceAll("$1");
            var contentWithMentions = replaceMentions(contentWithEmoji, mentions);

            final String format = Optional.ofNullable(config.defaultChatFormat).orElse(DEFAULT_CHAT_FORMAT);
            this.taskManager.runTask(() -> broadcastMessage(i18n, format, memberAsComponent(author), contentWithMentions, attachmentStrings));
    
        }

        private void sendMessage(final String message, final String fromUserName, final UUID fromUUID) {
            var emojiLookup = jda.getEmojis().stream().collect(Collectors.toMap(Emoji::getName, ISnowflake::getId));

            String content = replaceWithCallback(EMOJI_FROM_MINECRAFT, message, match -> {
                final String emojiId = emojiLookup.get(match.group(1));
                if (emojiId != null)
                {
                    return "<" + match.group() + emojiId + ">";
                }
                return match.group();
            });

            {
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.addContent(content);
                builder.setAllowedMentions(List.of(Message.MentionType.USER));
                var built = builder.build();
                var url = "https://minotar.net/helm/%s/128.png".formatted(fromUUID.toString());
                var createAction = hookClient.sendMessage(built);
                createAction.setUsername(fromUserName);
                createAction.setAvatarUrl(url);
                createAction.complete();
            }
        }
    }

    private static Component mentionAsComponent(final net.dv8tion.jda.api.entities.Member m) {
        return Component.text("@")
                .append(Component.text(m.getEffectiveName()))
                .color(colorFromMember(m));
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        this.discordApi = new MessageReceiveListener(config, taskManager, i18n);
        JDABuilder.createLight(config.botToken,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGES)
                .setActivity(Activity.watching("You"))
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .addEventListeners(this.discordApi).build();
    }

    public DiscordConfig getConfig()
    {
        return config;
    }

    private static String toPlainString(Component component)
    {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        DiscordData.register(event);
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event)
    {
        this.discordApi.shutdown();
    }

    @Listener(order = Order.LAST)
    public void onMinecraftChat(PlayerChatEvent.Submit event, @Root ServerPlayer player)
    {
        if (player.get(DiscordData.MUTED).orElse(false))
        {
            return;
        }
        // stripLegacy() should be done by a chat plugin, not by us: https://github.com/SpongePowered/SpongeAPI/issues/2259
        final String strippedMessage = stripLegacy(toPlainString(event.originalMessage()));
        if (strippedMessage.isEmpty())
        {
            return;
        }

        taskManager.runTaskAsync(() -> {
            final var playerName = toPlainString(player.displayName().get());
            discordApi.sendMessage(strippedMessage, playerName, player.uniqueId());
        });
    }


    private static Component replaceMentions(String input, Map<String, Component> replacements)
    {
        final Matcher matcher = MENTION_PATTERN.matcher(input);
        int offset = 0;
        List<Component> parts = new ArrayList<>();
        while (matcher.find())
        {
            int start = matcher.start();
            if (offset != start)
            {
                parts.add(Component.text(input.substring(offset, start)));
            }
            final Component mention = replacements.get(matcher.group(1));
            parts.add(Objects.requireNonNullElseGet(mention, () -> Component.text(matcher.group())));
            offset = matcher.end();
        }
        if (offset < input.length())
        {
            parts.add(Component.text(input.substring(offset)));
        }

        return Component.join(JoinConfiguration.noSeparators(), parts);
    }

    private static Component memberAsComponent(Member member)
    {
        return Component.text(member.getEffectiveName(), colorFromMember(member));
    }

    private static TextColor colorFromMember(net.dv8tion.jda.api.entities.Member member)
    {
        var color = member.getColor();
        if (color == null) {
            return NamedTextColor.GRAY;
        }
        return TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void broadcastMessage(I18n i18n, String template, Component name, Component message, Component attachments)
    {
        for (ServerPlayer onlinePlayer : Sponge.server().onlinePlayers())
        {
            if (!onlinePlayer.get(DiscordData.MUTED).orElse(false))
            {
//                Component content = attachments.append(autoLink(message, i18n.translate(onlinePlayer, "Open Link")));
                Component content = attachments.append(message);

                Map<String, Component> replacements = new HashMap<>();
                replacements.put("NAME", name);
                replacements.put("MESSAGE", content);

                onlinePlayer.sendMessage(ComponentUtil.legacyMessageTemplateToComponent(template, replacements));
            }
        }
    }

    private static String replace(String input, Pattern pattern, Map<String, String> replacements)
    {
        return replaceWithCallback(pattern, input, match -> {
            final String s = match.group();
            String replacement = replacements.get(s);
            if (replacement != null)
            {
                return replacement;
            }
            return s;
        });
    }
}
