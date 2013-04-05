package de.cubeisland.cubeengine.log.action.logaction.block.interaction;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.LogActionType;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

public class DoorUse extends BlockActionType
{
    public DoorUse(Log module)
    {
        super(module, 0x48, "door-use");
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (!((logEntry.getOldBlock().data & 0x4) == 0x4))
        {
            user.sendTranslated("%s&2%s &aopened the &6%s%s&a!",
                                time,logEntry.getCauserUser().getDisplayName(),
                                logEntry.getOldBlock(),loc);
        }
        else
        {
            user.sendTranslated("%s&2%s &aclosed the &6%s%s&a!",
                                time,logEntry.getCauserUser().getDisplayName(),
                                logEntry.getOldBlock(),loc);
        }
    }
}
