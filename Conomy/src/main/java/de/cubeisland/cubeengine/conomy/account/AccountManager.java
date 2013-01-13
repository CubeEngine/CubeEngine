package de.cubeisland.cubeengine.conomy.account;

import de.cubeisland.cubeengine.conomy.Conomy;
import de.cubeisland.cubeengine.conomy.ConomyPermissions;
import de.cubeisland.cubeengine.conomy.account.storage.AccountModel;
import de.cubeisland.cubeengine.conomy.account.storage.AccountStorage;
import de.cubeisland.cubeengine.conomy.currency.Currency;
import de.cubeisland.cubeengine.conomy.currency.CurrencyManager;
import de.cubeisland.cubeengine.core.user.User;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.Collection;

public class AccountManager
{
    private final AccountStorage accountStorage;
    private final CurrencyManager currencyManager;
    private final Conomy module;
    private THashMap<String, THashMap<Currency, Account>> bankaccounts = new THashMap<String, THashMap<Currency, Account>>();
    private TLongObjectHashMap<THashMap<Currency, Account>> useraccounts = new TLongObjectHashMap<THashMap<Currency, Account>>();

    public AccountManager(Conomy module)
    {
        this.module = module;
        this.currencyManager = module.getCurrencyManager();
        this.accountStorage = module.getAccountsStorage();
    }

    public AccountStorage getStorage()
    {
        return this.accountStorage;
    }

    /**
     * Returns the main currency.
     *
     * @return the main currency
     */
    public Currency getMainCurrency()
    {
        return this.module.getCurrencyManager().getMainCurrency();
    }

    /**
     * Returns the account of given user for the main currency
     *
     * @param user the user
     * @return the user's account
     */
    public Account getAccount(User user)
    {
        return this.getAccount(user, this.module.getCurrencyManager().getMainCurrency());
    }

    /**
     * Returns the account of given user for given currency
     *
     * @param user the user
     * @param currency the currency
     * @return the user's account
     */
    public Account getAccount(User user, Currency currency)
    {
        this.userAccountExists(user, currency); //loads accounts if not yet loaded
        return this.useraccounts.get(user.key).get(currency);
    }

    /**
     * Returns the bank-account with given name for the main currency
     *
     * @param name the banks name
     * @return the bank's account
     */
    public Account getAccount(String name)
    {
        return this.getAccount(name, this.getMainCurrency());
    }

    /**
     * Returns the bank-account with given name for given currency
     *
     * @param name the banks name
     * @param currency the currency
     * @return the bank's account
     */
    public Account getAccount(String name, Currency currency)
    {
        this.bankAccountExists(name, currency);
        return this.bankaccounts.get(name).get(currency);

    }

    /**
     * Returns if given user has an account in the main currency.
     *
     * @param user the user
     * @return true if the user has an account
     */
    public boolean userAccountExists(User user)
    {
        return this.userAccountExists(user, this.getMainCurrency());
    }

    /**
     * Returns if given user has an account in given currency.
     *
     * @param user the user
     * @param currency the currency
     * @return true if the user has an account
     */
    public boolean userAccountExists(User user, Currency currency)
    {
        if (this.useraccounts.containsKey(user.key))
        {
            return this.useraccounts.get(user.key).containsKey(currency);
        }
        else
        {
            this.loadUserAccounts(user);
            return this.useraccounts.get(user.key).containsKey(currency);
        }
    }

    /**
     * Returns if a bank account with given name exists for the main currency.
     *
     * @param name the bank's name
     * @return true if the bank-account exists
     */
    public boolean bankAccountExists(String name)
    {
        return this.bankAccountExists(name, this.getMainCurrency());
    }

    /**
     * Returns if a bank account with given name exists for the given currency.
     *
     * @param name the bank's name
     * @param currency the currency
     * @return true if the bank-account exists
     */
    public boolean bankAccountExists(String name, Currency currency)
    {
        if (this.bankaccounts.containsKey(name))
        {
            return this.bankaccounts.get(name).containsKey(currency);
        }
        else
        {
            this.loadBankAccounts(name);
            return this.bankaccounts.get(name).containsKey(currency);
        }
    }

    /**
     * Creates a main-currency account for given user.
     *
     * @param user the user
     * @return the new account
     */
    public Account createNewAccount(User user)
    {
        return this.createNewAccount(user, this.getMainCurrency());
    }

    /**
     * Creates an account in given currency for given user.
     *
     * @param user the user
     * @param currency the currency
     * @return the new account
     */
    public Account createNewAccount(User user, Currency currency)
    {
        if (this.userAccountExists(user, currency))
        {
            return null;
        }
        return this.createNewAccountNoCheck(user, currency);
    }

    /**
     * Creates a new account for the user in given currency without checking for
     * eventually existing account.
     *
     * @param user
     * @param currency
     * @return
     */
    private Account createNewAccountNoCheck(User user, Currency currency)
    {
        AccountModel model = new AccountModel(user.key, null, currency.getName(), currency.getDefaultBalance());
        this.accountStorage.store(model);
        Account account = new Account(this, currency, model);
        this.useraccounts.get(user.key).put(currency, account);
        return account;
    }

