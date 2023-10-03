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
package org.cubeengine.libcube;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Optional;

public abstract class CubeEnginePlugin {

    @Inject PluginContainer plugin;
    @Inject private Injector injector;
    @ConfigDir(sharedRoot = false) @Inject Path dataFolder;
    private final Class<?> module;
    private ModuleManager mm;
    private Object instance;

    public CubeEnginePlugin(Class<?> module)
    {
        this.module = module;
    }

    public void onConstruction(ConstructPluginEvent event)
    {
        final PluginContainer lib = event.game().pluginManager().plugin(PluginLibCube.LIBCUBE_ID)
                                         .orElseThrow(() -> new IllegalArgumentException("libcube not found"));
        PluginLibCube libCube = (PluginLibCube) lib.instance();
        this.mm = libCube.getCore().getModuleManager();
        this.instance = this.mm.registerAndCreate(this.module, this.plugin, this.injector);
        this.plugin.logger().info("Module " + module.getSimpleName() + " loaded!");
        this.mm.loadConfigs(this.plugin, this.module, true);
    }

    public void onInit(StartingEngineEvent<Server> event)
    {
        Object module = mm.getModule(this.module);
        this.mm.loadConfigs(this.plugin, this.module, false);
        if (module == null)
        {
            plugin.logger().error("Failed to load module for {}", plugin.metadata().name());
        }
    }

    public void onStarted(StartedEngineEvent<Server> event)
    {
        Object module = mm.getModule(this.module);
        for (Field field : ModuleManager.getAnnotatedFields(module, InjectService.class))
        {
            Optional<?> provided = Sponge.server().serviceProvider().provide(field.getType());
            if (!provided.isPresent())
            {
                plugin.logger().warn("Missing Service");
            }
            else
            {
                try
                {
                    field.setAccessible(true);
                    field.set(module, provided.get());
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public void onRegisterCommand(final RegisterCommandEvent<Command.Parameterized> event)
    {
        this.mm.registerCommands(event, this.plugin, this.instance);
    }

    public abstract String sourceVersion();
}
