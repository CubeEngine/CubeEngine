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
package org.cubeengine.module.zoned.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.module.zoned.ZoneManager;
import org.cubeengine.module.zoned.Zoned;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerWorld;

@ParserFor(ZoneConfig.class)
public class ZoneParser implements ValueParser<ZoneConfig>, ValueCompleter, DefaultParameterProvider<ZoneConfig>
{
    private final Zoned module;
    private final ZoneManager manager;
    private final I18n i18n;

    @Inject
    public ZoneParser(Zoned module, ZoneManager manager, I18n i18n)
    {
        this.module = module;
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public ZoneConfig apply(CommandCause cause)
    {
        // TODO for other command-sources?
        if (cause.audience() instanceof ServerPlayer)
        {
            ZoneConfig zone = module.getActiveZone(((ServerPlayer)cause.audience()));
            if (zone != null)
            {
                return zone;
            }
            List<ZoneConfig> zones = manager.getZonesAt(((ServerPlayer)cause.audience()).serverLocation());
            if (!zones.isEmpty())
            {
                return zones.get(0);
            }
        }

        i18n.send(cause.audience(), MessageType.NEGATIVE, "You need to provide a zone");
        return null;
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        String token = currentInput.toLowerCase();
        List<CommandCompletion> list = new ArrayList<>();
        ServerWorld world = null;
        boolean isLocatable = context.cause().audience() instanceof Locatable;
        if (isLocatable)
        {
            world = ((Locatable)context.cause().audience()).serverLocation().world();
            for (ZoneConfig zone : manager.getZones(null, world))
            {
                if (zone.name == null)
                {
                    continue;
                }
                if (zone.name.startsWith(token))
                {
                    list.add(CommandCompletion.of(zone.name));
                }
            }
        }

        for (ZoneConfig zone : manager.getZones(null, null))
        {
            /* TODO global region
            if ("global".startsWith(token))
            {
                list.add("global");
            }
             */
            if (world != null && zone.world.getWorld().uniqueId().equals(world.uniqueId())
                && !world.key().toString().startsWith(token.replace(".", "")))
            {
                continue; // Skip if already without world ; except when token starts with world
            }
            if (token.contains(".") || !isLocatable) // Skip if without dot and locatable
            {
                String fullName = zone.world.getName() + "." + zone.name;
                if (fullName.startsWith(token))
                {
                    list.add(CommandCompletion.of(fullName));
                }
                /* TODO world region?
                if ((worldName + ".world").startsWith(token))
                {
                    list.add(worldName + ".world");
                }
                */
            }
        }

        /* TODO world region?
        if (isLocatable && "world".startsWith(token))
        {
            list.add("world");
        }
        */
        /* TODO world regions?
        for (ZoneConfig ZoneConfig : manager.getWorldZoneConfigs())
        {
            String worldName = ZoneConfig.getWorld().getName();
            if (worldName.startsWith(token))
            {
                list.add(worldName);
            }
            if (token.contains(".") && (worldName + ".").startsWith(token))
            {
                list.add(worldName + ".world");
            }
        }
        */

        return list;
    }

    @Override
    public Optional<? extends ZoneConfig> parseValue(Parameter.Key<? super ZoneConfig> parameterKey,
                                                   ArgumentReader.Mutable reader,
                                                   CommandContext.Builder context) throws ArgumentParseException
    {
        final Audience audience = context.cause().audience();

        final String token = reader.parseUnquotedString();
        if (audience instanceof Locatable)
        {
            ServerWorld world = ((Locatable)audience).serverLocation().world();
            ZoneConfig zone = manager.getZone(token);
            if (zone != null)
            {
                return Optional.of(zone);
            }
            /* TODO world regions
            if ("world".equals(token))
            {
                ZoneConfig = manager.getWorldZoneConfig(world.getUniqueId());
                if (ZoneConfig != null)
                {
                    return ZoneConfig;
                }
            }
            */
        }

        /* TODO world region
        if (token.endsWith(".world"))
        {
            String worldName = token.replaceAll(".world$", "");
            Optional<WorldProperties> worldProp = Sponge.getServer().getWorldProperties(worldName);
            if (worldProp.isPresent())
            {
                return manager.getWorldZoneConfig(worldProp.get().getUniqueId());
            }
            else
            {
                throw new TranslatedParserException(
                        i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                "Unknown World {name} for world-ZoneConfig", token, worldName));
            }
        }
        */
        /* TODO global region
        if ("global".equals(token))
        {
            return manager.getGlobalZoneConfig();
        }
        */
        throw reader.createException(i18n.translate(audience, MessageType.NEGATIVE, "There is no zone named {name}", token));
    }
}
