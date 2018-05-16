package com.astute.accounts;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.torque.Accounts;
import org.apache.torque.AccountsAdmin;
import org.apache.torque.AccountsAdminPeer;
import org.apache.torque.AccountsPeer;
import org.apache.torque.AcctBatchRpt;
import org.apache.torque.AcctBatchRptFile;
import org.apache.torque.AcctBatchRptFilePeer;
import org.apache.torque.AcctBatchRptPeer;
import org.apache.torque.AcctFeature;
import org.apache.torque.AcctFeaturePeer;
import org.apache.torque.AcctPa;
import org.apache.torque.AcctPaBank;
import org.apache.torque.AcctPaBankPeer;
import org.apache.torque.AcctPaPeer;
import org.apache.torque.AcctPaRange;
import org.apache.torque.AcctPaRangePeer;
import org.apache.torque.AcctPaReports;
import org.apache.torque.AcctPaReportsPeer;
import org.apache.torque.AdminUsers;
import org.apache.torque.AdminUsersPeer;
import org.apache.torque.MakerCheckerPending;
import org.apache.torque.MakerCheckerPendingPeer;
import org.apache.torque.MenuProfileProtocolPeer;
import org.apache.torque.Merchant;
import org.apache.torque.MerchantGrouping;
import org.apache.torque.MerchantGroupingPeer;
import org.apache.torque.MerchantInAccounts;
import org.apache.torque.MerchantInAccountsPeer;
import org.apache.torque.MerchantInGroupingPeer;
import org.apache.torque.MerchantMenuProfilePeer;
import org.apache.torque.MerchantPeer;
import org.apache.torque.PricingParameter;
import org.apache.torque.PricingParameterPeer;
import org.apache.torque.PricingValue;
import org.apache.torque.PricingValuePeer;
import org.apache.torque.TerminalMenuProfilePeer;
import org.apache.torque.TerminalPeer;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.astute.admin.AdminProfile;
import com.astute.common.exception.ExceptionManager;
import com.astute.common.utils.StringUtil;
import com.astute.events.EventStatus;
import com.astute.makerchecker.MakerCheckerManager;
import com.astute.merchant.MerchantManager;
import com.astute.purse.PendingMakeEntry;
import com.astute.session.SessionData;
import com.workingdogs.village.Record;


