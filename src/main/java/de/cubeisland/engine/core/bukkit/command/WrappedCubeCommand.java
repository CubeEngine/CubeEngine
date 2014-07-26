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
package de.cubeisland.engine.core.bukkit.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.help.HelpTopic;

import de.cubeisland.engine.core.command.CubeCommand;

public class WrappedCubeCommand extends Command
{
    private final CubeCommand command;
    private final CubeCommandExecutor executor;
    private HelpTopic helpTopic;

    public WrappedCubeCommand(CubeCommand command)
    {
        super(command.getName());
        this.command = command;
        this.executor = new CubeCommandExecutor(command);
    }

    public CubeCommand getCommand()
    {
        return command;
    }

    @Override
    public String getName()
    {
        return this.command.getName();
    }

    @Override
    public boolean setLabel(String name)
    {
        this.command.setLabel(name);
        return true;
    }

    @Override
    public String getLabel()
    {
        return this.command.getLabel();
    }

    @Override
    public Command setDescription(String description)
    {
        this.command.setDescription(description);
        return this;
    }

    @Override
    public String getDescription()
    {
        return this.command.getDescription();
    }

    @Override
    public Command setAliases(List<String> aliases)
    {
        this.command.setAliases(new HashSet<>(aliases));
        return this;
    }

    @Override
    public List<String> getAliases()
    {
        return new ArrayList<>(this.command.getAliases());
    }

    @Override
    /**
     * The usage is autogenerated setting it makes no sense
     */
    public Command setUsage(String usage)
    {
        // Fail silently
        return this;
    }

    @Override
    public String getUsage()
    {
        return this.command.getUsage();
    }

    @Override
    public String getPermission()
    {
        return this.command.getPermission() == null ? null : this.command.getPermission().getFullName();
    }

    @Override
    public boolean testPermissionSilent(CommandSender target)
    {
        final String permission = this.getPermission();
        if ((permission == null) || (permission.length() == 0))
        {
            return true;
        }

        return target.hasPermission(permission);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args)
    {
        return this.executor.onCommand(sender, this, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException
    {
        List<String> result = this.executor.onTabComplete(sender, this, label, args);
        if (result == null)
        {
            result = super.tabComplete(sender, label, args);
        }
        return result;
    }

    public void setHelpTopic(HelpTopic helpTopic)
    {
        this.helpTopic = helpTopic;
    }

    public HelpTopic getHelpTopic()
    {
        return helpTopic;
    }
}
