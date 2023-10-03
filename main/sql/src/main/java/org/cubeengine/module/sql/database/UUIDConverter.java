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
package org.cubeengine.module.sql.database;

import org.jooq.impl.AbstractConverter;

import java.util.UUID;


public class UUIDConverter extends AbstractConverter<String, UUID>
{
    public UUIDConverter()
    {
        super(String.class, UUID.class);
    }

    @Override
    public UUID from(String databaseObject)
    {
        if (databaseObject == null)
        {
            return null;
        }
        return UUID.fromString(databaseObject);
    }

    @Override
    public String to(UUID userObject)
    {
        if (userObject == null)
        {
            return null;
        }
        return userObject.toString();
    }
}