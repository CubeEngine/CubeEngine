/**
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
package de.cubeisland.engine.module.basics.command.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.butler.parameter.FixedValues;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.core.util.matcher.EnchantMatcher;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.readers.EnchantmentReader;
import de.cubeisland.engine.module.service.command.result.paginated.PaginatedResult;
import de.cubeisland.engine.module.service.paginate.PaginatedResult;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import org.bukkit.enchantments.Enchantment;
import org.spongepowered.api.item.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.SkullData;
import org.spongepowered.api.data.manipulator.entity.SkinData;
import org.spongepowered.api.data.manipulator.item.DurabilityData;
import org.spongepowered.api.data.manipulator.item.LoreData;
import org.spongepowered.api.data.property.UseLimitProperty;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.item.inventory.entity.HumanInventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Literal;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.service.command.readers.EnchantmentReader.getPossibleEnchantments;
import static org.bukkit.Material.AIR;
import static org.bukkit.Material.SKULL_ITEM;
import static org.spongepowered.api.item.ItemTypes.SKULL;
import static org.spongepowered.api.item.inventory.ItemStackComparators.ITEM_DATA;
import static org.spongepowered.api.item.inventory.ItemStackComparators.TYPE;

/**
 * item-related commands
 * <p>/itemdb
 * <p>/rename
 * <p>/headchange
 * <p>/unlimited
 * <p>/enchant
 * <p>/give
 * <p>/item
 * <p>/more
 * <p>/repair
 * <p>/stack
 */
public class ItemCommands
{
    private final Basics module;
    private MaterialMatcher materialMatcher;
    private EnchantMatcher enchantMatcher;
    private Game game;

    public ItemCommands(Basics module, MaterialMatcher materialMatcher, EnchantMatcher enchantMatcher, Game game)
    {
        this.module = module;
        this.materialMatcher = materialMatcher;
        this.enchantMatcher = enchantMatcher;
        this.game = game;
    }

    @Command(desc = "Looks up an item for you!")
    @SuppressWarnings("deprecation")
    public PaginatedResult itemDB(CommandContext context, @Optional String item)
    {
        if (item != null)
        {
            List<ItemStack> itemList = materialMatcher.itemStackList(item);
            if (itemList == null || itemList.size() <= 0)
            {
                context.sendTranslated(NEGATIVE, "Could not find any item named {input}!", item);
                return null;
            }
            List<Text> lines = new ArrayList<>();
            ItemStack key = itemList.get(0);
            lines.add(Texts.of(context.getSource().getTranslation(POSITIVE,
                                                                   "Best Matched {input#item} ({integer#id} for {input}",
                                                                   materialMatcher.getNameFor(key),
                                                                   key.getItem().getId(), item)));
            itemList.remove(0);
            for (ItemStack stack : itemList) {
                lines.add(Texts.of(context.getSource().getTranslation(POSITIVE,
                                                                      "Matched {input#item} ({integer#id} for {input}",
                                                                      materialMatcher.getNameFor(stack),
                                                                      stack.getItem().getId(), item)));
            }
            return new PaginatedResult(context, lines);
        }
        if (!context.isSource(User.class))
        {
            throw new TooFewArgumentsException();
        }
        User sender = (User)context.getSource();
        if (!sender.getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You hold nothing in your hands!");
            return null;
        }
        ItemStack aItem = sender.getItemInHand().get();
        String found = materialMatcher.getNameFor(aItem);
        if (found == null)
        {
            context.sendTranslated(NEGATIVE, "Itemname unknown! Itemdata: {integer#id}",
                                   aItem.getItem().getId());
            return null;
        }
        context.sendTranslated(POSITIVE, "The Item in your hand is: {input#item} ({integer#id})",
                               found, aItem.getItem().getId());
        return null;
    }


    public enum OnOff implements FixedValues
    {
        ON(true), OFF(false);
        public final boolean value;

        OnOff(boolean value)
        {
            this.value = value;
        }

        @Override
        public String getName()
        {
            return this.name().toLowerCase();
        }
    }

    @Command(desc = "Grants unlimited items")
    @Restricted(User.class)
    public void unlimited(User context, @Optional OnOff unlimited)
    {
        boolean setTo = unlimited != null ? unlimited.value : !context.get(BasicsAttachment.class).hasUnlimitedItems();
        if (setTo)
        {
            context.sendTranslated(POSITIVE, "You now have unlimited items to build!");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "You no longer have unlimited items to build!");
        }
        context.get(BasicsAttachment.class).setUnlimitedItems(setTo);
    }

}
