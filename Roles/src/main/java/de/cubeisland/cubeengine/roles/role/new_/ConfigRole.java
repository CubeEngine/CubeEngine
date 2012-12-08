package de.cubeisland.cubeengine.roles.role.new_;

import de.cubeisland.cubeengine.roles.role.config.RoleConfig;
import java.util.List;

public class ConfigRole extends Role
{
    private RoleConfig config;

    public ConfigRole(RoleConfig config, List<Role> parentRoles, boolean isGlobal)
    {
        super(config.roleName, config.priority, config.perms, parentRoles, config.metadata, isGlobal);
    }
}
