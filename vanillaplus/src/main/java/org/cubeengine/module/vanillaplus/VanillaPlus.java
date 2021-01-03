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
package org.cubeengine.module.vanillaplus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.AnnotationCommandBuilder;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.matcher.EnchantMatcher;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.matcher.TimeMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.vanillaplus.addition.FoodCommands;
import org.cubeengine.module.vanillaplus.addition.GodCommand;
import org.cubeengine.module.vanillaplus.addition.HealCommand;
import org.cubeengine.module.vanillaplus.addition.InformationCommands;
import org.cubeengine.module.vanillaplus.addition.InvseeCommand;
import org.cubeengine.module.vanillaplus.addition.MovementCommands;
import org.cubeengine.module.vanillaplus.addition.PlayerInfoCommands;
import org.cubeengine.module.vanillaplus.addition.PluginCommands;
import org.cubeengine.module.vanillaplus.addition.StashCommand;
import org.cubeengine.module.vanillaplus.addition.SudoCommand;
import org.cubeengine.module.vanillaplus.addition.UnlimitedFood;
import org.cubeengine.module.vanillaplus.addition.UnlimitedItems;
import org.cubeengine.module.vanillaplus.fix.ColoredSigns;
import org.cubeengine.module.vanillaplus.fix.FlymodeFixListener;
import org.cubeengine.module.vanillaplus.fix.PaintingListener;
import org.cubeengine.module.vanillaplus.fix.SafeLoginData;
import org.cubeengine.module.vanillaplus.fix.SpawnFixListener;
import org.cubeengine.module.vanillaplus.fix.TamedListener;
import org.cubeengine.module.vanillaplus.improvement.BorderCommands;
import org.cubeengine.module.vanillaplus.improvement.ClearInventoryCommand;
import org.cubeengine.module.vanillaplus.improvement.DifficultyCommand;
import org.cubeengine.module.vanillaplus.improvement.GameModeCommand;
import org.cubeengine.module.vanillaplus.improvement.ItemCommands;
import org.cubeengine.module.vanillaplus.improvement.ItemModifyCommands;
import org.cubeengine.module.vanillaplus.improvement.KillCommands;
import org.cubeengine.module.vanillaplus.improvement.OpCommands;
import org.cubeengine.module.vanillaplus.improvement.PlayerListCommand;
import org.cubeengine.module.vanillaplus.improvement.SaveCommands;
import org.cubeengine.module.vanillaplus.improvement.StopCommand;
import org.cubeengine.module.vanillaplus.improvement.TimeCommands;
import org.cubeengine.module.vanillaplus.improvement.WeatherCommands;
import org.cubeengine.module.vanillaplus.improvement.WhitelistCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.ButcherCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.RemoveCommands;
import org.cubeengine.module.vanillaplus.improvement.summon.SpawnMobCommand;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;

