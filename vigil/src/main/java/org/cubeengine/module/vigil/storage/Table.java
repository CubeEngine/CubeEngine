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
package org.cubeengine.module.vigil.storage;

public record Table(String name, String alias) {

    @Override
    public String toString() {
        return "%s as %s".formatted(name, alias);
    }

    public Column idColumn() {
        return new Column(this, "id", null);
    }

    public Column fkColumn(Table table) {
        return new Column(this, table.name + "_id", null);
    }


    public Column column(String column) {
        return new Column(this, column, null);
    }


}
