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

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class LocatableChange {


    public LocatableChange(final Vector3d pos) {
        this.pos = pos;
        this.blockPos = pos.toInt();
    }

    public Vector3i blockPos;
    public Vector3d pos;

    public static LocatableChange fromDB(final Action action, final Vector3d pos,
            long entityChangeId, ResourceKey entityType, DataContainer origEntity, DataContainer replEntity, boolean living,
            long blockChangeId, String operation, BlockData origBlock, BlockData replBlock) {
        if (entityChangeId > 0) {
            return new EntityChange(action, pos, entityType, origEntity, replEntity, living);
        }
        if (blockChangeId > 0) {
            return new BlockChange(action, pos, operation, origBlock, replBlock);
        }
        return new LocatableChange(pos);
    }

    public Vector3i blockPos() {
        return blockPos;
    }

    public Vector3d pos() {
        return pos;
    }


    @Override
    public String toString() {
        return "%d:%d:%d";
    }
}
