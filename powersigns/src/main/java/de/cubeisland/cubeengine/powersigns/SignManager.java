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
package de.cubeisland.cubeengine.powersigns;

import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.powersigns.signtype.LiftSign;
import de.cubeisland.cubeengine.powersigns.signtype.SignType;
import de.cubeisland.cubeengine.powersigns.signtype.SignTypeInfo;
import de.cubeisland.cubeengine.powersigns.storage.PowerSignModel;
import de.cubeisland.cubeengine.powersigns.storage.PowerSignStorage;

import gnu.trove.map.hash.THashMap;

public class SignManager implements Listener
{
    private Map<String,SignType> registerdSignTypes = new THashMap<String, SignType>();

    private Map<Location,PowerSign> loadedPowerSigns = new THashMap<Location, PowerSign>();
    protected Powersigns module;

    public PowerSignStorage getStorage()
    {
        return storage;
    }

    private PowerSignStorage storage;

    public SignManager(Powersigns module)
    {
        this.module = module;
        this.storage = new PowerSignStorage(module);
    }

    public void init()
    {
        this.module.getCore().getEventManager().registerListener(this.module,this);
        this.registerSignType(new LiftSign());
        Set<PowerSignModel> powerSignModels = this.storage
            .loadFromLoadedChunks(this.module.getCore().getWorldManager().getWorlds());
        for (PowerSignModel powerSignModel : powerSignModels)
        {
            SignType signType = this.registerdSignTypes.get(powerSignModel.PSID);
            SignTypeInfo info = signType.createInfo(powerSignModel);
            if (info == null)
            {
                continue;
            }
            PowerSign powerSign = new PowerSign(signType,info);
            this.loadedPowerSigns.put(powerSign.getLocation(),powerSign);
        }
    }

    public SignManager registerSignType(SignType<?,?> signType)
    {
        if (registerdSignTypes.put(signType.getPSID(),signType) != null)
        {
            throw new IllegalStateException("Already registered String!" + signType.getPSID());
        }
        this.module.getLog().log(LogLevel.DEBUG, "Registered SignType: " + signType.getPSID());
        for (String name : signType.getNames().keySet())
        {
            if (registerdSignTypes.put(name,signType) != null)
            {
                throw new IllegalStateException("Already registered String! "+ name);
            }
        }
        signType.init(this.module);
        return this;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event)
    {
        Set<PowerSignModel> powerSignModels = this.storage
            .loadFromChunk(event.getChunk());
        for (PowerSignModel powerSignModel : powerSignModels)
        {
            SignType signType = this.registerdSignTypes.get(powerSignModel.PSID);
            SignTypeInfo info = signType.createInfo(powerSignModel);
            PowerSign powerSign = new PowerSign(signType,info);
            this.loadedPowerSigns.put(powerSign.getLocation(),powerSign);
        }
    }

    public void onChunkUnload(ChunkLoadEvent event)
    {

    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        if (event.getLine(1).startsWith("[") && event.getLine(1).endsWith("]"))
        {
            String idLine = event.getLine(1);
            idLine = idLine.substring(1,idLine.length()-1);
            System.out.print("IdentifierLine: "+idLine);
            idLine = idLine.toLowerCase();
            SignType signType = registerdSignTypes.get(idLine);
            if (signType == null)
            {
                return; //not valid -> ignore
            }
            User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer());
            PowerSign powerSign = new PowerSign(signType,event.getBlock().getLocation(),user,event.getLines());
            this.loadedPowerSigns.put(powerSign.getLocation(),powerSign);
            powerSign.updateSignText();
            powerSign.getSignTypeInfo().saveData();
            event.setCancelled(true);
        }
        //TODO detect new signs
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event)
    {
        if (event.getClickedBlock() == null
            || event.getClickedBlock().getType().equals(Material.AIR)
            || !(event.getClickedBlock().getType().equals(Material.WALL_SIGN)
            || event.getClickedBlock().equals(Material.SIGN_POST)))
        {
            return;
        }
        Location location = event.getClickedBlock().getLocation();
        PowerSign powerSign = this.loadedPowerSigns.get(location);
        User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer());
        if (powerSign == null)
        {
            String psid = getPSID(location);
            if (psid == null)
            {
                event.getPlayer().sendMessage("NO SIGN HERE");
                //TODO check if it could be a PowerSign
                //TODO create the sign if user has permission
                return;
            }
            //TODO load in sign from nbt!
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
        {
            if (event.getPlayer().isSneaking())
            {
                event.setCancelled(powerSign.getSignType().onSignShiftRightClick(user,powerSign));
            }
            else
            {
                event.setCancelled(powerSign.getSignType().onSignRightClick(user, powerSign));
            }
        }
        else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
        {
            if (event.getPlayer().isSneaking())
            {
                event.setCancelled(powerSign.getSignType().onSignShiftLeftClick(user, powerSign));
            }
            else
            {
                event.setCancelled(powerSign.getSignType().onSignLeftClick(user, powerSign));
            }
        }
    }

    public String getPSID(Location location)
    {
        PowerSign powerSign = this.loadedPowerSigns.get(location);
        if (powerSign == null) return null;
        return powerSign.getSignType().getPSID();
    }

    public PowerSign getPowerSign(Location location)
    {
        return this.loadedPowerSigns.get(location);
    }

}