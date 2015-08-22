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
package de.cubeisland.engine.module.selector;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.permission.PermissionDescription;

@ModuleInfo(name = "Selector", description = "Select Areas")
public class Selector extends Module
{
    @Inject private PermissionManager pm;
    @Inject private CommandManager cm;
    @Inject private Game game;
    private PermissionDescription selectPerm;

    @Enable
    public void onEnable()
    {
        selectPerm = pm.register(this, "use-wand", "Allows using the selector wand",null);
        cm.addCommands(this, new SelectorCommand(game));
    }

    public PermissionDescription getSelectPerm()
    {
        return selectPerm;
    }
}
