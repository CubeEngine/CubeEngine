package de.cubeisland.cubeengine.log.action.logaction.block.explosion;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

public class FireballExplode extends BlockActionType
{
    public FireballExplode(Log module)
    {
        super(module, 0x13, "tnt-explode");
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.hasAttached())
        {
            int amount = logEntry.getAttached().size()+1;
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aA Fireball flying towards &2%s &ablasted away &6%dx %s&a%s!",
                                    time, logEntry.getCauserUser().getDisplayName(), amount,
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aA Fireball blasted away &6%dx %s&a%s!",
                                    time, amount,
                                    logEntry.getOldBlock(),loc);
            }
        }
        else
        {
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aA Fireball flying towards &2%s &ablasted away &6%s&a%s!",
                                    time, logEntry.getCauserUser().getDisplayName(),
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aA Fireball blasted away &6%s&a%s!",
                                    time,logEntry.getOldBlock(),loc);
            }
        }
    }
}
