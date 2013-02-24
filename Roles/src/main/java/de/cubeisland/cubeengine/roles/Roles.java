package de.cubeisland.cubeengine.roles;

import de.cubeisland.cubeengine.core.module.Module;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.convert.Convert;
import de.cubeisland.cubeengine.roles.commands.*;
import de.cubeisland.cubeengine.roles.role.RoleManager;
import de.cubeisland.cubeengine.roles.role.RolesEventHandler;
import de.cubeisland.cubeengine.roles.role.config.*;
import de.cubeisland.cubeengine.roles.storage.AssignedRoleManager;
import de.cubeisland.cubeengine.roles.storage.UserMetaDataManager;
import de.cubeisland.cubeengine.roles.storage.UserPermissionsManager;

public class Roles extends Module
{
    private RolesConfig config;
    private RoleManager manager;
    private AssignedRoleManager dbManager;
    private UserMetaDataManager dbUserMeta;
    private UserPermissionsManager dbUserPerm;
    private RolesAPI api;

    public Roles()
    {
        Convert.registerConverter(PermissionTree.class, new PermissionTreeConverter(this));
        Convert.registerConverter(Priority.class, new PriorityConverter());
        Convert.registerConverter(RoleMirror.class, new RoleMirrorConverter(this));
    }

    @Override
    public void onEnable()
    {
        this.dbManager = new AssignedRoleManager(this.getDatabase());
        this.dbUserMeta = new UserMetaDataManager(this.getDatabase());
        this.dbUserPerm = new UserPermissionsManager(this.getDatabase());
        this.manager = new RoleManager(this);

        this.registerCommand(new RoleCommands(this));
        this.registerCommand(new RoleManagementCommands(this), "roles");
        this.registerCommands(new RoleInformationCommands(this), "roles", "role");
        this.registerCommand(new UserManagementCommands(this), "roles");
        this.registerCommands(new UserInformationCommands(this), "roles", "user");
        this.registerCommand(new ModuleManagementCommands(this), "roles");

        this.getEventManager().registerListener(this, new RolesEventHandler(this));
        //init on FinishedLoadModulesEvent

        this.api = new RolesAPI(this);
    }

    @Override
    public void onDisable()
    {
        for (User user : this.getUserManager().getLoadedUsers())
        {
            user.clearAttributes(this);
        }
    }

    public RolesConfig getConfiguration()
    {
        return this.config;
    }

    public AssignedRoleManager getDbManager()
    {
        return dbManager;
    }

    public UserMetaDataManager getDbUserMeta()
    {
        return dbUserMeta;
    }

    public UserPermissionsManager getDbUserPerm()
    {
        return dbUserPerm;
    }

    public RoleManager getManager()
    {
        return manager;
    }

    public RolesAPI getApi()
    {
        return api;
    }

    public RolesConfig getConfig()
    {
        return config;
    }
}
