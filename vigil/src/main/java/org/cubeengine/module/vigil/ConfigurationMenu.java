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
package org.cubeengine.module.vigil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.data.ConfigAction;
import org.cubeengine.module.vigil.data.ConfigOp;
import org.cubeengine.module.vigil.data.ConfigSection;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.cubeengine.module.vigil.data.VigilData;
import org.cubeengine.module.vigil.report.ReportManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.menu.handler.SlotClickHandler;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ConfigurationMenu implements SlotClickHandler {

    private final PluginContainer plugin;
    private final ReportManager reportManager;
    private final I18n i18n;
    private ServerPlayer player;
    private LookupSettings data;
    private InventoryMenu menu;

    public ConfigurationMenu(final PluginContainer plugin, final ReportManager reportManager, final I18n i18n, final ServerPlayer player,
            final LookupSettings data) {
        this.plugin = plugin;
        this.reportManager = reportManager;
        this.i18n = i18n;
        this.player = player;
        this.data = data;
    }


    private void openReadonly(final ServerPlayer player, final ViewableInventory.Custom inventory, final Component title) {
        var menu = InventoryMenu.of(inventory).setReadOnly(true);
        menu.registerHandler(this);
        menu.setTitle(title);
        menu.open(player);
        this.menu = menu;
    }

    private Component configureAreaModeTitle() {
        return switch (data.areaMode) {
            case SINGLE -> i18n.translate(player, "Area Mode: Single");
            case RADIUS -> {
                final var title = i18n.translate(player, "Area Mode: Radius");
                var cfg = Component.text("[").append(Component.text(data.radius).color(NamedTextColor.DARK_GREEN)).append(Component.text("]"));
                yield title.append(Component.space()).append(cfg);
            }
            case AREA -> i18n.translate(player, "Area Mode: Area");
        };
    }


    private ViewableInventory.Custom  vigilReportConfigurationLimit() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);

        var minutes1 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "1 Minute"), null);
        minutes1.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        minutes1.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofMinutes(1).toMinutes());
        var minutes10 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "10 Minutes"), null);
        minutes10.setQuantity(10);
        minutes10.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        minutes10.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofMinutes(10).toMinutes());
        var hours1 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "1 Hour"), null);
        hours1.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        hours1.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofHours(1).toMinutes());
        var hours8 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "8 Hours"), null);
        hours8.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        hours8.setQuantity(8);
        hours8.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofHours(8).toMinutes());
        var days1 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "1 Day"), null);
        days1.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        days1.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofDays(1).toMinutes());
        var days7 = buildItem(ItemTypes.CLOCK, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "7 Days"), null);
        days7.setQuantity(7);
        days7.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
        days7.offer(VigilData.CONFIG_LONG_VALUE, Duration.ofDays(7).toMinutes());

        builder.dummySlots(1, Vector2i.from(0,0)).item(minutes1);
        builder.dummySlots(1, Vector2i.from(1,0)).item(minutes10);
        builder.dummySlots(1, Vector2i.from(0,1)).item(hours1);
        builder.dummySlots(1, Vector2i.from(1,1)).item(hours8);
        builder.dummySlots(1, Vector2i.from(0,2)).item(days1);
        builder.dummySlots(1, Vector2i.from(1,2)).item(days7);

        var sub1 = buildItem(ItemTypes.REPEATER, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Subtract 1"), null);
        sub1.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);
        sub1.offer(VigilData.CONFIG_LONG_VALUE, 1L);
        var sub5 = buildItem(ItemTypes.REPEATER, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Subtract 5"), null);
        sub5.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);
        sub5.setQuantity(5);
        sub5.offer(VigilData.CONFIG_LONG_VALUE, 5L);
        var add1 = buildItem(ItemTypes.REPEATER, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Add 1"), null);
        add1.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);
        add1.offer(VigilData.CONFIG_LONG_VALUE, 1L);
        var add5 = buildItem(ItemTypes.REPEATER, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Add 5"), null);
        add5.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);
        add5.setQuantity(5);
        add5.offer(VigilData.CONFIG_LONG_VALUE, 5L);

        var div2 = buildItem(ItemTypes.COMPARATOR, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Divide by 2"), null);
        div2.offer(VigilData.CONFIG_OPERATION, ConfigOp.DIV);
        div2.offer(VigilData.CONFIG_LONG_VALUE, 2L);
        div2.setQuantity(2);
        var mul2 = buildItem(ItemTypes.COMPARATOR, ConfigAction.SET, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Multiply by 2"), null);
        mul2.offer(VigilData.CONFIG_OPERATION, ConfigOp.MUL);
        mul2.offer(VigilData.CONFIG_LONG_VALUE, 2L);
        mul2.setQuantity(2);

        builder.dummySlots(1, Vector2i.from(7,0)).item(sub1);
        builder.dummySlots(1, Vector2i.from(8,0)).item(add1);
        builder.dummySlots(1, Vector2i.from(7,1)).item(sub5);
        builder.dummySlots(1, Vector2i.from(8,1)).item(add5);

        builder.dummySlots(1, Vector2i.from(7,2)).item(div2);
        builder.dummySlots(1, Vector2i.from(8,2)).item(mul2);
        return builder.completeStructure().plugin(plugin).build();
    }

    private ViewableInventory.Custom vigilReportConfigurationAreaMode() {
        var areaModeSingle = buildItem(ItemTypes.TARGET, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.SINGLE,
                i18n.translate(player, "Single Position"), null);
        var areaModeRadius = buildItem(ItemTypes.SCULK_SENSOR, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.RADIUS,
                i18n.translate(player, "Radius around Position"), null);
        var areaModeArea = buildItem(ItemTypes.END_PORTAL_FRAME, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.AREA,
                i18n.translate(player, "Configured Area"), null);


        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);
        builder.dummySlots(1, Vector2i.from(0, 0)).item(areaModeSingle);
        builder.dummySlots(1, Vector2i.from(0, 1)).item(areaModeRadius);
        builder.dummySlots(1, Vector2i.from(0, 2)).item(areaModeArea);
//        builder.dummyGrid(Vector2i.from(3,3), Vector2i.from(3,0)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(3,0)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(4,0)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(5,0)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(3,1)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(4,1)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(5,1)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(3,2)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(4,2)).item(ItemStack.of(ItemTypes.PAPER));
        builder.dummySlots(1, Vector2i.from(5,2)).item(ItemStack.of(ItemTypes.PAPER));
        return builder.completeStructure().plugin(plugin).build();
    }


    private ViewableInventory.Custom vigilCauseFilterAddPlayerConfigInventory() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);

        // TODO online players, then separate for offline players
        // TODO how to list interesting players? based on recent area lookup?


        List<GameProfile> profiles = new ArrayList<>();
        Sponge.server().onlinePlayers().stream().filter(player -> !data.playerFilters().contains(player.uniqueId())).map(Player::profile)
                .forEach(profiles::add);
        int i = 0;
        for (final var profile : profiles) {
            final var headStack = headOfProfile(profile);
            headStack.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);
            final var playerName = Component.text(profile.name().get()).color(NamedTextColor.DARK_GREEN);
            headStack.offer(Keys.CUSTOM_NAME, i18n.translate(player, "Add {txt} to filter", playerName).color(NamedTextColor.YELLOW));
            builder.dummySlots(1, i++).item(headStack);
            if (i % 8 == 7) {
                i++;
            }
        }


        // TODO recently seen players (in lookups)
        return builder.completeStructure().plugin(plugin).build();
    }

    private ViewableInventory.Custom vigilCauseFilterPlayerConfigInventory() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);

        final GameProfile plusProfile = GameProfile.of(UUID.fromString("c0bbeabc-c17a-45af-995c-6d5b6e048442"), "ChatPlus").withProperty(
                ProfileProperty.of(ProfileProperty.TEXTURES,
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzOGQwOGJkMDQwNWMwNWY0N2VhODZkNjY2NDM0MzRmZGQyZThjNDZmZjFlNmY4ODJiYjliZjg5MWM3ZDNhNSJ9fX0="));
        var plusHead = ItemStack.of(ItemTypes.PLAYER_HEAD, 1);
        plusHead.offer(Keys.GAME_PROFILE, plusProfile);
        plusHead.offer(VigilData.CONFIG_ACTION, ConfigAction.SUB_CONFIG);
        plusHead.offer(VigilData.CONFIG_SECTION, ConfigSection.PLAYERS);
        plusHead.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);
        plusHead.offer(Keys.CUSTOM_NAME, i18n.translate(player, "Add Player Filter").color(NamedTextColor.YELLOW));

        builder.dummySlots(1, Vector2i.from(8, 2)).item(plusHead);

        int i = 0;

        for (final var playerFilter : data.playerFilters()) {
            var profile = Sponge.server().gameProfileManager().profile(playerFilter).join();
            final var headStack = headOfProfile(profile);
            final var playerName = Component.text(profile.name().get()).color(NamedTextColor.DARK_GREEN);
            headStack.offer(Keys.CUSTOM_NAME, i18n.translate(player, "Remove {txt} from Cause Filter", playerName).color(NamedTextColor.YELLOW));
            builder.dummySlots(1, i++).item(headStack);
        }
        return builder.completeStructure().plugin(plugin).build();
    }

    private static ItemStack headOfProfile(final GameProfile pp) {
        final var headStack = ItemStack.of(ItemTypes.PLAYER_HEAD, 1);
        headStack.offer(Keys.GAME_PROFILE, pp);
        headStack.offer(VigilData.CONFIG_ACTION, ConfigAction.SET);
        headStack.offer(VigilData.CONFIG_SECTION, ConfigSection.PLAYERS);
        headStack.offer(VigilData.PLAYER, pp.uuid());
        return headStack;
    }

    private ViewableInventory.Custom vigilReportConfigurationInventory() {

        // TODO crafting report
        // TODO explosion report
        // TODO entity interact mode

        // TODO grouped reports
        int i = 0;
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);
        for (final var entry : reportManager.getReports().entrySet()) {
            final var report = entry.getValue();
            final var reportName = entry.getKey();
            final var icon = report.getIcon(i18n, player);
            icon.offer(VigilData.CONFIG_ACTION, ConfigAction.SET);
            icon.offer(VigilData.CONFIG_SECTION, ConfigSection.REPORTS);
            icon.offer(VigilData.CONFIG_REPORT, reportName);
            icon.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, data.reports().contains(reportName));
            builder.dummySlots(1, i++).item(icon);
        }
        return builder.completeStructure().plugin(plugin).build();
    }


    private <T> ItemStack buildItem(final Supplier<ItemType> type, final ConfigAction action, final ConfigSection section,
            final Component title,
            final Stream<T> loreObjects,
            final Function<T, Component> loreFunction)
    {
        return this.buildItem(type, action, section, null, title, loreObjects.map(loreFunction).toArray(Component[]::new));
    }

    private ItemStack buildItem(final Supplier<ItemType> type, final ConfigAction action, final ConfigSection section,
            final LookupSettings.AreaMode areaMode, final Component title,
            final Component... lore) {
        var item = ItemStack.of(type, 1);
        item.offer(VigilData.CONFIG_ACTION, action);
        item.offer(VigilData.CONFIG_SECTION, section);
        if (areaMode != null) {
            item.offer(VigilData.CONFIG_AREAMODE, areaMode);
        }
        if (title != null) {
            item.offer(Keys.CUSTOM_NAME, title);
        }
        if (lore != null) {
            item.offer(Keys.LORE, List.of(lore));
        }
        return item;
    }

    private ViewableInventory.Custom vigilMainConfigurationInventory() {
        var areaMode = buildItem(ItemTypes.SCULK_SENSOR, ConfigAction.SUB_CONFIG, ConfigSection.AREA_MODE, null,
                i18n.translate(player, "Configure Area Mode").color(NamedTextColor.GOLD),
                configureAreaModeTitle().color(NamedTextColor.YELLOW)
        );

        var reports = buildItem(ItemTypes.CALIBRATED_SCULK_SENSOR, ConfigAction.SUB_CONFIG, ConfigSection.REPORTS,
                i18n.translate(player, "Configure Active Reports").color(NamedTextColor.GOLD),
                reportManager.getReports().entrySet().stream().filter(e -> data.reports().contains(e.getKey())),
                        e -> Component.text(" - ").color(NamedTextColor.GRAY).append(e.getValue().getIcon(i18n, player).get(Keys.CUSTOM_NAME).get().color(NamedTextColor.YELLOW)));

        var players = buildItem(ItemTypes.PLAYER_HEAD, ConfigAction.SUB_CONFIG, ConfigSection.PLAYERS,
                i18n.translate(player, "Configure Cause Filter: Players").color(NamedTextColor.GOLD),
                data.playerFilters().stream().map(uuid -> Sponge.server().gameProfileManager().profile(uuid).join()),
                p -> Component.text(" - ").color(NamedTextColor.GRAY).append(Component.text(p.name().orElse(p.uuid().toString())).color(NamedTextColor.DARK_GREEN)));
        if (data.playerFilters().isEmpty()) {
            players.offer(Keys.LORE, List.of(i18n.translate(player, "Shows Any").color(NamedTextColor.GRAY)));
        }
        players.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);
        // TODO non-player causes?
        // enderman/creeper
        // fire/tnt/plants

        var limit = buildItem(ItemTypes.CLOCK, ConfigAction.SUB_CONFIG, ConfigSection.LIMIT_TIME, null,
                i18n.translate(player, "Configure Time Limit").color(NamedTextColor.GOLD), limitTimeTitle().color(NamedTextColor.YELLOW));
