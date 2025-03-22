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

import org.spongepowered.math.vector.Vector3d;

public final class BlockChange extends LocatableChange {

    private final transient Action action;

    private final String operation;

    private final BlockData original;
    private final BlockData replacement;

    public BlockChange(Action action, Vector3d pos, String operation, BlockData original, BlockData replacement) {
        super(pos);
        this.action = action;
        this.original = original;
        this.replacement = replacement;
        this.operation = operation;
    }

    public BlockData original() {
        return original;
    }

    public BlockData replacement() {
        return replacement;
    }

    public String operation() {
        return operation;
    }

    public Action action() {
        return action;
    }

    @Override
    public String toString() {
        return "%s %s->%s".formatted(super.toString(), this.original.block().asMinimalString(), this.replacement.block().asMinimalString());
    }
}
