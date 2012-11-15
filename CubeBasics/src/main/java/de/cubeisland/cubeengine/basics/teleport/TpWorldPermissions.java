package de.cubeisland.cubeengine.basics.teleport;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.core.permission.Permission;
import gnu.trove.map.hash.THashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionDefault;

/**
 * Dynamicly registerd Permissions for each world.
 */
public class TpWorldPermissions
{
    private static Map<String, Permission> permissions = new THashMap<String, Permission>();

    public Permission[] getPermissions()
    {
        return permissions.values().toArray(new Permission[0]);
    }

    public static Permission getPermission(String world)
    {
        return permissions.get(world);
    }

    public TpWorldPermissions(Basics basics)
    {
        for (final World world : basics.getCore().getServer().getWorlds())
        {
            permissions.put(world.getName(), new Permission()
            {
                private String permission = "cubeengine.basics.command.tpworld." + world.getName();

                public boolean isAuthorized(Permissible player)
                {
                    return player.hasPermission(permission);
                }

                public String getPermission()
                {
                    return this.permission;
                }

                public PermissionDefault getPermissionDefault()
                {
                    return PermissionDefault.OP;
                }
            });
        }
    }
}
