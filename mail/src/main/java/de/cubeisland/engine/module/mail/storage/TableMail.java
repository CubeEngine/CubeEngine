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
package de.cubeisland.engine.module.mail.storage;

import de.cubeisland.engine.module.core.util.Version;
import de.cubeisland.engine.service.database.AutoIncrementTable;
import de.cubeisland.engine.service.database.Database;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.service.user.TableUser.TABLE_USER;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableMail extends AutoIncrementTable<Mail, UInteger>
{
    public static TableMail TABLE_MAIL;
    public final TableField<Mail, UInteger> KEY = createField("key", U_INTEGER.nullable(false), this);
    public final TableField<Mail, String> MESSAGE = createField("message", VARCHAR.length(100).nullable(false), this);
    public final TableField<Mail, UInteger> USERID = createField("userId", U_INTEGER.nullable(false), this);
    public final TableField<Mail, UInteger> SENDERID = createField("senderId", U_INTEGER, this);

    public TableMail(String prefix, Database db)
    {
        super(prefix + "mail", new Version(1), db);
        setAIKey(KEY);
        addForeignKey(TABLE_USER.getPrimaryKey(), USERID);
        addForeignKey(TABLE_USER.getPrimaryKey(), SENDERID);
        addFields(KEY, MESSAGE, USERID, SENDERID);
        TABLE_MAIL = this;
    }

    @Override
    public Class<Mail> getRecordType()
    {
        return Mail.class;
    }
}
