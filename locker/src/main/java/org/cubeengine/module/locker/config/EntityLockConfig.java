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
package org.cubeengine.module.locker.config;

import org.apache.logging.log4j.Logger;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.module.locker.data.ProtectedType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.registry.RegistryTypes;

public class EntityLockConfig extends LockConfig<EntityLockConfig, EntityType>
{
    public EntityLockConfig(EntityType entityType)
    {
        super(ProtectedType.getProtectedType(entityType));
        this.type = entityType;
    }

    public String getTitle()
    {
        return type.key(RegistryTypes.ENTITY_TYPE).asString();
    }

    public static class EntityLockerConfigConverter extends LockConfigConverter<EntityLockConfig>
    {
        public EntityLockerConfigConverter(Logger logger)
        {
            super(logger);
        }

        protected EntityLockConfig fromString(String s) throws ConversionException
        {
            return RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.resolve(s))
                         .map(EntityLockConfig::new).orElseThrow(() -> ConversionException.of(this, s, "Invalid EntityType!"));
        }
    }
}
