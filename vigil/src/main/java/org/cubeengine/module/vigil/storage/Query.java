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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.util.AABB;
import org.spongepowered.math.vector.Vector3i;

public class Query {

    private Set<Table> tables = new HashSet<>();


    public Query() {
        this.tables.add(Tables.ACTION);
        this.fromSql = "\nFROM %s".formatted(Tables.ACTION);
        this.projection.add(Tables.ACTION.idColumn());
        this.projection.add(Tables.ACTION.column("timestamp"));
        this.projection.add(Tables.ACTION.column("report_type"));
        this.projection.add(Tables.ACTION.column("world"));
    }

    private List<Column> projection = new ArrayList<>();
    private String fromSql;
    private List<String> whereClauses = new ArrayList<>();
    private List<Object> whereParams = new ArrayList<>();

    private boolean distinct = true;


    private void addJoin(Table rightTable, Column leftColumn, Column rightColumn) {
        if (this.tables.add(rightTable)) {
            fromSql += "\nJOIN %s ON %s = %s".formatted(rightTable, leftColumn, rightColumn);
        }
    }

    private void addWhere(Column column, SQLOperator op, Object value) {
        whereClauses.add("%s %s ?".formatted(column, op));
        whereParams.add(value);
    }

    private QueryOrGroup withOrGroup() {
        return new QueryOrGroup(this, new ArrayList<>(), new ArrayList<>());
    }

    public Query filterPlayers(Set<UUID> players) {
        if (!players.isEmpty()) {
            addJoin(Tables.CAUSE, Tables.ACTION.idColumn(), Tables.CAUSE.fkColumn(Tables.ACTION));
            addWhere(Tables.CAUSE.column("type"), SQLOperator.EQ, "PLAYER");
            addWhere(Tables.CAUSE.column("context").coalesce("''"), SQLOperator.NEQ, "sponge:creator");
            try (var orGroup = withOrGroup()) {
                for (final var player : players) {
                    orGroup.addWhere(Tables.CAUSE.column("uuid"), SQLOperator.EQ, player);
                }
            }
        }
        return this;
    }

    public Query withReports(Set<String> reports) {
        try (var queryOrGroup = withOrGroup()) {
            for (final var report : reports) {
                queryOrGroup.addWhere(Tables.ACTION.column("report_type"), SQLOperator.EQ, report);
            }
        }
        return this;
    }

    Query inBoundingBox(AABB bb) {
        if (bb == null) {
            return this;
        }
        addJoin(Tables.LOCATABLE, Tables.ACTION.idColumn(), Tables.LOCATABLE.fkColumn(Tables.ACTION));
        addWhere(Tables.LOCATABLE.column("block_x"), SQLOperator.GE, bb.min().toInt().x());
        addWhere(Tables.LOCATABLE.column("block_x"), SQLOperator.LE, bb.max().toInt().x());
        addWhere(Tables.LOCATABLE.column("block_y"), SQLOperator.GE, bb.min().toInt().y());
        addWhere(Tables.LOCATABLE.column("block_y"), SQLOperator.LE, bb.max().toInt().y());
        addWhere(Tables.LOCATABLE.column("block_z"), SQLOperator.GE, bb.min().toInt().z());
        addWhere(Tables.LOCATABLE.column("block_z"), SQLOperator.LE, bb.max().toInt().z());
        return this;
    }


    public String serialize() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT %s ".formatted(distinct ? "DISTINCT" : ""));
        queryBuilder.append(String.join(",\n\t", projection.stream().map(Column::toString).toList()));
        queryBuilder.append(fromSql);
        queryBuilder.append("\nWHERE 1=1");
        whereClauses.forEach(clause -> queryBuilder.append("\nAND %s".formatted(clause)));
        return queryBuilder.toString();
    }

    public Query inTimeLimit(final Duration timeLimit) {

        addWhere(Tables.ACTION.column("timestamp"), SQLOperator.GE, Timestamp.from(Instant.now().minus(timeLimit)));
        return this;
    }

    public Query inWorld(final ResourceKey world) {
        if (world == null) {
            return this;
        }
        addWhere(Tables.ACTION.column("world"), SQLOperator.EQ, world.asString());
        return this;
    }

    public Query atPosition(final Vector3i position) {
        if (position == null) {
            return this;
        }
        addJoin(Tables.LOCATABLE, Tables.ACTION.idColumn(), Tables.LOCATABLE.fkColumn(Tables.ACTION));
        addWhere(Tables.LOCATABLE.column("block_x"), SQLOperator.EQ, position.x());
        addWhere(Tables.LOCATABLE.column("block_y"), SQLOperator.EQ, position.y());
        addWhere(Tables.LOCATABLE.column("block_z"), SQLOperator.EQ, position.z());
        return this;
    }


    public Object[] whereParams() {
        return this.whereParams.toArray();
    }

    private record QueryOrGroup(Query query, List<String> whereClauses, List<Object> whereParams) implements AutoCloseable {

        public void addWhere(Column column, SQLOperator op, Object value) {
            whereClauses.add("%s %s ?".formatted(column, op));
            whereParams.add(value);
        }

        @Override
        public void close() {
            if (!whereClauses.isEmpty()) {
                var grouped = String.join("\nOR ", whereClauses);
                query.whereClauses.add("(\n%s\n)".formatted(grouped));
                query.whereParams.addAll(whereParams);
            }
        }
    }
}
