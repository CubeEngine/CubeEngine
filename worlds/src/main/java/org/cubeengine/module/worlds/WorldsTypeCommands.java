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
package org.cubeengine.module.worlds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.TimeMatcher;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.WorldType;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "type", desc = "World types commands")
public class WorldsTypeCommands extends DispatcherCommand
{
    private final I18n i18n;
    private final TimeMatcher tm;

    @Inject
    public WorldsTypeCommands(I18n i18n, TimeMatcher tm)
    {
        this.i18n = i18n;
        this.tm = tm;
    }

    @Command(desc = "Show info about a world type")
    public void info(CommandCause context, WorldType type)
    {
        i18n.send(context, POSITIVE, "World type {input}", type.key(RegistryTypes.WORLD_TYPE).asString());

        if (type.scorching())
        {
            i18n.send(context, NEUTRAL, "- scorching: behaves like the nether (increased lava flow, evaporating water, etc...)");
        }
        else
        {
            i18n.send(context, NEUTRAL, "- not scorching: behaves like the overworld");
        }
        if (type.natural())
        {
            i18n.send(context, NEUTRAL, "- natural: allows sleeping in beds etc.");
        }
        else
        {
            i18n.send(context, NEUTRAL, "- not natural: denies sleeping in beds etc.");
        }
        if (type.coordinateMultiplier() != 1)
        {
            i18n.send(context, NEUTRAL, "- coordinate multiplier: {decimal}", type.coordinateMultiplier());
        }
        if (type.hasSkylight())
        {
            i18n.send(context, NEUTRAL, "- has skylight");
        }
        if (type.hasCeiling())
        {
            i18n.send(context, NEUTRAL, "- has ceiling");
        }
        i18n.send(context, NEUTRAL, "- ambient light: {decimal}", type.ambientLighting());
        type.fixedTime().ifPresent(fixed -> {
            final long ticks = fixed.asTicks().ticks();
            i18n.send(context, NEUTRAL, "- fixed time: {input#time} {input#neartime}",
                      tm.format(ticks), tm.matchTimeName(ticks));
        });
        // TODO light spawn settings
        type.spawnLightRange();
        type.spawnLightLimit();
        // monster_spawn_light_level: Value between 0 and 15 (both inclusive). Maximum light required when the monster spawns. The formula of this light is: max( skyLight - 10, blockLight ) during thunderstorms, and max( internalSkyLight, blockLight ) during other weather.
        // monster_spawn_block_light_limit: Value between 0 and 15 (both inclusive). Maximum block light required when the monster spawns.
        if (type.piglinSafe())
        {
            i18n.send(context, NEUTRAL, "- piglin and hoglin are safe");
        }
        else
        {
            i18n.send(context, NEUTRAL, "- piglin and hoglin get zombified");
        }
        if (!type.bedsUsable())
        {
            i18n.send(context, NEUTRAL, "- beds explode on use");
        }
        if (!type.respawnAnchorsUsable())
        {
            i18n.send(context, NEUTRAL, "- respawn anchors explode on use");
        }
        if (type.hasRaids())
        {
            i18n.send(context, NEUTRAL, "- raids are triggered by bad omen");
        }
        // TODO logical_height: The maximum height to which chorus fruits and nether portals can bring players within this dimension. This excludes portals that were already built above the limit as they still connect normally. Cannot be greater than  height.
        i18n.send(context, NEUTRAL, "- world height ranges from {integer} to {integer}", type.floor(), type.floor() + type.height());
        i18n.send(context, NEUTRAL, "- blocks with {input} burn indefinitely", type.infiniburn().key().asString());
        i18n.send(context, NEUTRAL, "- world effects: {input}", type.effect().key().asString());
    }

}