/**
 * Created by IntelliJ IDEA.
 * User: Kenneth Chan
 * Date: Nov 30, 2004
 * Time: 1:53:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccountsManager {

    public static Logger logger = Logger.getLogger(AccountsManager.class);
    public static final int STATUS_CREATE_OK = 1;
    public static final int STATUS_EDIT_OK = 2;
    public static final int STATUS_DELETE_OK = 3;
    public static final int ACCOUNT_CREATED = 4;
    public static final int ACCOUNT_EDITED = 5;
    public static final int ACCOUNT_DELETED = 6;
    
    public static final int ERR_CREATE_FAIL = -1;
    public static final int ERR_EDIT_FAIL = -2;
    public static final int ERR_NOT_FOUND = -3;
    public static final int ERR_DELETE_FAIL = -4;
    public static final int ERR_SELECT_FAIL = -5;
    public static final int ERR_ACCT_CODE_USED = -6;
    public static final int ERR_PENDING_EXISTED = -7;
    public static final int ERR_SYSTEM_ERROR = -99;
    public static final short DEL_FLAG_NOT_DELETED = 0;
    public static final short DEL_FLAG_DELETED = -1;
    private static final int UPDATE_IGNORE = -1;
    private static final int UPDATE_DELETE = 0;
    private static final int UPDATE_ADD = 1;
    private static final int MAX_ACCT_CODE_LENGTH = 5;
    public static final String SESSION_ATTR_ACCOUNTS_ID = "MY_ACCOUNTS_ID";


    /**
     * Return all non-deleted account entries sorted by account name in ascending order.
     * Return null if not found.
     *
     * @return AccountEntry array
     */
    public static AccountEntry[] getAllAccountEntries() {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);
        crit.addAscendingOrderByColumn(AccountsPeer.ACCT_NAME);

        AccountEntry[] entries = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        if (list != null && list.size() > 0) {
            entries = new AccountEntry[list.size()];
            Iterator iterator = list.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
                AccountEntry account = new AccountEntry((Accounts) iterator.next());
                entries[counter] = account;
                counter++;
            }
        } else {
            logger.debug("No Accounts found");
        }
        return entries;
    }


    /**
     * Return all non-deleted account entries sorted by account name in ascending order.
     * Return null if not found.
     *
     * @return AccountEntry array
     */
    public static AccountEntry[] getAllNonLoyaltyAccountEntries() {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);
        crit.add(AccountsPeer.ACCT_TYPE, AccountEntry.ACCOUNT_TYPE_LOYALTY, Criteria.NOT_EQUAL);
        crit.addAscendingOrderByColumn(AccountsPeer.ACCT_NAME);

        AccountEntry[] entries = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        if (list != null && list.size() > 0) {
            entries = new AccountEntry[list.size()];
            Iterator iterator = list.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
                AccountEntry account = new AccountEntry((Accounts) iterator.next());
                entries[counter] = account;
                counter++;
            }
        } else {
            logger.debug("No Accounts found");
        }
        return entries;
    }


    /**
     * Search non-deleted account entries by account name, account code, account type,
     * sorted by orderBy field in ascending order,
     * return the entries based on page number and number of rows to display.
     * Return null if not found.
     *
     * @param acctName     Account Name, put null or "" if do not filter by account name
     * @param acctCode     Account Code, put null or "" if do not filter by account code
     * @param acctType     Account Type, put negative value if do not filter by account type
     * @param pageNo       Page Number, <0 if not required
     * @param rowTodisplay Number of rows to display, <0 if not required
     * @param orderBy      Order by field in ascending order
     * @return AccountEntry array
     */
    public static AccountEntry[] searchAccountEntries(String acctName, String acctCode, int acctType, int pageNo, int rowTodisplay, String orderBy) {
        Criteria crit = new Criteria();
        if (pageNo >= 0 && rowTodisplay >= 0) {
            crit.setOffset((pageNo - 1) * rowTodisplay);
            crit.setLimit(rowTodisplay);
        }
        if (acctName != null && !acctName.equals("")) {
            crit.add(AccountsPeer.ACCT_NAME, (Object) acctName, Criteria.LIKE);
        }
        if (acctCode != null && !acctCode.equals("")) {
            crit.add(AccountsPeer.ACCT_CODE, (Object) acctCode, Criteria.LIKE);
        }
        if (acctType >= 0) {
            crit.add(AccountsPeer.ACCT_TYPE, acctType);
        }
        crit.addAscendingOrderByColumn(orderBy);
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);

        AccountEntry[] entries = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        if (list != null && list.size() > 0) {
            entries = new AccountEntry[list.size()];
            Iterator iterator = list.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
                AccountEntry account = new AccountEntry((Accounts) iterator.next());
                entries[counter] = account;
                counter++;
            }
        }

        return entries;
    }


    /**
     * Get the total number of non-deleted accounts searched by account name, account code, account type.
     *
     * @param acctName Account Name, put null or "" if do not filter by account name
     * @param acctCode Account Code, put null or "" if do not filter by account name
     * @param acctType Account Type, put negative value if do not filter by account type
     * @return Number of non-deleted accounts searched by account name, account code, account type
     */
    public static int getTotalAccountCount(String acctName, String acctCode, int acctType) {
        Criteria crit = new Criteria();
        if (acctName != null && !acctName.equals("")) {
            crit.add(AccountsPeer.ACCT_NAME, (Object) acctName, Criteria.LIKE);
        }
        if (acctCode != null && !acctCode.equals("")) {
            crit.add(AccountsPeer.ACCT_CODE, (Object) acctCode, Criteria.LIKE);
        }
        if (acctType >= 0) {
            crit.add(AccountsPeer.ACCT_TYPE, acctType);
        }
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);
        crit.addSelectColumn("Count(" + AccountsPeer.ACCT_ID + ") as total");

        int totalAccountCount = 0;
        List list = null;
        try {
            String query = AccountsPeer.createQueryString(crit);
            list = AccountsPeer.executeQuery(query);
            if (list != null) {
                Record r = (Record) list.get(0);
                totalAccountCount = r.getValue(1).asInt();
            }
        } catch (Exception e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        return totalAccountCount;
    }


    /**
     * Get the total number of non-deleted accounts.
     *
     * @return Number of non-deleted accounts
     */
    public static int getTotalAccountCount() {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);

        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        return (list == null ? 0 : list.size());
    }


    /**
     * Get AccountEntry by Primary Key.
     *
     * @param acctID Account ID
     * @return Account entry
     */
    public static AccountEntry getAccountByPK(int acctID) {
        AccountEntry accountEntry = null;
        try {
            accountEntry = new AccountEntry(AccountsPeer.retrieveByPK(acctID));
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return accountEntry;
    }


    /**
     * Get AccountEntry by Account Code.
     *
     * @param acctCode Account Code
     * @return Account entry
     */
    private static AccountEntry getAccountByAcctCode(String acctCode) {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.ACCT_CODE, acctCode);

        AccountEntry accountEntry = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        if (list != null && list.size() > 0) {
            Iterator iterator = list.iterator();
            if (iterator.hasNext()) {
                accountEntry = new AccountEntry((Accounts) iterator.next());// should have only 1 entry
            }
        }

        return accountEntry;
    }


    /**
     * Do soft delete (set del_flag) of Account based on account ID.
     *
     * @param acctID Account ID
     * @return Event status for deletion
     */
    public static EventStatus deleteAccounts(int acctID) {
        EventStatus status = new EventStatus();

        try {
            Accounts accounts = AccountsPeer.retrieveByPK(acctID);
            accounts.setDelFlag(DEL_FLAG_DELETED);
            accounts.save();

            deleteAdminInAccounts(acctID);
            status.setStatus(AccountsManager.STATUS_DELETE_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_NOT_FOUND);
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            logger.error("Failed to delete account");
            ExceptionManager.logStackTraceString(e, logger);
        }

        return status;
    }


    /**
     * Deletes an account using maker checker approach
     * @param acctId Account Id
     * @param acctId Admin Id
     * @return EventStatus The status of deleting action
     */
    public static EventStatus deleteAccountMakerChecker(int acctId, int adminId)
    {
        EventStatus status = new EventStatus();

        AccountEntry currAcct = AccountsManager.getAccountByPK(acctId);

        // makes sure there is no other pending action on this account
        Vector pendingAcct = MakerCheckerManager.getPendingMakeEntryByMakeInfo("accounts");
        if ((pendingAcct != null) && (pendingAcct.size() > 0))
        {
            int total = pendingAcct.size();
            Object obj = null;
            for (int i = 0; i < total; i++)
            {
                obj = pendingAcct.elementAt(i);
                if ((obj != null) && (obj instanceof AccountEntry))
                {
                    AccountEntry ae = (AccountEntry) obj;
                    if (acctId == ae.getAcctID())
                    {
                        logger.error("AccountsManager deleteAccountMakerChecker Error this account already existed in the pending table");
                        status.setStatus(AccountsManager.ERR_PENDING_EXISTED);
                        return status;
                    }
                }
            }
        }

        // creates an account entry to store all the information of the deleted account
        AccountEntry ae = new AccountEntry();
        ae.setAcctID(acctId);
        ae.setAcctName(currAcct.getAcctName());
        ae.setAcctCode(currAcct.getAcctCode());
        ae.setAcctType((short) currAcct.getAcctType());
        ae.setCreatedBy(currAcct.getCreatedBy());

        AccountsAdmin[] accountsAdmin = null;
        AdminProfile[] tempAdmin = AccountsManager.getAdminUsersByAcctID(acctId);
        if ((tempAdmin != null) && (tempAdmin.length > 0))
        {
            accountsAdmin = new AccountsAdmin[tempAdmin.length];
            for (int i = 0; i < tempAdmin.length; i++)
            {
                try
                {
                    accountsAdmin[i] = new AccountsAdmin();
                    accountsAdmin[i].setAdminID(tempAdmin[i].getAdminID());
                    accountsAdmin[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager deleteAccountMakerChecker Error while creating accounts_admin entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_DELETE_FAIL);
                    return status;
                }
            }
        }
        ae.setAccountsAdmin(accountsAdmin);

        MerchantInAccounts[] merchantInAccounts = null;
        int[] mid = MerchantManager.getMerchantIdByAcctId(acctId);
        if ((mid != null) && (mid.length > 0))
        {
            merchantInAccounts = new MerchantInAccounts[mid.length];
            for (int i = 0; i < mid.length; i++)
            {
                try
                {
                    merchantInAccounts[i] = new MerchantInAccounts();
                    merchantInAccounts[i].setMerchantID(mid[i]);
                    merchantInAccounts[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager deleteAccountMakerChecker Error while creating merchant_in_accounts entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_DELETE_FAIL);
                    return status;
                }
            }
        }
        ae.setMerchantInAccounts(merchantInAccounts);

        PricingParameter[] pricingParameter = null;
        PricingParameterEntry[] ppe = AccountsManager.getPricingParameters(acctId);
        if ((ppe != null) && (ppe.length > 0))
        {
            pricingParameter = new PricingParameter[ppe.length];
            for (int i = 0; i < ppe.length; i++)
            {
                try
                {
                    pricingParameter[i] = new PricingParameter();
                    pricingParameter[i].setPricingParameterName(ppe[i].getPricingParameterName());
                    pricingParameter[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager deleteAccountMakerChecker Error while creating pricing_parameter entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_DELETE_FAIL);
                    return status;
                }
            }
        }
        ae.setPricingParameter(pricingParameter);

        // creates a byte output stream for the pending table entry
        ByteArrayOutputStream baos = null;
        try
        {
            baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ae);
        }
        catch (IOException ioe)
        {
            logger.error("AccountsManager deleteAccountMakerChecker Error while creating a byte output stream");
            logger.error(ioe);
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            return status;
        }

        // creates maker checker pending entry to store all the information of the new account
        MakerCheckerPending mcp = new MakerCheckerPending();
        mcp.setMakeData(com.astute.common.Int2Hex.getHex(baos.toByteArray()));
        mcp.setMakeDateTime(new Date());
        mcp.setMakerID(adminId);
        mcp.setMakeInfo("accounts");
        mcp.setDescription(ae.getAcctName());
        mcp.setAction("Delete Account");
        mcp.setReferenceData("" + ae.getAcctID());

        // creates a connection to store this maker checker pending entry
        Connection con = null;
        try
        {
            con = Transaction.begin(Torque.getDefaultDB());

        } catch (TorqueException te)
        {
            logger.error("AccountsManager deleteAccountMakerChecker Error when creating a connection to database");
            logger.error(te);
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            return status;
        }

        // save the maker checker pending entry using the newly created connection
        try
        {
            mcp.save(con);
            Transaction.commit(con);
            status.setStatus(AccountsManager.STATUS_DELETE_OK);
        } catch (Exception e)
        {
            Transaction.safeRollback(con);
            logger.error("AccountsManager deleteAccountMakerChecker Error when saving the maker checker pending entry");
            logger.error(e);
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            return status;
        }

        return status;
    }


    /**
     * Create Account
     *
     * @param acctName Account Name
     * @param acctCode Account Code
     * @param acctType Account Type
     * @param offActInputType Offline Activation Input Type
     * @param adminID  Admin ID who creates the account
     * @param aid      Associating Admin IDs
     * @param mid      Associating Merchant IDs
     * @param ppid     Associating Pricing Parameter
     * @param token    User Token
     * @return Event status for creation, AccountEntry object will be set as the Return Object
     */
    public static EventStatus createAccount(String acctName, String acctCode, int acctType, int offActInputType, int adminID, String[] aid, String[] mid, String[] ppid, String token) {
        EventStatus status = new EventStatus();

        try {
            if (getAccountByAcctCode(acctCode) != null) {
                status.setStatus(AccountsManager.ERR_ACCT_CODE_USED);
                logger.error("Account Code (" + acctCode + ")is used");
                return status;
            }

            Accounts account = new Accounts();
            account.setAcctName(acctName);
            account.setAcctCode(acctCode);
            account.setAcctType((short) acctType);
            account.setOffActInputType((short)offActInputType);
            account.setCreatedBy(adminID);
            account.save();
            // todo: put in 1 transaction

            AccountEntry accountEntry = new AccountEntry(account);

            if (aid != null && aid.length != 0) {
                for (int i = 0; i < aid.length; i++) {
//                    logger.debug("creating accounts admin");
                    AccountsAdmin accountsAdmin = new AccountsAdmin();
                    accountsAdmin.setAdminID(Integer.parseInt(aid[i]));
                    accountsAdmin.setAcctID(account.getAcctID());
                    accountsAdmin.save();
                }
            }

            if (mid != null && mid.length != 0) {
                for (int i = 0; i < mid.length; i++) {
//                    logger.debug("creating merchant in accounts");
                    MerchantInAccounts merchantInAccounts = new MerchantInAccounts();
                    merchantInAccounts.setAcctID(account.getAcctID());
                    merchantInAccounts.setMerchantID(Integer.parseInt(mid[i]));
                    merchantInAccounts.save();

                    // saves merchant linkings into merchant_linking_report table
                    Merchant merchant = MerchantPeer.retrieveByPK(Integer.parseInt(mid[i]));
                    AdminUsers au = AdminUsersPeer.retrieveByPK(adminID);
                    MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                MerchantManager.MERCHANT_LINKING_REPORT_ACTION_ADDED,
                                                                merchant.getMerchantID(), merchant.getName(),
                                                                account.getAcctID(), "", new Date(),
                                                                au.getAdminName());
                }
            }

            if (ppid != null && ppid.length != 0) {
                for (int i = 0; i < ppid.length; i++) {

                    PricingParameter pricingParameter = new PricingParameter();
                    pricingParameter.setAcctID(account.getAcctID());
                    pricingParameter.setPricingParameterName(ppid[i]);
                    pricingParameter.save();
                }
            }

            status.setStatus(AccountsManager.STATUS_CREATE_OK);
            status.setReturnObj(accountEntry);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create account 1");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save account 2");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        return status;
    }
    
    
    

    /**
     * Creates a new account using maker checker approach
     * @param acctName Account Name
     * @param acctCode Account Code
     * @param acctType Account Type
     * @param adminID  Admin ID who creates the account
     * @param aid Associating Admin IDs
     * @param mid Associating Merchant IDs
     * @param ppid Associating Pricing Parameter
     * @return EventStatus The status of creating action
     */
    public static EventStatus createAccountMakerChecker(String acctName, String acctCode, int acctType, int adminID, String[] aid, String[] mid, String[] ppid)
    {
        EventStatus status = new EventStatus();

        // checks if the account code is already used
        if (getAccountByAcctCode(acctCode) != null)
        {
            logger.error("AccountsManager createAccountMakerChecker Account code " + acctCode + " is used");
            status.setStatus(AccountsManager.ERR_ACCT_CODE_USED);
            return status;
        }

        // do not need to check for pending accounts since the real account info is still in the accounts table
        /*
        Vector pendingAcct = MakerCheckerManager.getPendingMakeEntryByMakeInfo("accounts");
        if ((pendingAcct != null) && (pendingAcct.size() > 0))
        {
            int total = pendingAcct.size();
            Object obj = null;
            for (int i = 0; i < total; i++)
            {
                obj = pendingAcct.elementAt(i);
                if ((obj != null) && (obj instanceof AccountEntry))
                {
                    AccountEntry ae = (AccountEntry) obj;
                    if (acctCode.equals(ae.getAcctCode()) == true)
                    {
                        logger.error("AccountsManager createAccountMakerChecker Account code " + acctCode + " is used");
                        status.setStatus(AccountsManager.ERR_ACCT_CODE_USED);
                        return status;
                    }
                }
            }
        }
        */

        // creates an account entry to store all the information of the new account
        AccountEntry ae = new AccountEntry();
        ae.setAcctName(acctName);
        ae.setAcctCode(acctCode);
        ae.setAcctType((short) acctType);
        ae.setCreatedBy(adminID);

        AccountsAdmin[] accountsAdmin = null;
        if ((aid != null) && (aid.length > 0))
        {
            accountsAdmin = new AccountsAdmin[aid.length];
            for (int i = 0; i < aid.length; i++)
            {
                try
                {
                    accountsAdmin[i] = new AccountsAdmin();
                    accountsAdmin[i].setAdminID(Integer.parseInt(aid[i]));
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager createAccountMakerChecker Error while creating accounts_admin entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_CREATE_FAIL);
                    return status;
                }
            }
        }
        ae.setAccountsAdmin(accountsAdmin);

        MerchantInAccounts[] merchantInAccounts = null;
        if ((mid != null) && (mid.length > 0))
        {
            merchantInAccounts = new MerchantInAccounts[mid.length];
            for (int i = 0; i < mid.length; i++)
            {
                try
                {
                    merchantInAccounts[i] = new MerchantInAccounts();
                    merchantInAccounts[i].setMerchantID(Integer.parseInt(mid[i]));
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager createAccountMakerChecker Error while creating merchant_in_accounts entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_CREATE_FAIL);
                    return status;
                }
            }
        }
        ae.setMerchantInAccounts(merchantInAccounts);

        PricingParameter[] pricingParameter = null;
        if ((ppid != null) && (ppid.length > 0))
        {
            pricingParameter = new PricingParameter[ppid.length];
            for (int i = 0; i < ppid.length; i++)
            {
                    pricingParameter[i] = new PricingParameter();
                    pricingParameter[i].setPricingParameterName(ppid[i]);
            }
        }
        ae.setPricingParameter(pricingParameter);

        // creates a byte output stream for the pending table entry
        ByteArrayOutputStream baos = null;
        try
        {
            baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ae);
        }
        catch (IOException ioe)
        {
            logger.error("AccountsManager createAccountMakerChecker Error while creating a byte output stream");
            logger.error(ioe);
            status.setStatus(AccountsManager.ERR_CREATE_FAIL);
            return status;
        }

        // creates maker checker pending entry to store all the information of the new account
        MakerCheckerPending mcp = new MakerCheckerPending();
        mcp.setMakeData(com.astute.common.Int2Hex.getHex(baos.toByteArray()));
        mcp.setMakeDateTime(new Date());
        mcp.setMakerID(adminID);
        mcp.setMakeInfo("accounts");
        mcp.setDescription(ae.getAcctName());
        mcp.setAction("Create Account");

        // creates a connection to store this maker checker pending entry
        Connection con = null;
        try
        {
            con = Transaction.begin(Torque.getDefaultDB());

        } catch (TorqueException te)
        {
            logger.error("AccountsManager createAccountMakerChecker Error when creating a connection to database");
            logger.error(te);
            status.setStatus(AccountsManager.ERR_CREATE_FAIL);
            return status;
        }

        // save the maker checker pending entry using the newly created connection
        try
        {
            mcp.save(con);
            Transaction.commit(con);
            status.setStatus(AccountsManager.STATUS_CREATE_OK);
        } catch (Exception e)
        {
            Transaction.safeRollback(con);
            logger.error("AccountsManager createAccountMakerChecker Error when saving the maker checker pending entry");
            logger.error(e);
            status.setStatus(AccountsManager.ERR_CREATE_FAIL);
            return status;
        }

        return status;
    }


    /**
     * Get admin users by Account ID
     *
     * @param accountID Account ID
     * @return Array of AdminiProfile
     */
    public static AdminProfile[] getAdminUsersByAcctID(int accountID) {
        logger.debug("account id is = " + accountID);
        //get the accounts admin entry first
        Criteria criteria = new Criteria();
        criteria.add(AccountsAdminPeer.ACCT_ID, accountID);

        List list = null;
        Vector tempAdminProfiles = new Vector();
        AdminProfile[] adminProfiles = null;
        try {
            list = AccountsAdminPeer.doSelect(criteria);
            if (list != null && list.size() > 0) {
                logger.debug("list size = " + list.size());
//                adminProfiles = new AdminProfile[list.size()];
                Iterator iterator = list.iterator();
//                int counter = 0;
                while (iterator.hasNext()) {
                    AccountsAdmin acctAdmin = new AccountsAdmin();
                    acctAdmin = (AccountsAdmin) iterator.next();
                    AdminUsers admin = acctAdmin.getAdminUsers();
                    // ignores the deleted admin users
                    if (admin.getAccountStatus() == ((short) AdminProfile.ACCOUNT_ACTIVE))
                    {
                        AdminProfile ap = new AdminProfile(admin.getAdminID());
                        ap.setAdminLogin(admin.getAdminLogin());
                        ap.setAdminName(admin.getAdminName());
                        ap.setAdminRole(admin.getAdminRole());
                        tempAdminProfiles.add(ap);
                    }
//                    adminProfiles[counter] = new AdminProfile(admin.getAdminID());
//                    adminProfiles[counter].setAdminLogin(admin.getAdminLogin());
//                    adminProfiles[counter].setAdminName(admin.getAdminName());
//                    adminProfiles[counter].setAdminRole(admin.getAdminRole());
//                    counter++;
                }

                // generates the list of admins which belong to the given account
                if (tempAdminProfiles.size() > 0)
                {
                    int total = tempAdminProfiles.size();
                    adminProfiles = new AdminProfile[total];
                    for (int i = 0; i < total; i++)
                        adminProfiles[i] = ((AdminProfile) tempAdminProfiles.elementAt(i));
                }
            } else {
                logger.debug("List admins gets no return at all");
                return null;
            }
        } catch (TorqueException e) {
            logger.error("Accounts admin not found");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return adminProfiles;
    }


    /**
     * Get pricing parameters by Account ID
     *
     * @param accountID Account ID
     * @return Array of PricingParameterEntry
     */
    public static PricingParameterEntry[] getPricingParameters(int accountID) {
        Criteria crit = new Criteria();
        crit.add(PricingParameterPeer.ACCT_ID, accountID);
        crit.addAscendingOrderByColumn(PricingParameterPeer.PRICING_PARAMETER_ID);

        List list = null;
        try {
            list = PricingParameterPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error("Failed to retrive pricing parameter");
            ExceptionManager.logStackTraceString(e, logger);
        }

        PricingParameterEntry[] parameters = null;
        if (list != null && list.size() > 0) {
            parameters = new PricingParameterEntry[list.size()];
            Iterator iterator = list.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
                PricingParameterEntry parameter = new PricingParameterEntry((PricingParameter) iterator.next());
                parameters[counter] = parameter;
                counter++;
            }
        }
        return parameters;
    }


    /**
     * Update Account
     *
     * @param accountID Account ID
     * @param acctName  Account Name
     * @param offActInputType Offline Activation Input Type
     * @param aid       Associating Admin IDs
     * @param mid       Associating Merchant IDs
     * @param ppid      Associating Pricing Parameter
     * @return Event status for updating
     */
    public static EventStatus updateAccounts(int accountID, String acctName, int offActInputType, String[] aid, String[] mid, String[] ppid) {
        EventStatus status = new EventStatus();
        try {
            //todo: put into 1 transaction
            //update account table
            Accounts account = AccountsPeer.retrieveByPK(accountID);
            account.setAcctName(acctName);
            account.setOffActInputType((short)offActInputType);

            //update admin access
            status = updateAccountsAdminOptimized(accountID, aid);
//            status = updateAccountsAdmin(accountID, aid);

            //update merchant in accounts
            //status = updateMerchantInAccounts(accountID, mid);
            status = updateMerchantInAccountsOptimized(accountID, mid);

            //update pricing parameter
            status = updatePricingParameter(accountID, ppid);

            account.save();
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save account");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return status;
    }


    /**
     * Edit an existing account using maker checker approach
     * @param acctId The current account Id
     * @param acctName The current account name
     * @param adminId The id of admin who edits the account
     * @param aid Associating admin Ids of the current account
     * @param mid Associating merchant Ids of the current account
     * @param ppid Associating pricing parameter of the current account
     * @return EventStatus The status of editting action
     */
    public static EventStatus updateAccountMakerChecker(int acctId, String acctName, int adminId, String[] aid, String[] mid, String[] ppid)
    {
        EventStatus status = new EventStatus();

        AccountEntry currAcct = AccountsManager.getAccountByPK(acctId);

        // makes sure there is no other pending action on this account
        Vector pendingAcct = MakerCheckerManager.getPendingMakeEntryByMakeInfo("accounts");
        if ((pendingAcct != null) && (pendingAcct.size() > 0))
        {
            int total = pendingAcct.size();
            Object obj = null;
            for (int i = 0; i < total; i++)
            {
                obj = pendingAcct.elementAt(i);
                if ((obj != null) && (obj instanceof AccountEntry))
                {
                    AccountEntry ae = (AccountEntry) obj;
                    if (acctId == ae.getAcctID())
                    {
                        logger.error("AccountsManager updateAccountMakerChecker Error this account already existed in the pending table");
                        status.setStatus(AccountsManager.ERR_PENDING_EXISTED);
                        return status;
                    }
                }
            }
        }

        // creates an account entry to store all the information of the new account
        AccountEntry ae = new AccountEntry();
        ae.setAcctID(acctId);
        ae.setAcctName(acctName);
        ae.setAcctCode(currAcct.getAcctCode());
        ae.setAcctType((short) currAcct.getAcctType());
        ae.setCreatedBy(currAcct.getCreatedBy());

        AccountsAdmin[] accountsAdmin = null;
        if ((aid != null) && (aid.length > 0))
        {
            accountsAdmin = new AccountsAdmin[aid.length];
            for (int i = 0; i < aid.length; i++)
            {
                try
                {
                    accountsAdmin[i] = new AccountsAdmin();
                    accountsAdmin[i].setAdminID(Integer.parseInt(aid[i]));
                    accountsAdmin[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager updateAccountMakerChecker Error while creating accounts_admin entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    return status;
                }
            }
        }
        ae.setAccountsAdmin(accountsAdmin);

        MerchantInAccounts[] merchantInAccounts = null;
        if ((mid != null) && (mid.length > 0))
        {
            merchantInAccounts = new MerchantInAccounts[mid.length];
            for (int i = 0; i < mid.length; i++)
            {
                try
                {
                    merchantInAccounts[i] = new MerchantInAccounts();
                    merchantInAccounts[i].setMerchantID(Integer.parseInt(mid[i]));
                    merchantInAccounts[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager updateAccountMakerChecker Error while creating merchant_in_accounts entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    return status;
                }
            }
        }
        ae.setMerchantInAccounts(merchantInAccounts);

        PricingParameter[] pricingParameter = null;
        if ((ppid != null) && (ppid.length > 0))
        {
            pricingParameter = new PricingParameter[ppid.length];
            for (int i = 0; i < ppid.length; i++)
            {
                try
                {
                    pricingParameter[i] = new PricingParameter();
                    pricingParameter[i].setPricingParameterName(ppid[i]);
                    pricingParameter[i].setAcctID(acctId);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager updateAccountMakerChecker Error while creating pricing_parameter entries");
                    logger.error(te);
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    return status;
                }
            }
        }
        ae.setPricingParameter(pricingParameter);

        // creates a byte output stream for the pending table entry
        ByteArrayOutputStream baos = null;
        try
        {
            baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ae);
        }
        catch (IOException ioe)
        {
            logger.error("AccountsManager updateAccountMakerChecker Error while creating a byte output stream");
            logger.error(ioe);
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            return status;
        }

        // creates maker checker pending entry to store all the information of the new account
        MakerCheckerPending mcp = new MakerCheckerPending();
        mcp.setMakeData(com.astute.common.Int2Hex.getHex(baos.toByteArray()));
        mcp.setMakeDateTime(new Date());
        mcp.setMakerID(adminId);
        mcp.setMakeInfo("accounts");
        mcp.setDescription(ae.getAcctName());
        mcp.setAction("Edit Account");
        mcp.setReferenceData("" + ae.getAcctID());

        // creates a connection to store this maker checker pending entry
        Connection con = null;
        try
        {
            con = Transaction.begin(Torque.getDefaultDB());

        } catch (TorqueException te)
        {
            logger.error("AccountsManager updateAccountMakerChecker Error when creating a connection to database");
            logger.error(te);
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            return status;
        }

        // save the maker checker pending entry using the newly created connection
        try
        {
            mcp.save(con);
            Transaction.commit(con);
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
        } catch (Exception e)
        {
            Transaction.safeRollback(con);
            logger.error("AccountsManager updateAccountMakerChecker Error when saving the maker checker pending entry");
            logger.error(e);
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            return status;
        }

        return status;
    }

    private static boolean isFoundInAdminUserList(List list, int targetAid) {
        boolean found = false;
        if (list != null) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                AccountsAdmin aa = (AccountsAdmin) i.next();
                if (targetAid == aa.getAdminID()) {
                    found = true;
                    break;
                }

            }
        }
        return found;
    }


    private static EventStatus updateAccountsAdminOptimized(int accountID, String[] selectedAdminIDs) {
        EventStatus status = new EventStatus();
        status.setStatus(AccountsManager.ERR_EDIT_FAIL); // default to Fail

        boolean completed = false;

        // ****************  logger - to be removed *******************************
//        if (selectedAdminIDs != null) {
//            logger.debug("ming :: AdminUser :: accountID=" + accountID + ", Number of Selected Users=" + selectedAdminIDs.length);
//            for (int i = 0; i < selectedAdminIDs.length; i++) {
//                logger.debug("ming :: AdminUser :: selected mid[" + i + "] = " + selectedAdminIDs[i]);
//            }
//        }
        // ********************************************************

        Connection con = null;
        try {
            con = Transaction.begin(Torque.getDefaultDB());
            logger.debug("ming :: AdminUser :: Connection con created");

            // If nothing selected, just delete based on the account id
            if (selectedAdminIDs == null || selectedAdminIDs.length <= 0) {
                Criteria crit = new Criteria();
                crit.add(AccountsAdminPeer.ACCT_ID, accountID);
                AccountsAdminPeer.doDelete(crit);
                completed = true;
            } else {

                // deleting database entries that are not found in Selected list
                Criteria delCrit = new Criteria();
                delCrit.add(AccountsAdminPeer.ACCT_ID, accountID);
                delCrit.addNotIn(AccountsAdminPeer.ADMIN_ID, selectedAdminIDs);

                logger.debug("ming :: AdminUser :: Performing doDelete ...");
                AccountsAdminPeer.doDelete(delCrit, con);
                logger.debug("ming :: AdminUser :: doDelete DONE");


                // inserting selected list not found in database
                Criteria selCrit = new Criteria();
                List adminIDInAcctList = null;
                selCrit.add(AccountsAdminPeer.ACCT_ID, accountID);
                //selCrit.addSelectColumn(AccountsAdminPeer.ADMIN_ID);

                logger.debug("ming :: Performing doSelect ...");
                adminIDInAcctList = AccountsAdminPeer.doSelect(selCrit, con);
                logger.debug("ming :: doSelect DONE");

                logger.debug("ming :: # of Selected Admin=" + selectedAdminIDs.length + ", # of Admin already in Database=" + adminIDInAcctList.size());
//                for (int i = 0; i < adminIDInAcctList.size(); i++) {
//                    logger.debug("ming :: Existing Admin in Database[" + i + "] = " + adminIDInAcctList.get(i));
//                }

                logger.debug("ming :: Performing doInsert ...");
                for (int i = 0; i < selectedAdminIDs.length; i++) {
                    if (isFoundInAdminUserList(adminIDInAcctList, Integer.parseInt(selectedAdminIDs[i])) == false) {
//                        logger.debug("ming :: Inserting Admin id=" + selectedAdminIDs[i]);
                        AccountsAdmin accountsAdmin = new AccountsAdmin();
                        accountsAdmin.setAcctID(accountID);
                        accountsAdmin.setAdminID(Integer.parseInt(selectedAdminIDs[i]));
                        accountsAdmin.save(con);
                    }
                }
                logger.debug("ming :: doInsert DONE");


                completed = true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            ExceptionManager.logStackTraceString(e, logger);

        } finally {
            if (con != null) {
                try {
                    if (completed) {
                        Transaction.commit(con);
                        status.setStatus(AccountsManager.STATUS_EDIT_OK);
                    } else {
                        Transaction.safeRollback(con);
                    }
                } catch (Exception e) {
                    ExceptionManager.logStackTraceString(e, logger);
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                }
            }
        }

        return status;
    }


    /**
     * Update associating admins for the account
     *
     * @param accountID Account ID
     * @param aid       New associating Admin IDs
     * @return Event status for updating
     */
    private static EventStatus updateAccountsAdmin(int accountID, String[] aid) {
        EventStatus status = new EventStatus();
        status.setStatus(AccountsManager.STATUS_EDIT_OK); // default to ok

        // If nothing selected, just delete based on the account id
        if (aid == null || aid.length <= 0) {
            Criteria crit = new Criteria();
            crit.add(AccountsAdminPeer.ACCT_ID, accountID);
            try {
                AccountsAdminPeer.doDelete(crit);
            } catch (TorqueException e) {
                status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                ExceptionManager.logStackTraceString(e, logger);
            }
            return status;
        }

        // at least 1 admin user is selected
        Hashtable actions = new Hashtable();

        //get list of all active admins that exist in the system, default to IGNORE
        Criteria crit1 = new Criteria();
        //crit1.add(AdminUsersPeer.ACCOUNT_STATUS, AdminProfile.ACCOUNT_ACTIVE);
        List allAdminList = null;
        try {
            allAdminList = AdminUsersPeer.doSelect(crit1);
            logger.debug("available admins = " + allAdminList.size());
            if (allAdminList != null && allAdminList.size() > 0) {
                Iterator iterator = allAdminList.iterator();
                AdminUsers adminUsers = null;
                while (iterator.hasNext()) {
                    adminUsers = (AdminUsers) iterator.next();
                    actions.put(new Integer(adminUsers.getAdminID()), new Integer(UPDATE_IGNORE));
                }
            }
        } catch (TorqueException e) { //if no admin users in the system return fail(very unlikely)
            status.setStatus(AccountsManager.ERR_SELECT_FAIL);
            logger.error("No admin users in the system");
            ExceptionManager.logStackTraceString(e, logger);
            return status;
        }

        // If selected, set to Add
        for (int i = 0; i < aid.length; i++) {
            actions.put(new Integer(aid[i]), new Integer(UPDATE_ADD));
        }

        //get a list of all admins that have access to this account
        Criteria crit2 = new Criteria();
        crit2.add(AccountsAdminPeer.ACCT_ID, accountID);
        List currentAcctAdminList = null;
        try {
            currentAcctAdminList = AccountsAdminPeer.doSelect(crit2);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_SELECT_FAIL);
            logger.error("Failed to retrieve current accounts admin");
            ExceptionManager.logStackTraceString(e, logger);
            return status;
        }

        int action;
        if (currentAcctAdminList != null && currentAcctAdminList.size() > 0) {
            Iterator ito = currentAcctAdminList.iterator();
            AccountsAdmin accountsAdmin = null;
            Integer adminID = null;
            for (int i = 0; i < currentAcctAdminList.size(); i++) {
                accountsAdmin = (AccountsAdmin) ito.next();
                adminID = new Integer(accountsAdmin.getAdminID());
                action = ((Integer) (actions.get(adminID))).intValue();
                // if in the database and selected, just ignore
                if (action == UPDATE_ADD) {
                    actions.put(adminID, new Integer(UPDATE_IGNORE));
                    // if in the database and not selected, delete it
                } else if (action == UPDATE_IGNORE) {
                    actions.put(adminID, new Integer(UPDATE_DELETE));
                }
            }
        }

        Enumeration enu = actions.keys();
        Integer adminID;
        while (enu.hasMoreElements()) {
            adminID = (Integer) enu.nextElement();
            action = ((Integer) actions.get(adminID)).intValue();
            if (action == UPDATE_DELETE) {
                Criteria crit = new Criteria();
                crit.add(AccountsAdminPeer.ACCT_ID, accountID);
                crit.add(AccountsAdminPeer.ADMIN_ID, adminID.intValue());
                try {
                    AccountsAdminPeer.doDelete(crit);
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }
            } else if (action == UPDATE_ADD) {
                try {
                    AccountsAdmin accountsAdmin = new AccountsAdmin();
                    accountsAdmin.setAcctID(accountID);
                    accountsAdmin.setAdminID(adminID.intValue());
                    accountsAdmin.save();
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                } catch (Exception e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    logger.error("Failed to save new admin accounts");
                    ExceptionManager.logStackTraceString(e, logger);
                }
            }
        }

        return status;
    }


    private static boolean isFoundInMerchantList(List list, int targetMid) {
        boolean found = false;
        if (list != null) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                MerchantInAccounts mia = (MerchantInAccounts) i.next();
                if (targetMid == mia.getMerchantID()) {
                    found = true;
                    break;
                }

            }
        }
        return found;
    }


    private static EventStatus updateMerchantInAccountsOptimized(int accountID, String[] selectedMIDs) {
        EventStatus status = new EventStatus();
        status.setStatus(AccountsManager.ERR_EDIT_FAIL); // default to FAIL

        boolean completed = false;

        // ****************  logger - to be removed *******************************
//        if (selectedMIDs != null) {
//            logger.debug("ming :: merchant :: accountID=" + accountID + ", Number of Selected merchant=" + selectedMIDs.length);
//            for (int i = 0; i < selectedMIDs.length; i++) {
//                logger.debug("ming :: merchant :: selected selectedMIDs[" + i + "] = " + selectedMIDs[i]);
//            }
//        }
        // ********************************************************

        Connection con = null;
        try {
            con = Transaction.begin(Torque.getDefaultDB());
            logger.debug("ming :: merchant :: Connection con created");

            // If nothing selected, just delete based on the account id
            if (selectedMIDs == null || selectedMIDs.length <= 0) {
                Criteria crit = new Criteria();
                crit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
                MerchantInAccountsPeer.doDelete(crit, con);
                //merchant in grouping deletion by davidliu
//                crit = new Criteria();
//                crit.addJoin(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, MerchantGroupingPeer.MERCHANT_GROUPING_ID);
//                crit.add(MerchantGroupingPeer.ACCT_ID, accountID);
//                MerchantInGroupingPeer.doDelete(crit, con);
                crit = new Criteria();
                crit.add(MerchantGroupingPeer.ACCT_ID, accountID);
                crit.addSelectColumn(MerchantGroupingPeer.MERCHANT_GROUPING_ID);
                
                String query = "";
                try {
                    query = MerchantGroupingPeer.createQueryString(crit);
                } catch (TorqueException e) {
                    logger.error(e);
                    ExceptionManager.logStackTraceString(e, logger);
                }
                
                
                List list = null;
                try {
                    list = MerchantGroupingPeer.executeQuery(query);
                    if (list != null && list.size()>0){
                    	int[] merchangGroupingIds = new int[list.size()];
                    	Iterator iter = list.iterator();
                    	int i = 0;
                    	while (iter.hasNext()) {
                            Record r = (Record) iter.next();
                            int id = r.getValue(1).asInt();
                            merchangGroupingIds[i] = id;
                            i++;
                    	}
                    	
                    	//deletion
	                      crit = new Criteria();
	                      crit.addIn(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, merchangGroupingIds);
	                      MerchantInGroupingPeer.doDelete(crit, con);
                    	
                    }

                    


                } catch (Exception e) {
                    logger.error(e);
                    ExceptionManager.logStackTraceString(e, logger);
                }
                //
                
                completed = true;
            } else {
                // gets the linkings that are inside the database but not found in the selected list
                Criteria crit = new Criteria();
                crit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
                crit.addNotIn(MerchantInAccountsPeer.MERCHANT_ID, selectedMIDs);

                List list = null;
                logger.debug("AccountsManager updateMerchantInAccountsOptimized Select the linkings that need to be deleted");
                list = MerchantInAccountsPeer.doSelect(crit);
                logger.debug("AccountsManager updateMerchantInAccountsOptimized Finish selecting");
                if ((list != null) && (list.size() > 0))
                {
                    Iterator ito = list.iterator();
                    while (ito.hasNext())
                    {
                        MerchantInAccounts temp = (MerchantInAccounts) ito.next();

                        // saves deleted merchant linkings into merchant_linking_report table
                        Merchant merchant = MerchantPeer.retrieveByPK(temp.getMerchantID());
                        Accounts account = AccountsPeer.retrieveByPK(accountID);
                        AdminUsers au = AdminUsersPeer.retrieveByPK(account.getCreatedBy());
                        MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                    MerchantManager.MERCHANT_LINKING_REPORT_ACTION_DELETED,
                                                                    merchant.getMerchantID(), merchant.getName(),
                                                                    accountID, "", new Date(),
                                                                    au.getAdminName());
                    }
                }


                // deleting database entries that are not found in Selected list
                Criteria delCrit = new Criteria();
                delCrit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
                delCrit.addNotIn(MerchantInAccountsPeer.MERCHANT_ID, selectedMIDs);

                logger.debug("ming :: merchant :: Performing doDelete ...");
                MerchantInAccountsPeer.doDelete(delCrit, con);
                logger.debug("ming :: merchant :: doDelete DONE");
                
                // deleting merchant in grouping, by davidliu
//                delCrit = new Criteria();
//                delCrit.addJoin(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, MerchantGroupingPeer.MERCHANT_GROUPING_ID);
//                delCrit.add(MerchantGroupingPeer.ACCT_ID, accountID);
//                delCrit.addNotIn(MerchantInGroupingPeer.MERCHANT_ID, selectedMIDs);
//                
//                logger.debug("ming :: merchant :: Performing doDelete ...");
//                MerchantInGroupingPeer.doDelete(delCrit, con);
//                logger.debug("ming :: merchant :: doDelete DONE");
                crit = new Criteria();
                crit.add(MerchantGroupingPeer.ACCT_ID, accountID);
                crit.addSelectColumn(MerchantGroupingPeer.MERCHANT_GROUPING_ID);
                
                String query = "";
                try {
                    query = MerchantGroupingPeer.createQueryString(crit);
                } catch (TorqueException e) {
                    logger.error(e);
                    ExceptionManager.logStackTraceString(e, logger);
                }
                
                
                list = null;
                try {
                    list = MerchantGroupingPeer.executeQuery(query);
                    if (list != null && list.size()>0){
                    	int[] merchangGroupingIds = new int[list.size()];
                    	Iterator iter = list.iterator();
                    	int i = 0;
                    	while (iter.hasNext()) {
                            Record r = (Record) iter.next();
                            int id = r.getValue(1).asInt();
                            merchangGroupingIds[i] = id;
                            i++;
                    	}
                    	
                    	//deletion
	                      crit = new Criteria();
	                      crit.addIn(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, merchangGroupingIds);
	                      crit.addNotIn(MerchantInGroupingPeer.MERCHANT_ID, selectedMIDs);
	                      MerchantInGroupingPeer.doDelete(crit, con);
                    	
                    }

                    


                } catch (Exception e) {
                    logger.error(e);
                    ExceptionManager.logStackTraceString(e, logger);
                }
                //


                // inserting selected list not found in database
                Criteria selCrit = new Criteria();
                List midInAcctList = null;
                selCrit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
                //selCrit.addSelectColumn(MerchantInAccountsPeer.MERCHANT_ID);

                logger.debug("ming :: merchant :: Performing doSelect ...");
                midInAcctList = MerchantInAccountsPeer.doSelect(selCrit, con);
                logger.debug("ming :: merchant :: doSelect DONE");

                logger.debug("ming :: merchant :: # of Selected merchant=" + selectedMIDs.length + ", # of merchant in Database=" + midInAcctList.size());
//                for (int i = 0; i < midInAcctList.size(); i++) {
//                    logger.debug("ming :: merchant :: Database selectedMIDs[" + i + "] = " + midInAcctList.get(i));
//                }

                logger.debug("ming :: merchant :: Performing doInsert ...");
                for (int i = 0; i < selectedMIDs.length; i++) {
                    if (isFoundInMerchantList(midInAcctList, Integer.parseInt(selectedMIDs[i])) == false) {
//                        logger.debug("ming :: merchant :: ##### Inserting selectedMIDs=" + selectedMIDs[i]);
                        MerchantInAccounts merchantInAcct = new MerchantInAccounts();
                        merchantInAcct.setAcctID(accountID);
                        merchantInAcct.setMerchantID(Integer.parseInt(selectedMIDs[i]));
                        merchantInAcct.save(con);

                        // saves inserted merchant linkings into merchant_linking_report table
                        Merchant merchant = MerchantPeer.retrieveByPK(merchantInAcct.getMerchantID());
                        Accounts account = AccountsPeer.retrieveByPK(accountID);
                        AdminUsers au = AdminUsersPeer.retrieveByPK(account.getCreatedBy());
                        MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                    MerchantManager.MERCHANT_LINKING_REPORT_ACTION_ADDED,
                                                                    merchant.getMerchantID(), merchant.getName(),
                                                                    accountID, "", new Date(),
                                                                    au.getAdminName());
                    }
                }
                logger.debug("ming :: merchant :: doInsert DONE");

                completed = true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            ExceptionManager.logStackTraceString(e, logger);

        } finally {
            if (con != null) {
                try {
                    if (completed) {
                        Transaction.commit(con);
                        status.setStatus(AccountsManager.STATUS_EDIT_OK);
                    } else {
                        Transaction.safeRollback(con);
                    }
                } catch (Exception e) {
                    ExceptionManager.logStackTraceString(e, logger);
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                }
            }
        }

        return status;
    }


    /**
     * Update associating merchants for the account
     *
     * @param accountID Account ID
     * @param mid       New associating Merchant IDs
     * @return Event status for updating
     */
    private static EventStatus updateMerchantInAccounts(int accountID, String[] mid) {
        EventStatus status = new EventStatus();
        status.setStatus(AccountsManager.STATUS_EDIT_OK); // default to ok

        // If nothing selected, just delete based on the account id
        if (mid == null || mid.length <= 0) {
            Criteria crit = new Criteria();
            crit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
            try {
                MerchantInAccountsPeer.doDelete(crit);
            } catch (TorqueException e) {
                status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                ExceptionManager.logStackTraceString(e, logger);
            }
            return status;
        }

        // at least 1 merchant is selected
        Hashtable actions = new Hashtable();

        //get list of all active merchants that exist in the system, default to IGNORE
        Criteria crit1 = new Criteria();
        crit1.add(MerchantPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);
        List allMerchantList = null;
        try {
            allMerchantList = MerchantPeer.doSelect(crit1);
            logger.debug("available merchants = " + allMerchantList.size());
            if (allMerchantList != null && allMerchantList.size() > 0) {
                Iterator iterator = allMerchantList.iterator();
                Merchant merchant = null;
                while (iterator.hasNext()) {
                    merchant = (Merchant) iterator.next();
                    actions.put(new Integer(merchant.getMerchantID()), new Integer(UPDATE_IGNORE));
                }
            }
        } catch (TorqueException e) { //if no merchants in the system return fail(very unlikely)
            status.setStatus(AccountsManager.ERR_SELECT_FAIL);
            logger.error("No merchants in the system");
            ExceptionManager.logStackTraceString(e, logger);
            return status;
        }

        // If selected, set to Add
        for (int i = 0; i < mid.length; i++) {
            actions.put(new Integer(mid[i]), new Integer(UPDATE_ADD));
        }

        //get a list of all merchants that belongs to this account
        Criteria crit2 = new Criteria();
        crit2.add(MerchantInAccountsPeer.ACCT_ID, accountID);
        List currentMerchantList = null;
        try {
            currentMerchantList = MerchantInAccountsPeer.doSelect(crit2);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_SELECT_FAIL);
            logger.error("Failed to retrieve current merchants in account");
            ExceptionManager.logStackTraceString(e, logger);
            return status;
        }

        int action;
        if (currentMerchantList != null && currentMerchantList.size() > 0) {
            Iterator ito = currentMerchantList.iterator();
            MerchantInAccounts merchantInAccounts = null;
            Integer merchantID = null;
            for (int i = 0; i < currentMerchantList.size(); i++) {
                merchantInAccounts = (MerchantInAccounts) ito.next();
                merchantID = new Integer(merchantInAccounts.getMerchantID());
                action = ((Integer) (actions.get(merchantID))).intValue();
                // if in the database and selected, just ignore
                if (action == UPDATE_ADD) {
                    actions.put(merchantID, new Integer(UPDATE_IGNORE));
                    // if in the database and not selected, delete it
                } else if (action == UPDATE_IGNORE) {
                    actions.put(merchantID, new Integer(UPDATE_DELETE));
                }
            }
        }

        Enumeration enu = actions.keys();
        Integer merchantID;
        while (enu.hasMoreElements()) {
            merchantID = (Integer) enu.nextElement();
            action = ((Integer) actions.get(merchantID)).intValue();
            if (action == UPDATE_DELETE) {
                Criteria crit = new Criteria();
                crit.add(MerchantInAccountsPeer.ACCT_ID, accountID);
                crit.add(MerchantInAccountsPeer.MERCHANT_ID, merchantID.intValue());
                try {
                    MerchantInAccountsPeer.doDelete(crit);
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }
            } else if (action == UPDATE_ADD) {
                try {
                    MerchantInAccounts merchantInAcct = new MerchantInAccounts();
                    merchantInAcct.setAcctID(accountID);
                    merchantInAcct.setMerchantID(merchantID.intValue());
                    merchantInAcct.save();
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                } catch (Exception e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    logger.error("Failed to save new merchant in accounts");
                    ExceptionManager.logStackTraceString(e, logger);
                }
            }
        }

        return status;
    }


    /**
     * Update associating pricing parameters for the account
     *
     * @param accountID Account ID
     * @param ppid      New associating Pricing Parameter
     * @return Event status for updating
     */
    private static EventStatus updatePricingParameter(int accountID, String[] ppid) {
        EventStatus status = new EventStatus();
        status.setStatus(AccountsManager.STATUS_EDIT_OK); // default to ok

        // If nothing selected, just delete based on the account id
        if (ppid == null || ppid.length <= 0) {
            Criteria crit = new Criteria();
            crit.add(PricingParameterPeer.ACCT_ID, accountID);
            Criteria crit2 = new Criteria();
            crit2.addJoin(PricingParameterPeer.PRICING_PARAMETER_ID, PricingValuePeer.PRICING_PARAMETER_ID);
            crit2.add(PricingParameterPeer.ACCT_ID, accountID);

            try {
                PricingValuePeer.doDelete(crit2);
                PricingParameterPeer.doDelete(crit);
            } catch (TorqueException e) {
                status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                ExceptionManager.logStackTraceString(e, logger);
            }
            return status;
        }

        // at least 1 pricing parameter is selected
        Hashtable actions = new Hashtable();

        //get list of all pricing parameters that exist in the system, default to IGNORE
        String[] parameterNames = PricingParameterEntry.getAllPricingParameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            actions.put(parameterNames[i], new Integer(UPDATE_IGNORE));
        }

        // If selected, set to Add
        for (int i = 0; i < ppid.length; i++) {
            actions.put(ppid[i], new Integer(UPDATE_ADD));
        }

        //get a list of all pricing parameters that belongs to this account
        Criteria crit2 = new Criteria();
        crit2.add(PricingParameterPeer.ACCT_ID, accountID);
        List currentPricingParameter = null;
        try {
            currentPricingParameter = PricingParameterPeer.doSelect(crit2);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_SELECT_FAIL);
            logger.error("Failed to retrieve current pricing parameters in account");
            ExceptionManager.logStackTraceString(e, logger);
            return status;
        }

        int action;
        if (currentPricingParameter != null && currentPricingParameter.size() > 0) {
            Iterator ito = currentPricingParameter.iterator();
            PricingParameter pricingParameter = null;
            String name = null;
            for (int i = 0; i < currentPricingParameter.size(); i++) {
                pricingParameter = (PricingParameter) ito.next();
                name = pricingParameter.getPricingParameterName();
                action = ((Integer) (actions.get(name))).intValue();
                // if in the database and selected, just ignore
                if (action == UPDATE_ADD) {
                    actions.put(name, new Integer(UPDATE_IGNORE));
                    // if in the database and not selected, delete it
                } else if (action == UPDATE_IGNORE) {
                    actions.put(name, new Integer(UPDATE_DELETE));
                }
            }
        }
        Hashtable pvs = new Hashtable();
        for (int j = 0; j < parameterNames.length; j++) {
            action = ((Integer) (actions.get(parameterNames[j]))).intValue();
             if (action == UPDATE_ADD) {
                try {
                    Criteria criteria = new Criteria();
                    criteria.addJoin(PricingParameterPeer.PRICING_PARAMETER_ID, PricingValuePeer.PRICING_PARAMETER_ID);
                    criteria.add(PricingParameterPeer.ACCT_ID, accountID);
                    criteria.add(PricingParameterPeer.PRICING_PARAMETER_NAME, parameterNames[j]);
                    List a=PricingValuePeer.doSelect(criteria);
                    Iterator ito = a.iterator();
                    Vector v= new Vector();
                    while(ito.hasNext()){
                        PricingValue pv= (PricingValue) ito.next();
                        v.add(pv);
                    }
                    pvs.put(parameterNames[j],v);

                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                } catch (Exception e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }
            }
        }

        for (int i = 0; i < parameterNames.length; i++) {
            action = ((Integer) (actions.get(parameterNames[i]))).intValue();
            if (action == UPDATE_DELETE) {

                Criteria criteria2 = new Criteria();
                criteria2.addJoin(PricingParameterPeer.PRICING_PARAMETER_ID, PricingValuePeer.PRICING_PARAMETER_ID);
                criteria2.add(PricingParameterPeer.ACCT_ID, accountID);
                criteria2.add(PricingParameterPeer.PRICING_PARAMETER_NAME, parameterNames[i]);

                try {
                    PricingValuePeer.doDelete(criteria2);


                    status.setStatus(AccountsManager.STATUS_EDIT_OK);
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }

                Criteria criteria = new Criteria();
                         criteria.add(PricingParameterPeer.ACCT_ID, accountID);
                         criteria.add(PricingParameterPeer.PRICING_PARAMETER_NAME, parameterNames[i]);
                
                try {
                    PricingParameterPeer.doDelete(criteria);


                    status.setStatus(AccountsManager.STATUS_EDIT_OK);
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }
            } else if (action == UPDATE_ADD) {
                try {
                    PricingParameter parameter = new PricingParameter();
                    parameter.setPricingParameterName(parameterNames[i]);
                    parameter.setAcctID(accountID);
                    parameter.save();
                    Vector v = (Vector)pvs.get(parameterNames[i]);
                    if(v!=null &&v.size()>0){
                        for(int k=0;k<v.size();k++){
                            PricingValue pv = new PricingValue();
                            PricingValue pvT=(PricingValue)v.get(k);
                            pv.setCardProgramID(pvT.getCardProgramID());
                            pv.setPricingParameterID(parameter.getPricingParameterID());
                            pv.setLevel(pvT.getLevel());
                            pv.setValue(pvT.getValue());
                            pv.save();

                        }

                    }
                    status.setStatus(AccountsManager.STATUS_EDIT_OK);
                } catch (TorqueException e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                } catch (Exception e) {
                    status.setStatus(AccountsManager.ERR_EDIT_FAIL);
                    ExceptionManager.logStackTraceString(e, logger);
                }
            }
        }

        return status;
    }


    /**
     * Get Account Entries by Admin ID, return null if not found
     *
     * @param adminID Admin ID
     * @return Account Entries of the Admin, return null if not found
     */
    public static AccountEntry[] getAccountIDsByAdminID(int adminID) {
        AccountEntry[] accountEntries = null;

        Criteria crit = new Criteria();
        crit.add(AccountsAdminPeer.ADMIN_ID, adminID);
        crit.addJoin(AccountsPeer.ACCT_ID, AccountsAdminPeer.ACCT_ID);
        crit.addAscendingOrderByColumn(AccountsPeer.ACCT_NAME);

        List list = null;
        try {
            list = AccountsAdminPeer.doSelect(crit);
        } catch (TorqueException e) {
            ExceptionManager.logStackTraceString(e, logger);
        }
        try {
            if (list != null && list.size() > 0) {
                Iterator ito = list.iterator();
                accountEntries = new AccountEntry[list.size()];
                AccountsAdmin accountsAdmin = null;
                int counter = 0;
                while (ito.hasNext()) {
                    accountsAdmin = (AccountsAdmin) ito.next();
                    accountEntries[counter] = new AccountEntry(accountsAdmin.getAccounts());
                    counter++;
                }
            }
        } catch (TorqueException e) {
            ExceptionManager.logStackTraceString(e, logger);
            return null;
        }
        return accountEntries;
    }


    /**
     * Get Account ID by session, return -1 if not found
     *
     * @param sd User session data
     * @return Account ID of the Admin, return -1 if not found
     */
    public static int getAccountIDBySession(SessionData sd) {
        int accountID = -1;

        Object accountIDObj = sd.getSession().getAttribute(SESSION_ATTR_ACCOUNTS_ID);

        String accountIDStr = null;
        if (accountIDObj != null) {
            accountIDStr = (String) accountIDObj;
        }
        try {
            if (accountIDStr != null && accountIDStr.trim().length() > 0) {
                accountID = Integer.parseInt(accountIDStr);
            }
        } catch (Exception e) {
            ExceptionManager.logStackTraceString(e, logger);
        }
        return accountID;
    }


    /**
     * delete admins that belongs to this account
     *
     * @param acctID
     * @return Event status for updating
     */
    private static EventStatus deleteAdminInAccounts(int acctID) {
        EventStatus status = new EventStatus();
        Criteria crit = new Criteria();
        crit.add(AccountsAdminPeer.ACCT_ID, acctID);
        try {
            AccountsAdminPeer.doDelete(crit);
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
        } catch (TorqueException e) {
            ExceptionManager.logStackTraceString(e, logger);
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
        }
        logger.debug("status is " + status.getStatus());
        return status;
    }


    /**
     * Get the next available account code
     *
     * @return account code
     */
    public static String getNextAvailableAccountCode() {
        String nextAvailAcctCode = "";
        int max_acct_code = (int) Math.pow(10, MAX_ACCT_CODE_LENGTH);
        Criteria crit = new Criteria();
        String acctCode = null;
        int i = 1;
        boolean found = false;
        while (!found && i < max_acct_code) {
            acctCode = StringUtil.getExactLengthString(new Integer(i).toString(), MAX_ACCT_CODE_LENGTH, '0');
            crit.add(AccountsPeer.ACCT_CODE, acctCode);
            List list = null;
            try {
                list = AccountsPeer.doSelect(crit);
                if (list == null || list.size() <= 0) {
                    found = true;
                    nextAvailAcctCode = acctCode;
                }
            } catch (TorqueException e) {
                logger.error(e);
                ExceptionManager.logStackTraceString(e, logger);
            }
            i++;
        }
        logger.debug("The next available acctCode is " + nextAvailAcctCode);
        return nextAvailAcctCode;
    }


    /**
     * Checks if the protocol is allowed for a terminal. To be used for global prepaid.
     *
     * @param protocol
     * @param mid
     * @param tid
     * @return true if the protocol is allowed. false if the protocol is not allowed
     */
    public static boolean isProtocolAllowed(String protocol, String mid, String tid) {
        boolean retVal = false;
        // first check at terminal level, if no record exist, then check at merchant level, if both not exist means allow.
        Criteria crit = new Criteria();
        crit.addJoin(TerminalPeer.TERMINAL_ID, TerminalMenuProfilePeer.TERMINAL_ID);
        crit.add(TerminalPeer.TERMINAL_IDENTIFIER, tid);


        List ls = null;
        try {
            ls = TerminalMenuProfilePeer.doSelect(crit);
        } catch (TorqueException e) {
            e.printStackTrace();
            ExceptionManager.logStackTraceString(e, logger);
        }

        if (ls != null && ls.size() > 0) {
            // there is a profile linked to the terminal
            crit.addJoin(MenuProfileProtocolPeer.MENU_PROFILE_ID, TerminalMenuProfilePeer.MENU_PROFILE_ID);
            // change DISALLOWED to ALLOWED, tzeyong, 24012006
            crit.add(MenuProfileProtocolPeer.ALLOWED_PROTOCOL, protocol);

            ls = null;
            try {
                ls = TerminalMenuProfilePeer.doSelect(crit);
            } catch (TorqueException e) {
                e.printStackTrace();
                ExceptionManager.logStackTraceString(e, logger);
            }

            if (ls != null && ls.size() > 0) {
                // if record is found, means that this protocol is allowed for this terminal
                // change to return true when protocol found in table, tzeyong, 24012006
                retVal = true;
            }

        } else {
            crit.clear();
            crit.addJoin(MerchantPeer.MERCHANT_ID, MerchantMenuProfilePeer.MERCHANT_ID);
            crit.addJoin(MenuProfileProtocolPeer.MENU_PROFILE_ID, MerchantMenuProfilePeer.MENU_PROFILE_ID);
            crit.add(MerchantPeer.MERCHANT_NO, mid);
            // change from DISALLOWED to ALLOWED, tzeyong, 24012006
            crit.add(MenuProfileProtocolPeer.ALLOWED_PROTOCOL, protocol);

            ls = null;
            try {
                ls = MerchantMenuProfilePeer.doSelect(crit);
            } catch (TorqueException e) {
                e.printStackTrace();
                ExceptionManager.logStackTraceString(e, logger);
            }

            if (ls != null && ls.size() > 0) {
                // if record found, mean this protocol is allowed for this merchant.
                // change to return true when protocol found in table, tzeyong, 24012006
                retVal = true;
            }

        }

        return retVal;
    }


    /* Added for breakage report */
    public static AccountEntry[] getAllGlobalPrepaidAccountEntries() {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_NOT_DELETED, Criteria.EQUAL);
        crit.add(AccountsPeer.ACCT_TYPE, AccountEntry.ACCOUNT_TYPE_GLOBAL_PREPAID, Criteria.EQUAL);

        AccountEntry[] ae = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        if ((list != null) && (list.size() > 0)) {
            ae = new AccountEntry[list.size()];
            Iterator ito = list.iterator();
            int count = 0;
            while (ito.hasNext()) {
                ae[count] = new AccountEntry((Accounts) ito.next());
                count++;
            }
//            logger.debug("Nhan BREAKAGE REPORT\t" + "Num of global accounts " + ae.length);
        } else {
            logger.debug("No accounts found");
        }

        return ae;
    }
    /* end */

    /**
     * @param acctType - Account Type : Global Prepaid / Merchant Prepaid / Loyalty
     * @return int[] of all the AcctID that is of Account Type = acctType
     */
    public static int[] getAcctIDsWithAcctType(int acctType) {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.DEL_FLAG, DEL_FLAG_NOT_DELETED, Criteria.EQUAL);
        crit.add(AccountsPeer.ACCT_TYPE, acctType, Criteria.EQUAL);

        int[] resultAcctIDs = null;
        List list = null;
        try {
            list = AccountsPeer.doSelect(crit);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        if ((list != null) && (list.size() > 0)) {
            resultAcctIDs = new int[list.size()];
            Iterator ito = list.iterator();
            int count = 0;
            try {
                while (ito.hasNext()) {
                    resultAcctIDs[count++] = ((Accounts) ito.next()).getAcctID();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.error(e.toString());
            } catch (ArrayStoreException e) {
                logger.error(e.toString());
            }
        } else {
            logger.debug("No accounts found");
        }

        return resultAcctIDs;
    }


    /**
     * Get account ID by account code.
     *
     * @param acctCode account code
     * @return int account ID
     */
    public static int getAccountIDByAccountCode(String acctCode)
    {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.ACCT_CODE, acctCode);
        crit.add(AccountsPeer.DEL_FLAG, 0, Criteria.EQUAL);

        List list = null;
        try
        {
            list = AccountsPeer.doSelect(crit);
        }
        catch (TorqueException e)
        {
            logger.error("ERROR when selecting the account");
            ExceptionManager.logStackTraceString(e, logger);
        }

        int acctId = -1;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            acctId = ((Accounts) ito.next()).getAcctID();
        }

        return acctId;
    }


    /**
     * Gets MerchantInAccounts by acctId and merchantId
     * @param acctId
     * @param merchantId
     * @return
     */
    public static MerchantInAccounts getMerchantInAccountsByAcctMerchantID(int acctId, int merchantId)
    {
        Criteria crit = new Criteria();
        crit.add(MerchantInAccountsPeer.ACCT_ID, acctId);
        crit.add(MerchantInAccountsPeer.MERCHANT_ID, merchantId);

        List list = null;
        try
        {
            list = MerchantInAccountsPeer.doSelect(crit);
        }
        catch (TorqueException e)
        {
            logger.error("ERROR when selecting merchant in accounts");
            ExceptionManager.logStackTraceString(e, logger);
        }

        MerchantInAccounts mia = null;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            mia = (MerchantInAccounts) ito.next();
        }

        return mia;
    }


    /**
     * Counts the total number of entries in the pending table which belong to the accounts category
     * @param makeInfo the category
     * @return int total number of entries
     */
    public static int getTotalPendingMakeCount(String makeInfo)
    {
        Criteria crit = new Criteria();
        if ((makeInfo != null) && (makeInfo.equals("") == false))
        {
            crit.add(MakerCheckerPendingPeer.MAKE_INFO, makeInfo);
        }

        List list = null;
        try
        {
            list = MakerCheckerPendingPeer.doSelect(crit);
        }
        catch (TorqueException te)
        {
            logger.error("AccountsManager getTotalPendingMakeCount Error when selecting pending entries");
            logger.error(te);
        }

        int count = 0;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            while (ito.hasNext())
            {
                MakerCheckerPending mcp = (MakerCheckerPending) ito.next();

                try
                {
                    ByteArrayInputStream bais = new ByteArrayInputStream(com.astute.common.Int2Hex.getHex(mcp.getMakeData()));
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    ois.close();
                    bais.close();

                    if ((obj != null) && (obj instanceof AccountEntry))
                        count++;
                }
                catch (Exception e)
                {
                    logger.error("AccountsManager getTotalPendingCount Error when creating input stream");
                    logger.error(e);
                }
            }
        }

        return count;
    }


    /**
     * Gets the selected entries in the pending table which belong to the given category
     * @param makeInfo category
     * @param orderBy orderring
     * @param page the current page (if page < 0 and rowsPerPage < 0, then get all records)
     * @param rowsPerPage the number of rows per page
     * @return Vector the selected entries
     */
    public static Vector getPendingMake(String makeInfo, String orderBy, int page, int rowsPerPage)
    {
        Criteria crit = new Criteria();
        if ((page > 0) && (rowsPerPage > 0))
            crit.setLimit(page * rowsPerPage);
        if ((makeInfo != null) && (makeInfo.equals("") == false))
            crit.add(MakerCheckerPendingPeer.MAKE_INFO, makeInfo);
        crit.addAscendingOrderByColumn(orderBy);

        List list = null;
        try
        {
            List tempList = MakerCheckerPendingPeer.doSelect(crit);
            if ((page > 0) && (rowsPerPage > 0))
                list = tempList.subList((page - 1) * rowsPerPage, tempList.size());
            else
                list = tempList;
        }
        catch (TorqueException te)
        {
            logger.error("AccountsManager getPendingMake Error when selecting from database");
            logger.error(te);
            return new Vector();
        }

        Vector result = new Vector();
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            while (ito.hasNext())
            {
                MakerCheckerPending mcp = (MakerCheckerPending) ito.next();
                PendingMakeEntry pme = new PendingMakeEntry(mcp);

                try
                {
                    ByteArrayInputStream bais = new ByteArrayInputStream(com.astute.common.Int2Hex.getHex(pme.getMakeData()));
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    ois.close();
                    bais.close();

                    if ((obj != null) && (obj instanceof AccountEntry))
                        result.add(pme);
                }
                catch (Exception e)
                {
                    logger.error("AccountsManager getTotalPendingCount Error when creating input stream");
                    logger.error(e);
                    return new Vector();
                }
            }
        }

        return result;
    }


    /**
     * Creates a new account
     * @param pid The pending Id
     * @return EventStatus The status of creating action
     */
    public static EventStatus commitCreateAccounts(int pid)
    {
        EventStatus status = new EventStatus();

        Object obj = MakerCheckerManager.getPendingMakeEntry(pid);
        if ((obj != null) && (obj instanceof AccountEntry))
        {
            Connection con = null;
            try
            {
                con = Transaction.begin(Torque.getDefaultDB());
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitCreateAccounts Error when creating a connection to database");
                logger.error(te);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            AccountEntry ae = (AccountEntry) obj;
            Accounts acct = new Accounts();
            try
            {
                acct = new Accounts();
                acct.setAcctName(ae.getAcctName());
                acct.setAcctCode(ae.getAcctCode());
                acct.setAcctType((short) ae.getAcctType());
                acct.setCreatedBy((short) ae.getCreatedBy());
                acct.save(con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitCreateAccounts Error when saving account info to database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            AccountsAdmin[] acctAdmin = ae.getAccountsAdmin();
            if (acctAdmin != null)
            {
                try
                {
                    for (int i = 0; i < acctAdmin.length; i++)
                    {
                        acctAdmin[i].setAcctID(acct.getAcctID());
                        acctAdmin[i].save(con);
                    }
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitCreateAccounts Error when saving accounts_admin info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            MerchantInAccounts[] merInAcct = ae.getMerchantInAccounts();
            if (merInAcct != null)
            {
                try
                {
                    for (int i = 0; i < merInAcct.length; i++)
                    {
                        merInAcct[i].setAcctID(acct.getAcctID());
                        merInAcct[i].save(con);

                        // saves merchant linkings into merchant_linking_report table
                        Merchant merchant = MerchantPeer.retrieveByPK(merInAcct[i].getMerchantID());
                        AdminUsers au = AdminUsersPeer.retrieveByPK(acct.getCreatedBy());
                        MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                    MerchantManager.MERCHANT_LINKING_REPORT_ACTION_ADDED,
                                                                    merchant.getMerchantID(), merchant.getName(),
                                                                    acct.getAcctID(), "", new Date(),
                                                                    au.getAdminName());
                    }
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitCreateAccounts Error when saving merchant_in_accounts info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            PricingParameter[] pricing = ae.getPricingParameter();
            if (pricing != null)
            {
                try
                {
                    for (int i = 0; i < pricing.length; i++)
                    {
                        pricing[i].setAcctID(acct.getAcctID());
                        pricing[i].save(con);
                    }
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitCreateAccounts Error when saving merchant_in_accounts info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            try
            {
                Transaction.commit(con);
                status.setStatus(AccountsManager.ACCOUNT_CREATED);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitCreateAccounts Error when commit the Torque transaction");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }
        }

        return status;
    }


    /**
     * Edits an account
     * @param pid The pending Id
     * @return EventStatus The status of editting action
     */
    public static EventStatus commitEditAccounts(int pid)
    {
        EventStatus status = new EventStatus();

        Object obj = MakerCheckerManager.getPendingMakeEntry(pid);
        if ((obj != null) && (obj instanceof AccountEntry))
        {
            Connection con = null;
            try
            {
                con = Transaction.begin(Torque.getDefaultDB());
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error when creating a connection to database");
                logger.error(te);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // the account info in the pending table
            AccountEntry ae = (AccountEntry) obj;

            // retrieves the account from the accounts table
            Accounts acct = AccountsManager.getAcctByAcctId(ae.getAcctID());
            if (acct != null)
            {
                // updates the account info in the accounts table with data from the pending table
                try
                {
                    acct.setAcctName(ae.getAcctName());
                    acct.setAcctCode(ae.getAcctCode());
                    acct.setAcctType((short) ae.getAcctType());
                    acct.setCreatedBy((short) ae.getCreatedBy());
                    acct.save(con);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitEditAccounts Error when saving account info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }
            else
            {
                logger.error("AccountsManager commitEditAccounts Error the account not found");
                status.setStatus(AccountsManager.ERR_NOT_FOUND);
                return status;
            }

            // deletes the current accounts_admin entries of this account
            Criteria crit = new Criteria();
            crit.add(AccountsAdminPeer.ACCT_ID, ae.getAcctID());

            try
            {
                AccountsAdminPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error deleting the current accounts_admin from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // saves the new accounts_admin of this account to database
            AccountsAdmin[] acctAdmin = ae.getAccountsAdmin();
            if (acctAdmin != null)
            {
                try
                {
                    for (int i = 0; i < acctAdmin.length; i++)
                        acctAdmin[i].save(con);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitEditAccounts Error when saving new accounts_admin info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            // gets the current merchant ids of this account in the merchant_in_accounts table
            crit = new Criteria();
            crit.add(MerchantInAccountsPeer.ACCT_ID, ae.getAcctID());

            List list = null;
            try
            {
                list = MerchantInAccountsPeer.doSelect(crit);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error when selecting current merchant linkings from database");
                logger.error(te);
            }

            int[] currentMerId = null;
            if ((list != null) && (list.size() > 0))
            {
                currentMerId = new int[list.size()];
                Iterator ito = list.iterator();
                int count = 0;
                while (ito.hasNext())
                {
                    currentMerId[count] = ((MerchantInAccounts) ito.next()).getMerchantID();
                    count++;
                }
            }

            // deletes the current merchant_in_accounts entries of this account
            crit = new Criteria();
            crit.add(MerchantInAccountsPeer.ACCT_ID, ae.getAcctID());

            try
            {
                MerchantInAccountsPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error deleting the current merchant_in_accounts from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // saves the new merchant_in_accounts of this account to database
            MerchantInAccounts[] merInAcct = ae.getMerchantInAccounts();
            if (merInAcct != null)
            {
                try
                {
                    for (int i = 0; i < merInAcct.length; i++)
                        merInAcct[i].save(con);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitEditAccounts Error when saving merchant_in_accounts info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            // saves merchant linkings into merchant_linking_report table
            try
            {
                // saves deleted merchant linkings into merchant_linking_report table
                if (currentMerId != null)
                {
                    if (merInAcct != null)
                    {
                        int totalCurrent = currentMerId.length;
                        int totalMer = merInAcct.length;

                        for (int i = 0; i < totalCurrent; i++)
                        {
                            boolean deleted = true;
                            for (int j = 0; j < totalMer; j++)
                                if (currentMerId[i] == merInAcct[j].getMerchantID())
                                {
                                    deleted = false;
                                    break;
                                }

                            if (deleted == true)
                            {
                                Merchant merchant = MerchantPeer.retrieveByPK(currentMerId[i]);
                                AdminUsers au = AdminUsersPeer.retrieveByPK(acct.getCreatedBy());
                                MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                            MerchantManager.MERCHANT_LINKING_REPORT_ACTION_DELETED,
                                                                            merchant.getMerchantID(), merchant.getName(),
                                                                            acct.getAcctID(), "", new Date(),
                                                                            au.getAdminName());
                            }
                        }
                    }
                    else
                    {
                        int totalCurrent = currentMerId.length;
                        for (int i = 0; i < totalCurrent; i++)
                        {
                            Merchant merchant = MerchantPeer.retrieveByPK(currentMerId[i]);
                            AdminUsers au = AdminUsersPeer.retrieveByPK(acct.getCreatedBy());
                            MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                        MerchantManager.MERCHANT_LINKING_REPORT_ACTION_DELETED,
                                                                        merchant.getMerchantID(), merchant.getName(),
                                                                        acct.getAcctID(), "", new Date(),
                                                                        au.getAdminName());
                        }
                    }
                }

                // saves inserted merchant linkings into merchant_linking_report table
                if (merInAcct != null)
                {
                    if (currentMerId != null)
                    {
                        int totalMer = merInAcct.length;
                        int totalCurrent = currentMerId.length;

                        for (int i = 0; i < totalMer; i++)
                        {
                            boolean inserted = true;
                            for (int j = 0; j < totalCurrent; j++)
                                if (merInAcct[i].getMerchantID() == currentMerId[j])
                                {
                                    inserted = false;
                                    break;
                                }

                            if (inserted == true)
                            {
                                Merchant merchant = MerchantPeer.retrieveByPK(merInAcct[i].getMerchantID());
                                AdminUsers au = AdminUsersPeer.retrieveByPK(acct.getCreatedBy());
                                MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                            MerchantManager.MERCHANT_LINKING_REPORT_ACTION_ADDED,
                                                                            merchant.getMerchantID(), merchant.getName(),
                                                                            acct.getAcctID(), "", new Date(),
                                                                            au.getAdminName());
                            }
                        }
                    }
                    else
                    {
                        int totalMer = merInAcct.length;
                        for (int i = 0; i < totalMer; i++)
                        {
                            Merchant merchant = MerchantPeer.retrieveByPK(merInAcct[i].getMerchantID());
                            AdminUsers au = AdminUsersPeer.retrieveByPK(acct.getCreatedBy());
                            MerchantManager.updateMerchantLinkingReport(MerchantManager.MERCHANT_LINKING_REPORT_TYPE_ACCOUNT,
                                                                        MerchantManager.MERCHANT_LINKING_REPORT_ACTION_DELETED,
                                                                        merchant.getMerchantID(), merchant.getName(),
                                                                        acct.getAcctID(), "", new Date(),
                                                                        au.getAdminName());
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("AccountsManager commitEditAccounts Error when saving into merchant_linking_report table");
                logger.error(e);
            }

            int[] usedPricingParameters = {};

            try {
                crit = new Criteria();
                crit.setDistinct();
                crit.addSelectColumn(PricingValuePeer.PRICING_PARAMETER_ID);
                List ls = PricingValuePeer.doSelectVillageRecords(crit, con);
                usedPricingParameters = new int[ls.size()];
                Iterator iter = ls.iterator();
                int i = 0;
                while (iter.hasNext()) {
                    Record r = (Record) iter.next();
                    usedPricingParameters[i] = r.getValue(1).asInt();
                    i++;
                }
            } catch (Exception e) {
                logger.error(e);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // deletes the current pricing_parameter entries of this account
            crit = new Criteria();
            crit.add(PricingParameterPeer.ACCT_ID, ae.getAcctID());
            if (usedPricingParameters != null && usedPricingParameters.length > 0) {
                crit.add(PricingParameterPeer.PRICING_PARAMETER_ID, usedPricingParameters, Criteria.NOT_IN);
            }

            try
            {
                PricingParameterPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error deleting the pricing_parameter merchant_in_accounts from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // saves the new pricing_parameter of this account to database
            PricingParameter[] pricing = ae.getPricingParameter();
            if (pricing != null)
            {
                try
                {
                    for (int i = 0; i < pricing.length; i++) {
                        crit = new Criteria();
                        crit.add(PricingParameterPeer.ACCT_ID, pricing[i].getAcctID());
                        crit.add(PricingParameterPeer.PRICING_PARAMETER_NAME, pricing[i].getPricingParameterName());
                        List l = PricingParameterPeer.doSelect(crit, con);
                        if (l.size() <= 0) {
                            pricing[i].save(con);
                        }
                    }
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitEditAccounts Error when saving pricing_parameter info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }

            try
            {
                Transaction.commit(con);
                status.setStatus(AccountsManager.ACCOUNT_EDITED);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitEditAccounts Error when commit the Torque transaction");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }
        }

        return status;
    }


    /**
     * Gets the account given account Id
     * @param acctId Account Id
     * @return Accounts The account needed
     */
    public static Accounts getAcctByAcctId(int acctId)
    {
        Criteria crit = new Criteria();
        crit.add(AccountsPeer.ACCT_ID, acctId);

        List list = null;
        try
        {
            list = AccountsPeer.doSelect(crit);
        }
        catch (TorqueException te)
        {
            logger.error("AccountsManager getAcctByAcctId Error retrieving the account from database");
            logger.error(te);
            return null;
        }

        Accounts acct = null;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            acct = (Accounts) ito.next();
        }

        return acct;
    }


    /**
     * Deletes an account
     * @param pid The pending Id
     * @return EventStatus The status of deleting action
     */
    public static EventStatus commitDeleteAccounts(int pid)
    {
        EventStatus status = new EventStatus();

        Object obj = MakerCheckerManager.getPendingMakeEntry(pid);
        if ((obj != null) && (obj instanceof AccountEntry))
        {
            Connection con = null;
            try
            {
                con = Transaction.begin(Torque.getDefaultDB());
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitDeleteAccounts Error when creating a connection to database");
                logger.error(te);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // the account info in the pending table
            AccountEntry ae = (AccountEntry) obj;

            // retrieves the account from the accounts table
            Accounts acct = AccountsManager.getAcctByAcctId(ae.getAcctID());
            if (acct != null)
            {
                // updates the account info in the accounts table with data from the pending table
                try
                {
                    acct.setDelFlag(AccountsManager.DEL_FLAG_DELETED);
                    acct.save(con);
                }
                catch (TorqueException te)
                {
                    logger.error("AccountsManager commitDeleteAccounts Error when saving account info to database");
                    logger.error(te);
                    Transaction.safeRollback(con);
                    status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                    return status;
                }
            }
            else
            {
                logger.error("AccountsManager commitDeleteAccounts Error the account not found");
                status.setStatus(AccountsManager.ERR_NOT_FOUND);
                return status;
            }

            // deletes the current accounts_admin entries of this account
            Criteria crit = new Criteria();
            crit.add(AccountsAdminPeer.ACCT_ID, ae.getAcctID());

            try
            {
                AccountsAdminPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitDeleteAccounts Error deleting the current accounts_admin from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // deletes the current merchant_in_accounts entries of this account
            crit = new Criteria();
            crit.add(MerchantInAccountsPeer.ACCT_ID, ae.getAcctID());

            try
            {
                MerchantInAccountsPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitDeleteAccounts Error deleting the current merchant_in_accounts from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            // deletes the current pricing_parameter entries of this account
            crit = new Criteria();
            crit.add(PricingParameterPeer.ACCT_ID, ae.getAcctID());

            try
            {
                PricingParameterPeer.doDelete(crit, con);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitDeleteAccounts Error deleting the pricing_parameter merchant_in_accounts from database");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }

            try
            {
                Transaction.commit(con);
                status.setStatus(AccountsManager.ACCOUNT_DELETED);
            }
            catch (TorqueException te)
            {
                logger.error("AccountsManager commitDeleteAccounts Error when commit the Torque transaction");
                logger.error(te);
                Transaction.safeRollback(con);
                status.setStatus(AccountsManager.ERR_SYSTEM_ERROR);
                return status;
            }
        }

        return status;
    }

    public static String getAccountName(int accountID) {
        String accountName = null;

        Criteria crit = new Criteria();
        crit.add(AccountsPeer.ACCT_ID, accountID);
        crit.add(AccountsPeer.DEL_FLAG, 0);
//        crit.addSelectColumn(AccountsPeer.ACCT_NAME);
        List list = null;
        try {
            System.out.println("getAccountName sql 1="+AccountsPeer.createQueryString(crit));
            list = AccountsPeer.doSelect(crit);
            if (list != null) {
                System.out.println("list is not null");
                Iterator iter = list.iterator();
                if (iter.hasNext()) {
                    Accounts account = (Accounts) iter.next();
                    accountName = account.getAcctName();
                }
            }

        } catch (TorqueException e) {
            logger.debug("Error:" + e);
            System.out.println("Error :"+e);
//            return accountName;

        }
        System.out.println("success, accountName="+accountName);
        return accountName;
    }

    /**
     * Gets the account pa given account Id
     * @param acctId Account Id
     * @return AcctPaEntry The account pa needed
     */
    public static AcctPaEntry getAcctPaymentAdviceByAcctId(int acctId)
    {
        Criteria crit = new Criteria();
        crit.add(AcctPaPeer.ACCT_ID, acctId);

        List list = null;
        try{
            list = AcctPaPeer.doSelect(crit);
            logger.info("print the list size"+list.size());
        }catch (TorqueException e){
            e.printStackTrace();
            logger.error(e);
        }
        AcctPaEntry acctPa = null;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            if (ito.hasNext()){
                try{
                	acctPa = new AcctPaEntry((AcctPa)ito.next());
                }catch(Exception e){
                    logger.error("AccountManager getAcctPaymentAdviceByAcctId The result retrieved from Torque does not have the correct format");
                }
            }
        }
        return acctPa;
    }
    
    /**
     * Gets the account pa range array given accountPa Id
     * @param acctPaId AccountPa Id
     * @return AcctPaRange array The account needed
     */
    public static AcctPaRange[] getAcctPaRangesByAcctPaId(int acctPaId)
    {
        Criteria criteria = new Criteria();
        criteria.add(AcctPaRangePeer.ACCT_PA_ID, acctPaId);
        criteria.add(AcctPaRangePeer.DEL_FLAG, 0);
        criteria.addAscendingOrderByColumn(AcctPaRangePeer.DATE_RANGE);

        List list = null;
        AcctPaRange[] acctPaRanges = null;
        try {
            list = AcctPaRangePeer.doSelect(criteria);
            if (list != null && list.size() > 0) {
                logger.debug("list size = " + list.size());
                acctPaRanges = new AcctPaRange[list.size()];
                Iterator iterator = list.iterator();
                int count = 0;
                while (iterator.hasNext()) {
                	AcctPaRange acctPaRange = (AcctPaRange) iterator.next();
                	acctPaRanges[count] = acctPaRange;
                	count++;
                }
            } else {
                logger.debug("List account pa range gets no return at all");
                return null;
            }
        } catch (TorqueException e) {
            logger.error("Account pa range not found");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return acctPaRanges;
    }
    
    public static AcctPaRangeEntry getAccountsPaymentAdviceRangeByAcctPaId(int acctPaRangeId) {
        Criteria criteria = new Criteria();
        criteria.add(AcctPaRangePeer.ACCT_PA_RANGE_ID, acctPaRangeId);
        criteria.add(AcctPaRangePeer.DEL_FLAG, 0);
        List list = null;
        try {
            list = AcctPaRangePeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e.getMessage());
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        AcctPaRange acctPaRange = (AcctPaRange) list.get(0);
        AcctPaRangeEntry acctPaRangeEntry = new AcctPaRangeEntry(acctPaRange);
        //me.setTerminals(TerminalManager.getAssignedTerminalsWithMerchantID(m.getmerchantID()));
        logger.info("Print the list in range " +acctPaRangeEntry.getCycleId());
        list.clear();
        list = null;
        return acctPaRangeEntry;
    }
    
    public static AcctPaRange getAccountsPaymentAdvicePeriodRangeByAcctPaId(int acctPaId) {
        Criteria criteria = new Criteria();
        criteria.add(AcctPaRangePeer.ACCT_PA_ID, acctPaId);
        List list = null;
        try {
            list = AcctPaRangePeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e.getMessage());
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        AcctPaRange acctPaRange = (AcctPaRange) list.get(0);
        list.clear();
        list = null;
        return acctPaRange;
    }
    
    public static AcctPaRangeEntry[] getAcctPaRangesEntryByAcctPaId(int acctPaId)
    {
        Criteria criteria = new Criteria();
        criteria.add(AcctPaRangePeer.ACCT_PA_ID, acctPaId);
        criteria.add(AcctPaRangePeer.DEL_FLAG, DEL_FLAG_NOT_DELETED);
        criteria.addAscendingOrderByColumn(AcctPaRangePeer.CYCLE_ID);
        List list = null;
        AcctPaRangeEntry[] acctPaRanges = new AcctPaRangeEntry[0];
        try {
            list = AcctPaRangePeer.doSelect(criteria);
            if (list != null && list.size() > 0) {
                logger.debug("list size = " + list.size());
                acctPaRanges = new AcctPaRangeEntry[list.size()];
                Iterator iterator = list.iterator();
                int count = 0;
                while (iterator.hasNext()) {
                	AcctPaRangeEntry acctPaRange = new AcctPaRangeEntry((AcctPaRange) iterator.next());                     
                	acctPaRanges[count] = acctPaRange;
                	count++;
                }
            }else {
                logger.debug("List account pa range gets no return at all");
                return acctPaRanges;
            }
        } catch (TorqueException e) {
            logger.error("Account pa range not found");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return acctPaRanges;
    }
    //acct pa range table stores the period data run day , credit day , cycle id
    public static AcctPaRangeEntry[] getAllAcctPaRangesEntryByAcctPaId()
    {
        Criteria criteria = new Criteria();
        criteria.addAscendingOrderByColumn(AcctPaRangePeer.DATE_RANGE);
        List list = null;
        AcctPaRangeEntry[] acctPaRanges = null;
        try {
            list = AcctPaRangePeer.doSelect(criteria);
            if (list != null && list.size() > 0) {
                logger.debug("list size = " + list.size());
                acctPaRanges = new AcctPaRangeEntry[list.size()];
                Iterator iterator = list.iterator();
                int count = 0;
                while (iterator.hasNext()) {
                	AcctPaRangeEntry acctPaRange = new AcctPaRangeEntry((AcctPaRange) iterator.next());                     
                	acctPaRanges[count] = acctPaRange;
                	count++;
                }
            }else {
                logger.debug("List account pa range has no records");
                return null;
            }
        } catch (TorqueException e) {
            logger.error("Account pa range not found");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return acctPaRanges;
    }

      public static AcctPaReportsEntry[] getPaymentAdviceList(int acctId) {
        Criteria crit = new Criteria();
        crit.add(AcctPaReportsPeer.ACCT_ID, acctId);
        crit.addAscendingOrderByColumn(AcctPaReportsPeer.CREATED_DT);
        List list = null;
        AcctPaReportsEntry[] appEntry = null;
        int count = 0;
        try {
            String sql = AcctPaReportsPeer.createQueryString(crit);
            logger.debug("getPaymentAdviceList :: sql=" + sql);
            list = AcctPaReportsPeer.doSelect(crit);
            if (list != null) {
                appEntry = new AcctPaReportsEntry[list.size()];
            }

            for (Iterator it = list.iterator(); it.hasNext();) {
                appEntry[count++] = new AcctPaReportsEntry((AcctPaReports) it.next());
            }
        } catch (TorqueException te) {
            te.printStackTrace();
            logger.error(te);
        }
        return appEntry;
    }

    public static AcctBatchRptFileEntry[] getAcctBatchReportFileList(int acctId) {
        Criteria crit = new Criteria();
        crit.add(AcctBatchRptFilePeer.ACCT_ID, acctId);
        crit.add(AcctBatchRptFilePeer.DEL_FLAG, 0);
        crit.addAscendingOrderByColumn(AcctBatchRptFilePeer.CREATED_DT);

        List list = null;
        AcctBatchRptFileEntry[] fileEntry = null;
        int count = 0;
        try {
            String sql = AcctBatchRptFilePeer.createQueryString(crit);
            logger.debug("getAcctBatchReportFileList :: sql=" + sql);
            list = AcctBatchRptFilePeer.doSelect(crit);
            if (list != null) {
                fileEntry = new AcctBatchRptFileEntry[list.size()];
            }

            for (Iterator it = list.iterator(); it.hasNext();) {
                fileEntry[count++] = new AcctBatchRptFileEntry((AcctBatchRptFile) it.next());
            }
        } catch (TorqueException te) {
            te.printStackTrace();
            logger.error(te);
        }
        return fileEntry;
    }

    public static AcctPaReportsEntry getPaymentAdviceReportById(int reportId){
        AcctPaReportsEntry apaReport = null;
        try{
            AcctPaReports acctPaRpt = AcctPaReportsPeer.retrieveByPK(reportId);
            if(acctPaRpt != null){
                apaReport = new AcctPaReportsEntry(acctPaRpt);   
            }
        } catch (TorqueException te) {
            te.printStackTrace();
            logger.error(te);
        }
        return apaReport;
    }
    
    
    public static AcctPaReportsEntry getPaymentAdviceReportByAcctID(int acctId){        
        Criteria criteria = new Criteria();
    	criteria.add(AcctPaReportsPeer.ACCT_ID, acctId);    	
    	List list = null;
        try {
            list = AcctPaReportsPeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e.getMessage());
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        AcctPaReports acctPaReports = (AcctPaReports) list.get(0);
        AcctPaReportsEntry acctPaRptEntry = new AcctPaReportsEntry(acctPaReports);
        logger.info("Print the list in range " +acctPaRptEntry.getAcctID());
        list.clear();
        list = null;
        return acctPaRptEntry;
    }
    
    public static AcctBatchRptEntry getAccountBatchReportsByAccId(int acctId,int batchReportTypeId){
              	
        	Criteria criteria = new Criteria();
        	criteria.add(AcctBatchRptPeer.ACCT_ID, acctId);
        	criteria.add(AcctBatchRptPeer.BATCH_REPORT_TYPE_ID, batchReportTypeId);
        	criteria.add(AcctBatchRptPeer.DEL_FLAG, 0);
            
            List list = null;
            try {
                list = AcctBatchRptPeer.doSelect(criteria);
            } catch (TorqueException e) {
                logger.error(e.getMessage());
                return null;
            }
            if (list.isEmpty()) {
                return null;
            }
            AcctBatchRpt acctBatchRpt = (AcctBatchRpt) list.get(0);
            AcctBatchRptEntry acctBatchRptEntry = new AcctBatchRptEntry(acctBatchRpt);
            list.clear();
            list = null;
            return acctBatchRptEntry;
    }
    
    public static AcctBatchRptEntry getAccountBatchReportsByAccIdMid(int acctId,int batchReportTypeId,int merchantID){
      	
    	Criteria criteria = new Criteria();
    	criteria.add(AcctBatchRptPeer.ACCT_ID, acctId);
    	criteria.add(AcctBatchRptPeer.BATCH_REPORT_TYPE_ID, batchReportTypeId);
    	criteria.add(AcctBatchRptPeer.MERCHANT_ID, merchantID);
    	criteria.add(AcctBatchRptPeer.DEL_FLAG, 0);
        
        List list = null;
        try {
            list = AcctBatchRptPeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e.getMessage());
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        AcctBatchRpt acctBatchRpt = (AcctBatchRpt) list.get(0);
        AcctBatchRptEntry acctBatchRptEntry = new AcctBatchRptEntry(acctBatchRpt);
        list.clear();
        list = null;
        return acctBatchRptEntry;
    }
    
    public static AcctBatchRptEntry[] getAccountBatchReportsByAccIdForMerchants(int acctId,int batchReportTypeId){
      	
    	 Criteria criteria = new Criteria();
         criteria.add(AcctBatchRptPeer.ACCT_ID, acctId);
         criteria.add(AcctBatchRptPeer.BATCH_REPORT_TYPE_ID, batchReportTypeId);
         criteria.add(AcctBatchRptPeer.DEL_FLAG, 0);
         List list = null;
         AcctBatchRptEntry[] acctBatchRptEntry = null;
         try {
             list = AcctBatchRptPeer.doSelect(criteria);
             logger.debug(" before if list size in account maanager = " + list.size());
             if (list != null && list.size() > 0) {
                 logger.debug("inside if list size in account maanager = " + list.size());
                 acctBatchRptEntry = new AcctBatchRptEntry[list.size()];
                 Iterator iterator = list.iterator();
                 int count = 0;
                 while (iterator.hasNext()) {
                	 AcctBatchRptEntry acctBatchReport = new AcctBatchRptEntry((AcctBatchRpt) iterator.next());                     
                 	acctBatchRptEntry[count] = acctBatchReport;
                 	count++;
                 }
             }else {
                 logger.debug("List account batch report gets no return at all");
                 return null;
             }
         } catch (TorqueException e) {
             logger.error("Account batch report not found");
             ExceptionManager.logStackTraceString(e, logger);
         }
         return acctBatchRptEntry;
    }   
  

    public static AcctBatchRptFileEntry getBatchReportById(int reportId){
        AcctBatchRptFileEntry batchReport = null;
        try{
            AcctBatchRptFile acctPaRpt = AcctBatchRptFilePeer.retrieveByPK(reportId);
            if(acctPaRpt != null){
                batchReport = new AcctBatchRptFileEntry(acctPaRpt);
            }
        } catch (TorqueException te) {
            te.printStackTrace();
            logger.error(te);
        }
        return batchReport;
    }
    
    public static EventStatus createAccountPaymentAdvice(AcctPaEntry acctPaEntry) {
        EventStatus eventStatus = new EventStatus();
        try {
           AcctPa acctPa = new AcctPa();
           acctPa.setAcctID(acctPaEntry.getAcctID());
           acctPa.setCompanyName(acctPaEntry.getCompanyName());
           acctPa.setAddress1(acctPaEntry.getAddress1());
           acctPa.setAddress2(acctPaEntry.getAddress2());
           acctPa.setAddress3(acctPaEntry.getAddress3());
           acctPa.setPostal(acctPaEntry.getPostal());
           acctPa.setFreqType((short)acctPaEntry.getFreqType());
           acctPa.save();
           
           AcctPaEntry accEntry = new AcctPaEntry(acctPa);
           eventStatus.setStatus(AccountsManager.STATUS_CREATE_OK); 
           eventStatus.setReturnObj(accEntry) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create accountpa in database");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save accountpa");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return  eventStatus;
    }
    
    public static EventStatus updateAccountPaymentAdvice(AcctPaEntry acctPaEntry) {
        EventStatus status = new EventStatus();
        try {
        	AcctPa acctPa = AcctPaPeer.retrieveByPK(acctPaEntry.getAcctPaymentAdviceID());
        	acctPa.setAcctID(acctPaEntry.getAcctID());
        	acctPa.setCompanyName(acctPaEntry.getCompanyName());
            acctPa.setAddress1(acctPaEntry.getAddress1());
            acctPa.setAddress2(acctPaEntry.getAddress2());
            acctPa.setAddress3(acctPaEntry.getAddress3());
            acctPa.setPostal(acctPaEntry.getPostal());
            acctPa.setFreqType((short)acctPaEntry.getFreqType());
            acctPa.save();
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save account pa");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return status;
    }

    public static EventStatus createAccountPaymentAdvicePeriodRange(AcctPaRangeEntry acctPaRangeEntry) {
        EventStatus eventStatus = new EventStatus();
        try {
           AcctPaRange acctPaRange = new AcctPaRange();
           acctPaRange.setAcctPaymentAdviceID(acctPaRangeEntry.getAcctPaymentAdviceID());
           acctPaRange.setDateRange(acctPaRangeEntry.getDateRange());
           acctPaRange.setCycleId(acctPaRangeEntry.getCycleId());
           acctPaRange.setCreditDay(acctPaRangeEntry.getCreditDay());
           acctPaRange.setRunDay(acctPaRangeEntry.getRunDay());
           acctPaRange.setDelFlag(DEL_FLAG_NOT_DELETED);
           acctPaRange.save();
           
           AcctPaRangeEntry accRangeEntry = new AcctPaRangeEntry(acctPaRange);
           eventStatus.setStatus(AccountsManager.STATUS_CREATE_OK); 
           eventStatus.setReturnObj(accRangeEntry) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create Account Pa Range in database");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save accountpa");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return  eventStatus;
    }
    
    public static EventStatus updateAccountPaymentAdvicePeriodRange(AcctPaRangeEntry acctPaRangeEntry) {
        EventStatus eventStatus = new EventStatus();
        try {        	
        	//AcctPaRange acctPaRange = getAccountsPaymentAdvicePeriodRangeByAcctPaId(acctPaRangeEntry.getAcctPaymentAdviceID());
        	AcctPaRange acctPaRange = AcctPaRangePeer.retrieveByPK(acctPaRangeEntry.getAcctPaRangeId());
        	acctPaRange.setAcctPaymentAdviceID(acctPaRangeEntry.getAcctPaymentAdviceID());
            acctPaRange.setDateRange(acctPaRangeEntry.getDateRange());
            acctPaRange.setCycleId(acctPaRangeEntry.getCycleId());
            acctPaRange.setCreditDay(acctPaRangeEntry.getCreditDay());
            acctPaRange.setRunDay(acctPaRangeEntry.getRunDay());
            acctPaRange.setDelFlag(DEL_FLAG_NOT_DELETED);
            acctPaRange.save();         
            AcctPaRangeEntry accRangeEntry = new AcctPaRangeEntry(acctPaRange);
            eventStatus.setStatus(AccountsManager.STATUS_EDIT_OK);
            eventStatus.setReturnObj(accRangeEntry) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to edit period to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to edit period");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return eventStatus;
    }
    
    public static EventStatus deletePeriod(int periodRangeId) {
        EventStatus status = new EventStatus();
        try {
        	AcctPaRange acctPaRange = AcctPaRangePeer.retrieveByPK(periodRangeId);
        	acctPaRange.setDelFlag(DEL_FLAG_DELETED);
        	acctPaRange.save();
            status.setStatus(AccountsManager.STATUS_DELETE_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_NOT_FOUND);
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            logger.error("Failed to delete account");
            ExceptionManager.logStackTraceString(e, logger);
        }

        return status;
    }
    
    public static EventStatus createAccountBatchReportsForSPARecipients(AcctBatchRptEntry acctBatchRptEntry) {
        EventStatus eventStatus = new EventStatus();
        try {
        	logger.debug("Inside createAccountBatchReportsForSPARecipients debug " );
           AcctBatchRpt acctBatchRpt = new AcctBatchRpt();
           acctBatchRpt.setAcctID(acctBatchRptEntry.getAcctID());
           acctBatchRpt.setBatchReportTypeId(acctBatchRptEntry.getBatchReportTypeId());
           acctBatchRpt.setDateUpdated(acctBatchRptEntry.getDateUpdated());
           acctBatchRpt.setEmailList(acctBatchRptEntry.getEmailList());
           acctBatchRpt.setEmailSubject(acctBatchRptEntry.getEmailSubject());
           acctBatchRpt.setEmailContent(acctBatchRptEntry.getEmailContent());
           acctBatchRpt.setAdminId(acctBatchRptEntry.getAdminId());
           acctBatchRpt.setDelFlag((short)0);
           
           //logger.debug("Before save "+acctBatchRpt.getAcctID()+ "," +acctBatchRpt.getMerchantID());
           acctBatchRpt.save();
           eventStatus.setStatus(AccountsManager.STATUS_CREATE_OK);
           AcctBatchRptEntry acctBatchRptEntries = new AcctBatchRptEntry(acctBatchRpt);
            
           eventStatus.setReturnObj(acctBatchRptEntries) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create account batch reports in database");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save account batch reports");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return  eventStatus;
    }
    
    public static EventStatus updateAccountBatchReportsForSPARecipients(AcctBatchRptEntry acctBatchRptEntry) {
        EventStatus status = new EventStatus();
        try {
        	logger.debug("Inside createAccountBatchReportsForSPARecipients  " +acctBatchRptEntry.toString());
        	AcctBatchRpt acctBatchRpt = AcctBatchRptPeer.retrieveByPK(acctBatchRptEntry.getAcctBatchReportID());
        	acctBatchRpt.setAcctID(acctBatchRptEntry.getAcctID());
        	acctBatchRpt.setDateUpdated(acctBatchRptEntry.getDateUpdated());
        	acctBatchRpt.setDelFlag((short)0);
        	acctBatchRpt.setEmailContent(acctBatchRptEntry.getEmailContent());
        	acctBatchRpt.setEmailList(acctBatchRptEntry.getEmailList());
        	acctBatchRpt.setEmailSubject(acctBatchRptEntry.getEmailSubject());
        	acctBatchRpt.setAdminId(acctBatchRptEntry.getAdminId());
        	acctBatchRpt.setBatchReportTypeId(acctBatchRptEntry.getBatchReportTypeId());  
        	acctBatchRpt.save();
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
            logger.debug("Inside createAccountBatchReportsForSPARecipients edited "+acctBatchRpt.toString());
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save account pa");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return status;
    }
    
    
    /*---------------------------------------------------------------*/
    
    public static EventStatus createAccountBatchReportsForMPARecipients(AcctBatchRptEntry acctBatchRptEntry ) {
        EventStatus eventStatus = new EventStatus();
        try {
        	AcctBatchRpt acctBatchRpt = null;
        	logger.debug("Inside createAccountBatchReportsForMPARecipients debug ");
        	//for (int i = 0; i < mids.length; i++) {
        		acctBatchRpt = new AcctBatchRpt();
                acctBatchRpt.setAcctID(acctBatchRptEntry.getAcctID());
                acctBatchRpt.setBatchReportTypeId(acctBatchRptEntry.getBatchReportTypeId());
                acctBatchRpt.setDateUpdated(acctBatchRptEntry.getDateUpdated());
                acctBatchRpt.setEmailList(acctBatchRptEntry.getEmailList());
                acctBatchRpt.setEmailSubject(acctBatchRptEntry.getEmailSubject());
                acctBatchRpt.setEmailContent(acctBatchRptEntry.getEmailContent());
                acctBatchRpt.setAdminId(acctBatchRptEntry.getAdminId());
                acctBatchRpt.setMerchantID(acctBatchRptEntry.getMerchantId());           
                acctBatchRpt.setDelFlag((short)0);                
                acctBatchRpt.save();
			//}           
           logger.debug("Inside createAccountBatchReportsForMPARecipients saved "+acctBatchRpt.toString());
           AcctBatchRptEntry acctBatchRptEntries = new AcctBatchRptEntry(acctBatchRpt);
           eventStatus.setStatus(AccountsManager.STATUS_CREATE_OK); 
           eventStatus.setReturnObj(acctBatchRptEntries) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create account batch reports in database");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save account batch reports");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return  eventStatus;
    }
    
    public static EventStatus updateAccountBatchReportsForMPARecipients(AcctBatchRptEntry acctBatchRptEntry) {
        EventStatus status = new EventStatus();
        try {
        	logger.debug("Inside updateAccountBatchReportsForMPARecipients"  + acctBatchRptEntry.getAcctBatchReportID()+" Merchant id "+ acctBatchRptEntry.getMerchantId());
        	//for (int i = 0; i < mids.length; i++) {
        		AcctBatchRpt acctBatchRpt = AcctBatchRptPeer.retrieveByPK(acctBatchRptEntry.getAcctBatchReportID());
            	acctBatchRpt.setAcctID(acctBatchRptEntry.getAcctID());
            	acctBatchRpt.setDateUpdated(acctBatchRptEntry.getDateUpdated());
            	acctBatchRpt.setDelFlag((short)0);
            	acctBatchRpt.setEmailContent(acctBatchRptEntry.getEmailContent());
            	acctBatchRpt.setEmailList(acctBatchRptEntry.getEmailList());
            	acctBatchRpt.setEmailSubject(acctBatchRptEntry.getEmailSubject());
            	acctBatchRpt.setAdminId(acctBatchRptEntry.getAdminId());
            	acctBatchRpt.setBatchReportTypeId(acctBatchRptEntry.getBatchReportTypeId());
            	acctBatchRpt.setMerchantID(acctBatchRptEntry.getMerchantId());  
            	acctBatchRpt.save();
			//}        	
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save account pa");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return status;
    }
    
    /*-----------------------------------------------------------------------------------------*/
    public static int getTotalAcctPaBankCount(int acctId) {
        Criteria criteria = new Criteria();
        criteria.addJoin(AcctPaBankPeer.MERCHANT_ID, MerchantInAccountsPeer.MERCHANT_ID);
        criteria.add(MerchantInAccountsPeer.ACCT_ID, acctId);       

        List list = null;
        try {
            list = AcctPaBankPeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }

        return (list == null ? 0 : list.size());
    }
    
    
    public static AcctPaBankEntry[] getAllMerchantBankAccounts(int acctId) {
    	AcctPaBankEntry bankAcctEntry[] = new AcctPaBankEntry[0];
        Criteria criteria = new Criteria();
        criteria.addJoin(AcctPaBankPeer.MERCHANT_ID, MerchantInAccountsPeer.MERCHANT_ID);
        criteria.add(MerchantInAccountsPeer.ACCT_ID, acctId);  
        criteria.add(AcctPaBankPeer.DEL_FLAG, DEL_FLAG_NOT_DELETED);  
        criteria.addAscendingOrderByColumn(AcctPaBankPeer.ACCT_PA_BANK_ID);       
        List list = new ArrayList();
        try {
            list = AcctPaBankPeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e);
            e.printStackTrace();
        }
        if (list != null && list.size() > 0) {
            int counter = 0;
            bankAcctEntry = new AcctPaBankEntry[list.size()];
            for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            	AcctPaBankEntry acctPaBankEntry = new AcctPaBankEntry((AcctPaBank) iterator.next());
                bankAcctEntry[counter] = acctPaBankEntry;
                counter++;
            }
        }else {
            logger.debug("No merchant accounts found");
            return bankAcctEntry;
        }
        return bankAcctEntry;
    }
    public static List getMerchantBankAccountsForAcctID(int acctId) {
    	List list = null;
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("SELECT apb.acct_pa_bank_id as bankId, apb.merchant_id as merchantID, apb.acct_no as accountNo , apb.bank_code as bankCode");
    	buffer.append("apb.branch_code as branchCode, apb.bank_code as bankCode, m.name as name");
    	buffer.append("FROM acct_pa_bank apb , merchant m , merchant_in_accounts ma");
    	buffer.append("WHERE apb.merchant_id = ma.merchant_id");
    	buffer.append("AND apb.merchant_id = m.merchant_id");
    	buffer.append("AND ma.acct_id = "+acctId);
    	try {
			String query = buffer.toString();
			if (logger.isDebugEnabled()) {
				logger.debug("@@@ getMerchantBankAccountsForAcctID : " + query);
			}
		list = BasePeer.executeQuery(query);		

		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				AcctPaBank acctPaBank = (AcctPaBank) iterator.next();
				acctPaBank.setAccountNo(acctPaBank.getAccountNo());
				acctPaBank.setAcctName(acctPaBank.getAcctName());
				acctPaBank.setMerchant(acctPaBank.getMerchant());
				acctPaBank.setBankCode(acctPaBank.getBankCode());
				acctPaBank.setBranchCode(acctPaBank.getBranchCode());
				acctPaBank.setMerchantID(acctPaBank.getMerchantID());				
			}
		}catch (TorqueException e) {
			logger.error(e.getMessage(), e);
		}finally {
			buffer = null;
		}
		return list;    	  
    }
    
    public static AcctPaBankEntry getMerchantBankAccountsForPaymentAdvice(int acctPaBankId) {
        Criteria criteria = new Criteria();
        criteria.add(AcctPaBankPeer.ACCT_PA_BANK_ID,acctPaBankId);
        List list = null;
        try {
            list = AcctPaBankPeer.doSelect(criteria);
        } catch (TorqueException e) {
            logger.error(e.getMessage());
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        AcctPaBank acctPaBank = (AcctPaBank) list.get(0);
        AcctPaBankEntry acctPaBankEntry = new AcctPaBankEntry(acctPaBank);
        list.clear();
        list = null;
        return acctPaBankEntry;
    }
    
    public static EventStatus createMerchantBankAccounts(AcctPaBankEntry acctPaBankEntry) {
        EventStatus eventStatus = new EventStatus();
        try {
          logger.debug("Inside createMerchantBankAccounts()  " +acctPaBankEntry.getMerchantID());
          
          Criteria criteria = new Criteria();
          criteria.add(AcctPaBankPeer.MERCHANT_ID, acctPaBankEntry.getMerchantID());
          criteria.add(AcctPaBankPeer.DEL_FLAG, -1, Criteria.NOT_EQUAL);
          
          List list = null;
          try {
              list = AcctPaBankPeer.doSelect(criteria);
              logger.debug("Print the list size "+list.size());
          } catch (TorqueException e) {
              logger.error(e.toString());
          }
          
          if (!list.isEmpty()) {
        	  eventStatus.setStatus(AcctPaBankEntry.CREATED_FAILED_NAME_EXIST);
              return eventStatus;
          }
          AcctPaBank acctBank = new AcctPaBank();
          acctBank.setAccountNo(acctPaBankEntry.getAccountNo());
          acctBank.setAcctName(acctPaBankEntry.getAcctName());
          acctBank.setBankCode(acctPaBankEntry.getBankCode());
          acctBank.setBranchCode(acctPaBankEntry.getBranchCode());
          acctBank.setMerchantID(acctPaBankEntry.getMerchantID());
          acctBank.setDelFlag(DEL_FLAG_NOT_DELETED);
          acctBank.save();
          AcctPaBankEntry acctPaBankEntries = new AcctPaBankEntry(acctBank);
           eventStatus.setStatus(AccountsManager.STATUS_CREATE_OK); 
           eventStatus.setReturnObj(acctPaBankEntries) ;
        } catch (TorqueException e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to create accounts for merchants in database");
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
        	eventStatus.setStatus(AccountsManager.ERR_CREATE_FAIL);
            logger.error("Failed to save accounts for merchants");
            logger.error("e2 " + e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        return  eventStatus;
    }
    
    public static EventStatus updateMerchantBankAccounts(AcctPaBankEntry acctPaBankEntry) {
        EventStatus status = new EventStatus();
        try {
        	AcctPaBank acctPaBank = AcctPaBankPeer.retrieveByPK(acctPaBankEntry.getAcctPaymentBankId());
        	acctPaBank.setAcctPaymentBankId(acctPaBankEntry.getAcctPaymentBankId());
        	acctPaBank.setAccountNo(acctPaBankEntry.getAccountNo());
        	acctPaBank.setAcctName(acctPaBankEntry.getAcctName());
        	acctPaBank.setBankCode(acctPaBankEntry.getBankCode());
        	acctPaBank.setBranchCode(acctPaBankEntry.getBranchCode());
        	acctPaBank.setMerchantID(acctPaBankEntry.getMerchantID());
        	acctPaBank.setDelFlag(DEL_FLAG_NOT_DELETED);
        	acctPaBank.save();
            status.setStatus(AccountsManager.STATUS_EDIT_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to database");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save payment advice bank accounts for merchants");
            ExceptionManager.logStackTraceString(e, logger);
        }
        return status;
    }
    
    public static EventStatus deleteMerchantBankAccounts(int bankId) {
        EventStatus status = new EventStatus();
        try {
        	AcctPaBank acctPaBank = AcctPaBankPeer.retrieveByPK(bankId);
        	acctPaBank.setDelFlag(DEL_FLAG_DELETED);
        	acctPaBank.save();
            status.setStatus(AccountsManager.STATUS_DELETE_OK);
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_NOT_FOUND);
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_DELETE_FAIL);
            logger.error("Failed to delete account");
            ExceptionManager.logStackTraceString(e, logger);
        }

        return status;
    }
    
    
    public static AcctBatchRptEntry[] getAllAcctBatchReports() {
        Criteria criteria = new Criteria();
        criteria.add(AcctBatchRptPeer.DEL_FLAG, DEL_FLAG_DELETED, Criteria.NOT_EQUAL);
        System.out.print("Inside Account Manager getAllAcctBatchReports() method");
        AcctBatchRptEntry[] entries = null;
        List list = null;
        try {
            list = AcctBatchRptPeer.doSelect(criteria);
            System.out.print("Inside Account Manager getAllAcctBatchReports() method list size " + list.size());
        } catch (TorqueException e) {
            logger.error(e);
            ExceptionManager.logStackTraceString(e, logger);
        }
        if (list != null && list.size() > 0) {
            entries = new AcctBatchRptEntry[list.size()];
            Iterator iterator = list.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
            	AcctBatchRptEntry account = new AcctBatchRptEntry((AcctBatchRpt) iterator.next());
                entries[counter] = account;
                counter++;
            }
        } else {
            logger.debug("No Accounts found");
        }
       
        return entries;
    } 
    
    //To get all the mids
    public static int[] getAllMerchantsForAcctId(int acctId, int batchReportTypeId) {
    	Criteria criteria = new Criteria();       
        criteria.add(AcctBatchRptPeer.ACCT_ID, acctId);
        criteria.add(AcctBatchRptPeer.BATCH_REPORT_TYPE_ID, batchReportTypeId);

        List list = null;
        try {
            list = AcctBatchRptPeer.doSelect(criteria);
            logger.info("Inside Account Manager getAllMerchantsForAcctId()" +list.size());
        } catch (TorqueException e1) {
            logger.error("TorqueException e1 at AccountManager.getAllMerchantsForAcctId: " + e1.getMessage());
            return null;
        }

        int[] merchantIds = new int[list.size()];
        Iterator i = list.iterator();
        int x = 0;

        while (i.hasNext()) {
        	logger.info("Inside iterator to check how many times it runs getAllMerchantsForAcctId()");
        	AcctBatchRpt acctBatchReport = new AcctBatchRpt();
        	acctBatchReport = (AcctBatchRpt) i.next();
            merchantIds[x] = acctBatchReport.getMerchantID();
            logger.info("Inside iterator print the merchant ids getAllMerchantsForAcctId() "+merchantIds[x]);
            x = x + 1;
        }

        return merchantIds;
    }
    
    //for fraser requirement, by david
    public static boolean isRedemptionEnabled(String mId){
    	logger.info("method isRedemptionEnabled begins, mId = " + mId);
    	Criteria crit = new Criteria();   
    	crit.add(MerchantPeer.MERCHANT_NO, mId);  
    	crit.addJoin(MerchantPeer.MERCHANT_ID, MerchantInAccountsPeer.MERCHANT_ID);
	    crit.addJoin(MerchantInAccountsPeer.ACCT_ID, AccountsPeer.ACCT_ID);
	    crit.addJoin(AccountsPeer.ACCT_ID, AcctFeaturePeer.ACCT_ID);
	    //crit.addJoin(MerchantPeer.MERCHANT_ID, MerchantInGroupingPeer.MERCHANT_ID);
	    //crit.addJoin(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, AcctFeaturePeer.SELECTIVE_REDEMPTION_MG_ID);
	    //crit.add(AcctFeaturePeer.ALLOW_SELECTIVE_REDEMPTION, "1");
	    crit.addSelectColumn(AcctFeaturePeer.ALLOW_SELECTIVE_REDEMPTION);
	    crit.addSelectColumn(AcctFeaturePeer.SELECTIVE_REDEMPTION_MG_ID);
	    //crit.setDistinct();
	      
	    List list = null;
        try {
        	String query = AccountsPeer.createQueryString(crit);
        	logger.info("::: sql = " + query);
            list = AcctFeaturePeer.executeQuery(query);
        } catch (TorqueException e) {
            ExceptionManager.logStackTraceString(e, logger);
            return false;
        }
        
        if(list == null){
        	//default allow redemption
        	logger.info("list == null, no acct_feature record, default allow redemption");
        	return true;
        }
        
        if(list != null && list.size() == 0){
        	//default allow redemption
        	logger.info("list != null && list.size() == 0, no acct_feature record, default allow redemption");
        	return true;
        }
        
        try {
            if (list != null && list.size() > 0) {
                Record r = (Record) list.get(0);
                int allow = r.getValue(1).asInt();
                if(allow == 0){
                	logger.info("ALLOW_SELECTIVE_REDEMPTION flag is 0, allow redemption");
                	return true;
                } 
                if(allow == 1){
                	logger.info("ALLOW_SELECTIVE_REDEMPTION flag is 1");
                	int redemptionMgId = r.getValue(2).asInt();
                	logger.info("redemptionMgId is " + redemptionMgId);
                	
                	//query merchant_in_grouping to determine allow redemption or not
                	crit = new Criteria(); 
                	crit.add(MerchantPeer.MERCHANT_NO, mId);  
                	crit.addJoin(MerchantPeer.MERCHANT_ID, MerchantInGroupingPeer.MERCHANT_ID);
                	crit.add(MerchantInGroupingPeer.MERCHANT_GROUPING_ID, redemptionMgId);
                	crit.addSelectColumn(MerchantInGroupingPeer.MERCHANT_GROUPING_ID);
            	    String query = MerchantInGroupingPeer.createQueryString(crit);
            	    logger.info("::: sql1 = " + query);
            	    list = MerchantInGroupingPeer.executeQuery(query);
            	    
	                  if(list == null){
	                	logger.info("list == null, no merchant_in_grouping record, throw exception");
	                	return false;
	                  }
	                  
	                  if(list != null && list.size() == 0){
	                  	logger.info("list != null && list.size() == 0, no merchant_in_grouping record, throw exception");
	                  	return false;
	                  }
	                    
	                  if(list != null && list.size() > 0){
                    	logger.info("there is merchant_in_grouping record forredemptionMgId = " + redemptionMgId + ", allow redemption");
                    	return true;
	                  }
            	    
                	
                	//query merchant group id the merchant belong to
//                	crit = new Criteria(); 
//                	crit.add(MerchantPeer.MERCHANT_NO, mId);  
//            	    crit.addJoin(MerchantPeer.MERCHANT_ID, MerchantInGroupingPeer.MERCHANT_ID);
//            	    crit.addSelectColumn(MerchantInGroupingPeer.MERCHANT_GROUPING_ID);
//            	    String query = MerchantInGroupingPeer.createQueryString(crit);
//            	    logger.info("::: sql = " + query);
//            	    list = MerchantInGroupingPeer.executeQuery(query);
//            	    
//                    if(list == null){
//                    	logger.info("list == null, no merchant_in_grouping record, throw exception");
//                    	return false;
//                    }
//                    
//                    if(list != null && list.size() == 0){
//                    	logger.info("list != null && list.size() == 0, no merchant_in_grouping record, throw exception");
//                    	return false;
//                    }
//                    
//                    if(list != null && list.size() > 0){
//                    	r = (Record) list.get(0);
//                    	int mgId = r.getValue(1).asInt();
//                    	logger.info("::: merchant " + mId + " belongs to merchant group with id " + mgId);
//                    	if(mgId == redemptionMgId){
//                    		logger.info("::: mgId = " + mgId + " redemptionMgId = " + redemptionMgId + " allow redemption");
//                    		return true;
//                    	} else {
//                    		logger.info("::: mgId = " + mgId + " redemptionMgId = " + redemptionMgId + " throw error");
//                    		return false;
//                    	}
//                    }
            	    
                }
            }
        } catch (Exception e) {
            ExceptionManager.logStackTraceString(e, logger);
            return false;
        }
		
        return false;
 
    }

    //for fraser requirement, by david
	public static AccountFeatureEntry getAccountFeatureEntryByAcctId(int acctId) {

		AccountFeatureEntry afe = new AccountFeatureEntry();
		Criteria crit = new Criteria();
        crit.add(AcctFeaturePeer.ACCT_ID, acctId);

        List list = null;
        try
        {
            list = AcctFeaturePeer.doSelect(crit);
        }
        catch (TorqueException te)
        {
            logger.error("AccountsManager getAccountFeatureEntryByAcctId Error retrieving AcctFeature from database");
            logger.error(te);
            return null;
        }
        
        
        AcctFeature af = null;
        if ((list != null) && (list.size() > 0))
        {
            Iterator ito = list.iterator();
            af = (AcctFeature) ito.next();
            
            afe.setAcctID(acctId);
            afe.setAllowSelectiveRedemption(af.getAllowSelectiveRedemption());
            afe.setAllowTxnMonitor(af.getAllowTxnMonitor());
            afe.setSelectiveRedemptionMgID(af.getSelectiveRedemptionMgID());
            afe.setTxnMonitorCnt(af.getTxnMonitorCnt());
            afe.setTxnMonitorLastTrigger(af.getTxnMonitorLastTrigger());
            afe.setTxnMonitorRecipient(af.getTxnMonitorRecipient());
        
        }         
        
        //merchant grouping
        crit = new Criteria();
        crit.add(MerchantGroupingPeer.ACCT_ID, acctId);
        list = null;
        try
        {
            list = MerchantGroupingPeer.doSelect(crit);
        }
        catch (TorqueException te)
        {
            logger.error("AccountsManager getAccountFeatureEntryByAcctId Error retrieving MerchantGrouping from database");
            logger.error(te);
            return null;
        }
        
        Hashtable mgMap = new Hashtable();
        //only if first time init, add N/A entry
        if(afe.getSelectiveRedemptionMgID() == 0){
        	mgMap.put(new Integer(-1), "N/A");
        }
        if ((list != null) && (list.size() > 0))
        {
        	MerchantGrouping mg = new MerchantGrouping();
            for(Iterator ito = list.iterator(); ito.hasNext(); ){
            	mg = (MerchantGrouping) ito.next();
            	mgMap.put(new Integer(mg.getMerchantGroupingID()), mg.getGroupingName());
           	
            }
        }   
        
        afe.setMerchantGroups(mgMap);
        return afe;

	}

	//for fraser requirement, by david
	public static EventStatus updateAccountFeature(int accountID,
			String allowSelectiveRedemption, int merchantGroupId,
			String allowTxnMonitor, int txnMonitorCnt,
			String txnMonitorRecipient) {
		
		EventStatus status = new EventStatus();
		boolean completed = false;
        try {
        	
            //todo: put into 1 transaction
        	Criteria crit = new Criteria();
        	crit.add(AcctFeaturePeer.ACCT_ID, accountID);
        	List list = null;
        	list = AcctFeaturePeer.doSelect(crit);
        	AcctFeature af = null;
        	
            if((list == null) || (list != null && list.size() == 0)){
            	//there's no AcctFeature for this account, need insert
            	af = new AcctFeature();
            	af.setAcctID(accountID);
                      	
            	if(allowSelectiveRedemption != null){
            		if("1".equals(allowSelectiveRedemption)){
                		af.setAllowSelectiveRedemption((short)1);
            		} else {
            			af.setAllowSelectiveRedemption((short)0);
            		}
            	} else {
            		af.setAllowSelectiveRedemption((short)0);
            	}
            	
            	if(merchantGroupId != 0){
            		af.setSelectiveRedemptionMgID(merchantGroupId);
            	}
            	
            	if(allowTxnMonitor != null){
            		if("1".equals(allowTxnMonitor)){
            			af.setAllowTxnMonitor((short)1);
            		} else {
            			af.setAllowTxnMonitor((short)0);
            		}
            	} else {
            		af.setAllowTxnMonitor((short)0);
            	}
            	
            	if(txnMonitorCnt != 0){
            		af.setTxnMonitorCnt(txnMonitorCnt);
            	}
            	
            	if(txnMonitorRecipient != null){
            		af.setTxnMonitorRecipient(txnMonitorRecipient);
            	}
            	
            	af.save();
            	completed = true;
            
            } else if(list.size() == 1){
            	//update
            	af = (AcctFeature)list.get(0);
            	af.setAcctID(accountID);
            	
            	if(allowSelectiveRedemption != null){
            		if("1".equals(allowSelectiveRedemption)){
                		af.setAllowSelectiveRedemption((short)1);
            		} else {
            			af.setAllowSelectiveRedemption((short)0);
            		}
            	} else {
            		af.setAllowSelectiveRedemption((short)0);
            	}
            	
            	if(merchantGroupId != -1){
            		af.setSelectiveRedemptionMgID(merchantGroupId);
            	}
            	
            	if(allowTxnMonitor != null){
            		if("1".equals(allowTxnMonitor)){
            			af.setAllowTxnMonitor((short)1);
            		} else {
            			af.setAllowTxnMonitor((short)0);
            		}
            	} else {
            		af.setAllowTxnMonitor((short)0);
            	}
            	
            	if(txnMonitorCnt != 0){
            		af.setTxnMonitorCnt(txnMonitorCnt);
            	}
            	
            	if(txnMonitorRecipient != null){
            		af.setTxnMonitorRecipient(txnMonitorRecipient);
            	}
            	
            	af.save();
            	completed = true;
            	
            }
//            account.setAcctName(acctName);
//            account.setOffActInputType((short)offActInputType);
//
//            //update admin access
//            status = updateAccountsAdminOptimized(accountID, aid);
////            status = updateAccountsAdmin(accountID, aid);
//
//            //update merchant in accounts
//            //status = updateMerchantInAccounts(accountID, mid);
//            status = updateMerchantInAccountsOptimized(accountID, mid);
//
//            //update pricing parameter
//            status = updatePricingParameter(accountID, ppid);
//
//            account.save();
        } catch (TorqueException e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save to account feature");
            ExceptionManager.logStackTraceString(e, logger);
        } catch (Exception e) {
            status.setStatus(AccountsManager.ERR_EDIT_FAIL);
            logger.error("Failed to save account feature");
            ExceptionManager.logStackTraceString(e, logger);
        } finally {
        	if(completed){
        		status.setStatus(AccountsManager.STATUS_EDIT_OK);
        	}
        	
        }
        
        return status;
	}

}


