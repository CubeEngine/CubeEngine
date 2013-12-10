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
package de.cubeisland.engine.chat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.roles.role.RolesAttachment;

public class RoleChatFormatListener implements Listener
{
    private final Chat chat;

    public RoleChatFormatListener(Chat chat)
    {
        this.chat = chat;
    }

    @EventHandler
    public void onChatFormat(ChatFormatEvent event)
    {
        final RolesAttachment rolesAttachment = event.getUser().get(RolesAttachment.class);
        if (rolesAttachment == null)
        {
            this.chat.getLog().warn("Missing RolesAttachment!");
            return;
        }

        event.setVariable("ROLE.PREFIX", ChatFormat.parseFormats(rolesAttachment.getCurrentMetadata("prefix")));
        event.setVariable("ROLE.SUFFIX", ChatFormat.parseFormats(rolesAttachment.getCurrentMetadata("suffix")));
    }
}