    /**
     * Creates a main-currency bank-account with given name.
     *
     * @param name the bank-name
     * @return the new account
     */
    public Account createNewAccount(String name)
    {
        return this.createNewAccount(name, this.getMainCurrency());
    }

    /**
     * Creates a bank-account in given currency with given name.
     *
     * @param name the bank-name
     * @param currency the currency
     * @return the new account
     */
    public Account createNewAccount(String name, Currency currency)
    {
        if (this.bankAccountExists(name, currency))
        {
            return null;
        }
        return this.createNewAccountNoCheck(name, currency);
    }

    private Account createNewAccountNoCheck(String name, Currency currency)
    {
        AccountModel model = new AccountModel(null, name, currency.getName(), currency.getDefaultBalance());
        this.accountStorage.store(model);
        Account account = new Account(this, currency, model);
        this.bankaccounts.get(name).put(currency, account);
        return account;
    }

    private void loadUserAccounts(User user)
    {
        Collection<AccountModel> models = this.accountStorage.loadAccounts(user.key);
        this.useraccounts.put(user.key, new THashMap<Currency, Account>());
        for (AccountModel model : models)
        {
            Currency currency = this.currencyManager.getCurrencyByName(model.currencyName);
            Account account = new Account(this, currency, model);
            this.useraccounts.get(user.key).put(currency, account);
        }
        if (user.hasPlayedBefore()) // only if user has played on the server create missing accounts
        {
            for (Currency currency : this.currencyManager.getAllCurrencies())
            {
                if (!this.useraccounts.get(user.key).containsKey(currency))
                {
                    this.createNewAccountNoCheck(user, currency); // No check i just did check in db
                }
            }
        }
    }

    private void loadBankAccounts(String name)
    {
        Collection<AccountModel> models = this.accountStorage.loadAccounts(name);
        this.bankaccounts.put(name, new THashMap<Currency, Account>());
        for (AccountModel model : models)
        {
            Currency currency = this.currencyManager.getCurrencyByName(model.currencyName);
            Account account = new Account(this, currency, model);
            this.bankaccounts.get(name).put(currency, account);
        }
    }

    /**
     * Returns the accounts of given user
     *
     * @param user the user
     * @return the account or null if
     */
    public Collection<Account> getAccounts(User user)
    {
        this.loadUserAccounts(user);
        return this.useraccounts.get(user.key).values();
    }

    /**
     * Transfers money from the source-account to the target-amount.
     *
     * @param source the source
     * @param target the target
     * @param amount the amount in target-currency
     * @param force force the transaction ingoring possible user-permissions
     * @return true if the transaction was succesful
     * @throws IllegalArgumentException when currencies are not convertible
     */
    public boolean transaction(Account source, Account target, Long amount, boolean force) throws IllegalArgumentException
    {
        if (source.getCurrency().canConvert(target.getCurrency()))
        {
            return false; // currencies are not convertible
        }
        if (!force)
        {
            if (source != null)
            {
                if (source.getBalance() - amount < source.getCurrency().getMinMoney())
                {
                    if (source.isUserAccount() && !ConomyPermissions.ACCOOUNT_ALLOWUNDERMIN.isAuthorized(source.getUser()))
                    {
                        return false;
                    }
                    else
                    {
                        return false; // TODO bank minimum
                    }
                }
            }
            //TODO perm checks etc.
        }
        target.transaction(source, amount);
        //TODO logging
        return true;
    }

    /**
     * Gives money to all user-accounts
     *
     * @param currency the currency
     * @param amount the amount
     * @param online set to true if only online users
     */
    public void transactAll(Currency currency, long amount, boolean online)
    {
        if (online)
        {
            for (User user : this.module.getUserManager().getOnlineUsers())
            {
                this.userAccountExists(user);
                Account acc = this.useraccounts.get(user.key).get(currency);
                if (acc != null)
                {
                    acc.transaction(null, amount);
                }
            }
        }
        else
        {
            this.accountStorage.transactAll(currency, amount);
            this.useraccounts = new TLongObjectHashMap<THashMap<Currency, Account>>();
        }
    }

    /**
     * Sets all user-account
     *
     * @param currency the currency
     * @param amount the amount
     * @param online set to true if only online users
     */
    public void setAll(Currency currency, long amount, boolean online)
    {
        if (online)
        {
            for (User user : this.module.getUserManager().getOnlineUsers())
            {
                this.userAccountExists(user);
                Account acc = this.useraccounts.get(user.key).get(currency);
                if (acc != null)
                {
                    acc.set(amount);
                }
            }
        }
        else
        {
            this.accountStorage.setAll(currency, amount);
            this.useraccounts = new TLongObjectHashMap<THashMap<Currency, Account>>();
        }
    }
}