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
package org.cubeengine.module.headvillager;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.ProfessionTypes;
import org.spongepowered.api.data.type.VillagerTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.trader.Villager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.math.vector.Vector3d;

/**
 * A module to buy heads
 */
@Singleton
@Module
public class HeadVillager
{
    private static final String mcHeadUrl = "https://minecraft-heads.com/scripts/api.php?tags=true&cat=";
    @Inject Logger logger;
    @Inject private FileManager fm;


    @Listener
    public void onStartUp(StartedEngineEvent<Server> event)
    {
        final Gson gson = new Gson();

        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();

        try
        {
            final Path cachePath = fm.getModulePath(HeadVillager.class).resolve("headcache");
            Files.createDirectories(cachePath);

            for (Category category : Category.values())
            {

                try
                {
                    Path categoryFile = cachePath.resolve(category.name() + ".json");
                    if (!Files.exists(categoryFile))
                    {
                        logger.info("Fetching {} heads from {} ...", category, mcHeadUrl + category);
                        final HttpRequest request = HttpRequest.newBuilder(new URI(mcHeadUrl + category)).timeout(Duration.ofMillis(2000)).build();
                        final HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                        final String body = response.body();
                        Files.writeString(categoryFile, body);
                        logger.info("done Fetching {} heads...", category);
                    }
                }
                catch (IOException | URISyntaxException | InterruptedException e)
                {
                    logger.warn("Could not get heads for {}", category.name(), e);
                }
            }

            int catCount = 0;
            int headCount = 0;
            try (var dirStream = Files.newDirectoryStream(cachePath, "*.json"))
            {
                for (final Path file : dirStream)
                {
                    catCount++;
                    for (Head head : gson.fromJson(new FileReader(file.toFile()), Head[].class))
                    {
                        try
                        {
                            headCount++;
                            head.init(Category.valueOf(StringUtils.stripFileExtension(file.getFileName().toString())));
                        }
                        catch (Exception e)
                        {
                            logger.warn("Invalid Head: " + head, e);
                        }
                    }
                }
            }
            logger.info("Successfully loaded {} heads in {} categories", headCount, catCount);
        }
        catch (IOException ignored)
        {
            logger.warn("Could not cache Heads Data");
        }
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        HeadVillagerData.register(event);
    }

    @Listener
    public void onRegisterRecipe(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        HeadVillagerItems.register(event);
    }

    @SuppressWarnings("unchecked")
    @Listener
    public void onRightClickBlock(InteractEntityEvent.Secondary.On event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()) || !(event.entity() instanceof Villager))
        {
            return;
        }
        final Entity villager = event.entity();
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);

        if (!villager.get(HeadVillagerData.VILLAGER).isPresent())
        {
            if (itemInHand.get(HeadVillagerData.VILLAGER).isPresent())
            {
                if (!player.gameMode().get().equals(GameModes.CREATIVE.get()))
                {
                    itemInHand.setQuantity(itemInHand.quantity() - 1);
                    player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
                }
                villager.offer(HeadVillagerData.VILLAGER, "bob");
                villager.world().playSound(Sound.sound(SoundTypes.ENTITY_VILLAGER_WORK_CLERIC, Source.NEUTRAL, 3, 2f), villager.position());
                final ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.SMOKE).offset(new Vector3d(0.2, 1, 0.2)).quantity(50).build();
                villager.world().spawnParticles(effect, villager.position());
                villager.offer(Keys.PROFESSION_TYPE, ProfessionTypes.NITWIT.get());
                villager.offer(Keys.VILLAGER_TYPE, VillagerTypes.SNOW.get());
                villager.offer(Keys.PROFESSION_LEVEL, 6);
                final List<String> names = Arrays.asList("Bob", "Steve", "Jeff", "Phil", "Pete", "Kevin", "Tim", "Bill", "Manfred", "Todd", "Laurel", "Karen", "Bertha");
                final String name = names.get(villager.world().random().nextInt(names.size()));
                villager.offer(Keys.CUSTOM_NAME, Component.text(name + " - Master Headsman", NamedTextColor.DARK_RED));
                villager.offer(Keys.MAX_HEALTH, 100d);
                villager.offer(Keys.HEALTH, 100d);
                event.setCancelled(true);
            }
            return;
        }

        if (itemInHand.type().isAnyOf(ItemTypes.PLAYER_HEAD))
        {
            final List<TradeOffer> tradeOffers = Arrays.asList(TradeOffer.builder().canGrantExperience(false).maxUses(2)
                                                                  .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK))
                                                                  .sellingItem(itemInHand).build());
            villager.offer(Keys.TRADE_OFFERS, tradeOffers);
            return;
        }
        if (!itemInHand.type().isAnyOf(ItemTypes.WRITABLE_BOOK))
        {
            return;
        }
        final List<String> list = itemInHand.get(Keys.PLAIN_PAGES).get();
        final String firstPage = list.get(0);
        final String[] wishList = firstPage.split("\n");
        final List<Head> headStacks = new ArrayList<>();
        for (Category category : Category.values())
        {
            for (String headName : category.heads.keySet())
            {
                for (Head head : category.heads.get(headName))
                {
                    boolean match = true;
                    for (String wishListItem : wishList)
                    {
                        if (!(headName.toLowerCase().contains(wishListItem.toLowerCase()) || (head.tags != null && head.tags.toLowerCase().contains(wishListItem.toLowerCase()))))
                        {
                            match = false;
                            break;
                        }
                    }
                    if (match)
                    {
                        headStacks.add(head);
                    }
                }
            }
        }
        Collections.shuffle(headStacks);
        final List<TradeOffer> tradeOffers = headStacks.subList(0, Math.min(headStacks.size(), 20)).stream().sorted(
            Comparator.comparing(h -> h.name)).map(head -> TradeOffer.builder().canGrantExperience(false).maxUses(2)
                                                          .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK))
                                                          .sellingItem(head.stack).build()).collect(Collectors.toList());
        if (wishList[0].equals(player.name()))
        {
            final ItemStack selfHead = ItemStack.of(ItemTypes.PLAYER_HEAD);
            selfHead.offer(Keys.GAME_PROFILE, player.profile());
            tradeOffers.add(TradeOffer.builder().canGrantExperience(false).maxUses(2)
                      .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK))
                      .sellingItem(selfHead).build());
        }
        villager.offer(Keys.TRADE_OFFERS, tradeOffers);
    }

}
