package de.cubeisland.cubeengine.log.action.logaction.block.entity;

import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreakDoorEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.storage.ActionType.ENTITY_BREAK;
import static org.bukkit.Material.AIR;

public class EntityBreak extends BlockActionType
{
    public EntityBreak(Log module)
    {
        super(module, 0x06, "entity-break");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreakDoor(final EntityBreakDoorEvent event)
    {
        if (this.isActive(event.getEntity().getWorld()))
        {
            BlockState state = event.getBlock().getState();
            state = this.adjustBlockForDoubleBlocks(state);
            this.logBlockChange(state.getLocation(),event.getEntity(),BlockData.of(state),AIR,null);
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&aA &6%s &adestroyed &6%s&a%s!",
                            time,
                            this.getPrettyName(logEntry.getCauserEntity()),
                            logEntry.getOldBlock(),
                            loc);
    }
}
