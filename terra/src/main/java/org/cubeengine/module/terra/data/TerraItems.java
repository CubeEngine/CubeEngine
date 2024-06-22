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
import org.spongepowered.api.util.Range;
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
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfigs;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.WorldTemplate.Builder;
import org.spongepowered.math.vector.Vector3i;

import static org.spongepowered.api.world.biome.Biomes.*;

public class TerraItems
{

    public static final ItemStack INK_BOTTLE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_INK_BOTTLE = ItemStack.of(ItemTypes.SPLASH_POTION.get());
    public static final ItemStack TERRA_ESSENCE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_TERRA_ESSENCE = ItemStack.of(ItemTypes.SPLASH_POTION.get());
    public static final List<RegistryReference<Biome>> CAVE_BIOMES = Arrays.asList(DRIPSTONE_CAVES, LUSH_CAVES);
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
        GREEN_LANDSCAPE("Green Landscape", Color.ofRgb(0x336633),
                        Arrays.asList(PLAINS, SUNFLOWER_PLAINS, FOREST, BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST, MEADOW, CHERRY_GROVE),
                        CAVE_BIOMES
        ),
        SWAMP("Dark Swamp", Color.ofRgb(0x333333), Arrays.asList(Biomes.SWAMP, MANGROVE_SWAMP)),
        DARK_FOREST("Dark Forest", Color.ofRgb(0x073800), Arrays.asList(Biomes.DARK_FOREST), CAVE_BIOMES),
        JUNGLE("Viney Jungle", Color.ofRgb(0x339933), Arrays.asList(Biomes.JUNGLE, BAMBOO_JUNGLE, SPARSE_JUNGLE)),
        MUSHROOMS("Mushrooms", Color.ofRgb(0x996666), Arrays.asList(MUSHROOM_FIELDS)),
        SAVANNA("Dry Savanna", Color.ofRgb(0x666633), Arrays.asList(Biomes.SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA), CAVE_BIOMES),
        DESERT("Hot Desert", Color.ofRgb(0xCCCC99), Arrays.asList(Biomes.DESERT), List.of(DRIPSTONE_CAVES)),
        MESA("Colorful Badlands", Color.ofRgb(0xCC6633), Arrays.asList(BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS)),
        TAIGA("Chilly Mountains", Color.ofRgb(0x333300), Arrays.asList(Biomes.TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, SNOWY_TAIGA, GROVE, JAGGED_PEAKS, STONY_PEAKS, WINDSWEPT_FOREST, WINDSWEPT_GRAVELLY_HILLS, WINDSWEPT_HILLS), CAVE_BIOMES),

        // Special Biomes
        ICE_SPIKES("Frozen World", Color.ofRgb(0x6699CC), Arrays.asList(Biomes.ICE_SPIKES)),
        DEEP_SEA("Deep Sea", Color.ofRgb(0x131D75), Arrays.asList(OCEAN, DEEP_FROZEN_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN, DEEP_OCEAN)),
        SEA("Sea", Color.ofRgb(0x40ACA4), Arrays.asList(LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN, BEACH)),
        CORAL_REEF("Coral Reef", Color.ofRgb(0xCC66CC), Arrays.asList(WARM_OCEAN)),
        FLOWERY_FOREST("Flowery Forest", Color.ofRgb(0xCC6600), Arrays.asList(FLOWER_FOREST), CAVE_BIOMES),
        ENDLESS_DEPTHS("Endless Depths", Color.ofRgb(0x415F7E), Arrays.asList(DEEP_DARK)),

        CAVEWORLD("Caveworld", Color.ofRgb(0x767272), CAVE_BIOMES),

        END("End Highlands", Color.ofRgb(0x999966), Arrays.asList(END_HIGHLANDS, END_BARRENS, END_MIDLANDS, SMALL_END_ISLANDS, THE_END)),
        NETHER("Hellscape", Color.ofRgb(0x660000), Arrays.asList(NETHER_WASTES, CRIMSON_FOREST, WARPED_FOREST, SOUL_SAND_VALLEY, BASALT_DELTAS)),
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

            final var HALF_RANGE = Range.floatRange(-1f, 1f);
            final var HALF_DEPTH = Range.floatRange(0.2f, 0.2f);

