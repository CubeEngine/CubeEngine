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

import org.cubeengine.module.vigil.action.Action;
import org.postgresql.util.PGobject;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class JDBCUtil {

    public static void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        setParams(0, stmt, params);
    }

    public static void setParams(final int startIndex, final PreparedStatement stmt, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1 + startIndex, params[i]); // JDBC index starts at 1
        }
    }

    public static PGobject toJSON(final DataContainer data) {
        try {
            if (data == null) {
                return null;
            }
            final var pGobject = new PGobject();
            pGobject.setType("jsonb");
            final var json = DataFormats.JSON.get().write(data);
            pGobject.setValue(json);
            return pGobject;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DataContainer fromJSON(PGobject json) {
        if (json.getValue() == null) {
            return null;
        }
        try {
            return DataFormats.JSON.get().read(json.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    static ResourceKey toKey(final String key) {
        if (key == null) {
            return null;
        }
        return ResourceKey.resolve(key);
    }

    static Object toLongArray(final Map<Long, Action> actionMap) {
        return (Object) actionMap.keySet().toArray(Long[]::new);
    }
}
