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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.registry.RegistryTypes;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.entity.EntityTypes.*;

public class EntityFilterParser implements ValueParser<EntityFilter>, ValueCompleter
{
    private I18n i18n;

    public EntityFilterParser(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        return Stream.of(ITEM, ARROW, MINECART, PAINTING, ITEM_FRAME, EXPERIENCE_ORB)
              .map(DefaultedRegistryReference::location)
              .filter(key -> currentInput.startsWith(key.asString()) || "minecraft".equals(key.namespace()) && currentInput.startsWith(key.value()))
              .map(ResourceKey::asString)
              .map(CommandCompletion::of)
              .collect(Collectors.toList());
    }

    @Override
    public Optional<? extends EntityFilter> parseValue(Key<? super EntityFilter> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final CommandCause cmdSource = context.cause();
        String token = reader.parseString();
        List<Predicate<Entity>> filters = new ArrayList<>();
        if ("*".equals(token)) // All non living
        {
            filters.add(e -> !(e instanceof Living));
            return Optional.of(new EntityFilter(filters, true));
        }
        final Locale locale = cmdSource.audience() instanceof ServerPlayer ? ((ServerPlayer)cmdSource.audience()).locale() : Locale.getDefault();
        for (String entityString : StringUtils.explode(",", token))
        {
            Optional<ItemType> itemType = Optional.empty();
            var type = RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.resolve(entityString));
            if (type.isEmpty() && entityString.contains(":"))
            {
                final var firstDelimiterIdx = entityString.indexOf(":");
                final var baseType = entityString.substring(0, firstDelimiterIdx);
                type = RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.resolve(baseType));
                if (type.isEmpty() || !ITEM.get().equals(type.get()))
                {
                    i18n.send(cmdSource, NEGATIVE, "You can only specify data for removing items!");
                    return Optional.empty();
                }
                String itemKey = entityString.substring(firstDelimiterIdx + 1);
                itemType = RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.resolve(itemKey));
                if (itemType.isEmpty())
                {
                    i18n.send(cmdSource, NEGATIVE, "Cannot find itemtype {input}", itemKey);
                    return Optional.empty();
                }
            }
            if (type.isEmpty())
            {
                i18n.send(cmdSource, NEGATIVE, "Invalid entity-type!");
                i18n.send(cmdSource, NEUTRAL, "Try using one of those instead:");

                final List<Component> types = Stream.of(ITEM, ARROW, MINECART, PAINTING, ITEM_FRAME, EXPERIENCE_ORB)
                                                    .map(DefaultedRegistryReference::get)
                                                    .map(EntityType::asComponent)
                                                    .collect(Collectors.toList());
                cmdSource.sendMessage(Identity.nil(), Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.YELLOW)), types));
                return Optional.empty();
            }
//            if (Living.class.isAssignableFrom(type.getEntityClass()))
//            {
//                i18n.send(cmdSource, NEGATIVE, "To kill living entities use the {text:/butcher} command!");
//                return Optional.empty();
//            }
            final ItemType item = itemType.orElse(null);
            final EntityType<?> eType = type.get();
            filters.add(entity -> entity.type().equals(eType) && (item == null || entity.get(Keys.ITEM_STACK_SNAPSHOT).get().type().equals(item)));
        }
        return Optional.of(new EntityFilter(filters));
    }

}
