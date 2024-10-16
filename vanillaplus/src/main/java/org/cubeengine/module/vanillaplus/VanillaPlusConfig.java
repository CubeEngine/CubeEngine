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

import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;


@SuppressWarnings("all")
public class VanillaPlusConfig extends ReflectedYaml
{
    public Fixes fix;
    public Improvments improve;
    public Additions add;

    public static class Fixes implements Section
    {
        @Comment("Allows using & followed by the appropriate color-code or style-code to write colored signs")
        public boolean styledSigns = true;

        @Comment("Retains fly-mode on login")
        public boolean safeLoginFly = true;

        @Comment("Teleports to spawn when logging in outside of worldborder")
        public boolean safeLoginBorder = true;

        @Comment("Right click on a painting allows switching the painting with the mouse wheel")
        public boolean paintingSwitcher = true;

        public int paintingSwitcherMaxDistance = 10;

        @Comment("Shows the tamer of an animal when rightclicking on it")
        public boolean showTamer = true;
    }

    public static class Improvments implements Section
    {
        @Comment("Adds /remove as an alternative for killing non living entities only")
        public boolean commandRemove = true;
        public int commandRemoveDefaultRadius = 20;

        @Comment("Also /butcher for living entities only")
        public boolean commandButcher = true;
        public int commandButcherDefaultRadius = 20;

        @Comment("Improves /clear and adds some aliases")
        public boolean commandClearinventory = true;
        @Comment("Improves /difficulty")
        public boolean commandDifficulty = true;
        @Comment("Improves /gamemode e.g. allowing to toggle between survival and creative")
        public boolean commandGamemode = true;

        @Comment("Improves /give\n"
            + "Adds an alias /item to give an item to yourself\n"
            + "Adds /more to refill itemstacks\n"
            + "Adds /stack to stack similar items together")
        public boolean commandItem = true;

        @Comment("Allows stacking tools and other items up to 64 even when they usually do not stack that high")
        public boolean commandStackTools = false;

        @Comment("Improves /enchant\n"
            + "Adds /rename and /lore to allow colored ItemNames and Lore\n"
            + "Adds /headchange to change any head to a player-head of your choice\n"
            + "Adds /repair to refill the durability of tools")
        public boolean commandItemModify = true;

        @Comment("Improves /kill\n"
            + "Adds an alias /suicide to kill yourself")
        public boolean commandKill = true;

        @Comment("Improves /op and /deop")
        public boolean commandOp = true;

        @Comment("Improves /list")
        public boolean commandList = true;

        @Comment("Improves /save-all /save-on /save-off")
        public boolean commandSave = true;

        @Comment("Improves /stop including a kick reason for the players")
        public boolean commandStop = true;

        public String commandStopDefaultMessage = "Server is shutting down.";

        @Comment("Improves /time with per World time and adds /ptime for per Player time")
        public boolean commandTime = true;

        @Comment("Improves /weather with per World weather and adds /pweather for per Player weather")
        public boolean commandWeather = true;

        @Comment("Improves /whitelist")
        public boolean commandWhitelist = true;

        @Comment("Improves /worldborder")
        public boolean commandBorderEnable = true;

        @Comment("Maximum world border diameter for generation")
        public int commandBorderMax = 5000;
    }

    public static class Additions implements Section
    {
        @Comment("Adds /god")
        public boolean commandGod = true;

        @Comment("Adds /heal")
        public boolean commandHeal = true;

        @Comment("Adds /biome, /seed, /compass, /depth, /getPos, /near, /ping, /lag, /listWorlds")
        public boolean commandsInformation = true;

        public int commandNearDefaultRadius = 20;

        @Comment("Adds /invsee")
        public boolean commandInvsee = true;

        @Comment("Adds /walkspeed and /fly")
        public boolean commandsMovement = true;

        @Comment("Adds /feed and /starve")
        public boolean commandsFood = true;

        @Comment("Adds /seen and /whois")
        public boolean commandsPlayerInformation = true;

        @Comment("Adds /plugins and /version")
        public boolean commandsPlugins = true;

        @Comment("Adds /stash")
        public boolean commandStash = true;

        @Comment("Adds /sudo")
        public boolean commandSudo = true;

        @Comment("Adds /unlimited")
        public boolean commandUnlimited = true;

        @Comment("Grants unlimited food for players with the permission")
        public boolean unlimitedFood = true;
    }
}
