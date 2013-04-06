package de.cubeisland.cubeengine.log.action.logaction.block.explosion;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Type.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Type.ENTITY;
import static de.cubeisland.cubeengine.log.action.ActionType.Type.PLAYER;

/**
 * Enderdragon-Explosions
 * <p>Events: {@link ExplodeActionType}</p>
 */
public class EnderdragonExplode extends BlockActionType
{
    public EnderdragonExplode(Log module)
    {
        super(module, "eenderdragon-explode", BLOCK, ENTITY, PLAYER);
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.hasAttached())
        {
            int amount = logEntry.getAttached().size()+1;
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aAn enderdragon attacking &2%s &achanged the integrity of &6%dx %s&a%s!",
                                    time, logEntry.getCauserUser().getDisplayName(), amount,
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aAn enderdragon changed the integrity of &6%dx %s&a%s!",
                                    time,amount,
                                    logEntry.getOldBlock(),loc);
            }
        }
        else
        {
            if (logEntry.hasCauserUser())
            {
                user.sendTranslated("%s&aAn enderdragon attacking &2%s &achanged the integrity of &6%s&a%s!",
                                    time, logEntry.getCauserUser().getDisplayName(),
                                    logEntry.getOldBlock(),loc);
            }
            else
            {
                user.sendTranslated("%s&aAn enderdragon changed the integrity of &6%s&a%s!",
                                    time,logEntry.getOldBlock(),loc);
            }
        }
    }
}
