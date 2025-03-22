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

public class BlockData {

    private final ResourceKey block;
    private final DataContainer state; // TODO toRawData
    private final DataContainer snapshot;  // TODO toRawData


    public BlockData(final ResourceKey key, final DataContainer blockState, final DataContainer block) {
        this.block = key;
        this.state = blockState;
        this.snapshot = block;
    }

    public DataContainer state() {
        return state;
    }

    public DataContainer snapshot() {
        return this.snapshot;
    }

    public ResourceKey block() {
        return block;
    }
}
