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
package org.cubeengine.libcube.service.i18n.formatter.component;

import net.kyori.adventure.text.event.HoverEvent;
import org.cubeengine.dirigent.parser.Text;
import org.cubeengine.dirigent.parser.component.Component;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.item.inventory.ItemStack;

public class HoverComponent implements Component
{
    private final HoverEvent<?> hoverEvent;
    private final Component component;

    private HoverComponent(HoverEvent<?> hover, Component component)
    {
        this.hoverEvent = hover;
        this.component = component;
    }

    public HoverEvent<?> getHoverEvent()
    {
        return hoverEvent;
    }

    public Component getComponent()
    {
        return component;
    }

    /*
    public static Component hoverAchievment(Achievement achievement, Component component)
    {
        return new HoverComponent(achievement, component);
    }
    */
    /*
    public static Component hoverAchievment(Achievement achievement, String component)
    {
        return new HoverComponent(achievement, component);
    }
    */

    public static Component hoverItem(ItemStack item, Component component)
    {
        return new HoverComponent(item.createSnapshot().asHoverEvent(), component);
    }
    public static Component hoverItem(ItemStack item, String component)
    {
        return hoverItem(item, new Text(component));
    }

    public static Component hoverEntity(Entity entity, String name, Component component)
    {
        return new HoverComponent(entity.asHoverEvent(), component);
    }

    public static Component hoverEntity(Entity entity, String name, String component)
    {
        return hoverEntity(entity, name, new Text(component));
    }

    public static Component hoverText(net.kyori.adventure.text.Component text, Component component)
    {
        return new HoverComponent(HoverEvent.showText(text), component);
    }

    public static Component hoverText(String text, Component component)
    {
        return hoverText(net.kyori.adventure.text.Component.text(text), component);
    }

    public static Component hoverText(net.kyori.adventure.text.Component text, String component)
    {
        return hoverText(text, new Text(component));
    }

    public static Component hoverText(String text, String component)
    {
        return hoverText(net.kyori.adventure.text.Component.text(text), new Text(component));
    }
}
