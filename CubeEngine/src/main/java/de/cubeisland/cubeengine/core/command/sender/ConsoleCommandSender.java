package de.cubeisland.cubeengine.core.command.sender;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.i18n.I18n;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;

public class ConsoleCommandSender extends WrappedCommandSender implements org.bukkit.command.ConsoleCommandSender
{
    public ConsoleCommandSender(org.bukkit.command.ConsoleCommandSender sender)
    {
        super(sender);
    }

    @Override
    public String getName()
    {
        return ":console";
    }

    @Override
    public String getDisplayName()
    {
        final I18n i18n = CubeEngine.getI18n();
        return i18n.translate(i18n.getDefaultLanguage(), "core", "Console");
    }

    @Override
    public org.bukkit.command.ConsoleCommandSender getWrappedSender()
    {
        return (org.bukkit.command.ConsoleCommandSender)super.getWrappedSender();
    }

    @Override
    public boolean hasPermission(String name)
    {
        return true;
    }

    @Override
    public boolean hasPermission(Permission perm)
    {
        return true;
    }

    @Override
    public boolean isConversing()
    {

        return this.getWrappedSender().isConversing();
    }

    @Override
    public void acceptConversationInput(String input)
    {
        this.getWrappedSender().acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation)
    {
        return this.getWrappedSender().beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation)
    {
        this.getWrappedSender().abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details)
    {
        this.getWrappedSender().abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(String message)
    {
        this.getWrappedSender().sendRawMessage(message);
    }
}