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
package org.cubeengine.module.conomy.bank.command;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.libcube.service.command.TranslatedReaderException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class VirtualAccountReader implements ArgumentReader<BaseAccount.Virtual>, DefaultValue<BaseAccount.Virtual>
{
    private final BankConomyService service;
    private final I18n i18n;

    public VirtualAccountReader(BankConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public BaseAccount.Virtual read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String arg = invocation.consume(1);
        Optional<BaseAccount.Virtual> target = Optional.empty();
        if (service.hasAccount(arg))
        {
            target = service.getOrCreateAccount(arg).filter(a -> a instanceof BaseAccount.Virtual).map(BaseAccount.Virtual.class::cast);
        }
        if (!target.isPresent())
        {
            throw new TranslatedReaderException(i18n.translate(invocation.getContext(Locale.class), NEGATIVE, "There is no bank account named {input#name}!", arg));
        }
        return target.get();
    }

    @Override
    public BaseAccount.Virtual getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof User)
        {
            User user = (User) invocation.getCommandSource();
            List<BaseAccount.Virtual> banks = service.getBanks(user, AccessLevel.SEE);
            if (banks.isEmpty())
            {
                throw new TranslatedReaderException(i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE,
                        "You have no banks available!"));
            }
            return banks.get(0);
        }
        throw new TranslatedReaderException(i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE,
                "You have to specify a bank!"));
    }
}
