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

public interface CreateTables {

    // language=PostgreSQL
    String ACTION = """
            CREATE TABLE IF NOT EXISTS action
            (
                id          BIGSERIAL PRIMARY KEY,
                timestamp   TIMESTAMP    NOT NULL,
                report_type VARCHAR(200) NOT NULL,
                world       TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_actions_world ON action (world);
            CREATE INDEX IF NOT EXISTS idx_actions_date ON action (timestamp desc);
            CREATE INDEX IF NOT EXISTS idx_actions_type ON action (report_type desc);
            """;
    // language=PostgreSQL
    String LOCATABLE = """
            CREATE TABLE IF NOT EXISTS locatable
            (
                id        BIGSERIAL PRIMARY KEY,
                action_id BIGINT REFERENCES action (id) ON DELETE CASCADE,
                block_x   INT NOT NULL,
                block_y   INT NOT NULL,
                block_z   INT NOT NULL,
                x         DOUBLE PRECISION,
                y         DOUBLE PRECISION,
                z         DOUBLE PRECISION
            );
            CREATE INDEX IF NOT EXISTS idx_locatable_xyz ON locatable (x, z, y);
            CREATE INDEX IF NOT EXISTS idx_locatable_block_xyz ON locatable (block_x, block_z, block_y);
            """;
    // language=PostgreSQL
    String BLOCK_CHANGE = """
            CREATE TABLE IF NOT EXISTS block_change
            (
                id                   BIGSERIAL PRIMARY KEY,
                locatable_id         BIGINT REFERENCES locatable (id) ON DELETE CASCADE,
                operation            VARCHAR(50) NOT NULL, -- PLACE/BREAK/MODIFY/GROWTH/DECAY/LIQUID_SPREAD/LIQUID_DECAY
                original_block       text,
                original_state       JSONB,
                original_snapshot    JSONB,
                replacement_block    text,
                replacement_state    JSONB,
                replacement_snapshot JSONB
            );
            CREATE INDEX IF NOT EXISTS idx_block_change_operation ON block_change (operation);
            CREATE INDEX IF NOT EXISTS idx_block_change_original ON block_change (original_block);
            CREATE INDEX IF NOT EXISTS idx_block_change_replacement ON block_change (replacement_block);
            """;
    // language=PostgreSQL
    String ENTITY_CHANGE = """
            CREATE TABLE IF NOT EXISTS entity_change
            (
                id           BIGSERIAL PRIMARY KEY,
                locatable_id BIGINT REFERENCES locatable (id) ON DELETE CASCADE,
                entity_type  text NOT NULL,
                original     JSONB,
                replacement  JSONB,
                living       bool
            );
            CREATE INDEX IF NOT EXISTS idx_entity_change_type ON entity_change (entity_type);
            CREATE INDEX IF NOT EXISTS idx_entity_change_living ON entity_change (living);
            """;
    // language=PostgreSQL
    String STRING_ACTION = """
            CREATE TABLE IF NOT EXISTS string_action
            (
                id        BIGSERIAL PRIMARY KEY,
                action_id BIGINT REFERENCES action (id) ON DELETE CASCADE,
                type      varchar(200),
                data      TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_string_action_type ON string_action (type);
            """;
    // language=PostgreSQL
    String INVENTORY_CHANGE = """
            CREATE TABLE IF NOT EXISTS inventory_change
            (
                id                BIGSERIAL PRIMARY KEY,
                action_id         BIGINT REFERENCES action (id) ON DELETE CASCADE,
                original_stack    JSONB,
                replacement_stack JSONB,
                slot              int
            );
            """;


    // language=PostgreSQL
    String CAUSE = """
            CREATE TABLE If NOT EXISTS cause
            (
                id        BIGSERIAL PRIMARY KEY,
                action_id BIGINT REFERENCES action (id) ON DELETE CASCADE,
                type      VARCHAR(50) NOT NULL, -- player/block/tnt/damage/entity
                reason    VARCHAR(50) NOT NULL, -- default/indirect/detonator/agent_target
                ord       int         not null, -- 0=context, 1+ causes
                uuid      UUID,                 -- player/entity uuid
                name      TEXT,                 -- player name / entity custom name
                key       TEXT,                 -- resource key of type
                context   TEXT                  -- context-key
            );
            """;
    String[] CREATE_TABLES = new String[]{ACTION, LOCATABLE, BLOCK_CHANGE, ENTITY_CHANGE, STRING_ACTION, INVENTORY_CHANGE, CAUSE};
}
