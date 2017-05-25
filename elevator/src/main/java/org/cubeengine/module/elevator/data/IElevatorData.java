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
package org.cubeengine.module.elevator.data;

import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.key.KeyFactory.makeSingleKey;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.UUID;

public interface IElevatorData
{
    TypeToken<UUID> TT_UUID = new TypeToken<UUID>() {};
    TypeToken<Value<UUID>> TTV_UUID = new TypeToken<Value<UUID>>() {};
    TypeToken<Vector3i> TT_VECTOR = new TypeToken<Vector3i>() {};
    TypeToken<Value<Vector3i>> TTV_VECTOR = new TypeToken<Value<Vector3i>>() {};

    Key<Value<UUID>> OWNER = makeSingleKey(TT_UUID, TTV_UUID, of("owner"), "cubeengine:elevator:owner", "Owner");
    Key<Value<Vector3i>> TARGET = makeSingleKey(TT_VECTOR, TTV_VECTOR, of("target"), "cubeengine:elevator:target", "Target");

    UUID getOwner();
    Vector3i getTarget();

    ElevatorData asMutable();
}
