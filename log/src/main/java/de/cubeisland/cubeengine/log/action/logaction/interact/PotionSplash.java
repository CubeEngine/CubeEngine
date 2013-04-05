package de.cubeisland.cubeengine.log.action.logaction.interact;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PotionSplash extends SimpleLogActionType
{
    public PotionSplash(Log module)
    {
        super(module, 0x55, "potion-splash");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event)
    {
        if (this.isActive(event.getPotion().getWorld()))
        {
            LivingEntity livingEntity = event.getPotion().getShooter();
            String additionalData = this.serializePotionLog(event);
            this.logSimple(livingEntity,additionalData);
        }
    }

    public String serializePotionLog(PotionSplashEvent event)
    {
        ObjectNode json = this.om.createObjectNode();
        ArrayNode effects = json.putArray("effects");
        for (PotionEffect potionEffect : event.getPotion().getEffects())
        {
            ArrayNode effect = effects.addArray();
            effects.add(effect);
            effect.add(potionEffect.getType().getName());
            effect.add(potionEffect.getAmplifier());
            effect.add(potionEffect.getDuration());
        }
        if (!event.getAffectedEntities().isEmpty())
        {
            json.put("amount", event.getAffectedEntities().size());
            ArrayNode affected = json.putArray("affected");
            for (LivingEntity livingEntity : event.getAffectedEntities())
            {
                if (livingEntity instanceof Player)
                {
                    User user = um.getExactUser((Player)livingEntity);
                    affected.add(user.key);
                }
                else
                {
                    affected.add(-livingEntity.getType().getTypeId());
                }
            }
        }
        return json.toString();
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendMessage("Potion stuff happened!");//TODO
    }
}