// TODO time grouping
//        var diff = buildItem(ItemTypes.REPEATER, ConfigAction.SUB_CONFIG, ConfigSection.GROUP_TIME, null,
//                i18n.translate(player, "Configure temporal grouping"));
        // TODO grouping
//        var grouping = ItemStack.of(ItemTypes.LEAD, 1);

        return ViewableInventory.builder().type(ContainerTypes.GENERIC_3X3)
                .dummySlots(1, Vector2i.from(0, 0)).item(areaMode)
                //                .dummySlots(1, Vector2i.from(1, 0)).item(areaModeRadius)
                //                .dummySlots(1, Vector2i.from(2, 0)).item(areaModeArea)
//                .dummySlots(1, Vector2i.from(1, 1)).item(grouping)
//                .dummySlots(1, Vector2i.from(2, 1)).item(diff)
                .dummySlots(1, Vector2i.from(0, 1)).item(reports)
                .dummySlots(1, Vector2i.from(2, 2)).item(limit)
                .dummySlots(1, Vector2i.from(0, 2)).item(players)
                .completeStructure()
                .plugin(plugin).build();
    }

    @Override
    public boolean handle(Cause cause, Container container, Slot slot, int slotIndex, ClickType<?> clickType) {
        final var slotItem = slot.peek();
        final var cfgAction = slotItem.getOrNull(VigilData.CONFIG_ACTION);
        switch (cfgAction) {
            case SET -> handleSetConfig(slot, slotItem);
            case SUB_CONFIG -> handleSubConfig(slotItem);
            case null, default -> {
            }
        }
        player.itemInHand(HandTypes.MAIN_HAND).offer(VigilData.LOOKUP_DATA, data);
        return false;
    }

    private void handleSetConfig(final Slot slot, final ItemStack slotItem) {
        final var cfgSection = slotItem.getOrNull(VigilData.CONFIG_SECTION);
        final var cfgOp = slotItem.getOrNull(VigilData.CONFIG_OPERATION);
        switch (cfgSection) {
            case LIMIT_TIME -> {
                final var cfgLong = slotItem.getOrNull(VigilData.CONFIG_LONG_VALUE);
                handleSetLimitTime(slotItem, cfgOp, cfgLong);
            }
            case AREA_MODE -> {
                final var cfgAreaMode = slotItem.getOrNull(VigilData.CONFIG_AREAMODE);
                handleSetAreaMode(slotItem, cfgAreaMode, cfgOp);
            }
            case REPORTS -> {
                final var report = slotItem.require(VigilData.CONFIG_REPORT);
                data.toggleReport(report);
                slotItem.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, data.reports().contains(report));
                slot.set(slotItem);
            }
            case PLAYERS -> {
                var togglePlayer = slotItem.require(VigilData.PLAYER);
                data.toggleFilterPlayer(togglePlayer);
                if (cfgOp == ConfigOp.ADD) {
                    var reportConf = vigilCauseFilterPlayerConfigInventory();
                    openReadonly(player, reportConf, causeFilterPlayersTitle());
                } else {
                    slot.poll();
                    if (data.playerFilters().isEmpty()) {
                        openReadonly(player, vigilCauseFilterAddPlayerConfigInventory(), causeFilterAddPlayerTitle());
                    }
                }
            }
        }
    }

    private void handleSetLimitTime(final ItemStack slotItem, final ConfigOp cfgOp, final Long cfgLong) {
        var days = data.limitTime.toDays();
        var hours = data.limitTime.toHours();
        var hoursOnly = data.limitTime.minusDays(days).toHours();
        var minutesOnly = data.limitTime.minusDays(days).minusHours(hours).toMinutes();
        switch (cfgOp) {
            case SET -> {
                data.limitTime = Duration.ofMinutes(cfgLong);
            }
            case ADD -> {
                if (days > 1) {
                    if (hoursOnly == 0) {
                        data.limitTime = data.limitTime.plusDays(cfgLong);
                    } else {
                        data.limitTime = data.limitTime.plusHours(cfgLong);
                    }
                } else if (hours > 1) {
                    if (minutesOnly == 0) {
                        data.limitTime = data.limitTime.plusHours(cfgLong);
                    } else {
                        data.limitTime = data.limitTime.plusMinutes(cfgLong);
                    }
                } else {
                    data.limitTime = data.limitTime.plusMinutes(cfgLong);
                }
            }
            case SUB -> {
                if (days > cfgLong || days > 1 && hoursOnly == 0) {
                    data.limitTime = data.limitTime.minusDays(cfgLong);
                } else if (hours > cfgLong || hours > 1 && minutesOnly == 0) {
                    data.limitTime = data.limitTime.minusHours(cfgLong);
                } else {
                    data.limitTime = data.limitTime.minusMinutes(cfgLong);
                }
            }
            case DIV -> {
                data.limitTime = data.limitTime.dividedBy(2).truncatedTo(ChronoUnit.MINUTES);
            }
            case MUL -> {
                data.limitTime = data.limitTime.multipliedBy(2);

            }
        }
        if (data.limitTime.isZero() || data.limitTime().isNegative()) {
            data.limitTime = Duration.ofMinutes(1);
        }

        if (data.limitTime.toDays() > 0) {
            data.limitTime = data.limitTime.truncatedTo(ChronoUnit.HOURS);
        }
        if (data.limitTime.toDays() > 9000) {
            data.limitTime = Duration.ofDays(9000).plusMinutes(1);
        }
        menu.setTitle(limitTimeTitle());
    }

    private void handleSetAreaMode(final ItemStack slotItem, final LookupSettings.AreaMode cfgAreaMode, final ConfigOp cfgOp) {
        data.areaMode = cfgAreaMode;
        switch (cfgAreaMode) {
            case SINGLE -> {

            }
            case RADIUS -> {
                var quantity = slotItem.quantity();
                switch (cfgOp) {
                    case null -> {
                    }
                    case DIV, MUL -> {}
                    case SET -> data.radius = quantity;
                    case ADD -> data.radius += quantity;
                    case SUB -> {
                        if (data.radius - quantity > 0) {
                            data.radius -= quantity;
                        }
                    }
                }

            }
            case AREA -> {
                // TODO
            }
        }
        menu.setTitle(configureAreaModeTitle()); // must be BEFORE changing items
        updateAreaModeMenu(menu.inventory());
    }

    private void handleSubConfig(final ItemStack slotItem) {
        final var cfgSection = slotItem.getOrNull(VigilData.CONFIG_SECTION);
        final var cfgOp = slotItem.getOrNull(VigilData.CONFIG_OPERATION);
        switch (cfgSection) {
            case LIMIT_TIME -> {
                final var inventory = vigilReportConfigurationLimit();
                openReadonly(player, inventory, limitTimeTitle());
            }
            case AREA_MODE -> {
                final var inventory = vigilReportConfigurationAreaMode();
                updateAreaModeMenu(inventory);
                openReadonly(player, inventory, configureAreaModeTitle());
            }
            case REPORTS -> {
                var reportConf = vigilReportConfigurationInventory();
                openReadonly(player, reportConf, i18n.translate(player, "Vigil Report Configuration"));
            }
            case PLAYERS -> {
                if (cfgOp == ConfigOp.ADD || data.playerFilters().isEmpty()) {
                    var reportConf = vigilCauseFilterAddPlayerConfigInventory();
                    openReadonly(player, reportConf, causeFilterAddPlayerTitle());
                } else if (cfgOp == ConfigOp.SUB) {
                    var reportConf = vigilCauseFilterPlayerConfigInventory();
                    openReadonly(player, reportConf, causeFilterPlayersTitle());
                }

            }
            case GROUP_TIME -> {
                var conf = vigilReportConfigurationTimeGrouping();
                openReadonly(player, conf, causeTimeGroupingTitle());
            }
        }
    }


    private Component causeFilterAddPlayerTitle() {
        return causeFilterPlayersTitle().append(Component.space()).append(i18n.translate(player, "Add"));
    }

    private Component causeFilterPlayersTitle() {
        return i18n.translate(player, "Cause Filter: Players ({txt})",
                Component.text(data.playerFilters().size(), NamedTextColor.DARK_GREEN));
    }

    private ViewableInventory.Custom vigilReportConfigurationTimeGrouping() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);
        return builder.completeStructure().plugin(plugin).build();
    }

    private Component causeTimeGroupingTitle() {
        // TODO
        return i18n.translate(player, "Grouping: {txt}", Component.text(null, NamedTextColor.DARK_GREEN));
    }

    private Component limitTimeTitle() {
        var days = data.limitTime.toDays();
        var hours = data.limitTime.minusDays(days).toHours();
        var minutes = data.limitTime.minusDays(days).minusHours(hours).toMinutes();

        var title = i18n.translate(player, "Time limit").append(Component.text(":"));
        if (days > 0) {
            title = title.append(Component.space())
                    .append(Component.text(days)).append(Component.space()).append(i18n.translate(player, "days"));
        }
        if (hours > 0) {
            title = title.append(Component.space())
                    .append(Component.text(hours)).append(Component.space()).append(i18n.translate(player, "hours"));
        }
        if (minutes > 0) {
            title = title.append(Component.space())
                    .append(Component.text(minutes)).append(Component.space()).append(i18n.translate(player, "minutes"));
        }
        return title;
    }


    private void updateAreaModeMenu(Inventory inventory) {
        final var grid = inventory.query(GridInventory.class).get();
        var areaModeSingle = grid.peek(0, 0).get();
        var areaModeRadius = grid.peek(0, 1).get();
        var areaModeArea = grid.peek(0, 2).get();
        areaModeSingle.remove(Keys.ENCHANTMENT_GLINT_OVERRIDE);
        areaModeRadius.remove(Keys.ENCHANTMENT_GLINT_OVERRIDE);
        areaModeArea.remove(Keys.ENCHANTMENT_GLINT_OVERRIDE);
        var radiusDec = List.of(ItemStack.empty(), ItemStack.empty(), ItemStack.empty());
        var radiusSet = radiusDec;
        var radiusInc = radiusDec;
        switch (data.areaMode) {
            case SINGLE -> areaModeSingle.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
            case RADIUS -> {
                areaModeRadius.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
                var decItem = buildItem(ItemTypes.PAINTING, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.RADIUS,
                        i18n.translate(player, "Decrease Size").color(NamedTextColor.GOLD), null);
                decItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);

                radiusDec = new ArrayList<>();
                decItem.setQuantity(10);
                radiusDec.add(decItem.copy());
                decItem.setQuantity(5);
                radiusDec.add(decItem.copy());
                decItem.setQuantity(1);
                radiusDec.add(decItem.copy());

                var setItem = buildItem(ItemTypes.PAINTING, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.RADIUS,
                        i18n.translate(player, "Set Size").color(NamedTextColor.GOLD), null);
                setItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);
                radiusSet = new ArrayList<>();
                setItem.setQuantity(5);
                radiusSet.add(setItem.copy());
                setItem.setQuantity(30);
                radiusSet.add(setItem.copy());
                setItem.setQuantity(60);
                radiusSet.add(setItem.copy());

                var incItem = buildItem(ItemTypes.PAINTING, ConfigAction.SET, ConfigSection.AREA_MODE, LookupSettings.AreaMode.RADIUS,
                        i18n.translate(player, "Increase Size").color(NamedTextColor.GOLD), null);
                incItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);
                radiusInc = new ArrayList<>();
                radiusInc.add(incItem.copy());
                incItem.setQuantity(5);
                radiusInc.add(incItem.copy());
                incItem.setQuantity(10);
                radiusInc.add(incItem.copy());
            }
            case AREA -> areaModeArea.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        for (var i = 0; i < radiusDec.size(); i++) {
            grid.set(3, i, radiusDec.get(i));
        }
        for (var i = 0; i < radiusSet.size(); i++) {
            grid.set(4, i, radiusSet.get(i));
        }
        for (var i = 0; i < radiusInc.size(); i++) {
            grid.set(5, i, radiusInc.get(i));
        }

        grid.set(0, 0, areaModeSingle);
        grid.set(0, 1, areaModeRadius);
        grid.set(0, 2, areaModeArea);
    }


    public void show() {
        final var configInv = vigilMainConfigurationInventory();
        openReadonly(player, configInv, Component.text("Vigil Configuration"));
    }
}
