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
package de.cubeisland.engine.module.basics;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.basics.command.general.ColoredSigns;
import de.cubeisland.engine.module.fixes.FixListener;
import de.cubeisland.engine.module.basics.command.general.GeneralsListener;
import de.cubeisland.engine.module.basics.command.general.InformationCommands;
import de.cubeisland.engine.module.basics.command.general.LagTimer;
import de.cubeisland.engine.module.basics.command.general.ListCommand;
import de.cubeisland.engine.module.basics.command.general.PlayerCommands;
import de.cubeisland.engine.module.basics.command.general.RolesListCommand;
import de.cubeisland.engine.module.basics.command.moderation.DoorCommand;
import de.cubeisland.engine.module.basics.command.moderation.InventoryCommands;
import de.cubeisland.engine.module.basics.command.moderation.ItemCommands;
import de.cubeisland.engine.module.basics.command.moderation.PaintingListener;
import de.cubeisland.engine.module.basics.command.moderation.TimeControlCommands;
import de.cubeisland.engine.module.basics.command.moderation.WorldControlCommands;
import de.cubeisland.engine.module.vanillaplus.SpawnMobCommand;
import de.cubeisland.engine.module.core.filesystem.FileManager;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.InventoryGuardFactory;
import de.cubeisland.engine.module.core.util.Profiler;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.service.ban.BanManager;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;

@ModuleInfo(name = "Basics", description = "Basic Functionality")
public class Basics extends Module
{
    private BasicsConfiguration config;
    private LagTimer lagTimer;


    public BasicsPerm perms()
    {
        return perms;
    }

    private BasicsPerm perms;
    @Inject private Database db;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private UserManager um;
    @Inject private FileManager fm;
    @Inject private WorldManager wm;
    @Inject private TaskManager taskManager;
    @Inject private BanManager banManager;
    @Inject private PermissionManager pm;
    @Inject private InventoryGuardFactory invGuard;
    @Inject private MaterialMatcher materialMatcher;
    @Inject private Game game;

    @Enable
    public void onEnable()
    {
        this.config = fm.loadConfig(this, BasicsConfiguration.class);
        perms = new BasicsPerm(this, wm, pm);
        um.addDefaultAttachment(BasicsAttachment.class, this);
        em.registerListener(this, new ColoredSigns(this));
        cm.addCommands(cm, this, new InformationCommands(this, wm, materialMatcher));
        cm.addCommands(cm, this, new PlayerCommands(this, um, em, taskManager, cm, banManager));
        em.registerListener(this, new GeneralsListener(this, um));
        cm.addCommands( this, new InventoryCommands(this, invGuard));
        cm.addCommands( this, new ItemCommands(this));
        cm.addCommands(cm, this, new TimeControlCommands(this, taskManager, wm));
        cm.addCommands(cm, this, new WorldControlCommands(this));
        em.registerListener(this, new PaintingListener(this, um));
        em.registerListener(this, new FixListener(this));
        this.lagTimer = new LagTimer(this);
        cm.addCommands(cm, this, new DoorCommand(this));
    }

    public BasicsConfiguration getConfiguration()
    {
        return this.config;
    }

    public LagTimer getLagTimer()
    {
        return this.lagTimer;
    }
}
