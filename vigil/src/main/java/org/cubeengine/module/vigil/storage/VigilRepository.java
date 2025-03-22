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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Logger;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.VigilConfig;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockData;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.InventoryChange;
import org.cubeengine.module.vigil.action.LocatableChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.action.StringAction;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.postgresql.util.PGobject;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VigilRepository {

    private final Logger logger;
    private HikariDataSource datasource;

    public VigilRepository(final Logger logger) {
        this.logger = logger;
    }

    public VigilRepository init(VigilConfig config) {
        var poolConfig = new HikariConfig();
        final var conf = config.persistence;
        poolConfig.setJdbcUrl("jdbc:postgresql://%s:%d/%s".formatted(conf.host, conf.port, conf.authentication.database));
        poolConfig.setUsername(conf.authentication.username);
        poolConfig.setPassword(conf.authentication.password);

        poolConfig.setMaximumPoolSize(Math.min(2, Runtime.getRuntime().availableProcessors() / 2));
        poolConfig.setConnectionTimeout(5000L);

        this.datasource = new HikariDataSource(poolConfig);

        try (var connection = this.datasource.getConnection();
                var stmt = connection.createStatement();
        ) {

            for (final var createTableSQL : CreateTables.CREATE_TABLES) {
                stmt.executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize connection to database");
        }
        logger.info("Connected to Vigil Database!");
        return this;
    }


    public void store(Action action) throws SQLException {
        try (Connection connection = this.datasource.getConnection()) {
            Inserts.insertFullAction(action, connection);
        }
        // TODO store in bulk? but we have dependencies...
    }


    public List<Action> find(final Lookup lookup) {

        try {
            final var singlePos = lookup.settings().areaMode == LookupSettings.AreaMode.SINGLE ? lookup.position() : null;
            var query = new Query()
                    .inBoundingBox(lookup.boundingBox())
                    .atPosition(singlePos)
                    .inTimeLimit(Duration.ofHours(24))
                    .withReports(lookup.settings().reports())
                    .filterPlayers(lookup.settings().playerFilters())
                    .inWorld(lookup.world());
            // TODO more filtering


            var actionMap = findActions(query);
            fillLocatables(actionMap, singlePos);
            fillStringActions(actionMap);
            fillInventoryChanges(actionMap);
            fillCauses(actionMap);

            return new ArrayList<>(actionMap.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private Map<Long, Action> findActions(final Query query) throws SQLException {
        var sql = query.serialize() + " ORDER BY a.timestamp DESC";
        var actions = new LinkedHashMap<Long, Action>();
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement(sql)) {
            setParams(stmt, query.whereParams());

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var id = rs.getLong("id");
                    var timestamp = rs.getTimestamp("timestamp");
                    var reportType = rs.getString("report_type");
                    var world = JDBCUtil.toKey((rs.getString("world")));

                    actions.put(id, Action.fromDatabase(id, timestamp, reportType, world));
                }
            }
        }
        return actions;
    }

    private void fillLocatables(final Map<Long, Action> actionMap, Vector3i singlePos) throws SQLException {
        // language=PostgreSQL
        String locatablesSql = """
                SELECT l.action_id,
                       l.x, l.y, l.z,
                       ec.id entity_change_id,
                       ec.entity_type, ec.original, ec.replacement, ec.living,
                       bc.id block_change_id ,bc.operation,
                       bc.original_block, bc.original_state, bc.original_snapshot,
                       bc.replacement_block, bc.replacement_state, bc.replacement_snapshot
                FROM locatable l
                LEFT JOIN entity_change ec ON l.id = ec.locatable_id
                LEFT JOIN block_change bc ON l.id = bc.locatable_id
                WHERE l.action_id = ANY(?)
                """;
        if (singlePos != null) {
            locatablesSql += " AND l.block_x = ? AND l.block_y = ? AND l.block_z = ?";
        }
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement(locatablesSql)) {
            setParams(stmt, JDBCUtil.toLongArray(actionMap));
            if (singlePos != null) {
                setParams(1, stmt, singlePos.x(), singlePos.y(), singlePos.z());
            }
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var actionId = rs.getLong("action_id");
                    var pos = new Vector3d(rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                    // entity
                    var entityChangeId = rs.getLong("entity_change_id");
                    var entityType = JDBCUtil.toKey(rs.getString("entity_type"));
                    var original = JDBCUtil.fromJSON(rs.getObject("original", PGobject.class));
                    var replacement = JDBCUtil.fromJSON(rs.getObject("replacement", PGobject.class));
                    var living = rs.getBoolean("living");
                    // block
                    var blockChangeId = rs.getLong("block_change_id");
                    var operation = rs.getString("operation");
                    var origBlock = JDBCUtil.toKey(rs.getString("original_block"));
                    var origState = JDBCUtil.fromJSON(rs.getObject("original_state", PGobject.class));
                    var origSnapshot = JDBCUtil.fromJSON(rs.getObject("original_snapshot", PGobject.class));
                    var orig = new BlockData(origBlock, origState, origSnapshot);
                    var replBlock = JDBCUtil.toKey(rs.getString("replacement_block"));
                    var replState = JDBCUtil.fromJSON(rs.getObject("replacement_state", PGobject.class));
                    var replSnapshot = JDBCUtil.fromJSON(rs.getObject("replacement_snapshot", PGobject.class));
                    var repl = new BlockData(replBlock, replState, replSnapshot);

                    var action = actionMap.get(actionId);
                    var locatable = LocatableChange.fromDB(action, pos,
                            entityChangeId, entityType, original, replacement, living,
                            blockChangeId, operation, orig, repl);
                    action.locatables.add(locatable);
                }
            }
        }
    }

    private void fillInventoryChanges(final Map<Long, Action> actionMap) throws SQLException {
        // language=PostgreSQL
        var inventoryChangesSql = """
                SELECT ic.action_id, ic.original_stack, ic.replacement_stack, ic.slot
                FROM inventory_change ic
                WHERE ic.action_id = ANY(?)
                """;
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement(inventoryChangesSql)) {
            setParams(stmt, JDBCUtil.toLongArray(actionMap));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var actionId = rs.getLong("action_id");
                    var originalStack = JDBCUtil.fromJSON(rs.getObject("original_stack", PGobject.class));
                    var replacementStack = JDBCUtil.fromJSON(rs.getObject("replacement_stack", PGobject.class));
                    var slot = rs.getInt("slot");
                    var inventoryChange = new InventoryChange(originalStack, replacementStack, slot);
                    var action = actionMap.get(actionId);
                    action.inventoryChanges.add(inventoryChange);
                }
            }
        }
    }

    private void fillStringActions(final Map<Long, Action> actionMap) throws SQLException {
        // language=PostgreSQL
        var stringActionsSql = """
                SELECT sa.action_id, sa.type,  sa.data
                FROM string_action sa
                WHERE sa.action_id = ANY(?)
                """;
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement(stringActionsSql)) {
            setParams(stmt, JDBCUtil.toLongArray(actionMap));
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var actionId = rs.getLong("action_id");
                    var type = rs.getString("type");
                    var data = rs.getString("data");
                    var action = actionMap.get(actionId);
                    action.stringAction = new StringAction(type, data);
                }
            }
        }
    }

    private void fillCauses(final Map<Long, Action> actionMap) throws SQLException {
        // language=PostgreSQL
        var causesSql = """
                SELECT c.action_id, c.type, c.reason, c.ord, c.uuid, c.name, c.key, c.context
                FROM cause c
                WHERE c.action_id = ANY(?)
                """;
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement(causesSql)) {
            setParams(stmt, JDBCUtil.toLongArray(actionMap));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    var actionId = rs.getLong("action_id");

                    var type = Causer.Type.valueOf(rs.getString("type"));
                    var reason = StoredCause.CauserReason.valueOf(rs.getString("reason"));
                    var ord = rs.getInt("ord");
                    var uuid = rs.getObject("uuid", UUID.class);
                    var name = rs.getString("name");
                    var resourceKey = JDBCUtil.toKey(rs.getString("key"));
                    var contextKey = rs.getString("context");
                    var causer = new Causer(type, uuid, name, resourceKey);

                    actionMap.get(actionId).causes.byOrdAndKey(ord, contextKey).put(reason, causer);
                }
            }
        }
    }


    public int purge() {
        try (var connection = this.datasource.getConnection();
                var stmt = connection.prepareStatement("DELETE FROM action")
        ) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
