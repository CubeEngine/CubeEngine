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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.cubeengine.module.vigil.data.VigilData;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.plugin.PluginContainer;

@Singleton
public class ToolListener {

    private QueryManager qm;
    private final Permission toolPerm;
    private final PluginContainer plugin;
    private final I18n i18n;
    private final ReportManager reportManager;

    @Inject
    public ToolListener(PermissionManager pm, QueryManager qm, I18n i18n, PluginContainer plugin, final ReportManager reportManager) {
        this.qm = qm;
        this.i18n = i18n;
        toolPerm = pm.register(Vigil.class, "use-logtool", "Allows using log-tools", null);
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Listener
    public void onClick(InteractBlockEvent.Secondary.Pre event, @First ServerPlayer player) {
        handleLRClicks(event, player);
    }

    @Listener
    public void onClick(InteractItemEvent.Secondary.Pre event, @First ServerPlayer player) {
        if (player.require(Keys.IS_SNEAKING)) {
            var mainItem = player.itemInHand(HandTypes.MAIN_HAND);
            var data = mainItem.get(VigilData.LOOKUP_DATA);
            if (data.isPresent()) {
                if (toolPerm.check(player)) {
                    new ConfigurationMenu(plugin, reportManager, i18n, player, data.get()).show();
                    event.setCancelled(true);
                }
            }
        }
    }

    @Listener
    public void onClick(InteractBlockEvent.Primary.Start event, @First ServerPlayer player) {
        handleLRClicks(event, player);
    }

    private void handleLRClicks(InteractBlockEvent event, ServerPlayer player) {
        if (!EventUtil.isMainHand(event.context()) || player.require(Keys.IS_SNEAKING)) {
            return;
        }
        ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        itemInHand.get(VigilData.LOOKUP_DATA).ifPresent(lookupData -> {
            if (!toolPerm.check(player) || event.block() == BlockSnapshot.empty()) {
                return;
            }
            ServerLocation loc;
            if (event instanceof InteractBlockEvent.Primary.Start e) {
                loc = event.block().location().get();
                e.setCancelled(true);
            } else if (event instanceof InteractBlockEvent.Secondary.Pre e) {
                loc = event.block().location().get().relativeTo(event.targetSide());
                e.setCancelled(true);
            } else {
                throw new IllegalStateException("impossible");
            }
            final var lookup = new Lookup(lookupData).with(loc);
            qm.queryAndShow(lookup, player);
            if (lookup.settings().areaMode == LookupSettings.AreaMode.SINGLE) {
                RenderUtil.renderArea(player, lookup.position());
            } else {
                RenderUtil.renderArea(player, lookup.boundingBox());
            }
        });
    }

    @Listener
    public void onDropTool(DropItemEvent.Pre event) {
        event.droppedItems().removeIf(item -> item.get(VigilData.CREATOR).isPresent());
    }
}
