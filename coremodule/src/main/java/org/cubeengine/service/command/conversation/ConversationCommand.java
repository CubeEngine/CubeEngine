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
package org.cubeengine.service.command.conversation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.property.RawPermission;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.TabCompleteEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;

import static org.spongepowered.api.text.format.TextColors.DARK_PURPLE;
import static org.spongepowered.api.text.format.TextColors.WHITE;

public abstract class ConversationCommand extends ContainerCommand
{
    private final Set<UUID> usersInMode = new HashSet<>();
    private final CommandManager cm;

    protected ConversationCommand(Module module)
    {
        super(module);
        module.getModularity().provide(EventManager.class).registerListener(module, this);
        cm = getModule().getModularity().provide(CommandManager.class);
        getDescriptor().setDispatcher(cm); // needed for exceptionhandler
        RawPermission permission = getDescriptor().getPermission();
        permission.registerPermission(module, module.getModularity().provide(PermissionManager.class), null);
        this.registerSubCommands();
    }

    public Module getModule()
    {
        return this.getDescriptor().getModule();
    }

    public boolean hasUser(Player user)
    {
        return usersInMode.contains(user.getUniqueId());
    }

    @Listener
    public void onChatHandler(MessageChannelEvent.Chat event, @First Player player)
    {
        if (this.hasUser(player))
        {
            player.sendMessage(Text.of(DARK_PURPLE, "[", WHITE, getDescriptor().getName(), DARK_PURPLE, "] ",
                                              WHITE, event.getMessage()));

            Text message = event.getRawMessage();
            CommandInvocation invocation = newInvocation(player, message.toPlain()); // TODO
            this.execute(invocation);

            event.setCancelled(true);
        }
    }

    private CommandInvocation newInvocation(CommandSource user, String message)
    {
        return new CommandInvocation(user, message, cm.getProviderManager());
    }

    @Listener
    public void onTabComplete(TabCompleteEvent.Chat event, @First Player player)
    {
        if (this.hasUser(player))
        {
            event.getTabCompletions().clear();
            String message = event.getRawMessage();
            List<String> suggestions = this.getSuggestions(newInvocation(player, message));
            if (suggestions != null)
            {
                event.getTabCompletions().addAll(suggestions);
            }
        }
    }

    /**
     * Adds a user to this chat-commands internal list
     *
     * @param user the user to add
     */
    public boolean addUser(Player user)
    {
        return this.usersInMode.add(user.getUniqueId());
    }

    /**
     * Removes a user from this chat-commands internal list
     *
     * @param user the user tp remove
     */
    public void removeUser(Player user)
    {
        this.usersInMode.remove(user.getUniqueId());
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        return super.selfExecute(invocation);
    }
}