            final List<AttributedBiome> finalBiomes = new ArrayList<>();
            var overworldBiomeDefaults = MultiNoiseBiomeConfig.overworld().attributedBiomes();
            var netherBiomeDefaults = MultiNoiseBiomeConfig.nether().attributedBiomes();
            for (final RegistryReference<Biome> biome : biomeList)
            {
                if (CAVE_BIOMES.contains(biome) && this.additionalBiomeList.contains(biome))
                {
                    final var defaultAttributes = BiomeAttributes.defaultAttributes(biome).get();
                    final var biomeAttributes = BiomeAttributes.range(defaultAttributes.temperature(), // temp
                                                                      defaultAttributes.humidity(), // humidity
                                                                      defaultAttributes.continentalness(), // continentalness
                                                                      Range.floatRange(-1f, 1f), // erosion
                                                                      defaultAttributes.depth(), // depth (0.2-0.9)
                                                                      Range.floatRange(-1f, 1f), // weirdness
                                                                      defaultAttributes.offset());
                    finalBiomes.add(AttributedBiome.of(biome, biomeAttributes));
                }
                else
                {
                    var list = overworldBiomeDefaults.stream().filter(ab -> ab.biome().equals(biome)).toList();
                    for (final AttributedBiome attributedBiome : list)
                    {
                        final var defaultAttributes = attributedBiome.attributes();
                        final var biomeAttributes = BiomeAttributes.range(HALF_RANGE, // temp
                                                                          HALF_RANGE, // humidity
                                                                          defaultAttributes.continentalness(), // continentalness
                                                                          defaultAttributes.erosion(),// erosion
                                                                          defaultAttributes.depth().min() == 0 ? defaultAttributes.depth() : HALF_DEPTH, // depth (0/1)
                                                                          defaultAttributes.weirdness(),// weirdness
                                                                          defaultAttributes.offset());
                        finalBiomes.add(AttributedBiome.of(biome, biomeAttributes));
                    }
                    list = netherBiomeDefaults.stream().filter(ab -> ab.biome().equals(biome)).toList();
                    finalBiomes.addAll(list);
                }
                if (this == END)
                {
                    final BiomeAttributes biomeAttributes = BiomeAttributes.point((float) biome.get(Sponge.server()).temperature(),
                                                                                  (float) biome.get(Sponge.server()).humidity(),
                                                                                  random.nextFloat() * 4 - 2,
                                                                                  random.nextFloat() * 4 - 2,
                                                                                  random.nextFloat() * 4 - 2,
                                                                                  random.nextFloat() / 5,
                                                                                  0f);
                    finalBiomes.add(AttributedBiome.of(biome, biomeAttributes));
                }
            }


            final MultiNoiseBiomeConfig multiNoiseBiomeConfig = MultiNoiseBiomeConfig.builder().addBiomes(finalBiomes).build();
            final NoiseGeneratorConfig noiseGeneratorConfig;
            if (this == NETHER)
            {
                noiseGeneratorConfig = NoiseGeneratorConfigs.NETHER.get();
                templateBuilder.add(Keys.WORLD_TYPE, WorldTypes.THE_NETHER.get());
            }
            else if (this == END)
            {
                noiseGeneratorConfig = NoiseGeneratorConfig.builder().fromValue(NoiseGeneratorConfigs.FLOATING_ISLANDS.get())
                                                           .surfaceRule(SurfaceRule.end())
                                                           .defaultBlock(BlockTypes.END_STONE.get().defaultState())
                                                           .key(ResourceKey.of(PluginTerra.TERRA_ID, "end"))
                                                           .build().config();

                templateBuilder.add(Keys.WORLD_TYPE, RegistryTypes.WORLD_TYPE.get().findValue(Terra.WORLD_TYPE_END).get());
            }
            else if (this == CAVEWORLD)
            {

                templateBuilder.add(Keys.SPAWN_POSITION, Vector3i.from(0, 64, 0));
                final var caves = NoiseGeneratorConfigs.CAVES.get();
                noiseGeneratorConfig = caves;
            }
            else
            {
                noiseGeneratorConfig = NoiseGeneratorConfigs.OVERWORLD.get();
            }
            templateBuilder.add(Keys.SERIALIZATION_BEHAVIOR, SerializationBehavior.NONE);
            templateBuilder.add(Keys.DISPLAY_NAME, Component.text("Dream world by " + player.name()));
            templateBuilder.add(Keys.CHUNK_GENERATOR, ChunkGenerator.noise(BiomeProvider.multiNoise(multiNoiseBiomeConfig), noiseGeneratorConfig));
            templateBuilder.add(Keys.SEED, random.nextLong());
            templateBuilder.add(Keys.WORLD_DIFFICULTY, Difficulties.HARD.get());
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
