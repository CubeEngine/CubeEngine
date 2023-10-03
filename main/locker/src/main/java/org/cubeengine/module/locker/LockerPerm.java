/*
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
package org.cubeengine.module.locker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@SuppressWarnings("all")
@Singleton
public class LockerPerm extends PermissionContainer
{
    @Inject
    public LockerPerm(PermissionManager pm)
    {
        super(pm, Locker.class);
    }

    public final Permission SHOW_OWNER = register("show-owner", "");
    public final Permission BREAK_OTHER = register("break-other", "");
    public final Permission ACCESS_OTHER = register("access-other", "");
    public final Permission EXPAND_OTHER = register("expand-other", "");

    public final Permission PREVENT_NOTIFY = register("prevent-notify", "");

    public final Permission CMD_REMOVE_OTHER = register("command.locker.remove.other", "");
    public final Permission CMD_KEY_OTHER = register("command.locker.key.other", "");
    public final Permission CMD_MODIFY_OTHER_FLAGS = register("command.locker.modifyflags.other", "");
    public final Permission CMD_MODIFY_OTHER_ACCESS = register("command.locker.modifyaccess.other", "");
    public final Permission CMD_GIVE_OTHER = register("command.locker.give.other", "");

    public final Permission CMD_INFO_OTHER = register("command.locker.info.other", "");
    public final Permission CMD_INFO_SHOW_OWNER =  register("command.locker.info.show-owner", "");
}
