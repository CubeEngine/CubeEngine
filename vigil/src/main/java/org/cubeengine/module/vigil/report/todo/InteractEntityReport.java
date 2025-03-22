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
package org.cubeengine.module.vigil.report.todo;

import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

/* TODO
entity
-dye
-fill soup
-shear
-milk

vehicle
-enter
-exit
-use-furnace-minecart

damage
 */
public abstract class InteractEntityReport<T extends InteractEntityEvent> extends BaseReport<T> implements Report.Readonly
{
    @Override
    public ItemStack getIcon(final I18n i18n, final Audience audience) {
        final var icon = ItemStack.of(ItemTypes.CHEST);
        var tr = i18n.translate(audience, "Inventory Changes");
        icon.offer(Keys.CUSTOM_NAME, tr);
        // TODO
        return icon;
    }

}
