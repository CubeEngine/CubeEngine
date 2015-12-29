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
package org.cubeengine.module.vigil.report;

import java.util.*;
import java.util.function.Function;

import org.cubeengine.module.core.util.converter.ItemStackConverter;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.report.block.BlockReport.*;

public class ReportUtil
{
    public static Text name(BlockSnapshot snapshot)
    {
        BlockType type = snapshot.getState().getType();
        Translation trans = type.getTranslation();
        if (snapshot.getState().getType().getItem().isPresent())
        {
            trans = ItemStack.builder().fromBlockSnapshot(snapshot).build().getTranslation();
        }
        // TODO sign lines
        return Texts.of(TextColors.GOLD, trans).builder()
                .onHover(TextActions.showText(Texts.of(type.getName()))).build();
    }

    public static <LT, T> boolean containsSingle(List<LT> list, Function<LT, T> func)
    {
        T onlyType = null;
        for (LT elem : list)
        {
            T type = func.apply(elem);
            if (onlyType == null || onlyType.equals(type))
            {
                onlyType = type;
            }
            else
            {
                return false;
            }
        }
        return true;
    }
}
