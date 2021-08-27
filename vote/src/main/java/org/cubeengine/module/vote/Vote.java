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
package org.cubeengine.module.vote;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vexsoftware.votifier.sponge8.event.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.libcube.util.ComponentUtil.clickableLink;
import static org.cubeengine.libcube.util.ComponentUtil.legacyMessageTemplateToComponent;

/**
 * A module to handle Votes coming from a {@link VotifierEvent}
 */
@Singleton
@Module(dependencies = @Dependency("nuvotifier"))
public class Vote
{
    @Inject private Broadcaster bc;
    @Inject private Logger logger;

    @ModuleConfig private VoteConfiguration config;
    @ModuleCommand private VoteCommands commands;

    @Listener
    public void onEnable(RegisterDataEvent event) {
        VoteData.register(event);
    }

    @Listener
    public void onVote(VotifierEvent event)
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        final String username = event.getVote().getUsername();
        final Optional<ServerPlayer> player = Sponge.server().player(username);
        final User user = Sponge.server().userManager().load(username).join().orElse(null);
        final DataHolder.Mutable dh = player.map(DataHolder.Mutable.class::cast).orElse(user);
        if (dh == null)
        {
            logger.info("{} voted but is not known to the server!", username);
            return;
        }

        final long lastVote = dh.get(VoteData.LAST_VOTE).orElse(0L);
        final long millisSinceLastVote = System.currentTimeMillis() - lastVote;
        if (millisSinceLastVote < config.voteCooldownTime.toMillis()) {
            return;
        }


        final int count = dh.get(VoteData.COUNT).orElse(0) + 1;
        dh.offer(VoteData.COUNT, count);
        dh.offer(VoteData.LAST_VOTE, System.currentTimeMillis());

        final int streak;
        final boolean isStreakVote = millisSinceLastVote < config.streakTimeout.toMillis();
        if (isStreakVote) {
            streak = dh.get(VoteData.STREAK).orElse(0) + 1;
        } else {
            streak = 1;
        }
        dh.offer(VoteData.STREAK, streak);

        final int countToStreakReward = this.config.streak - (streak % this.config.streak);

        final ItemStack reward;
        if (isStreakVote && streak % this.config.streak == 0)
        {
            reward = ItemStack.of(this.config.streakVoteReward);
            renameItemStack(reward, this.config.streakVoteRewardName);
        }
        else
        {
            reward = ItemStack.of(this.config.singleVoteReward);
            renameItemStack(reward, this.config.singleVoteRewardName);
        }

        Sponge.server().sendMessage(voteMessage(this.config.voteBroadcast, username, count, streak, countToStreakReward, reward));
        player.ifPresent(p -> {
            p.sendMessage(voteMessage(this.config.singleVoteMessage, username, count, streak, countToStreakReward, reward));
        });

        if (player.isPresent())
        {
            player.get().inventory().offer(reward);
        }
        else
        {
            user.inventory().offer(reward);
        }
    }

    public static void renameItemStack(ItemStack stack, String name) {
        if (name != null) {
            stack.offer(Keys.CUSTOM_NAME, PlainComponentSerializer.plain().deserialize(name));
        }
    }

    public Component voteMessage(String template, String username, int count, int streak, int toStreak, ItemStack reward)
    {
        Map<String, Component> replacements = new HashMap<>();
        replacements.put("PLAYER", Component.text(username));
        replacements.put("COUNT", Component.text(String.valueOf(count)));
        replacements.put("STREAK", Component.text(String.valueOf(streak)));
        replacements.put("VOTEURL", clickableLink(this.config.voteUrlLabel, this.config.voteUrl));
        replacements.put("TOSTREAK", Component.text(String.valueOf(toStreak)));
        replacements.put("REWARD", reward.get(Keys.DISPLAY_NAME).orElseThrow(() -> new IllegalArgumentException("ItemStack should always have a display name!")));

        return legacyMessageTemplateToComponent(template, replacements);
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
