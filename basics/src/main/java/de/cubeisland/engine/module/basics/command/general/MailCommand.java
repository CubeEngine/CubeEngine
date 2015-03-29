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
package de.cubeisland.engine.module.basics.command.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Greed;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.basics.storage.Mail;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.basics.storage.TableMail.TABLE_MAIL;

@Command(name = "mail", desc = "Manages your server mail.")
public class MailCommand extends ContainerCommand
{
    private final Basics module;

    public MailCommand(Basics module)
    {
        super(module);
        this.module = module;
    }

    @Alias(value = "readmail")
    @Command(desc = "Reads your mail.")
    public void read(CommandSender context, @Optional CommandSender player)
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        if (sender == null)
        {
            if (player == null)
            {
                context.sendTranslated(NEUTRAL, "Log into the game to check your mailbox!");

                return;
            }
            context.sendTranslated(NEUTRAL, "If you wanted to look into other players mail use: {text:/mail spy} {input#player}.", player);
            context.sendTranslated(NEGATIVE, "Otherwise be quiet!");
            return;
        }
        BasicsUser bUser = sender.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser();
        if (bUser.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        List<Mail> mails;
        if (player == null) //get mails
        {
            mails = bUser.getMails();
        }
        else //Search for mail of that user
        {
            mails = bUser.getMailsFrom(player);
        }
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail from {user}.", player);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mails.size(); i++)
        {
            Mail mail = mails.get(i);
            sb.append("\n").append(ChatFormat.WHITE).append(i+1).append(": ").append(mail.readMail());
        }
        context.sendTranslated(POSITIVE, "Your mail: {input#mails}", ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(value = "spymail")
    @Command(desc = "Shows the mail of other players.")
    public void spy(CommandSender context, User player)
    {
        List<Mail> mails = player.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().getMails();
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "{user} does not have any mail!", player);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Mail mail : mails)
        {
            i++;
            sb.append("\n").append(ChatFormat.WHITE).append(i).append(": ").append(mail.getValue(TABLE_MAIL.MESSAGE));
        }
        context.sendTranslated(NEUTRAL, "{user}'s mail: {input#mails}", player, ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(value = "sendmail")
    @Command(desc = "Sends mails to other players.")
    public void send(CommandSender context, User player, @Greed(INFINITE) String message)
    {
        this.mail(message, context, player);
        context.sendTranslated(POSITIVE, "Mail send to {user}!", player);
    }

    @Alias(value = "sendallmail")
    @Command(desc = "Sends mails to all players.")
    public void sendAll(CommandSender context, final @Greed(INFINITE) String message)
    {
        Set<User> users = this.module.getCore().getUserManager().getOnlineUsers();
        final Set<Long> alreadySend = new HashSet<>();
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        for (User user : users)
        {
            user.attachOrGet(BasicsAttachment.class, module).getBasicsUser().addMail(sender, message);
            alreadySend.add(user.getId());
        }
        final UInteger senderId = sender == null ? null : sender.getEntity().getKey();
        this.module.getCore().getTaskManager().runAsynchronousTaskDelayed(this.module,new Runnable()
        {
            public void run() // Async sending to all Users ever
            {
                DSLContext dsl = module.getCore().getDB().getDSL();
                Collection<Query> queries = new ArrayList<>();
                for (Long userId : module.getCore().getUserManager().getAllIds())
                {
                    if (!alreadySend.contains(userId))
                    {
                        queries.add(dsl.insertInto(TABLE_MAIL, TABLE_MAIL.MESSAGE, TABLE_MAIL.USERID, TABLE_MAIL.SENDERID).values(message, UInteger.valueOf(userId), senderId));
                    }
                }
                dsl.batch(queries).execute();
            }
        },0);
        context.sendTranslated(POSITIVE, "Sent mail to everyone!");
    }

    @Command(desc = "Removes a single mail")
    @Restricted(value = User.class, msg = "The console has no mails!")
    public void remove(User context, Integer mailId)
    {
        BasicsUser bUser = context.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser();
        if (bUser.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        try
        {
            Mail mail = bUser.getMails().get(mailId);
            module.getCore().getDB().getDSL().delete(TABLE_MAIL).where(TABLE_MAIL.KEY.eq(mail.getValue(TABLE_MAIL.KEY))).execute();
            context.sendTranslated(POSITIVE, "Deleted Mail #{integer#mailid}", mailId);
        }
        catch (IndexOutOfBoundsException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid Mail Id!");
        }
    }

    @Command(desc = "Clears your mail.")
    @Restricted(value = User.class, msg = "You will never have mail here!")
    public void clear(User context, @Optional User player)
    {
        if (player == null)
        {
            context.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().clearMail();
            context.sendTranslated(NEUTRAL, "Cleared all mails!");
            return;
        }
        // TODO console User from = "console".equalsIgnoreCase(context.getString(0)) ? null : context.<User>get(0);
        context.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().clearMailFrom(player);
        context.sendTranslated(NEUTRAL, "Cleared all mail from {user}!", player == null ? "console" : player);
    }

    private void mail(String message, CommandSender from, User... users)
    {
        for (User user : users)
        {
            user.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().addMail(from, message);
            if (user.isOnline())
            {
                user.sendTranslated(NEUTRAL, "You just got a mail from {user}!", from.getName());
            }
        }
    }
}
