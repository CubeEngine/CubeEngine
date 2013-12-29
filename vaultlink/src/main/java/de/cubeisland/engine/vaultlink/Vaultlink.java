/**
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
package de.cubeisland.engine.vaultlink;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import de.cubeisland.engine.core.bukkit.BukkitCore;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.roles.Roles;
import de.cubeisland.engine.vaultlink.service.CubeChatService;
import de.cubeisland.engine.vaultlink.service.CubeEconomyService;
import de.cubeisland.engine.vaultlink.service.CubePermissionService;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class Vaultlink extends Module implements Listener
{
    private final AtomicReference<de.cubeisland.engine.core.module.service.Economy> economyReference = new AtomicReference<>();

    @Override
    public void onLoad()
    {
        Module module = getCore().getModuleManager().getModule("roles");
        if (module != null && module instanceof Roles)
        {
            Roles roles = (Roles)module;
            Permission service = new CubePermissionService(this, roles);
            Bukkit.getServicesManager().register(Permission.class, service, (BukkitCore)getCore(), ServicePriority.Highest);
            Bukkit.getServicesManager().register(Chat.class, new CubeChatService(this, roles, service), (BukkitCore)getCore(), ServicePriority.Highest);
        }

        Bukkit.getServicesManager().register(Economy.class, new CubeEconomyService(this, economyReference), (BukkitCore)getCore(), ServicePriority.Highest);
    }

    @Override
    public void onEnable()
    {
        this.getCore().getEventManager().registerListener(this, this);
        this.economyReference.set(getCore().getModuleManager().getServiceManager().getServiceProvider(de.cubeisland.engine.core.module.service.Economy.class));
    }

    @Override
    public void onStartupFinished()
    {
        final ServicesManager sm = Bukkit.getServicesManager();
        for (Class<?> serviceClass : sm.getKnownServices())
        {
            getLog().debug("Service: {}", serviceClass.getName());
            for (RegisteredServiceProvider<?> p : sm.getRegistrations(serviceClass))
            {
                getLog().debug(" - Provider {} ({}) [{}]", p.getProvider().getClass().getName(), p.getPlugin().getName(), p.getPriority().name());
            }
        }
    }

    @EventHandler
    private void serviceRegistered(ServiceRegisterEvent event)
    {
        getLog().debug(event.getProvider().getClass().getName());
        getLog().debug(event.getProvider().getPriority().name());
    }

    @EventHandler
    private void serviceUnregister(ServiceUnregisterEvent event)
    {
        getLog().debug(event.getProvider().getClass().getName());
        getLog().debug(event.getProvider().getPriority().name());
    }
}
