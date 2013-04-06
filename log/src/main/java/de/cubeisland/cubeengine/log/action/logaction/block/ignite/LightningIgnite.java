package de.cubeisland.cubeengine.log.action.logaction.block.ignite;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Type.*;

/**
 * lightning-ignite
 * <p>Events: {@link IgniteActionType}</p>
 */
public class LightningIgnite extends BlockActionType
{
    public LightningIgnite(Log module)
    {
        super(module, "lightning-ignite", BLOCK, ENVIRONEMENT);
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&aFire got set by a lightning strike%s&a!",time,loc);
    }
}