/**
 * A module to improve vanilla commands:
 *
 * /clear 	Clears items from player inventory. {@link ClearInventoryCommand#clearinventory}
 * ??? /deop 	Revoke operator status from a player.
 * /difficulty 	Sets the difficulty level. {@link DifficultyCommand#difficulty}
 * ??? /effect 	Add or remove status effects.
 * /enchant 	Enchants a player item. {@link ItemModifyCommands#enchant}
 * /execute (sudo)	Executes another command. {@link SudoCommand#sudo}
 * /gamemode 	Sets a player's game mode. {@link GameModeCommand#gamemode}
 * /give 	Gives an item to a player. {@link ItemCommands#give},{@link ItemCommands#item}
 * ??? /help 	Provides help for commands.
 * /kill (butcher,remove,removeALl)   Kills entities (players, mobs, items, etc.). {@link ButcherCommand#butcher},{@link RemoveCommands#remove},{@link RemoveCommands#removeAll}
 * /list 	Lists players on the server. {@link PlayerListCommand#list}
 * ??? /op 	Grants operator status to a player.
 * ??? /replaceitem 	Replaces items in inventories.
 * /save-all 	Saves the server to disk. {@link SaveCommands#saveall}
 * ??? /save-off 	Disables automatic server saves.
 * ??? /save-on 	Enables automatic server saves.
 * configure say color??? /say 	Displays a message to multiple players.
 * /seed 	Displays the world seed.
 * ??? /setidletimeout 	Sets the time before idle players are kicked.
 * ??? /spreadplayers 	Teleports entities to random locations.
 * /stop 	Stops a server.
 * /summon (spawnmob) Summons an entity. {@link SpawnMobCommand#spawnMob}
 * /time 	Changes or queries the world's game time. {@link TimeCommands#time}
 * ??? /toggledownfall 	Toggles the weather.
 * /weather 	Sets the weather. {@link WeatherCommands#weather}
 * /whitelist 	Manages server whitelist. {@link WhitelistCommand}
 * ??? /xp 	Adds or removes player experience.
 *
 * Extra commands:
 *
 * /plugins {@link PluginCommands#plugins}
 * /version {@link PluginCommands#version}
 * /pweather {@link WeatherCommands#pweather}
 * /ptime {@link TimeCommands#ptime}
 * /more {@link ItemCommands#more}
 * /stack {@link ItemCommands#stack}
 * /rename {@link ItemModifyCommands#rename}
 * /headchange {@link ItemModifyCommands#headchange}
 * /repair {@link ItemModifyCommands#repair}
 * /kill (for players) {@link KillCommands#kill}
 * /suicide (kill self) {@link KillCommands#suicide}
 */

