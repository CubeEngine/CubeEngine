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
package org.cubeengine.module.vigil.reporting;

import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.action.BlockChange;
import org.cubeengine.module.vigil.action.Causer;
import org.cubeengine.module.vigil.action.EntityChange;
import org.cubeengine.module.vigil.action.LocatableChange;
import org.cubeengine.module.vigil.action.StoredCause;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.module.vigil.report.entity.DestructReport;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PreparedReport {

    private final Receiver receiver;
    private final List<ReportLine> reportLines;
    private final ReportManager reportManager;


    public PreparedReport(final Receiver receiver, final ReportManager reportManager, final List<Action> actions) {
        this.receiver = receiver;
        this.reportManager = reportManager;
        this.reportLines = this.computeReportLines(actions, receiver);
    }

    public void doShow() {
        receiver.sendReports(reportLines);
    }

    public record ReportLine(Report report, Set<Vector3i> positions, List<Action> actions, List<LocatableChange> locatables) {

        public void showReport(final Receiver receiver) {
            report.showReportLine(receiver, this);
        }
    }


    public record GroupKey(Set<Object> keys) {

    }

    public List<List<Action>> splitGaps(List<Action> actions) {
        if (actions.isEmpty()) {
            return List.of();
        }
        var firstReport = reportManager.reportOf(actions.getFirst());

        var maxDiff = firstReport.maxDiff();
        // TODO block max diff depending on player filter?
        // TODO configurable maxDiff in lookup/ different defaults on report
        List<List<Action>> split = actions.stream()
                .sorted(Comparator.comparing(a -> a.timestamp))
                .collect(ArrayList::new,
                        (acc, current) -> {
                            if (acc.isEmpty() ||
                                current.timestamp.getTime() - acc.getLast().getLast().timestamp.getTime() > maxDiff.toMillis()) {
                                acc.add(new ArrayList<>());
                            }
                            acc.getLast().add(current);
                        },
                        ArrayList::addAll);
        return split;
    }

    private List<ReportLine> computeReportLines(List<Action> actions, Receiver receiver) {
        receiver.getLookup().time(Lookup.LookupTiming.REPORT);
        var lookup = receiver.getLookup();


        var groupedActions = actions.stream().collect(Collectors.groupingBy(a -> findGroupKey(lookup, a)));

        var splitActions = groupedActions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> splitGaps(e.getValue())
        ));
        var collapsedActions = splitActions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().flatMap(this::collapseChanges).toList()
        ));


        // collapse block changes by position

        var playerLookup = lookup.player == null;
        if (playerLookup) {
            // group by blocktype?
        } else {
            // group by player is already done...
        }


        if (lookup.settings().areaMode == LookupSettings.AreaMode.SINGLE) {
            // single pos lookup
        } else {
            // global lookup
        }

        final var list = new ArrayList<>(collapsedActions.values().stream().flatMap(Collection::stream).toList());
        receiver.getLookup().time(Lookup.LookupTiming.REPORT);
        Comparator<Action> comparing = Comparator.comparing(a -> a.timestamp);
        list.sort(Comparator.comparing(rl -> rl.actions().stream().sorted(comparing.reversed()).findFirst().get().timestamp));
        final var revList = list.reversed();
        return revList;

    }

    private Stream<ReportLine> collapseChanges(final List<Action> actions) {

        if (actions.isEmpty()) {
            return Stream.empty();
        }
        // TODO other locatables
        // TODO mixed block changes with others?
        Map<Vector3i, List<BlockChange>> allBlockChanges = new HashMap<>();
        Set<Vector3i> positions = new HashSet<>();
        for (final var action : actions) {
            for (final var locatable : action.locatables) {
                positions.add(locatable.blockPos);
                if (locatable instanceof BlockChange bc) {
                    var listAtPos = allBlockChanges.computeIfAbsent(bc.blockPos(), k -> new ArrayList<>());
                    listAtPos.add(bc);
                }
            }
        }
        var report = reportManager.reportOf(actions.getFirst());
        if (allBlockChanges.isEmpty()) {
            return Stream.of(new ReportLine(report, positions, actions, List.of()));
        }
        if (positions.size() == 1) {
            return allBlockChanges.values().stream().flatMap(Collection::stream).map(bc ->
                    new ReportLine(report, positions, List.of(bc.action()), List.of(bc)));

        }


        Map<Vector3i, BlockChange> collapsedBlockChanges = new HashMap<>();
        for (final var entry : allBlockChanges.entrySet()) {
            final var pos = entry.getKey();
            final var blockChanges = entry.getValue();
            // TODO make sure this is an unbroken chain of changes
            final var lastBlock = blockChanges.getLast().replacement();
            final var firstBlock = blockChanges.getFirst().original();

            var operation = blockChanges.getLast().operation();

            collapsedBlockChanges.put(pos, new BlockChange(blockChanges.getFirst().action(), pos.toDouble(), operation, firstBlock, lastBlock));
            // TODO count number of times the block was changed in the same position?
        }

        var line = new ReportLine(report, positions, actions, new ArrayList<>(collapsedBlockChanges.values()));


        return Stream.of(line);


    }

    private GroupKey findGroupKey(final Lookup lookup, Action action) {
        Set<Object> keys = new HashSet<>();

        var report = reportManager.reportOf(action);
        // TODO maybe later allow different reports to be grouped
        keys.add(report);

        if (action.causes.causes().isEmpty()) {
            keys.add(action.causes);
        } else {
            final var firstCause = action.causes.causes().getFirst().get(StoredCause.CauserReason.DEFAULT);
            if (EntityTypes.ITEM.location().equals(firstCause.resourceKey())) {
                // Cause is an item, group all of them
                var snap = Recall.entitySnapshot(action);
                var stack = Recall.stackFromEntitySnapshot(snap);
                keys.add(EntityTypes.ITEM.get());
                keys.add(EntityTypes.ITEM.get());
                if (stack != null) {
                    keys.add(stack.type());
                }
            } else if (firstCause.type().equals(Causer.Type.ENTITY)) {
                keys.add(Causer.Type.ENTITY);
                keys.add(firstCause.resourceKey());
            } else if (firstCause.type().equals(Causer.Type.BLOCK)) {
                keys.add(Causer.Type.BLOCK);
                if (firstCause.resourceKey().equals(BlockTypes.STICKY_PISTON.location()) || firstCause.resourceKey()
                        .equals(BlockTypes.PISTON.location())) {
                    keys.add(BlockTypes.MOVING_PISTON.location());
                } else {
                    keys.add(firstCause.resourceKey());
                }
            } else {
                if (firstCause.type().equals(Causer.Type.PLAYER)) {
                    keys.add(Causer.Type.PLAYER);
                    keys.add(firstCause.uuid());
                } else {
                    keys.add(action.causes); // TODO find player in cause
                }
            }
        }

        if (report instanceof DestructReport) {
            if (action.locatables.getFirst() instanceof EntityChange entityChange) {
                keys.add(entityChange.living() ? "living" : "non_living");
            }
        }


        return new GroupKey(keys);
    }


}
