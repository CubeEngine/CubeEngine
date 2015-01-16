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
package de.cubeisland.engine.module.basics;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.basics.storage.Mail;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.basics.storage.TableMail.TABLE_MAIL;

public class BasicsUser
{
    private final DSLContext dsl;

    public BasicsUserEntity getEntity()
    {
        return bUEntity;
    }

    private BasicsUserEntity bUEntity;
    private List<Mail> mailbox = new ArrayList<>();

    public BasicsUser(Database database, User user)
    {
        this.dsl = database.getDSL();
        this.bUEntity = dsl.selectFrom(TABLE_BASIC_USER).where(TABLE_BASIC_USER.KEY.eq(user.getEntity().getValue(
            TABLE_USER.KEY))).fetchOneInto(TABLE_BASIC_USER);
        if (bUEntity == null)
        {
            this.bUEntity = this.dsl.newRecord(TABLE_BASIC_USER).newBasicUser(user);
            this.bUEntity.insertAsync();
        }
    }

    public void loadMails()
    {
        this.mailbox = this.dsl.selectFrom(TABLE_MAIL).where(TABLE_MAIL.USERID.eq(bUEntity.getValue(TABLE_BASIC_USER.KEY))).fetch();
    }

    public List<Mail> getMails()
    {
        if (this.mailbox.isEmpty())
        {
            this.loadMails();
        }
        return this.mailbox;
    }

    public List<Mail> getMailsFrom(User sender)
    {
        List<Mail> mails = new ArrayList<>();
        for (Mail mail : this.getMails())
        {
            if (mail.getValue(TABLE_MAIL.SENDERID).longValue() == sender.getId())
            {
                mails.add(mail);
            }
        }
        return mails;
    }

    public void addMail(CommandSender from, String message)
    {
        this.getMails(); // load if not loaded
        Mail mail;
        if (from instanceof User)
        {
            mail = this.dsl.newRecord(TABLE_MAIL).newMail(this.bUEntity, ((User)from).getEntity().getKey(), message);
        }
        else
        {
            mail = this.dsl.newRecord(TABLE_MAIL).newMail(this.bUEntity, null, message);
        }
        this.mailbox.add(mail);
        mail.insertAsync();
    }

    public int countMail()
    {
        return this.getMails().size();
    }

    public void clearMail()
    {
        this.dsl.delete(TABLE_MAIL).where(TABLE_MAIL.USERID.eq(this.bUEntity.getValue(TABLE_BASIC_USER.KEY))).execute();
        this.mailbox = new ArrayList<>();
    }

    public void clearMailFrom(User sender)
    {
        final List<Mail> mailsFrom = this.getMailsFrom(sender);
        this.mailbox.removeAll(mailsFrom);
        UInteger senderId = sender == null ? null : sender.getEntity().getKey();
        this.dsl.delete(TABLE_MAIL).where(
            TABLE_MAIL.USERID.eq(this.bUEntity.getValue(TABLE_BASIC_USER.KEY)),
            TABLE_MAIL.SENDERID.eq(senderId)).execute();
    }
}