@Singleton
@Module
public class VanillaPlus
{
    @Inject private I18n i18n;
    @Inject private MaterialMatcher mm;
    @Inject private EnchantMatcher em;
    @Inject private EntityMatcher enm;
    @Inject private TimeMatcher tm;
    @Inject private ModuleManager momu;
    @Inject private TaskManager tam;
    @Inject private PermissionManager pm;
    @Inject private Broadcaster bc;
    @Inject private InventoryGuardFactory invGuard;
    @ModuleConfig private VanillaPlusConfig config;
    @Inject private EventManager evm;
    @Inject private StringMatcher sm;
    @Inject private AnnotationCommandBuilder commandBuilder;
    @Inject private PluginContainer plugin;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        // Fixes
        if (config.fix.styledSigns)
        {
            evm.registerListener(VanillaPlus.class, new ColoredSigns(pm));
        }
        if (config.fix.safeLoginFly)
        {
            evm.registerListener(VanillaPlus.class, new FlymodeFixListener());
        }
        if (config.fix.safeLoginBorder)
        {
            evm.registerListener(VanillaPlus.class, new SpawnFixListener());
        }
        if (config.fix.paintingSwitcher)
        {
            evm.registerListener(VanillaPlus.class, new PaintingListener(pm, this, i18n));
        }
        if (config.fix.showTamer)
        {
            evm.registerListener(VanillaPlus.class, new TamedListener(i18n));
        }
    }

    public void onRegisterCommand(RegisterCommandEvent<Parameterized> event) {
        // additions
        if (config.add.commandGod)
        {
            momu.registerCommands(event, plugin, this, GodCommand.class);
        }
        if (config.add.commandHeal)
        {
            momu.registerCommands(event, plugin, this, HealCommand.class);
        }
        if (config.add.commandsInformation)
        {
            momu.registerCommands(event, plugin, this, InformationCommands.class);
        }
        if (config.add.commandInvsee)
        {
            momu.registerCommands(event, plugin, this, InvseeCommand.class);
        }
        if (config.add.commandsMovement)
        {
            momu.registerCommands(event, plugin, this, MovementCommands.class);
        }
        if (config.add.commandsFood)
        {
            momu.registerCommands(event, plugin, this, FoodCommands.class);
        }
        if (config.add.commandsPlayerInformation)
        {
            momu.registerCommands(event, plugin, this, PlayerInfoCommands.class);
        }
        if (config.add.commandsPlugins)
        {
            momu.registerCommands(event, plugin, this, PluginCommands.class);
        }
        if (config.add.commandStash)
        {
            momu.registerCommands(event, plugin, this, StashCommand.class);
        }
        if (config.add.commandSudo)
        {
            momu.registerCommands(event, plugin, this, SudoCommand.class);
        }
        if (config.add.commandUnlimited)
        {
            final UnlimitedItems cmd = momu.registerCommands(event, plugin, this, UnlimitedItems.class);
            evm.registerListener(VanillaPlus.class, cmd);
        }
        if (config.add.unlimitedFood)
        {
            new UnlimitedFood(pm, tam);
        }

        // improvements
        if (config.improve.commandRemove)
        {
            momu.registerCommands(event, plugin, this, RemoveCommands.class);
        }
        if (config.improve.commandButcher)
        {
            momu.registerCommands(event, plugin, this, ButcherCommand.class);
        }
        if (config.improve.commandSummon)
        {
            momu.registerCommands(event, plugin, this, SpawnMobCommand.class);
        }
        if (config.improve.commandClearinventory)
        {
            momu.registerCommands(event, plugin, this, ClearInventoryCommand.class);
        }
        if (config.improve.commandDifficulty)
        {
            momu.registerCommands(event, plugin, this, DifficultyCommand.class);
        }
        if (config.improve.commandGamemode)
        {
            momu.registerCommands(event, plugin, this, GameModeCommand.class);
        }
        if (config.improve.commandItem)
        {
            momu.registerCommands(event, plugin, this, ItemCommands.class);
        }
        if (config.improve.commandItemModify)
        {
            momu.registerCommands(event, plugin, this, ItemModifyCommands.class);
        }
        if (config.improve.commandKill)
        {
            momu.registerCommands(event, plugin, this, KillCommands.class);
        }
        if (config.improve.commandOp)
        {
            momu.registerCommands(event, plugin, this, OpCommands.class);
        }
        if (config.improve.commandList)
        {
            momu.registerCommands(event, plugin, this, PlayerListCommand.class);
        }
        if (config.improve.commandSave)
        {
            momu.registerCommands(event, plugin, this, SaveCommands.class);
        }
        if (config.improve.commandStop)
        {
            momu.registerCommands(event, plugin, this, StopCommand.class);
        }
        if (config.improve.commandTime)
        {
            momu.registerCommands(event, plugin, this, TimeCommands.class);
        }
        if (config.improve.commandWeather)
        {
            momu.registerCommands(event, plugin, this, WeatherCommands.class);
        }
        if (config.improve.commandWhitelist)
        {
            momu.registerCommands(event, plugin, this, WhitelistCommand.class);
        }
        if (config.improve.commandBorderEnable)
        {
            momu.registerCommands(event, plugin, this, BorderCommands.class);
        }

    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        if (config.fix.safeLoginFly)
        {
            SafeLoginData.register(event);
        }
    }


    public VanillaPlusConfig getConfig()
    {
        return config;
    }

    /*
    TODO onlinemode cmd

    @Command(desc = "Shows the online mode")
    public void onlinemode(CommandSource context)
    {
        if (Sponge.getServer().getOnlineMode())
        {
            i18n.sendTranslated(context, POSITIVE, "The Server is running in online mode");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The Server is running in offline mode");
        /* Changing online mode is no longer supported on a running server
        BukkitUtils.setOnlineMode(newState);
        if (newState)
        {
            context.sendTranslated(POSITIVE, "The server is now in online-mode.");
        }
        else
        {
            context.sendTranslated(POSITIVE, "The server is not in offline-mode.");
        }
        *//*
}

     @Command(alias = "finduser", desc = "Searches for a user in the database")
    public void searchuser(CommandContext context, @Reader(FindUserReader.class) @Desc("The name to search for") User name)
    {
        if (name.getName().equalsIgnoreCase(context.getString(0)))
        {
            i18n.sendTranslated(context.getSource(), POSITIVE, "Matched exactly! User: {user}", name);
            return;
        }
        i18n.sendTranslated(context.getSource(), POSITIVE, "Matched not exactly! User: {user}", name);
    }
    */
}
