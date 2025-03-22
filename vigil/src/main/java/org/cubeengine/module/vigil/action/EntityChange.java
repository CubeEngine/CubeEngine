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

public final class EntityChange extends LocatableChange {

    private final transient Action action;
    private final ResourceKey entityType;
    private final DataContainer original;
    private final DataContainer replacement;
    private final boolean living;

    public EntityChange(final Action action, Vector3d pos, final ResourceKey key, DataContainer original,
            DataContainer replacement, boolean living) {
        super(pos);
        this.action = action;
        this.entityType = key;
        this.original = original;
        this.replacement = replacement;
        this.living = living;
    }

    public DataContainer original() {
        return original;
    }

    public DataContainer replacement() {
        return replacement;
    }

    public boolean living() {
        return living;
    }

    public Action action() {
        return action;
    }

    public ResourceKey entityType() {
        return this.entityType;
    }

    @Override
    public String toString() {
        return "%s %s %s".formatted(super.toString(), this.entityType.asMinimalString(), replacement == null ? "removed" : "changed");
    }
}
