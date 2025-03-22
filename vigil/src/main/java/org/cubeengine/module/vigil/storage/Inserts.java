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

import static org.cubeengine.module.vigil.storage.JDBCUtil.setParams;
import static org.cubeengine.module.vigil.storage.JDBCUtil.toJSON;

import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockChange;
import org.cubeengine.module.vigil.action.EntityChange;
import org.cubeengine.module.vigil.action.InventoryChange;
import org.cubeengine.module.vigil.action.LocatableChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.action.StringAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public interface Inserts {


    static void insertFullAction(final Action action, final Connection connection) throws SQLException {
        var actionId = Inserts.insertAction(action, connection);
        insertLocatables(actionId, action.locatables, connection);
        insertStringAction(actionId, action.stringAction, connection);
        insertInventoryChanges(actionId, action.inventoryChanges, connection);
        insertCauses(actionId, action.causes, connection);
    }


    // language=PostgreSQL
    String ACTION = """
            INSERT INTO action (timestamp, report_type, world) 
            values (?, ?, ?)
            """;

    private static long insertAction(final Action action, final Connection connection) throws SQLException {

        try (var stmt = connection.prepareStatement(ACTION, Statement.RETURN_GENERATED_KEYS)) {
            setParams(stmt, action.timestamp, action.reportType, action.world.asString());
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // language=PostgreSQL
    String CAUSE = """
            INSERT INTO cause (action_id, type, reason, ord, uuid, name, key, context) 
            VALUES (?,?,?,?,?,?,?,?)
            """;

    private static void insertCauses(final long actionId, final StoredCause causes, final Connection conn) throws SQLException {

        try (var stmt = conn.prepareStatement(CAUSE)) {
            int ord = 0;
            for (final var contextEntry : causes.context().entrySet()) {
                for (final var causerEntry : contextEntry.getValue().entrySet()) {
                    final var causer = causerEntry.getValue();
                    final var reason = causerEntry.getKey();
                    setParams(stmt, actionId, causer.type().name(), reason.name(), ord,
                            causer.uuid(), causer.name(), causer.resourceKey() == null ? null : causer.resourceKey().asString(), contextEntry.getKey());
                    stmt.addBatch();
                }

            }
            for (final var cause : causes.causes()) {
                ord++;
                for (final var causerEntry : cause.entrySet()) {
                    final var reason = causerEntry.getKey();
                    final var causer = causerEntry.getValue();
                    setParams(stmt, actionId, causer.type().name(), reason.name(), ord,
                            causer.uuid(), causer.name(), causer.resourceKey() == null ? null : causer.resourceKey().asString(), null);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    // language=PostgreSQL
    String INVENTORY_CHANGE = """
            INSERT INTO inventory_change (action_id, original_stack, replacement_stack, slot) 
            VALUES (?,?,?,?)
            """;

    private static void insertInventoryChanges(final long actionId, final List<InventoryChange> iActions, final Connection conn) throws SQLException {
        if (iActions == null || iActions.isEmpty()) {
            return;
        }
        try (var stmt = conn.prepareStatement(INVENTORY_CHANGE)) {
            for (final var iAction : iActions) {
                var origStack = toJSON(iAction.originalStack());
                var replStack = toJSON(iAction.replacementStack());
                setParams(stmt, actionId, origStack, replStack, iAction.slot());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // language=PostgreSQL
    String STRING_ACTION = """
            INSERT INTO string_action (action_id, type, data)
            values (?,?,?)
            """;

    private static void insertStringAction(final long actionId, final StringAction stringAction, final Connection connection) throws SQLException {
        if (stringAction == null) {
            return;
        }

        try (var stmt = connection.prepareStatement(STRING_ACTION)) {
            setParams(stmt, actionId, stringAction.type(), stringAction.data());
            stmt.executeUpdate();
        }

    }

    // language=PostgreSQL
    String LOCATABLE = """
            INSERT INTO locatable (action_id, 
                                   block_x, block_y, block_z, 
                                   x, y, z) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    // language=PostgreSQL

    String BLOCK_CHANGE = """
            INSERT INTO block_change (locatable_id, operation, 
                                      original_block, original_state, original_snapshot, 
                                      replacement_block, replacement_state, replacement_snapshot) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;


    // language=PostgreSQL
    String ENTITY_CHANGE = """
            INSERT INTO entity_change (locatable_id, entity_type, original, replacement, living)
            VALUES (?,?,?,?,?)
            """;

    private static void insertLocatables(long actionId, List<LocatableChange> locatables, Connection connection) throws SQLException {

        var locatableIds = new ArrayList<Long>();

        try (var stmt = connection.prepareStatement(LOCATABLE, Statement.RETURN_GENERATED_KEYS)) {
            for (var locatable : locatables) {
                setParams(stmt, actionId,
                        locatable.blockPos.x(), locatable.blockPos.y(), locatable.blockPos.z(),
                        locatable.pos.x(), locatable.pos.y(), locatable.pos.z());
                stmt.addBatch(); // Add to batch
            }
            // TODO prevent 0 update batch
            stmt.executeBatch(); // Execute batch insert

            try (var rs = stmt.getGeneratedKeys()) {
                while (rs.next()) {
                    locatableIds.add(rs.getLong(1)); // Collect generated IDs
                }
            }
        }

        try (var stmt = connection.prepareStatement(BLOCK_CHANGE)) {
            for (int i = 0; i < locatables.size(); i++) {
                if (locatables.get(i) instanceof BlockChange blockChange) {
                    final var orig = blockChange.original();
                    final var repl = blockChange.replacement();
                    // TODO jsonB
                    var origState = toJSON(orig.state());
                    var replState = toJSON(repl.state());
                    var origSnap = toJSON(orig.snapshot());
                    var replSnap = toJSON(repl.snapshot());
                    setParams(stmt, locatableIds.get(i), blockChange.operation(),
                            orig.block().asString(), origState, origSnap,
                            repl.block().asString(), replState, replSnap
                    );
                    stmt.addBatch();
                }
            }
            // TODO prevent 0 update batch
            stmt.executeBatch();
        }

        try (var stmt = connection.prepareStatement(ENTITY_CHANGE)) {
            for (int i = 0; i < locatables.size(); i++) {
                if (locatables.get(i) instanceof EntityChange entityChange) {
                    var orig = toJSON(entityChange.original());
                    var repl = toJSON(entityChange.replacement());
                    setParams(stmt, locatableIds.get(i), entityChange.entityType().asString(),
                            orig, repl, entityChange.living());
                    stmt.addBatch();
                }
            }
            // TODO prevent 0 update batch
            stmt.executeBatch();
        }
    }


}
