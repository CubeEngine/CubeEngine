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
package de.cubeisland.engine.conomy.commands;

import de.cubeisland.engine.conomy.Conomy;
import de.cubeisland.engine.conomy.account.BankAccount;
import de.cubeisland.engine.conomy.account.ConomyManager;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.Grouped;
import de.cubeisland.engine.core.command.reflected.Indexed;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

public class EcoBankCommands extends ContainerCommand
{
    private final Conomy module;
    private final ConomyManager manager;

    public EcoBankCommands(Conomy module)
    {
        super(module, "bank", "Administrative commands for Conomy Banks.");
        this.module = module;
        this.manager = module.getManager();
    }

    @Command(names = {"give", "grant"},
             desc = "Gives money to a bank or all banks",
             indexed = { @Grouped(@Indexed(label = {"bank","!*"})),
                         @Grouped(@Indexed(label = "amount"))})
    public void give(ParameterizedContext context)
    {
        String amountString = context.getArg(1);
        Double amount = this.manager.parse(amountString, context.getSender().getLocale());
        if (amount == null)
        {
            context.sendTranslated(NEGATIVE, "Could not parse amount! {input}", amountString);
            return;
        }
        String format = manager.format(amount);
        if ("*".equalsIgnoreCase(context.<String>getArg(0)))
        {
            this.manager.transactionAll(false, true, amount);
            context.sendTranslated(POSITIVE, "You gave {input#amount} to every bank!", format);
        }
        else
        {
            String[] banks = StringUtils.explode(",", context.<String>getArg(0));
            for (String bankString : banks)
            {
                BankAccount target = this.manager.getBankAccount(bankString, false);
                if (target == null)
                {
                    context.sendTranslated(NEGATIVE, "There is no bank account named {input#name}!", bankString);
                    continue;
                }
                this.manager.transaction(null, target, amount, true);
                context.sendTranslated(POSITIVE, "You gave {input#amount} to the bank {input#bank}!", format, bankString);
                for (User user : this.module.getCore().getUserManager().getOnlineUsers())
                {
                    if (target.isOwner(user))
                    {
                        user.sendTranslated(POSITIVE, "{user} granted {input#amount} to your bank {input#bank}!", context.getSender(), format, bankString);
                    }
                }
            }
        }
    }

    @Command(names = {"take", "remove"},
             desc = "Takes money from given bank or all banks",
             indexed = { @Grouped(@Indexed(label = {"bank","!*"})),
                         @Grouped(@Indexed(label = "amount"))})
    public void take(ParameterizedContext context)
    {
        String amountString = context.getArg(1);
        Double amount = manager.parse(amountString, context.getSender().getLocale());
        if (amount == null)
        {
            context.sendTranslated(NEGATIVE, "Could not parse amount!");
            return;
        }
        String format = manager.format(amount);
        if ("*".equals(context.<String>getArg(0)))
        {
            this.manager.transactionAll(false, true, -amount);
            context.sendTranslated(POSITIVE, "You took {input#amount} from every bank!", format);
        }
        else
        {
            String[] banks = StringUtils.explode(",", context.<String>getArg(0));
            for (String bankString : banks)
            {
                BankAccount target = this.manager.getBankAccount(bankString, false);
                if (target == null)
                {
                    context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                    return;
                }
                this.manager.transaction(target, null, amount, true);
                context.sendTranslated(POSITIVE, "You took {input#amount} from the bank {input#bank}!", format, bankString);
                for (User onlineUser : this.module.getCore().getUserManager().getOnlineUsers())
                {
                    if (target.isOwner(onlineUser))
                    {
                        onlineUser.sendTranslated(POSITIVE, "{user} charged your bank {input#bank} for {input#amount}!", context.getSender(), bankString, format);
                    }
                }
            }
        }
    }

    @Command(desc = "Reset the money from given banks",
             indexed = @Grouped(@Indexed(label = {"bank","!*"})))
    public void reset(ParameterizedContext context)
    {
        if ("*".equals(context.<String>getArg(0)))
        {
            this.manager.setAll(false, true, this.manager.getDefaultBankBalance());
            context.sendTranslated(POSITIVE, "You reset every bank account!");
        }
        else
        {
            String[] banks = StringUtils.explode(",", context.<String>getArg(0));
            for (String bankString : banks)
            {
                BankAccount target = this.manager.getBankAccount(bankString, false);
                if (target == null)
                {
                    context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                    return;
                }
                target.reset();
                String format = this.manager.format(this.manager.getDefaultBalance());
                context.sendTranslated(POSITIVE, "The account of the bank {input#bank} got reset to {input#balance}!", bankString, format);
                for (User onlineUser : this.module.getCore().getUserManager().getOnlineUsers())
                {
                    if (target.isOwner(onlineUser))
                    {
                        onlineUser.sendTranslated(POSITIVE, "{user} reset the money of your bank {input#bank} to {input#balance}!", context.getSender(), bankString, format);
                    }
                }
            }
        }
    }

