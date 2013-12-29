/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.roles.role;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;

import de.cubeisland.engine.roles.storage.UserMetaData;
import de.cubeisland.engine.roles.storage.UserPermission;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.roles.storage.TableData.TABLE_META;
import static de.cubeisland.engine.roles.storage.TablePerm.TABLE_PERM;
import static de.cubeisland.engine.roles.storage.TableRole.TABLE_ROLE;

public class UserDatabaseStore extends ResolvedDataHolder
{
    private final World world;
    private final RolesManager manager;
    private final RolesAttachment attachment;

    public UserDatabaseStore(RolesAttachment attachment, RoleProvider provider, RolesManager manager, World world)
    {
        super(manager, provider);
        this.attachment = attachment;
        this.manager = manager;
        this.world = world;

        this.loadFromDatabase();
    }

    private HashSet<String> roles;
    private Map<String, Boolean> permissions;
    private Map<String, String> metadata;

    @Override
    public String getName()
    {
        return this.attachment.getHolder().getName();
    }

    protected void loadFromDatabase()
    {
        UInteger assignedRolesMirror = UInteger.valueOf(this.manager.assignedRolesMirrors.get(this.getMainWorldID()));
        Result<Record1<String>> roleFetch = manager.dsl.select(TABLE_ROLE.ROLENAME).from(TABLE_ROLE)
                    .where(TABLE_ROLE.USERID.eq(this.getUserID()),
                           TABLE_ROLE.WORLDID.eq(assignedRolesMirror)).fetch();
        this.roles = new HashSet<>(roleFetch.getValues(TABLE_ROLE.ROLENAME, String.class));
        UInteger userDataMirror = UInteger.valueOf(this.manager.assignedUserDataMirrors.get(this.getMainWorldID()));

        Result<Record2<String,Boolean>> permFetch = manager.dsl.select(TABLE_PERM.PERM, TABLE_PERM.ISSET).from(TABLE_PERM)
               .where(TABLE_PERM.USERID.eq(this.getUserID()),
                      TABLE_PERM.WORLDID.eq(userDataMirror)).fetch();
        this.permissions = new HashMap<>();
        for (Record2<String, Boolean> perm : permFetch)
        {
            this.permissions.put(perm.value1(), perm.value2());
        }
        Result<Record2<String,String>> metaFetch = manager.dsl.select(TABLE_META.KEY, TABLE_META.VALUE).from(TABLE_META)
                                                        .where(TABLE_META.USERID.eq(this.getUserID()),
                                                               TABLE_META.WORLDID.eq(userDataMirror)).fetch();
        this.metadata = new HashMap<>();
        for (Record2<String, String> meta : metaFetch)
        {
            this.metadata.put(meta.value1(), meta.value1());
        }
    }

    private UInteger getUserID()
    {
        return this.attachment.getHolder().getEntity().getKey();
    }

    @Override
    public PermissionType setPermission(String perm, PermissionType set)
    {
        if (set == PermissionType.NOT_SET)
        {
            manager.dsl.delete(TABLE_PERM).where(TABLE_PERM.USERID.eq(this.getUserID()),
                                                 TABLE_PERM.WORLDID.eq(this.getMirrorWorldId()),
                                                 TABLE_PERM.PERM.eq(perm)).execute();
        }
        else
        {
            UserPermission userPerm = manager.dsl.newRecord(TABLE_PERM).newPerm(this.getUserID(), this.getMirrorWorldId(), perm, set);
            manager.dsl.insertInto(TABLE_PERM).set(userPerm).onDuplicateKeyUpdate().set(userPerm).execute();
        }
        this.makeDirty();
        if (set == PermissionType.NOT_SET)
        {
            return PermissionType.of(this.permissions.remove(perm));
        }
        return PermissionType.of(this.permissions.put(perm, set == PermissionType.TRUE));
    }

    @Override
    public String setMetadata(String key, String value)
    {
        UserMetaData userMeta = manager.dsl.newRecord(TABLE_META).newMeta(this.getUserID(), this.getMirrorWorldId(), key, value);
        manager.dsl.insertInto(TABLE_META).set(userMeta).onDuplicateKeyUpdate().set(userMeta).execute();
        this.makeDirty();
        return this.metadata.put(key, value);
    }

    @Override
    public boolean removeMetadata(String key)
    {
        manager.dsl.delete(TABLE_META).where(TABLE_META.USERID.eq(this.getUserID()),
                                             TABLE_META.WORLDID.eq(this.getMirrorWorldId()),
                                             TABLE_META.KEY.eq(key)).execute();
        this.makeDirty();
        return this.metadata.remove(key) != null;
    }

