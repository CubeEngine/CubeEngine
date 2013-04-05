package de.cubeisland.cubeengine.log.action.logaction.interact;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleExitEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;


public class VehicleExit extends SimpleLogActionType
{
    public VehicleExit(Log module)
    {
        super(module, 0x54, "vehicle-exit");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(final VehicleExitEvent event)
    {
        if (this.isActive(event.getVehicle().getWorld()))
        {
            this.logSimple(event.getVehicle().getLocation(),event.getExited(),event.getVehicle(),null);
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.getCauserUser() == null)
        {
            user.sendTranslated("%s&6%s &aexited a &6%s%s&a!",
                                time, this.getPrettyName(logEntry.getCauserEntity()),
                                this.getPrettyName(logEntry.getEntity()),loc);
        }
        else
        {
            user.sendTranslated("%s&2%s &aexited a &6%s%s&a!",
                                time, logEntry.getCauserUser().getDisplayName(),
                                this.getPrettyName(logEntry.getEntity()),loc);
        }
    }
}
