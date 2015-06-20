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
package de.cubeisland.engine.module.conomy.account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.conomy.account.storage.AccountModel;
import de.cubeisland.engine.module.conomy.account.storage.BankAccessModel;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.conomy.account.storage.BankAccessModel.AccessLevel.*;
import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;
import static de.cubeisland.engine.module.conomy.account.storage.TableBankAccess.TABLE_BANK_ACCESS;

public class BankAccount extends Account
{
    private final Map<UInteger, BankAccessModel> owner;
    private final Map<UInteger, BankAccessModel> member;
    private final Map<UInteger, BankAccessModel> invites;

    protected BankAccount(ConomyManager manager, AccountModel model)
    {
        super(manager, model);
        this.owner = new HashMap<>();
        this.member = new HashMap<>();
        this.invites = new HashMap<>();

        for (BankAccessModel access : this.manager.getBankAccess(this.model))
        {
            switch (access.getAccessLevel())
            {
               case OWNER:
                   this.owner.put(access.getValue(TABLE_BANK_ACCESS.USERID), access);
                   break;
               case MEMBER:
                   this.member.put(access.getValue(TABLE_BANK_ACCESS.USERID), access);
                   break;
               case INVITED:
                   this.invites.put(access.getValue(TABLE_BANK_ACCESS.USERID), access);
            }
        }
    }

    @Override
    public String getName()
    {
        return this.model.getValue(TABLE_ACCOUNT.NAME);
    }

    @Override
    public void log(String action, Object value)
    {
        this.manager.logger.info("{} Bank:{} {} :: {}", action, this.getName(), value.toString(), ""+this.balance());
    }

    /**
     * Deletes this BankAccount
     */
    public void delete()
    {
        this.manager.deleteBankAccount(this.getName());
    }

    @Override
    public boolean has(double amount)
    {
        return (this.model.getValue(TABLE_ACCOUNT.VALUE) - amount * this.manager.fractionalDigitsFactor()) >= this.manager.getMinimumBankBalance();
    }

    @Override
    public double getDefaultBalance()
    {
        return this.manager.getDefaultBankBalance();
    }

    @Override
    public double getMinBalance()
    {
        return this.manager.getMinimumBankBalance();
    }

    /**
     * Sets the access-level of given user to owner.
     * The old owner will be set to member status.
     *
     * @param user the user to set as owner
     * @return false if given user is already owner
     */
    public boolean promoteToOwner(User user)
    {
        if (this.isOwner(user)) return false;
        // Search if new owner is moderator OR member
        BankAccessModel access = this.member.remove(user.getEntity().getId());
        if (access != null) // promote new owner
        {
            access.setAccessLevel(OWNER);
            access.updateAsync();
        }
        else // create new owner
        {
            access = this.manager.dsl.newRecord(TABLE_BANK_ACCESS).newAccess(this.model, user, OWNER);
            access.insertAsync();
        }
        this.owner.put(user.getEntity().getId(), access);
        return true;
    }

    /**
     * Demotes a users access onto this bank to member
     *
     * @param user the user to demote
     * @return false if already member rank OR not member
     */
    public boolean demote(User user)
    {
        if (this.isOwner(user))
        {
            BankAccessModel access = this.owner.remove(user.getEntity().getId());
            access.setAccessLevel(MEMBER);
            access.updateAsync();
            this.member.put(user.getEntity().getId(), access);
            return true;
        }
        return false;
    }

    /**
     * Promotes a non-member to access this bank
     *
     * @param user the user to promote to member
     * @return false if already member OR owner
     */
    public boolean promoteToMember(User user)
    {
        if (this.hasAccess(user)) return false;
        BankAccessModel access = this.invites.remove(user.getEntity().getId());
        if (access == null)
        {
            access = this.manager.dsl.newRecord(TABLE_BANK_ACCESS).newAccess(this.model, user, MEMBER);
            access.insertAsync();
        }
        else
        {
            access.setAccessLevel(MEMBER);
            access.updateAsync();
        }
        this.member.put(user.getEntity().getId(), access);
        return true;
    }

    /**
     * Removes access from given user onto this Bank.
     *
     * @param user the user to kick
     * @return false if the user had no access
     */
    public boolean kickUser(User user)
    {
        BankAccessModel oldAccess = this.owner.remove(user.getEntity().getId());
        if (oldAccess == null)
        {
            oldAccess = this.member.remove(user.getEntity().getId());
        }
        if (oldAccess != null)
        {
            oldAccess.deleteAsync();
            return true;
        }
        return false; // is not member OR moderator
    }

    /**
     * Returns whether the given user is owner of this bank
     *
     * @param user the user to check
     * @return true if given user is owner
     */
    public boolean isOwner(User user)
    {
        return this.owner.get(user.getEntity().getId()) != null;
    }

    public boolean isMember(User user)
    {
        return this.member.get(user.getEntity().getId()) != null;
    }

    public boolean isInvited(User user)
    {
        return this.invites.get(user.getEntity().getId()) != null;
    }

    public boolean hasAccess(User user)
    {
        return this.isOwner(user) || this.isMember(user);
    }

    public boolean invite(User user)
    {
        if (!needsInvite() || this.hasAccess(user) || this.invites.get(user.getEntity().getId()) != null) return false;
        BankAccessModel invite = this.manager.dsl.newRecord(TABLE_BANK_ACCESS).newAccess(this.model, user, INVITED);
        invite.insertAsync();
        this.invites.put(user.getEntity().getId(), invite);
        return true;
    }

    public boolean uninvite(User user)
    {
        if (!needsInvite() || this.hasAccess(user) || this.invites.get(user.getEntity().getId()) == null) return false;
        BankAccessModel invite = this.invites.remove(user.getEntity().getId());
        invite.deleteAsync();
        return true;
    }

    public boolean needsInvite()
    {
        return this.model.needsInvite();
    }

    public void setNeedsInvite(boolean set)
    {
        this.model.setNeedsInvite(set);
    }

    public boolean rename(String newName)
    {
        return this.manager.renameBank(this, newName);
    }

    public Set<String> getInvites()
    {
        Set<String> invites = new HashSet<>();
        for (BankAccessModel bankAccessModel : this.invites.values())
        {
            invites.add(this.manager.um.getUser(bankAccessModel.getValue(TABLE_BANK_ACCESS.USERID)).getName());
        }
        return invites;
    }

    public Set<String> getOwners()
    {
        Set<String> owners = new HashSet<>();
        for (BankAccessModel bankAccessModel : this.owner.values())
        {
            owners.add(this.manager.um.getUser(bankAccessModel.getValue(TABLE_BANK_ACCESS.USERID)).getName());
        }
        return owners;
    }

    public Set<String> getMembers()
    {
        Set<String> members = new HashSet<>();
        for (BankAccessModel bankAccessModel : this.member.values())
        {
            members.add(this.manager.um.getUser(bankAccessModel.getValue(TABLE_BANK_ACCESS.USERID)).getName());
        }
        return members;
    }
}