    @Command(desc = "Sets the money from given banks",
             indexed = { @Grouped(@Indexed(label = {"bank","!*"})),
                         @Grouped(@Indexed(label = "amount"))})
    public void set(ParameterizedContext context)
    {
        String amountString = context.getArg(1);
        Double amount = manager.parse(amountString, context.getSender().getLocale());
        if (amount == null)
        {
            context.sendTranslated(NEGATIVE, "Could not parse amount!");
            return;
        }
        String format = this.manager.format(amount);
        if ("*".equals(context.<String>getArg(0)))
        {
            this.manager.setAll(false, true, amount);
            context.sendTranslated(POSITIVE, "You have set every bank account to {input#balance}!", format);
        }
        else
        {
            String[] banks = StringUtils.explode(",", context.<String>getArg(0));
            for (String bankString : banks)
            {
                BankAccount target = this.manager.getBankAccount(bankString, false);
                if (target == null)
                {
                    context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                    return;
                }
                target.set(amount);
                context.sendTranslated(POSITIVE, "The money of bank account {input#bank} got set to {input#balance}!", bankString, format);
                for (User onlineUser : this.module.getCore().getUserManager().getOnlineUsers())
                {
                    if (target.isOwner(onlineUser))
                    {
                        onlineUser.sendTranslated(POSITIVE, "{user} set the money of your bank {input#bank} to {input#balance}!", context.getSender(), bankString, format);
                    }
                }
            }
        }
    }

    @Command(desc = "Scales the money from given banks",
             indexed = { @Grouped(@Indexed(label = {"bank","!*"})),
                         @Grouped(@Indexed(label = "factor"))})
    public void scale(ParameterizedContext context)
    {
        Float factor = context.getArg(1, null);
        if (factor == null)
        {
            context.sendTranslated(NEGATIVE, "Invalid factor: {input#factor}", context.getArg(1));
            return;
        }
        if ("*".equals(context.getArg(0)))
        {
            this.manager.scaleAll(false, true, factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of every bank by {decimal#factor}!", factor);
            return;
        }
        String[] banks = StringUtils.explode(",", context.<String>getArg(0));
        for (String bankString : banks)
        {
            BankAccount account = this.manager.getBankAccount(bankString, false);
            if (account == null)
            {
                context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                return;
            }
            account.scale(factor);
            context.sendTranslated(POSITIVE, "Scaled the balance of the bank {input#bank} by {decimal#factor}!", bankString, factor);
            for (User onlineUser : this.module.getCore().getUserManager().getOnlineUsers())
            {
                if (account.isOwner(onlineUser))
                {
                    onlineUser.sendTranslated(POSITIVE, "{user} scaled the money of your bank {input#bank} by {decimal#factor}", context.getSender().getName(), bankString, factor);
                }
            }
        }
    }

    @Command(desc = "Hides the account of given bank",
             indexed = @Grouped(@Indexed(label = {"bank","!*"})))
    public void hide(ParameterizedContext context)
    {
        if ("*".equals(context.getArg(0)))
        {
            this.manager.hideAll(false, true);
            return;
        }
        String[] banks = StringUtils.explode(",", context.<String>getArg(0));
        for (String bankString : banks)
        {
            BankAccount target = this.manager.getBankAccount(bankString, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                return;
            }
            if (target.isHidden())
            {
                context.sendTranslated(POSITIVE, "The bank {input#bank} is already hidden!", bankString);
            }
            else
            {
                target.setHidden(true);
                context.sendTranslated(POSITIVE, "The bank {input#bank} is now hidden!", bankString);
            }
        }
    }

    @Command(desc = "Unhides the account of given banks",
             indexed = @Grouped(@Indexed(label = {"bank","!*"})))
    public void unhide(ParameterizedContext context)
    {
        if ("*".equals(context.getArg(0)))
        {
            this.manager.unhideAll(false, true);
            return;
        }
        String[] banks = StringUtils.explode(",", context.<String>getArg(0));
        for (String bankString : banks)
        {
            BankAccount target = this.manager.getBankAccount(bankString, false);
            if (target == null)
            {
                context.sendTranslated(NEGATIVE, "There is no bank account named {input#bank}!", bankString);
                return;
            }
            if (target.isHidden())
            {
                target.setHidden(false);
                context.sendTranslated(POSITIVE, "The bank {input#bank} is no longer hidden!", bankString);
            }
            else
            {
                context.sendTranslated(POSITIVE, "The bank {input#bank} was not hidden!", bankString);
            }
        }
    }
}
