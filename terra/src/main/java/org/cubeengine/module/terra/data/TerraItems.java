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
package org.cubeengine.module.terra.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.cubeengine.module.terra.PluginTerra;
import org.cubeengine.module.terra.Terra;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.RecipeTypes;
import org.spongepowered.api.item.recipe.cooking.CookingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.RandomProvider.Source;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.biome.AttributedBiome;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.biome.BiomeAttributes;
import org.spongepowered.api.world.biome.Biomes;
import org.spongepowered.api.world.biome.provider.BiomeProvider;
import org.spongepowered.api.world.biome.provider.MultiNoiseBiomeConfig;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.config.SurfaceRule;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfigs;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.WorldTemplate.Builder;

import static org.spongepowered.api.world.biome.Biomes.*;

public class TerraItems
{

    public static final ItemStack INK_BOTTLE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_INK_BOTTLE = ItemStack.of(ItemTypes.SPLASH_POTION.get());
    public static final ItemStack TERRA_ESSENCE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_TERRA_ESSENCE = ItemStack.of(ItemTypes.SPLASH_POTION.get());
    private static Terra terra;

    public static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration> event, Terra terra)
    {
        TerraItems.terra = terra;

        INK_BOTTLE.offer(Keys.COLOR, Color.BLACK);
        INK_BOTTLE.offer(Keys.CUSTOM_NAME, Component.text("Ink Bottle"));
        INK_BOTTLE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration inkBottleRecipe = ShapelessCraftingRecipe.builder()
                              .addIngredients(ItemTypes.GLASS_BOTTLE, ItemTypes.INK_SAC)
                              .result(INK_BOTTLE)
                              .key(ResourceKey.of(PluginTerra.TERRA_ID, "inkbottle"))
                              .build();
        event.register(inkBottleRecipe);

        SPLASH_INK_BOTTLE.offer(Keys.COLOR, Color.BLACK);
        SPLASH_INK_BOTTLE.offer(Keys.CUSTOM_NAME, Component.text("Splash Ink Bottle"));
        SPLASH_INK_BOTTLE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration splashInkBottleRecipe = ShapelessCraftingRecipe.builder()
                              .addIngredients(ItemTypes.GLASS_BOTTLE, ItemTypes.INK_SAC, ItemTypes.GUNPOWDER)
                              .result(SPLASH_INK_BOTTLE)
                              .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_inkbottle"))
                              .build();
        event.register(splashInkBottleRecipe);

        TERRA_ESSENCE.offer(Keys.COLOR, Color.WHITE);
        TERRA_ESSENCE.offer(Keys.CUSTOM_NAME, Component.text("Terra Essence"));
        TERRA_ESSENCE.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.SATURATION.get(), 0, Ticks.of(20))));
        TERRA_ESSENCE.offer(Keys.HIDE_MISCELLANEOUS, true);
        TERRA_ESSENCE.offer(TerraData.TERRA_POTION, true);
        final RecipeRegistration terraEssenceRecipe = ShapelessCraftingRecipe.builder()
                               .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL)
                               .addIngredients(Ingredient.of(INK_BOTTLE))
                               .result(grid -> TerraItems.getCraftedEssence(), TERRA_ESSENCE)
                               .key(ResourceKey.of(PluginTerra.TERRA_ID, "terraessence"))
                               .build();
        event.register(terraEssenceRecipe);

        final RecipeRegistration randomTerraEssence = ShapelessCraftingRecipe.builder()
             .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL, ItemTypes.NETHER_STAR)
             .addIngredients(Ingredient.of(INK_BOTTLE))
             .result(grid -> TerraItems.getRandomCraftedEssence(TERRA_ESSENCE), TERRA_ESSENCE)
             .key(ResourceKey.of(PluginTerra.TERRA_ID, "random_terraessence"))
             .build();
        event.register(randomTerraEssence);

        SPLASH_TERRA_ESSENCE.offer(Keys.COLOR, Color.WHITE);
        SPLASH_TERRA_ESSENCE.offer(Keys.CUSTOM_NAME, Component.text("Splash Terra Essence"));
        SPLASH_TERRA_ESSENCE.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.SATURATION.get(), 0, Ticks.of(20))));
        SPLASH_TERRA_ESSENCE.offer(Keys.HIDE_MISCELLANEOUS, true);
        SPLASH_TERRA_ESSENCE.offer(TerraData.TERRA_POTION, true);
        final RecipeRegistration splashEssence = ShapelessCraftingRecipe.builder()
                                 .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL)
                                 .addIngredients(Ingredient.of(SPLASH_INK_BOTTLE))
                                 .result(grid -> TerraItems.getRandomCraftedEssence(SPLASH_TERRA_ESSENCE), TERRA_ESSENCE)
                                 .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_terraessence"))
                                 .build();
        event.register(splashEssence);

        final RecipeRegistration splashRandomTerraEssence = ShapelessCraftingRecipe.builder()
                                     .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL, ItemTypes.NETHER_STAR)
                                     .addIngredients(Ingredient.of(INK_BOTTLE))
                                     .result(grid -> TerraItems.getRandomCraftedEssence(SPLASH_TERRA_ESSENCE), TERRA_ESSENCE)
                                     .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_random_terraessence"))
                                     .build();
        event.register(splashRandomTerraEssence);

        final Ingredient coldPotionIngredient = Ingredient.of(ResourceKey.of(PluginTerra.TERRA_ID, "cold_potion"), stack -> isTerraEssence(stack.createSnapshot()), ItemStack.of(ItemTypes.POTION));
        final RecipeRegistration heatUpPotion = CookingRecipe.builder().type(RecipeTypes.CAMPFIRE_COOKING)
                                                     .ingredient(coldPotionIngredient)
                                                     .result(i -> TerraItems.heatedPotion(i), ItemStack.of(ItemTypes.POTION))
                                                     .cookingTime(Ticks.of(20)).experience(0)
                                                     .key(ResourceKey.of(PluginTerra.TERRA_ID, "heatup-potion"))
                                                     .build();
        event.register(heatUpPotion);
    }

    private static ItemStack heatedPotion(Inventory campFire)
    {
        final ItemStack original = campFire.peek();
        if (original.get(TerraData.WORLD_KEY).isPresent())
        {
            return terra.getListener().finalizePotion(original.copy());
        }
        return original;
    }

    public enum Essence
    {
        GREEN_LANDSCAPE("Green Landscape", Color.ofRgb(0x336633), Arrays.asList(PLAINS, SUNFLOWER_PLAINS, FOREST, BIRCH_FOREST, FLOWER_FOREST)),
        SWAMP_FOREST("Dark Swamp", Color.ofRgb(0x333333), Arrays.asList(DARK_FOREST, SWAMP)),
        JUNGLE("Viney Jungle", Color.ofRgb(0x339933), Arrays.asList(Biomes.JUNGLE, BAMBOO_JUNGLE)),
        MUSHROOMS("Mushrooms", Color.ofRgb(0x996666), Arrays.asList(MUSHROOM_FIELDS), Arrays.asList(COLD_OCEAN)),
        SAVANNA("Dry Savanna", Color.ofRgb(0x666633), Arrays.asList(Biomes.SAVANNA, SAVANNA_PLATEAU)),
        DESERT("Hot Desert", Color.ofRgb(0xCCCC99), Arrays.asList(Biomes.DESERT)),
        MESA("Colorful Badlands", Color.ofRgb(0xCC6633), Arrays.asList(BADLANDS, ERODED_BADLANDS)),
        TAIGA("Chilly Mountains", Color.ofRgb(0x333300), Arrays.asList(Biomes.TAIGA, SNOWY_TAIGA), Arrays.asList(SNOWY_BEACH, COLD_OCEAN)),
        // Special Biomes
        ICE_SPIKES("Frozen World", Color.ofRgb(0x6699CC), Arrays.asList(Biomes.ICE_SPIKES)),
        CORAL_REEF("Coral Reef", Color.ofRgb(0xCC66CC), Arrays.asList(WARM_OCEAN)),
        FLOWERY_FOREST("Flowery Forest", Color.ofRgb(0xCC6600), Arrays.asList(FLOWER_FOREST)),
        // Needs special casing
        END("End Highlands", Color.ofRgb(0x999966), Arrays.asList(END_HIGHLANDS, END_BARRENS, END_MIDLANDS, SMALL_END_ISLANDS, THE_END)),
        NETHER("Hellscape", Color.ofRgb(0x330000), Arrays.asList(NETHER_WASTES, CRIMSON_FOREST, WARPED_FOREST, SOUL_SAND_VALLEY, BASALT_DELTAS)),
        ;


        private final Color color;
        private final List<RegistryReference<Biome>> biomeList;
        private final List<RegistryReference<Biome>> additionalBiomeList;
        private final String name;

        Essence(String name, Color color, List<RegistryReference<Biome>> biomeList, List<RegistryReference<Biome>> additionalBiomeList)
        {
            this.name = name;
            this.color = color;
            this.biomeList = biomeList;
            this.additionalBiomeList = additionalBiomeList;
        }
        
        Essence(String name, Color color, List<RegistryReference<Biome>> biomeList)
        {
            this(name, color, biomeList, Collections.emptyList());
        }

        public List<RegistryReference<Biome>> getBiomes()
        {
            final List<RegistryReference<Biome>> allBiomes = new ArrayList<>();
            allBiomes.addAll(this.biomeList);
            allBiomes.addAll(this.additionalBiomeList);
            return allBiomes;
        }

        public boolean hasBiome(Biome Biome, ServerWorld world)
        {
            for (RegistryReference<Biome> biome : biomeList)
            {
                if (biome.get(world).equals(Biome))
                {
                    return true;
                }
            }
            return false;
        }

        public WorldTemplate createWorldTemplate(ServerPlayer player, ResourceKey worldKey)
        {
            final Builder templateBuilder = WorldTemplate.builder().from(WorldTemplate.overworld()).key(worldKey);

            final List<RegistryReference<Biome>> biomeList = getBiomes();
            if (this == END) // customize end biomes
            {
                biomeList.clear();
                biomeList.add(END_HIGHLANDS);
            }

            final Source random = player.world().random();

            final List<AttributedBiome> biomes = biomeList.stream().map(biome -> {
                final Biome originalBiome = biome.get(player.world());
                final BiomeAttributes biomeAttributes = BiomeAttributes.point((float) originalBiome.temperature(),
                                                                           (float) originalBiome.humidity(),
                                                                           random.nextFloat() * 4 - 2,
                                                                           random.nextFloat() * 4 - 2,
                                                                           random.nextFloat() * 4 - 2,
                                                                           random.nextFloat() / 5,
                                                                           0f);
                return AttributedBiome.of(biome, biomeAttributes);
            }).collect(Collectors.toList());

            final MultiNoiseBiomeConfig multiNoiseBiomeConfig = MultiNoiseBiomeConfig.builder().addBiomes(biomes).build();
            final NoiseGeneratorConfig noiseGeneratorConfig;
            if (this == NETHER)
            {
                noiseGeneratorConfig = NoiseGeneratorConfigs.NETHER.get();
                templateBuilder.add(Keys.WORLD_TYPE, WorldTypes.THE_NETHER.get());
            }
            else if (this == END)
            {
                // TODO structureConfig
//                final StructureGenerationConfig endStructures = StructureGenerationConfig.builder().addStructure(Structures.ENDCITY.get(), SeparatedStructureConfig.of(6, 4, random.nextInt())).build();
                noiseGeneratorConfig = NoiseGeneratorConfig.builder().fromValue(NoiseGeneratorConfigs.FLOATING_ISLANDS.get())
                                                           .surfaceRule(SurfaceRule.end())
                                                           .defaultBlock(BlockTypes.END_STONE.get().defaultState())
//                                                           .structureConfig(endStructures)
                                                           .build().config();

                templateBuilder.add(Keys.WORLD_TYPE, RegistryTypes.WORLD_TYPE.get().findValue(Terra.WORLD_TYPE_END).get());
            }
            else
            {
                noiseGeneratorConfig = NoiseGeneratorConfigs.OVERWORLD.get();
            }
            templateBuilder.add(Keys.SERIALIZATION_BEHAVIOR, SerializationBehavior.NONE);
            templateBuilder.add(Keys.DISPLAY_NAME, Component.text("Dream world by " + player.name()));
            templateBuilder.add(Keys.CHUNK_GENERATOR, ChunkGenerator.noise(BiomeProvider.multiNoise(multiNoiseBiomeConfig), noiseGeneratorConfig));
            templateBuilder.add(Keys.SEED, random.nextLong()); // TODO check if this works
            templateBuilder.add(Keys.WORLD_DIFFICULTY, Difficulties.HARD.get());
            templateBuilder.add(Keys.WORLD_GEN_CONFIG, WorldGenerationConfig.builder().seed(random.nextLong()).build()); // TODO or this is where the seed is set
            templateBuilder.add(Keys.IS_LOAD_ON_STARTUP, false);
            return templateBuilder.build();
        }
    }

    private static Random random = new Random();

    private static ItemStack getRandomCraftedEssence(ItemStack baseStack)
    {
        final Optional<ServerPlayer> player = Sponge.server().causeStackManager().currentCause().first(ServerPlayer.class);
        final Essence essence = Essence.values()[random.nextInt(Essence.values().length)];
        final ItemStack craftedEssence = baseStack.copy();
        craftedEssence.offer(Keys.COLOR, essence.color);
        craftedEssence.offer(Keys.CUSTOM_NAME, baseStack.get(Keys.CUSTOM_NAME).get().append(Component.space()).append(Component.text(essence.name, TextColor.color(essence.color.rgb()))));
        craftedEssence.offer(Keys.LORE, Arrays.asList(terra.getListener().coldPotionLore(player.get()),
                                                      terra.getListener().hintPotionLore(player.get())));
        return craftedEssence;
    }

    public static ItemStack getEssence(Essence essence, Audience audience)
    {
        final ItemStack newEssence = TerraItems.TERRA_ESSENCE.copy();
        newEssence.offer(Keys.COLOR, essence.color);
        newEssence.offer(Keys.CUSTOM_NAME, newEssence.get(Keys.CUSTOM_NAME).get().append(Component.space()).append(Component.text(essence.name, TextColor.color(essence.color.rgb()))));
        newEssence.offer(Keys.LORE, Arrays.asList(terra.getListener().coldPotionLore(audience),
                                                  terra.getListener().hintPotionLore(audience)));
        return newEssence;
    }

    private static ItemStack getCraftedEssence()
    {
        final ItemStack craftedEssence = TerraItems.TERRA_ESSENCE.copy();
        final Optional<ServerPlayer> player = Sponge.server().causeStackManager().currentCause().first(ServerPlayer.class);
        final Optional<Biome> biome = player.map(p -> p.world().biome(p.blockPosition()));
        if (biome.isPresent())
        {
            Essence essence = Essence.GREEN_LANDSCAPE;
            for (Essence value : Essence.values())
            {
                if (value.hasBiome(biome.get(), player.get().world()))
                {
                    essence = value;
                    break;
                }
            }
            craftedEssence.offer(Keys.COLOR, essence.color);
            craftedEssence.offer(Keys.CUSTOM_NAME, craftedEssence.get(Keys.CUSTOM_NAME).get().append(Component.space()).append(Component.text(essence.name, TextColor.color(essence.color.rgb()))));
            craftedEssence.offer(Keys.LORE, Arrays.asList(terra.getListener().coldPotionLore(player.get()),
                                                          terra.getListener().hintPotionLore(player.get())));

        }
        return craftedEssence;
    }

    public static boolean isTerraEssence(ItemStackSnapshot stack)
    {
        if (!stack.type().isAnyOf(ItemTypes.POTION))
        {
            return false;
        }
        return stack.get(Keys.COLOR).isPresent() && stack.get(TerraData.TERRA_POTION).isPresent();
    }

    public static Optional<Essence> getEssenceForItem(ItemStackSnapshot stack)
    {
        final Color color = stack.get(Keys.COLOR).orElse(Color.BLACK);
        for (Essence value : Essence.values())
        {
            if (value.color.equals(color))
            {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
