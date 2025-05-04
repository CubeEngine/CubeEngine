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
package org.cubeengine.module.vigil.data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

public class LookupSettings implements DataSerializable {

    public static final DataQuery FULL_DATE = DataQuery.of("fullDate");
    public static final DataQuery NO_DATE = DataQuery.of("noDate");
    public static final DataQuery SHOW_LOC = DataQuery.of("showLocation");
    public static final DataQuery FULL_LOC = DataQuery.of("fullLocation");
    public static final DataQuery DETAILED_INV = DataQuery.of("detInv");
    public static final DataQuery REPORTS = DataQuery.of("reports");
    public static final DataQuery RADIUS = DataQuery.of("radius");
    public static final DataQuery AREAMODE = DataQuery.of("areamode");
    public static final DataQuery PLAYER_FILTERS = DataQuery.of("playerFilters");
    public static final DataQuery TIME_LIMIT = DataQuery.of("timeLimit");



    public enum AreaMode implements EnumSerializable {
        SINGLE, RADIUS, AREA
    }

    public AreaMode areaMode = AreaMode.SINGLE;
    public int radius = 5;

    private boolean fullDate = false;
    private boolean noDate = false;
    private boolean showLocation = false;
    private boolean fullLocation = false;

    private boolean showDetailedInventory = false;

    private Set<String> reports = new HashSet<>();
    private Set<UUID> playerFilters = new HashSet<>();
    public Duration limitTime = Duration.ofHours(24);

    public void toggleFilterPlayer(final UUID player) {
        if (this.playerFilters.contains(player)) {
            this.playerFilters.remove(player);
        } else {
            this.playerFilters.add(player);
        }
    }

    public void toggleReport(final String report) {
        if (this.reports.contains(report)) {
            this.reports.remove(report);
        } else {
            this.reports.add(report);
        }
    }

    // ------ GETTERS ------

    public Duration limitTime() {
        return this.limitTime;
    }

    public Set<UUID> playerFilters() {
        return this.playerFilters;
    }


    public boolean isFullDate() {
        return fullDate;
    }

    public boolean isShowLocation() {
        return showLocation;
    }

    public boolean isNoDate() {
        return noDate;
    }

    public boolean isFullLocation() {
        return fullLocation;
    }

    public boolean showDetailedInventory() {
        return showDetailedInventory;
    }


    public Set<String> reports() {
        return reports;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final var container = DataContainer.createNew();
        container.set(Queries.CONTENT_VERSION, this.contentVersion());
        container.set(FULL_DATE, fullDate);
        container.set(NO_DATE, noDate);
        container.set(SHOW_LOC, showLocation);
        container.set(FULL_LOC, fullLocation);
        container.set(DETAILED_INV, showDetailedInventory);
        container.set(REPORTS, new ArrayList<>(reports));
        container.set(RADIUS, radius);
        container.set(AREAMODE, areaMode.name());
        container.set(PLAYER_FILTERS, playerFilters);
        container.set(TIME_LIMIT, limitTime.toMinutes());
        return container;
    }

    public LookupSettings copy() {
        final LookupSettings copy = new LookupSettings();
        copy.fullDate = this.fullDate;
        copy.noDate = this.noDate;
        copy.showLocation = this.showLocation;
        copy.fullLocation = this.fullLocation;
        copy.showDetailedInventory = this.showDetailedInventory;
        copy.reports = this.reports;
        copy.limitTime = this.limitTime;
        return copy;
    }

    public static class LookupDataBuilder implements DataBuilder<LookupSettings> {

        @Override
        public Optional<LookupSettings> build(final DataView container) throws InvalidDataException {
            if (container.isEmpty()) {
                return Optional.empty();
            }
            final var lookupSettings = new LookupSettings();
            lookupSettings.fullDate = container.getBoolean(FULL_DATE).orElse(false);
            lookupSettings.noDate = container.getBoolean(NO_DATE).orElse(false);
            lookupSettings.showLocation = container.getBoolean(SHOW_LOC).orElse(false);
            lookupSettings.fullLocation = container.getBoolean(FULL_LOC).orElse(false);
            lookupSettings.showDetailedInventory = container.getBoolean(DETAILED_INV).orElse(false);
            lookupSettings.reports.addAll(container.getStringList(REPORTS).orElse(List.of()));
            lookupSettings.radius = container.getInt(RADIUS).orElse(5);
            lookupSettings.areaMode = container.getString(AREAMODE).map(AreaMode::valueOf).orElse(AreaMode.SINGLE);
            lookupSettings.playerFilters.addAll(container.getObjectList(PLAYER_FILTERS, UUID.class).orElse(List.of()));
            lookupSettings.limitTime = container.getLong(TIME_LIMIT).map(Duration::ofMinutes).orElse(Duration.ofHours(24));

            return Optional.of(lookupSettings);

        }
    }
}