    @Override
    public boolean assignRole(Role role)
    {
        if (this.roles.contains(role.getName()))
        {
            return false;
        }
        manager.dsl.newRecord(TABLE_ROLE).newAssignedRole(this.getUserID(), this.getMirrorWorldId(), role.getName()).insert();
        this.removeRoles(role.getRoles());
        if (this.roles.isEmpty())
        {
            for (Role defRole : ((WorldRoleProvider)provider).getDefaultRoles())
            {
                this.removeTempRole(defRole);
            }
        }
        this.makeDirty();
        return this.roles.add(role.getName());
    }

    private void removeRoles(Set<Role> parentRoles)
    {
        for (Role role : parentRoles)
        {
            this.removeRoles(role.getRoles());
            this.removeRole(role);
        }
    }

    @Override
    public boolean removeRole(Role role)
    {
        if (this.roles.contains(role.getName()))
        {
            manager.dsl.delete(TABLE_ROLE).where(TABLE_ROLE.USERID.eq(this.getUserID()),
                                                 TABLE_ROLE.WORLDID.eq(this.getMirrorWorldId()),
                                                 TABLE_ROLE.ROLENAME.eq(role.getName())).execute();
            this.makeDirty();
            return this.roles.remove(role.getName());
        }
        return false;
    }

    @Override
    public void clearPermissions()
    {
        if (!this.permissions.isEmpty())
        {
            manager.dsl.delete(TABLE_PERM).where(TABLE_PERM.USERID.eq(this.getUserID()),
                                 TABLE_PERM.WORLDID.eq(this.getMirrorWorldId())).execute();
            this.makeDirty();
            this.permissions.clear();
        }
    }

    @Override
    public void clearMetadata()
    {
        if (!this.metadata.isEmpty())
        {
            manager.dsl.delete(TABLE_META).where(TABLE_META.USERID.eq(this.getUserID()),
                                                 TABLE_META.WORLDID.eq(this.getMirrorWorldId())).execute();
            this.makeDirty();
            this.metadata.clear();
        }
    }

    @Override
    public void clearRoles()
    {
        if (!this.roles.isEmpty())
        {
            manager.dsl.delete(TABLE_ROLE).where(TABLE_ROLE.USERID.eq(this.getUserID()),
                                                 TABLE_ROLE.WORLDID.eq(this.getMirrorWorldId())).execute();
            this.makeDirty();
            this.roles.clear();
        }
    }

    @Override
    public Map<String, Boolean> getRawPermissions()
    {
        return Collections.unmodifiableMap(this.permissions);
    }

    @Override
    public Map<String, String> getRawMetadata()
    {
        return Collections.unmodifiableMap(this.metadata);
    }

    @Override
    public Set<String> getRawRoles()
    {
        return Collections.unmodifiableSet(this.roles);
    }

    /* TODO
    @Override
    public void setRawPermissions(Map<String, Boolean> perms)
    {
        this.clearPermissions();
        Set<UserPermission> toInsert = new HashSet<>();
        for (Entry<String, Boolean> entry : perms.entrySet())
        {
            toInsert.add(manager.dsl.newRecord(TABLE_PERM)
                                .newPerm(this.getUserID(), this.getMirrorWorldId(), entry.getKey(), entry.getValue()));
        }
        manager.dsl.batchInsert(toInsert).execute();
        super.setRawPermissions(perms);
    }

    @Override
    public void setRawMetadata(Map<String, String> metadata)
    {
        this.clearMetadata();
        Set<UserMetaData> toInsert = new HashSet<>();
        for (Entry<String, String> entry : metadata.entrySet())
        {
            toInsert.add(manager.dsl.newRecord(TABLE_META).newMeta(this.getUserID(), this.getMirrorWorldId(), entry.getKey(), entry
                .getValue()));
        }
        manager.dsl.batchInsert(toInsert).execute();
        super.setRawMetadata(metadata);
    }

    @Override
    public void setRawRoles(Set<Role_old> roles)
    {
        this.clearRoles();
        Set<AssignedRole> toInsert = new HashSet<>();
        for (Role_old role : roles)
        {
            toInsert.add(manager.dsl.newRecord(TABLE_ROLE).newAssignedRole(this.getUserID(), this.getMirrorWorldId(), role.getName()));
        }
        manager.dsl.batchInsert(toInsert).execute();
        super.setRawRoles(roles);
    }
*/
}
