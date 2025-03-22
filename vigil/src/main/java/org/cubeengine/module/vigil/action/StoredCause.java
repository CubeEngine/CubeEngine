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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record StoredCause(Map<String, Map<CauserReason, Causer>> context,
                          List<Map<CauserReason, Causer>> causes) {


    public StoredCause() {
        this(new HashMap<>(), new ArrayList<>());
    }


    public Map<CauserReason, Causer> byOrdAndKey(final int ord, final String contextKey) {
        if (ord == 0) {
            return context.computeIfAbsent(contextKey, k -> new HashMap<>());
        }
        while (causes.size() <= ord - 1) {
            causes.add(new HashMap<>());
        }
        return causes.get(ord - 1);
    }

    @Override
    public String toString() {
        var firstCause = causes.getFirst().get(CauserReason.DEFAULT);
        return "%s %s".formatted(firstCause.type().name(), firstCause.name() == null ? firstCause.resourceKey() : firstCause.name());
    }

    public enum CauserReason {
        DEFAULT,
        INDIRECT,
        DETONATOR,
        AGENT_TARGET,

    }
}
