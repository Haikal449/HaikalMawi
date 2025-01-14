/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.contacts.activities;

import android.accounts.Account;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume; // Description: for SIM name display
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;

import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.common.util.AccountSelectionUtil;

import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.list.ContactsIntentResolverEx;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.widget.ImportExportItem;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.ext.ICtExtension.StringName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.UserHandle;


public class ContactImportExportActivity extends Activity implements View.OnClickListener,
        AdapterView.OnItemClickListener {
    private static final String TAG = "ContactImportExportActivity";

    public static final int REQUEST_CODE = 111111;
    public static final int RESULT_CODE = 111112;

    /*
     * To unify the storages(includes internal storage and external storage)
     * handling, we looks all of storages as one kind of account type.
     */
    public static final String STORAGE_ACCOUNT_TYPE = "_STORAGE_ACCOUNT";

    private static final int ACCOUNT_LOADER_ID = 0;

    private static final int SELECTION_VIEW_STEP_NONE = 0;
    private static final int SELECTION_VIEW_STEP_ONE = 1;
    private static final int SELECTION_VIEW_STEP_TWO = 2;

    private ListView mListView = null;
    private List<AccountWithDataSetEx> mAccounts = null;

    private int mShowingStep = SELECTION_VIEW_STEP_NONE;
    private int mCheckedPosition = 0;
    private boolean mIsFirstEntry = true;
    private AccountWithDataSetEx mCheckedAccount1 = null;
    private AccountWithDataSetEx mCheckedAccount2 = null;
    private List<ListViewItemObject> mListItemObjectList = new ArrayList<ListViewItemObject>();
    private AccountListAdapter mAdapter = null;

    private ProgressDialog mWaitingDialog = null;


    @Override
    protected void onCreate(Bundle icicle) {
       	int themeId = getResources().getIdentifier("androidhwext:style/Theme.Emui.WithActionBar", null, null);
    		if (themeId > 0){
    			setTheme(themeId); 
    		}
        super.onCreate(icicle);
        setContentView(R.layout.mtk_import_export_bridge_layout);

        ((Button) findViewById(R.id.btn_action)).setOnClickListener(this);
        ((Button) findViewById(R.id.btn_back)).setOnClickListener(this);

        ((LinearLayout) findViewById(R.id.buttonbar_layout)).setVisibility(View.GONE);

        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                            | ActionBar.DISPLAY_SHOW_HOME);
            actionBar.setTitle(R.string.imexport_title);
        }

        mAdapter = new AccountListAdapter(ContactImportExportActivity.this);
        ///to fix two internal storage in English and Chinese(ALPS00402258)
        getLoaderManager().restartLoader(ACCOUNT_LOADER_ID, null, new MyLoaderCallbacks());

    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setCheckedPosition(position);
        setCheckedAccount(position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mShowingStep > SELECTION_VIEW_STEP_ONE) {
            onBackAction();
        } else {
            super.onBackPressed();
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_action:
            case R.id.btn_back:
                int pos = 0;
                if (view.getId() == R.id.btn_action) {
                    onNextAction();
                } else {
                    onBackAction();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtils.i(TAG, "[onActivityResult]requestCode:" + requestCode +
                ",resultCode:" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ContactImportExportActivity.REQUEST_CODE) {
            if (resultCode == ContactImportExportActivity.RESULT_CODE) {
                this.finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mIsFinished = true;
        super.onDestroy();
        LogUtils.i(TAG, "[onDestroy]");
    }

    public void doImportExport() {
        LogUtils.d(TAG, "[doImportExport]...");

        if (AccountTypeUtils.isAccountTypeIccCard(mCheckedAccount1.type)) {
            // UIM
            int subId = ((AccountWithDataSetEx) mCheckedAccount1).getSubId();
            /** M: change for PHB Status Refacotring @{*/
            if (!SimCardUtils.isPhoneBookReady(subId)) {
                Toast.makeText(this, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
                finish();
                Log.i(TAG, "[doImportExport] phb is not ready.");
            } else {
                handleImportExportAction();
            }
            /** @} */
        } else {
            handleImportExportAction();
        }
    }

    public File getDirectory(String path, String defaultPath) {
        LogUtils.d(TAG, "[getDirectory]path : " + path + ",defaultPath:" + defaultPath);
        return path == null ? new File(defaultPath) : new File(path);
    }

    public List<AccountWithDataSetEx> getStorageAccounts() {
        List<AccountWithDataSetEx> storageAccounts = new ArrayList<AccountWithDataSetEx>();
        StorageManager mSM = (StorageManager) getApplicationContext().getSystemService(
                STORAGE_SERVICE);
        if (null == mSM) {
            LogUtils.w(TAG, "[getStorageAccounts]SM is null!");
            return storageAccounts;
        }
        StorageVolume volumes[] = mSM.getVolumeList();
        if (volumes != null) {
            LogUtils.d(TAG, "[getStorageAccounts]volumes are " + volumes);
            for (StorageVolume volume : volumes) {
                if (volume.getPath().startsWith(Environment.DIRECTORY_USBOTG)
                        || !checkSDCardAvaliable(volume.getPath())) {
                    continue;
                }
                storageAccounts.add(new AccountWithDataSetEx(volume.getDescription(this),
                        STORAGE_ACCOUNT_TYPE, volume.getPath()));
            }
        }
        return storageAccounts;
    }

    ////////////////////////////private funcation///////////////////////////////////////
    private class ListViewItemObject {
        public AccountWithDataSetEx mAccount;
        public ImportExportItem mView;

        public ListViewItemObject(AccountWithDataSetEx account) {
            mAccount = account;
        }

        public String getName() {
            if (mAccount == null) {
                LogUtils.w(TAG, "[getName]mAccount is null!");
                return "null";
            } else {
                String displayName = null;
                displayName = AccountFilterUtil.getAccountDisplayNameByAccount(mAccount.type,
                        mAccount.name);
                if (Build.TYPE.equals("eng")) {
                    LogUtils.d(TAG, "[getName]type : " + mAccount.type + ",name:" + mAccount.name
                            + ",displayName:" + displayName);
                }
                if (TextUtils.isEmpty(displayName)) {
                    if (AccountWithDataSetEx.isLocalPhone(mAccount.type)) {
                        return getString(R.string.account_phone_only);
                    }
                    return mAccount.name;
                } else {
                    return displayName;
                }
            }
        }
    }

    private class AccountListAdapter extends BaseAdapter {
        private final LayoutInflater mLayoutInflater;
        private Context mContext;
        private AccountTypeManager mAccountTypes;

        public AccountListAdapter(Context context) {
            mContext = context;
            mAccountTypes = AccountTypeManager.getInstance(context);
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mListItemObjectList.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public AccountWithDataSetEx getItem(int position) {
            return null;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ImportExportItem view;
            int slotId=0;

            if (convertView != null) {
                view = (ImportExportItem) convertView;
            } else {
                view = (ImportExportItem) mLayoutInflater.inflate(R.layout.mtk_contact_import_export_item, parent, false);
            }

            ListViewItemObject itemObj = mListItemObjectList.get(position);
            itemObj.mView = view;
            final AccountWithDataSet account = (AccountWithDataSet) itemObj.mAccount;
            final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);
            Drawable icon;
            final int subId = itemObj.mAccount.getSubId();
            LogUtils.d(TAG, "[getView]dataSet: " + account.dataSet + ",subId: " + subId);
            if (accountType != null && accountType.isIccCardAccount()) {
                icon = accountType.getDisplayIconBySubId(mContext, subId);
            } else {
                icon = accountType.getDisplayIcon(mContext);
            }
            if(accountType.isIccCardAccount()){
            	int subID=((AccountWithDataSetEx) account).mSubId;
                SubscriptionInfo sfr = SubInfoUtils.getSubInfoUsingSubId(subID);
                if (sfr != null) {
                    slotId=sfr.getSimSlotIndex();
                }
            }else {
				slotId=0;
			}
            view.bindView(icon, itemObj.getName(), account.dataSet,accountType,slotId);
            view.setActivated(mCheckedPosition == position);
            return view;
        }
    }

    private void onBackAction() {
        int pos = 0;
        setShowingStep(SELECTION_VIEW_STEP_ONE);
        pos = getCheckedAccountPosition(mCheckedAccount1);
        mCheckedPosition = pos;
        setCheckedAccount(mCheckedPosition);
        updateUi();
    }

    private void onNextAction() {
        int pos = 0;
        if (mShowingStep >= SELECTION_VIEW_STEP_TWO) {
            doImportExport();
            return;
        }
        setShowingStep(SELECTION_VIEW_STEP_TWO);
        if (mIsFirstEntry || (mCheckedAccount1 == null && mCheckedAccount2 == null)) {
            pos = 0;
        } else {
            pos = getCheckedAccountPosition(mCheckedAccount2);
        }
        mIsFirstEntry = false;
        mCheckedPosition = pos;
        setCheckedAccount(mCheckedPosition);
        updateUi();
    }

    private void updateUi() {
        setButtonState(true);
        mListView.setAdapter(mAdapter);
    }

    private int getCheckedAccountPosition(AccountWithDataSetEx checkedAccount) {
        for (int i = 0; i < mListItemObjectList.size(); i++) {
            ListViewItemObject obj = mListItemObjectList.get(i);
            if (obj.mAccount == checkedAccount) {
                return i;
            }
        }
        return 0;
    }

    private void handleImportExportAction() {
        LogUtils.d(TAG, "[handleImportExportAction]...");
        if (isStorageAccount(mCheckedAccount1) && !checkSDCardAvaliable(mCheckedAccount1.dataSet)
                || isStorageAccount(mCheckedAccount2)
                && !checkSDCardAvaliable(mCheckedAccount2.dataSet)) {
            new AlertDialog.Builder(this).setMessage(R.string.no_sdcard_message).setTitle(
                    R.string.no_sdcard_title).setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
            return;
        }

        if (isStorageAccount(mCheckedAccount1)) { // import from SDCard
            if (mCheckedAccount2 != null) {
                AccountSelectionUtil.doImportFromSdCard(this, mCheckedAccount1.dataSet,
                        mCheckedAccount2);
            }
        } else {
            if (isStorageAccount(mCheckedAccount2)) { // export to SDCard
                if (isSDCardFull(mCheckedAccount2.dataSet)) { // SD card is full
                    Log.i(TAG, "[handleImportExportAction] isSDCardFull");
                    new AlertDialog.Builder(this).setMessage(R.string.storage_full).setTitle(
                            R.string.storage_full).setIconAttribute(android.R.attr.alertDialogIcon)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    }).show();
                    return;
                }

                Intent intent = new Intent(this,
                        com.mediatek.contacts.list.ContactListMultiChoiceActivity.class).setAction(
                        ContactsIntent.LIST.ACTION_PICK_MULTICONTACTS).putExtra("request_type",
                        ContactsIntentResolverEx.REQ_TYPE_IMPORT_EXPORT_PICKER).putExtra(
                        "toSDCard", true).putExtra("fromaccount", mCheckedAccount1).putExtra(
                        "toaccount", mCheckedAccount2);
                startActivityForResult(intent, ContactImportExportActivity.REQUEST_CODE);
            } else { // account to account
                Intent intent = new Intent(this,
                        com.mediatek.contacts.list.ContactListMultiChoiceActivity.class).setAction(
                        ContactsIntent.LIST.ACTION_PICK_MULTICONTACTS).putExtra("request_type",
                        ContactsIntentResolverEx.REQ_TYPE_IMPORT_EXPORT_PICKER).putExtra(
                        "toSDCard", false).putExtra("fromaccount", mCheckedAccount1).putExtra(
                        "toaccount", mCheckedAccount2);
                startActivityForResult(intent, ContactImportExportActivity.REQUEST_CODE);
            }
        }
    }

    private boolean checkSDCardAvaliable(final String path) {
        if (TextUtils.isEmpty(path)) {
            LogUtils.w(TAG, "[checkSDCardAvaliable]path is null!");
            return false;
        }

        String volumeState = "";
        try {
            IMountService mountService = IMountService.Stub.asInterface(ServiceManager
                    .getService("mount"));
            volumeState = mountService.getVolumeState(path);
        } catch (RemoteException e) {
            LogUtils.e(TAG, "[checkSDCardAvaliable]catch exception:");
            e.printStackTrace();
        }

        return Environment.MEDIA_MOUNTED.equals(volumeState);
    }

    private boolean isSDCardFull(final String path) {
        if (TextUtils.isEmpty(path)) {
            LogUtils.w(TAG, "[isSDCardFull]path is null!");
            return false;
        }
        LogUtils.d(TAG, "[isSDCardFull] storage path is " + path);
        if (checkSDCardAvaliable(path)) {
            StatFs sf = null;
            try {
                sf = new StatFs(path);
            } catch (IllegalArgumentException e) {
                LogUtils.e(TAG, "[isSDCardFull]catch exception:");
                e.printStackTrace();
                return false;
            }
            if (sf == null) {
                LogUtils.e(TAG, "[isSDCardFull] sf is null ");
                return false;
            }
            long availCount = sf.getAvailableBlocks();
            return !(availCount > 0);
        }

        return true;
    }

    private void setButtonState(boolean isTrue) {
        findViewById(R.id.btn_back).setVisibility(
                (isTrue && (mShowingStep > SELECTION_VIEW_STEP_ONE)) ? View.VISIBLE : View.GONE);

        findViewById(R.id.btn_action).setEnabled(
                isTrue && (mShowingStep > SELECTION_VIEW_STEP_NONE));
    }

    /**
     *  showing the step of import/export step according to the parame
     *  shaoingStep
     * @param showingStep
     */
    private void setShowingStep(int showingStep) {
        mShowingStep = showingStep;
        mListItemObjectList.clear();

        ((LinearLayout) findViewById(R.id.buttonbar_layout)).setVisibility(View.VISIBLE);

        if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
            ((TextView) findViewById(R.id.tips)).setText(R.string.tips_source);
            for (AccountWithDataSetEx account : mAccounts) {
                /// For MTK multiuser in 3gdatasms @{
                if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
                    int userId = UserHandle.myUserId();
                    LogUtils.i(TAG, "[setShowingStep]MTK_ONLY_OWNER_SIM_SUPPORT is true,userId : " + userId);
                    if (userId != UserHandle.USER_OWNER) {
                        AccountTypeManager atm = AccountTypeManager.getInstance(this);
                        AccountType accountType = atm.getAccountType(account.getAccountTypeWithDataSet());
                        if (accountType.isIccCardAccount()) {
                            continue;
                        }
                    }
                }
                /// @}
                mListItemObjectList.add(new ListViewItemObject(account));
            }
        } else if (mShowingStep == SELECTION_VIEW_STEP_TWO) {
            ((TextView) findViewById(R.id.tips)).setText(R.string.tips_target);
            for (AccountWithDataSetEx account : mAccounts) {
                if (mCheckedAccount1 != account) {
                    /*
                     * It is not allowed for the importing from Storage -> SIM
                     * or USIM and from SIM or USIM -> Storage and also is not
                     * for importing from Storage -> Storage
                     */
                    AccountTypeManager atm = AccountTypeManager.getInstance(this);
                    AccountType accountType = atm.getAccountType(account.getAccountTypeWithDataSet());
                    AccountType checkedAccountType = atm.getAccountType(mCheckedAccount1.getAccountTypeWithDataSet());
                    if ((isStorageAccount(mCheckedAccount1) && accountType.isIccCardAccount())
                            || (checkedAccountType.isIccCardAccount() && isStorageAccount(account))
                            || (isStorageAccount(mCheckedAccount1) && isStorageAccount(account))) {
                        continue;
                    }

                    /// For MTK multiuser in 3gdatasms @{
                    if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
                        int userId = UserHandle.myUserId();
                        if (userId != UserHandle.USER_OWNER) {
                            if (accountType.isIccCardAccount()) {
                                continue;
                            }
                        }
                    }
                    /// @}
                    mListItemObjectList.add(new ListViewItemObject(account));
                }
            }
        }
    }

    private static boolean isStorageAccount(final Account account) {
        if (account != null) {
            return STORAGE_ACCOUNT_TYPE.equalsIgnoreCase(account.type);
        }
        return false;
    }

    private static class AccountsLoader extends AsyncTaskLoader<List<AccountWithDataSetEx>> {
        private Context mContext;

        public AccountsLoader(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public List<AccountWithDataSetEx> loadInBackground() {
            return loadAccountFilters(mContext);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
        }
    }

    private void setCheckedPosition(int checkedPosition) {
        if (mCheckedPosition != checkedPosition) {
            setListViewItemChecked(mCheckedPosition, false);
            mCheckedPosition = checkedPosition;
            setListViewItemChecked(mCheckedPosition, true);
        }
    }

    private void setCheckedAccount(int position) {
        if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
            mCheckedAccount1 = mListItemObjectList.get(position).mAccount;
        } else if (mShowingStep == SELECTION_VIEW_STEP_TWO) {
            mCheckedAccount2 = mListItemObjectList.get(position).mAccount;
        }
    }

    private void setListViewItemChecked(int checkedPosition, boolean checked) {
        if (checkedPosition > -1) {
            ListViewItemObject itemObj = mListItemObjectList.get(checkedPosition);
            if (itemObj.mView != null) {
                itemObj.mView.setActivated(checked);
            }
        }
    }

    private static List<AccountWithDataSetEx> loadAccountFilters(Context context) {
        List<AccountWithDataSetEx> accountsEx = new ArrayList<AccountWithDataSetEx>();
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> accounts = accountTypes.getAccounts(true);

        for (AccountWithDataSet account : accounts) {
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            if (accountType.isExtension() && !account.hasData(context)) {
                // Hide extensions with no raw_contacts.
                continue;
            }
            int subId = SubInfoUtils.getInvalidSubId();
            if (account instanceof AccountWithDataSetEx) {
                subId = ((AccountWithDataSetEx) account).getSubId();
            }
            accountsEx.add(new AccountWithDataSetEx(account.name, account.type, subId));
        }

        return accountsEx;
    }

    private class MyLoaderCallbacks implements LoaderCallbacks<List<AccountWithDataSetEx>> {
        @Override
        public Loader<List<AccountWithDataSetEx>> onCreateLoader(int id, Bundle args) {
            return new AccountsLoader(ContactImportExportActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<List<AccountWithDataSetEx>> loader,
                List<AccountWithDataSetEx> data) {
          ///M:check whether the Activity's status still ok
            if (isActivityFinished()) {
                LogUtils.w(TAG, "[onLoadFinished]isActivityFinished is true.");
                return;
            }

            if (data == null) { // Just in case...
                LogUtils.e(TAG, "[onLoadFinished]data is null!");
                return;
            }

            if (mAccounts == null) {
                mAccounts = data;
                // Add all of storages accounts
                mAccounts.addAll(getStorageAccounts());
                // If the accounts size is less than one item, we should not
                // show this view for user to import or export operations.
                if (mAccounts.size() <= 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ///M:[for op09]@{
                            String ctString = ExtensionManager.getInstance().getCtExtension()
                                    .getString(StringName.LOSE_ACCOUNT_OR_STORAGE);
                            if (!TextUtils.isEmpty(ctString)) {
                                MtkToast.toast(getApplicationContext(), ctString);
                                ///M@}
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.xport_error_one_account, Toast.LENGTH_SHORT)
                                .show();
                            }
                        }
                    });
                    finish();
                }

                if (mShowingStep == SELECTION_VIEW_STEP_NONE) {
                    setShowingStep(SELECTION_VIEW_STEP_ONE);
                } else {
                    setShowingStep(mShowingStep);
                }
                setCheckedAccount(mCheckedPosition);
                updateUi();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<AccountWithDataSetEx>> loader) {
        }
    }

    private boolean isActivityFinished() {
        return mIsFinished;
    }

    private boolean mIsFinished = false;
}
