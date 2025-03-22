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
package org.cubeengine.module.vigil.action;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.UUID;

public record Causer(org.cubeengine.module.vigil.action.Causer.Type type, UUID uuid, String name, ResourceKey resourceKey) {

    public static Causer ofEntity(final Entity entity) {
        var key = entity.type().key(RegistryTypes.ENTITY_TYPE);
        String name = null;
        if (entity.customName().isPresent()) {
            name = PlainTextComponentSerializer.plainText().serialize(entity.customName().get().get());
        }
        return new Causer(Type.ENTITY, entity.uniqueId(), name, key);
    }

    public static Causer ofPlayer(final Type type, final ServerPlayer player) {
        return new Causer(type, player.uniqueId(), player.name(), null);
    }

    public static Causer ofUser(final Type type, final User player) {
        return new Causer(type, player.uniqueId(), player.name(), null);
    }

    public static Causer of(final Type type, final ResourceKey resourceKey) {
        return new Causer(type, null, null, resourceKey);
    }

    public static Causer unknown() {
        return new Causer(Type.UNKNOWN, null, null, null);
    }


    public enum Type {
        PLAYER,
        BLOCK,
        TNT,
        DAMAGE,
        ENTITY,
        UNKNOWN
    }
}
