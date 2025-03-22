package org.cubeengine.module.vigil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.menu.handler.SlotClickHandler;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConfigurationMenu implements SlotClickHandler {

    private final PluginContainer plugin;
    private final ReportManager reportManager;
    private final I18n i18n;
    private ServerPlayer player;
    private LookupSettings data;
    private InventoryMenu menu;

    public ConfigurationMenu(final PluginContainer plugin, final ReportManager reportManager, final I18n i18n, final ServerPlayer player, final LookupSettings data) {
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

    private ViewableInventory.Custom vigilReportConfigurationRadiusMode() {

        var setItem = ItemStack.of(ItemTypes.PAINTING, 1);
        setItem.offer(VigilData.CONFIG_ACTION, ConfigAction.SET);
        setItem.offer(VigilData.CONFIG_SECTION, ConfigSection.AREA_MODE);
        setItem.offer(VigilData.CONFIG_AREAMODE, LookupSettings.AreaMode.RADIUS);

        var incItem = setItem.copy();
        var incSizeTitle = i18n.translate(player, "Increase Size").color(NamedTextColor.GOLD);
        incItem.offer(Keys.CUSTOM_NAME, incSizeTitle);
        incItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.ADD);

        var decItem = setItem.copy();
        var decSizeTitle = i18n.translate(player, "Decrease Size").color(NamedTextColor.GOLD);
        decItem.offer(Keys.CUSTOM_NAME, decSizeTitle);
        decItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);

        var setSizeTitle = i18n.translate(player, "Set Size").color(NamedTextColor.GOLD);
        setItem.offer(Keys.CUSTOM_NAME, setSizeTitle);
        setItem.offer(VigilData.CONFIG_OPERATION, ConfigOp.SET);


        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_3X3);
        builder.dummySlots(1, Vector2i.from(0, 2)).item(decItem.copy());
        decItem.setQuantity(5);
        builder.dummySlots(1, Vector2i.from(0, 1)).item(decItem.copy());
        decItem.setQuantity(10);
        builder.dummySlots(1, Vector2i.from(0, 0)).item(decItem.copy());

        setItem.setQuantity(5);
        builder.dummySlots(1, Vector2i.from(1, 0)).item(setItem.copy());
        setItem.setQuantity(30);
        builder.dummySlots(1, Vector2i.from(1, 1)).item(setItem.copy());
        setItem.setQuantity(60);
        builder.dummySlots(1, Vector2i.from(1, 2)).item(setItem.copy());

        builder.dummySlots(1, Vector2i.from(2, 0)).item(incItem.copy());
        incItem.setQuantity(5);
        builder.dummySlots(1, Vector2i.from(2, 1)).item(incItem.copy());
        incItem.setQuantity(10);
        builder.dummySlots(1, Vector2i.from(2, 2)).item(incItem.copy());

        return builder.completeStructure().plugin(plugin).build();
    }

    private ViewableInventory.Custom vigilReportConfigurationAreaMode() {
        // TODO
        return ViewableInventory.builder().type(ContainerTypes.GENERIC_3X3)
                .completeStructure()
                .plugin(plugin).build();
    }

    private ViewableInventory.Custom vigilReportAddPlayerConfigurationInventory() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);

        List<GameProfile> profiles = new ArrayList<>();
        Sponge.server().onlinePlayers().stream().filter(player -> !data.playerFilters().contains(player.uniqueId())).map(Player::profile).forEach(profiles::add);
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
    
    private ViewableInventory.Custom vigilReportPlayerConfigurationInventory() {
        var builder = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3);

        final GameProfile plusProfile = GameProfile.of(UUID.fromString("c0bbeabc-c17a-45af-995c-6d5b6e048442"), "Chat Plus").withProperty(
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
            headStack.offer(Keys.CUSTOM_NAME, i18n.translate(player, "Remove {txt} from filter", playerName).color(NamedTextColor.YELLOW));
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

    private ViewableInventory.Custom vigilMainConfigurationInventory() {
        var areaModeTitle = i18n.translate(player, "Area Mode").color(NamedTextColor.GOLD);
        var areaModeSingle = ItemStack.of(ItemTypes.TARGET, 1);
        areaModeSingle.offer(VigilData.CONFIG_ACTION, ConfigAction.SET);
        areaModeSingle.offer(VigilData.CONFIG_SECTION, ConfigSection.AREA_MODE);
        areaModeSingle.offer(VigilData.CONFIG_AREAMODE, LookupSettings.AreaMode.SINGLE);
        areaModeSingle.offer(Keys.CUSTOM_NAME, areaModeTitle);
        var areaModeSingleD = i18n.translate(player, "Single Position").color(NamedTextColor.YELLOW);
        areaModeSingle.offer(Keys.LORE, List.of(areaModeSingleD));
        var areaModeRadius = ItemStack.of(ItemTypes.SCULK_SENSOR, 1);
        areaModeRadius.offer(VigilData.CONFIG_ACTION, ConfigAction.SUB_CONFIG);
        areaModeRadius.offer(VigilData.CONFIG_SECTION, ConfigSection.AREA_MODE);
        areaModeRadius.offer(VigilData.CONFIG_AREAMODE, LookupSettings.AreaMode.RADIUS);
        areaModeRadius.offer(Keys.CUSTOM_NAME, areaModeTitle);
        var areaModeRadD = i18n.translate(player, "Radius around Position").color(NamedTextColor.YELLOW);
        areaModeRadius.offer(Keys.LORE, List.of(areaModeRadD));
        var areaModeArea = ItemStack.of(ItemTypes.END_PORTAL_FRAME, 1);
        areaModeArea.offer(VigilData.CONFIG_ACTION, ConfigAction.SUB_CONFIG);
        areaModeArea.offer(VigilData.CONFIG_SECTION, ConfigSection.AREA_MODE);
        areaModeArea.offer(VigilData.CONFIG_AREAMODE, LookupSettings.AreaMode.AREA);
        areaModeArea.offer(Keys.CUSTOM_NAME, areaModeTitle);
        var areaModeAreaD = i18n.translate(player, "Configured Area").color(NamedTextColor.YELLOW);
        areaModeArea.offer(Keys.LORE, List.of(areaModeAreaD));

        switch (this.data.areaMode) {
            case SINGLE -> areaModeSingle.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
            case RADIUS -> areaModeRadius.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
            case AREA -> areaModeArea.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        var reports = ItemStack.of(ItemTypes.CALIBRATED_SCULK_SENSOR, 1);
        reports.offer(VigilData.CONFIG_ACTION, ConfigAction.SUB_CONFIG);
        reports.offer(VigilData.CONFIG_SECTION, ConfigSection.REPORTS);

        // TODO online players, then separate for offline players
        // TODO how to list interesting players? based on recent area lookup?
        var players = ItemStack.of(ItemTypes.PLAYER_HEAD, 1);
        players.offer(VigilData.CONFIG_ACTION, ConfigAction.SUB_CONFIG);
        players.offer(VigilData.CONFIG_SECTION, ConfigSection.PLAYERS);
        players.offer(VigilData.CONFIG_OPERATION, ConfigOp.SUB);

        // TODO search time limits
        var limit = ItemStack.of(ItemTypes.CLOCK, 1);
        // TODO group diff time
        var diff = ItemStack.of(ItemTypes.REPEATER, 1);
        // TODO grouping
        var grouping = ItemStack.of(ItemTypes.LEAD, 1);

        return ViewableInventory.builder().type(ContainerTypes.GENERIC_3X3)
                .dummySlots(1, Vector2i.from(0, 0)).item(areaModeSingle)
                .dummySlots(1, Vector2i.from(1, 0)).item(areaModeRadius)
                .dummySlots(1, Vector2i.from(2, 0)).item(areaModeArea)
                .dummySlots(1, Vector2i.from(1, 1)).item(grouping)
                .dummySlots(1, Vector2i.from(2, 1)).item(diff)
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
        final var cfgSection = slotItem.getOrNull(VigilData.CONFIG_SECTION);
        final var cfgAreaMode = slotItem.getOrNull(VigilData.CONFIG_AREAMODE);
        final var cfgOp = slotItem.getOrNull(VigilData.CONFIG_OPERATION);

        switch (cfgAction) {
            case SET -> {
                switch (cfgSection) {
                    case AREA_MODE -> {

                        switch (cfgAreaMode) {
                            case SINGLE -> {
                                data.areaMode = cfgAreaMode;
                            }
                            case RADIUS -> {
                                var quantity = slotItem.quantity();
                                switch (cfgOp) {
                                    case SET -> {
                                        data.radius = quantity;
                                    }
                                    case ADD -> {
                                        data.radius += quantity;
                                    }
                                    case SUB -> {
                                        if (data.radius - quantity > 0) {
                                            data.radius -= quantity;
                                        }
                                    }
                                }
                                menu.setTitle(getConfigureRadiusTitle(data.radius));
                            }
                            case AREA -> {
                                // TODO
                            }
                        }
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
                            var reportConf = vigilReportPlayerConfigurationInventory();
                            openReadonly(player, reportConf, Component.text("Player Filter"));
                        } else {
                            slot.poll();
                        }
                    }
                }
            }
            case SUB_CONFIG -> {
                switch (cfgSection) {
                    case AREA_MODE -> {
                        switch (cfgAreaMode) {
                            case SINGLE -> { // Nothing to configure
                            }
                            case RADIUS -> {
                                data.areaMode = cfgAreaMode;
                                openReadonly(player, vigilReportConfigurationRadiusMode(),
                                        getConfigureRadiusTitle(data.radius));
                            }
                            case AREA -> {
                                data.areaMode = cfgAreaMode;
                                openReadonly(player, vigilReportConfigurationAreaMode(),
                                        i18n.translate(player, "Configure Area"));
                            }
                        }

                    }
                    case REPORTS -> {
                        var reportConf = vigilReportConfigurationInventory();
                        openReadonly(player, reportConf, Component.text("Vigil Report Configuration"));
                    }
                    case PLAYERS -> {
                        if (cfgOp == ConfigOp.ADD || data.playerFilters().isEmpty()) {
                            var reportConf = vigilReportAddPlayerConfigurationInventory();
                            openReadonly(player, reportConf, Component.text("Add Player Filter"));
                        }
                        else if (cfgOp == ConfigOp.SUB) {
                            var reportConf = vigilReportPlayerConfigurationInventory();
                            openReadonly(player, reportConf, Component.text("Player Filter"));    
                        }
                        
                    }
                }
            }
            case null -> {
            }
            default -> {
            }
        }
        player.itemInHand(HandTypes.MAIN_HAND).offer(VigilData.LOOKUP_DATA, data);
        return false;
    }

    private Component getConfigureRadiusTitle(int radius) {
        final var title = i18n.translate(player, "Configure Radius");
        var cfg = Component.text("[").append(Component.text(radius).color(NamedTextColor.DARK_GREEN)).append(Component.text("]"));
        return title.append(Component.space()).append(cfg);
    }

    public void show() {
        final var configInv = vigilMainConfigurationInventory();
        openReadonly(player, configInv, Component.text("Vigil Configuration"));
    }
}
