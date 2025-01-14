/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import com.android.internal.telephony.TelephonyProperties;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

/// M:
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.DefaultMmsFeatureManagerExt;
import com.mediatek.mms.ext.IMmsConfigExt;
import com.mediatek.mms.ext.IMmsFeatureManagerExt;
import com.mediatek.mms.ext.DefaultMmsConfigExt;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;

import com.android.mms.util.FeatureOption;
import com.android.mms.util.MmsLog;
/// M: add for ipmessage
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.android.mms.ui.MessageUtils;
import com.mediatek.custom.CustomProperties;
/// M: add for CMCC FT
import org.apache.http.params.HttpParams;

/// M: ALPS00527989, Extend TextView URL handling @ {
import android.widget.TextView;
/// @}
/// M: ALPS00956607, not show modify button on recipients editor @{
import android.view.inputmethod.EditorInfo;
/// @}
/// M: Add MmsService configure param @{
import android.os.Bundle;
/// @}
import com.android.mms.ui.ComposeMessageActivity;

import com.mediatek.common.MPlugin;
import android.telephony.SubscriptionInfo;
import android.provider.Settings;//add by lipeng
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
public class MmsConfig {
    private static final String TAG = "MmsConfig";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = false;

    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    /// KK migration, for default MMS function. @{
    private static final String MMS_APP_PACKAGE = "com.android.contacts";

    private static final String SMS_PROMO_DISMISSED_KEY = "sms_promo_dismissed_key";
    ///@}
    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH  = 640;

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean mTransIdEnabled = false;
    private static int mMmsEnabled = 1;                         // default to true
    private static int mMaxMessageSize = 300 * 1024;            // default to 300k max size
    private static String mUserAgent = DEFAULT_USER_AGENT;
    private static String mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String mUaProfUrl = null;
    private static String mHttpParams = null;
    private static String mHttpParamsLine1Key = null;
    private static String mEmailGateway = null;
    private static int mMaxImageHeight = MAX_IMAGE_HEIGHT;      // default value
    private static int mMaxImageWidth = MAX_IMAGE_WIDTH;        // default value
    private static int mRecipientLimit = Integer.MAX_VALUE;     // default value
    private static int mDefaultSMSMessagesPerThread = 500;    // default value
    private static int mDefaultMMSMessagesPerThread = 50;     // default value
    private static int mMinMessageCountPerThread = 2;           // default value
    private static int mMaxMessageCountPerThread = 10000;        // default value
    private static int mMinimumSlideElementDuration = 7;        // default to 7 sec
    private static boolean mNotifyWapMMSC = false;
    private static boolean mAllowAttachAudio = true;

    // This flag is somewhat confusing. If mEnableMultipartSMS is true, long sms messages are
    // always sent as multi-part sms messages, with no checked limit on the number of segments.
    // If mEnableMultipartSMS is false, then mSmsToMmsTextThreshold is used to determine the
    // limit of the number of sms segments before turning the long sms message into an mms
    // message. For example, if mSmsToMmsTextThreshold is 4, then a long sms message with three
    // or fewer segments will be sent as a multi-part sms. When the user types more characters
    // to cause the message to be 4 segments or more, the send button will show the MMS tag to
    // indicate the message will be sent as an mms.
    private static boolean mEnableMultipartSMS = true;

    private static boolean mEnableSlideDuration = true;
    private static boolean mEnableMMSReadReports = true;        // key: "enableMMSReadReports"
    private static boolean mEnableSMSDeliveryReports = true;    // key: "enableSMSDeliveryReports"
    private static boolean mEnableMMSDeliveryReports = true;    // key: "enableMMSDeliveryReports"
    private static int mMaxTextLength = -1;

    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int mMaxSizeScaleForPendingMmsAllowed = 4;       // default value

    // Email gateway alias support, including the master switch and different rules
    private static boolean mAliasEnabled = false;
    private static int mAliasRuleMinChars = 2;
    private static int mAliasRuleMaxChars = 48;

    private static int mMaxSubjectLength = 40;  // maximum number of characters allowed for mms
                                                // subject

    /// M: google jb.mr1 patch, group mms
    // If mEnableGroupMms is true, a message with multiple recipients, regardless of contents,
    // will be sent as a single MMS message with multiple "TO" fields set for each recipient.
    // If mEnableGroupMms is false, the group MMS setting/preference will be hidden in the settings
    // activity.
    private static boolean mEnableGroupMms = true;

    private static final int RECIPIENTS_LIMIT = 50;

    /// M: Mms size limit, default 300K.
    private static int mUserSetMmsSizeLimit = 300;
    /// M: Receive Mms size limit for 2G network
    private static int mReceiveMmsSizeLimitFor2G = 200;
    /// M: Receive Mms size limit for TD network
    private static int mReceiveMmsSizeLimitForTD = 400;

    /// M: default value
    private static int mMaxRestrictedImageHeight = 1200;
    private static int mMaxRestrictedImageWidth = 1600;
    private static int mSmsRecipientLimit = 100;

    private static boolean mDeviceStorageFull = false;

    private static IMmsConfigExt mMmsConfigPlugin = null;
    /// Mms operator plugin switchers
    private static IMmsFeatureManagerExt mMmsFeatureManagerPlugin = null;

    /// M: For OP09; Device storage low or not low.
    private static boolean mCTDeviceStorageLow = false;

    // / M: Add for get max text size from ip message
    private static Context mContext;

    /// Add for support Auto Test.
    public static boolean sIsRunTestCase = false;
    public static boolean sIsDefaultSMSInTestCase = false;
    /// @}
	private static List<SubscriptionInfo> mSubInfoList;

    private static List<Integer> allQuickTextIds = new ArrayList<Integer>();
    private static List<String> allQuickTexts = new ArrayList<String>();

    private static void initPlugin(Context context) {
        mMmsConfigPlugin = (IMmsConfigExt) MPlugin.createInstance(IMmsConfigExt.class.getName(), context);
        if (mMmsConfigPlugin == null) {
            mMmsConfigPlugin = new DefaultMmsConfigExt();
            MmsLog.d(TAG, "default mMmsConfigPlugin = " + mMmsConfigPlugin);
        }

        ///M: add for MMS Operator Feature switcher plugin @{
        mMmsFeatureManagerPlugin =
                    (IMmsFeatureManagerExt) MPlugin.createInstance(IMmsFeatureManagerExt.class.getName(), context);
        if (mMmsFeatureManagerPlugin == null) {
            mMmsFeatureManagerPlugin = new DefaultMmsFeatureManagerExt(context);
            MmsLog.d(TAG, "default mMmsFeatureManagerPlugin = " + mMmsFeatureManagerPlugin);
        }
        /// @}
    }

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.init()");
        }
        // Always put the mnc/mcc in the log so we can tell which mms_config.xml was loaded.
        Log.v(TAG, "mnc/mcc: " +
                android.os.SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC));

        mContext = context;
        // initialize the operator plugin
        initPlugin(context);

        loadMmsSettings(context);

        /// M: add for ipmessage
        if (IpMessageUtils.getIpMessagePlugin(context).isActualPlugin()) {
            initializeIpMessageFilePath(context);
        }
    }

    /// KK migration, for default MMS function. @{
    public static boolean isSmsEnabled(Context context) {
        /// Add for support Auto Test.
        if (sIsRunTestCase) {
            return sIsDefaultSMSInTestCase;
        }
        /// @}
        String defaultSmsApplication = Telephony.Sms.getDefaultSmsPackage(context);
		/*HQ_zhangjing 2015-08-10 modified for MMS merge begin*/
        //if (defaultSmsApplication != null && defaultSmsApplication.equals(MMS_APP_PACKAGE)) {
        if (defaultSmsApplication != null && defaultSmsApplication.equals(MMS_APP_PACKAGE)) {
		/*HQ_zhangjing 2015-08-10 modified for MMS merge end*/
            return true;
        }
        return false;
    }

    public static boolean isSmsPromoDismissed(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(SMS_PROMO_DISMISSED_KEY, false);
    }

    public static void setSmsPromoDismissed(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SMS_PROMO_DISMISSED_KEY, true);
        editor.apply();
    }

    public static Intent getRequestDefaultSmsAppActivity() {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, MMS_APP_PACKAGE);
        return intent;
    }
    ///@}

    /// M: this method is changed to use plugin
    public static int getSmsToMmsTextThreshold() {
        // Operator Plugin
        if (FeatureOption.MTK_C2K_SUPPORT) {
            return mMmsConfigPlugin.getSmsToMmsTextThresholdForC2K(mContext);
        }
        //add by lipeng for al813
        if(SystemProperties.get("ro.hq.mms.ap.smstomms").equals("1")) {
           mSubInfoList = SubscriptionManager.from(MmsApp.getApplication()).getActiveSubscriptionInfoList();
		   int mSubCount = (mSubInfoList != null && !mSubInfoList.isEmpty()) ? mSubInfoList.size() : 0; 
           int slotId = -1;
		if(mSubInfoList!=null && !mSubInfoList.isEmpty()){
           slotId = mSubInfoList.get(0).getSimSlotIndex();
		}
           Log.d("lipeng","smstomms_activieSlot = " + slotId);
		   if( mSubCount > 1 ){
			   Log.d("lipeng","mSubCount = " + mSubCount);
			   int SmsToMmsNum1 = MessageUtils.loadSmsToMmsByMncMcc(MessageUtils.getSimMccMnc(0));
			   int SmsToMmsNum2 = MessageUtils.loadSmsToMmsByMncMcc(MessageUtils.getSimMccMnc(1));
			   Log.d("lipeng","SmsToMmsNum1 = " + SmsToMmsNum1+"------"+"SmsToMmsNum2 = " + SmsToMmsNum2);
			   if(SmsToMmsNum1<=SmsToMmsNum2){
                  return SmsToMmsNum1;
			   }else{
			   	  return SmsToMmsNum2;
			   }
			  }else if ( mSubCount == 1 ){
			   Log.d("smstomms_lipeng","smstomms_slotId = " + slotId);
			   if(slotId == 1){
				int SmsToMmsNumber2 = MessageUtils.loadSmsToMmsByMncMcc(MessageUtils.getSimMccMnc(slotId));
				Log.d("lipeng","SmsToMmsNumber2 = " + SmsToMmsNumber2);
				return SmsToMmsNumber2;
			   }else{
				int SmsToMmsNumber1 = MessageUtils.loadSmsToMmsByMncMcc(MessageUtils.getSimMccMnc(slotId));
				Log.d("lipeng","SmsToMmsNumber1 = " + SmsToMmsNumber1);
				return SmsToMmsNumber1;
			   }
			  }
            //return MessageUtils.loadSmsToMmsByMncMcc(MessageUtils.getSimMccMnc(slotId));
        }//end by lipeng
        return mMmsConfigPlugin.getSmsToMmsTextThreshold();
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getMaxMessageSize() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.getMaxMessageSize(): " + mMaxMessageSize);
        }
        return mMaxMessageSize;
    }

    /**
     * This function returns the value of "enabledTransID" present in mms_config file.
     * In case of single segment wap push message, this "enabledTransID" indicates whether
     * TransactionID should be appended to URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        /// M: @{
        String value = CustomProperties.getString(
                CustomProperties.MODULE_MMS,
                CustomProperties.USER_AGENT,
                mUserAgent);
        /// @}
        return value;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        /// M: @{
        String value = CustomProperties.getString(
                CustomProperties.MODULE_MMS,
                CustomProperties.UAPROF_URL,
                mUaProfUrl);
        /// @}
        return value;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getRecipientLimit() {
        return mRecipientLimit;
    }

    /// M: change this to plugin
    public static int getMaxTextLimit() {
        if (isActivated(mContext) && !(IpMessageUtils.getMessageManager(mContext).getMaxTextLimit() == 0)) {
            return IpMessageUtils.getMessageManager(mContext).getMaxTextLimit();
        } else {
            return mMmsConfigPlugin.getMaxTextLimit();
        }
    }

    public static int getDefaultSMSMessagesPerThread() {
        return mDefaultSMSMessagesPerThread;
    }

    public static int getDefaultMMSMessagesPerThread() {
        return mDefaultMMSMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }

    public static int getHttpSocketTimeout() {
        return mMmsConfigPlugin.getHttpSocketTimeout();
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static boolean getMultipartSmsEnabled() {
        return mEnableMultipartSMS;
    }

    public static boolean getSlideDurationEnabled() {
        return mEnableSlideDuration;
    }

    public static boolean getMMSReadReportsEnabled() {
        return mEnableMMSReadReports;
    }

    public static boolean getSMSDeliveryReportsEnabled() {
        return mEnableSMSDeliveryReports;
    }

    public static boolean getMMSDeliveryReportsEnabled() {
        return mEnableMMSDeliveryReports;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }

    public static int getMaxSubjectLength() {
        return mMaxSubjectLength;
    }

    /// M: google jb.mr1 patch, group mms
    public static boolean getGroupMmsEnabled() {
        return mEnableGroupMms;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type = parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }

        if (type != parser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int type;
        while ((type = parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }
    }

    private static void loadMmsSettings(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                if (DEBUG) {
                    Log.v(TAG, "tag: " + tag + " value: " + value + " - " +
                            text);
                }
                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        } else if ("enableMultipartSMS".equalsIgnoreCase(value)) {
                            mEnableMultipartSMS = "true".equalsIgnoreCase(text);
                        } else if ("enableSlideDuration".equalsIgnoreCase(value)) {
                            mEnableSlideDuration = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSReadReports".equalsIgnoreCase(value)) {
                            mEnableMMSReadReports = "true".equalsIgnoreCase(text);
                        } else if ("enableSMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableSMSDeliveryReports = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableMMSDeliveryReports = "true".equalsIgnoreCase(text);
                        /// M: google jb.mr1 patch, group mms
                        } else if ("enableGroupMms".equalsIgnoreCase(value)) {
                            mEnableGroupMms = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        }
                        /// M: @{
                        else if ("maxRestrictedImageHeight".equalsIgnoreCase(value)) {
                            mMaxRestrictedImageHeight = Integer.parseInt(text);
                        } else if ("maxRestrictedImageWidth".equalsIgnoreCase(value)) {
                            mMaxRestrictedImageWidth = Integer.parseInt(text);
                        }
                        /// @}
                        else if ("defaultSMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultSMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("defaultMMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        }
                        else if ("smsToMmsTextThreshold".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setSmsToMmsTextThreshold(Integer.parseInt(text));
                        }
                        else if ("recipientLimit".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setMmsRecipientLimit(Integer.parseInt(text));
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mMmsConfigPlugin.setHttpSocketTimeout(Integer.parseInt(text));
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        } else if ("maxMessageTextSize".equalsIgnoreCase(value)) {
                            /// M: Operator Plugin
                            mMmsConfigPlugin.setMaxTextLimit(Integer.parseInt(text));
                        } else if ("maxSubjectLength".equalsIgnoreCase(value)) {
                            mMaxSubjectLength = Integer.parseInt(text);
                        }
                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            mUaProfUrl = text;
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err =
                String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                        errorStr);
            Log.e(TAG, err);
        }
    }

    /// M:
    /**
     * Notes:for CMCC customization,whether to enable SL automatically lanuch.
     * default set false
     */
    private static boolean mSlAutoLanuchEnabled = false;
    public static boolean getSlAutoLanuchEnabled() {
        return mSlAutoLanuchEnabled;
    }

    public static void setDeviceStorageFullStatus(boolean bFull) {
        mDeviceStorageFull = bFull;
    }

    public static boolean getDeviceStorageFullStatus() {
        return mDeviceStorageFull;
    }

    /// M: add for cmcc dir ui @{
    public static void setMmsDirMode(boolean mode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("CmccMmsUiMode", mode);
        editor.commit();
    }

    public static boolean getMmsDirMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean dirMode = sp.getBoolean("CmccMmsUiMode", false);
        return dirMode;
    }
    // @}

    /// M: add for cmcc dir ui @{
    public static void setSimCardInfo(int simcard) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("CmccSimCardInfo", simcard);
        editor.commit();
    }

    public static int getSimCardInfo() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        int siminfo = sp.getInt("CmccSimCardInfo", 0);
        return siminfo;
    }
    /// @}

    /// M: new feature, init defualt quick text @{
    public static void setInitQuickText(boolean init) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("InitQuickText", init);
        editor.commit();
    }

    public static boolean getInitQuickText() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean isFristInit = sp.getBoolean("InitQuickText", true);
        return isFristInit;
    }
    /// @}

    public static List<String> getQuicktexts() {
        return allQuickTexts;
    }

    public static List<Integer> getQuicktextsId() {
        return allQuickTextIds;
    }

    public static int updateAllQuicktexts() {
        Cursor cursor = mContext.getContentResolver().query(Telephony.MmsSms.CONTENT_URI_QUICKTEXT,
                null, null, null, null);
        allQuickTextIds.clear();
        allQuickTexts.clear();
        String[] defaultTexts = mContext.getResources().getStringArray(R.array.default_quick_texts);
        int maxId = defaultTexts.length;
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int qtId = cursor.getInt(0);
                    allQuickTextIds.add(qtId);
                    String qtText = cursor.getString(1);
                    if (qtText != null && !qtText.isEmpty()) {
                        allQuickTexts.add(qtText);
                    } else {
                        allQuickTexts.add(defaultTexts[qtId - 1]);
                    }
                    if (qtId > maxId) {
                        maxId = qtId;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return maxId;
    }

    public static int getUserSetMmsSizeLimit(boolean isBytes) {
        if (true == isBytes) {
            return mUserSetMmsSizeLimit * 1024;
        } else {
            return mUserSetMmsSizeLimit;
        }
    }

    public static void setUserSetMmsSizeLimit(int limit) {
        mUserSetMmsSizeLimit = limit;
    }

    public static int getMaxRestrictedImageHeight() {
        return mMaxRestrictedImageHeight;
    }

    public static int getMaxRestrictedImageWidth() {
        return mMaxRestrictedImageWidth;
    }

    /// M: change this method to plugin
    public static int getMmsRecipientLimit() {
        return mMmsConfigPlugin.getMmsRecipientLimit();
    }

    public static int getSmsRecipientLimit() {
        return mSmsRecipientLimit;
    }

    public static int getReceiveMmsLimitFor2G() {
        return mReceiveMmsSizeLimitFor2G;
    }

    public static int getReceiveMmsLimitForTD() {
        return mReceiveMmsSizeLimitForTD;
    }

    public static boolean getSmsEncodingTypeEnabled(){
    	//add by lipeng for 7bit
        if(SystemProperties.get("ro.hq.mms.ap.sevenbit").equals("1")) { 
            return true;
        }//end by lipeng
        // Operator Plugin
        return isEnableSmsEncodingType(); 
    }

    public static boolean getDeliveryReportAllowed(){
        // Operator Plugin
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_REPORT_ALLOWED);
    }

    public static boolean getFolderModeEnabled() {
        // Operator Plugin
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_FOLDER_MODE);
    }

    public static boolean getShowStorageStatusEnabled() {
        // Operator Plugin
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.DISPLAY_STORAGE_STATUS);
    }

    public static boolean getSIMSmsAtSettingEnabled() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SHOW_SIM_SMS_ENTRY_IN_SETTINGS);
    }

    public static boolean getSIMLongSmsConcatenateEnabled() {
        // Operator Plugin
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SMS_ENABLE_CONCATENATE_LONG_SIM_SMS);
    }

    public static int getPluginMenuIDBase() {
        return 0x100;
    }

    /// M: add for ipmessage
    private static String sPicTempPath = "";
    private static String sAudioTempPath = "";
    private static String sVideoTempPath = "";
    private static String sVcardTempPath = "";
    private static String sCalendarTempPath = "";
    private static void initializeIpMessageFilePath(Context context) {
        if (IpMessageUtils.getSDCardStatus()) {
            sPicTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }

            sAudioTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }

            sVideoTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }

            sVcardTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }

            sCalendarTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }

            String cachePath = IpMessageUtils.getCachePath(context);
            if (cachePath != null) {
                File f = new File(cachePath);
                if (!f.exists()) {
                    f.mkdirs();
                }
            }
        }
    }

    public static String getPicTempPath(Context context) {
        if (TextUtils.isEmpty(sPicTempPath)) {
            sPicTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }
        return sPicTempPath;
    }

    public static String getAudioTempPath(Context context) {
        if (TextUtils.isEmpty(sAudioTempPath)) {
            sAudioTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }
        }
        return sAudioTempPath;
    }

    public static String getVideoTempPath(Context context) {
        if (TextUtils.isEmpty(sVideoTempPath)) {
            sVideoTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }
        }
        return sVideoTempPath;
    }

    public static String getVcardTempPath(Context context) {
        if (TextUtils.isEmpty(sVcardTempPath)) {
            sVcardTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }
        }
        return sVcardTempPath;
    }

    public static String getVcalendarTempPath(Context context) {
        if (TextUtils.isEmpty(sCalendarTempPath)) {
            sCalendarTempPath = IpMessageUtils.getSDCardPath(context) + IpMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }
        }
        return sCalendarTempPath;
    }

    private static boolean sSlot1SimExist = true;
    private static boolean sSlot2SimExist = true;
    private static int sSlot1RetryCounter = 0;
    private static int sSlot2RetryCounter = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private static long sSim1Id = -1;
    private static long sSim2Id = -1;

   /* private static void loadSimInfo(Context context) {
        /// M: sim1 info maybe not loaded yet, load it.
        if (sSim1Id <= 0 && sSlot1SimExist) {
            sSim1Id = EncapsulatedSimInfoManager.getIdBySlot(context, PhoneConstants.SIM_ID_1);
            sSlot1RetryCounter++;
            /// M: if we tried 3 times and still can't get valid simId , we think the slot is empty.
            if (sSlot1RetryCounter == MAX_RETRY_COUNT && sSim1Id <= 0) {
                sSlot1SimExist = false;
            }
        }
        /// M: sim2 info maybe not loaded yet, load it.
        if (sSim2Id <= 0 && sSlot2SimExist) {
            sSim2Id = EncapsulatedSimInfoManager.getIdBySlot(context, PhoneConstants.SIM_ID_2);
            sSlot2RetryCounter++;
            /// M: if we tried 3 times and still can't get valid simId , we think the slot is empty.
            if (sSlot2RetryCounter == MAX_RETRY_COUNT && sSim2Id <= 0) {
                sSlot2SimExist = false;
            }
        }
    }*/

    public static boolean isActivated(Context context) {
        if (!IpMessageUtils.getServiceManager(context).serviceIsReady()) {
            return false;
        }

        return true;
    }

    public static boolean isServiceEnabled(Context context) {
        if (!IpMessageUtils.getServiceManager(context).serviceIsReady()) {
            return false;
        }
        return true;
    }

    public static int getMmsRetryPromptIndex() {
        return mMmsConfigPlugin.getMmsRetryPromptIndex();
    }

    public static int[] getMmsRetryScheme() {
        return mMmsConfigPlugin.getMmsRetryScheme();
    }

    public static int[] getMmsRetryScheme(int messageType) {
        return mMmsConfigPlugin.getMmsRetryScheme(messageType);
    }

    public static void setSoSndTimeout(HttpParams params) {
        mMmsConfigPlugin.setSoSndTimeout(params);
    }

    public static boolean isAllowRetryForPermanentFail() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_RETRY_FOR_PERMANENTFAIL);
    }

    public static boolean isRetainRetryIndexWhenInCall() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_RETAIN_RETRY_INDEX_WHEN_INCALL);
    }


    public static boolean isNeedExitComposerAfterForward() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.EXIT_COMPOSER_AFTER_FORWARD);
    }

    public static Uri appendExtraQueryParameterForConversationDeleteAll(Uri uri) {
        return mMmsConfigPlugin.appendExtraQueryParameterForConversationDeleteAll(uri);
    }

    /// M: ALPS00527989, Extend TextView URL handling @ {
    public static void setExtendUrlSpan(TextView textView) {
        mMmsConfigPlugin.setExtendUrlSpan(textView);
    }
    /// @}
    public static boolean isSupportAutoSelectSubId() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SUPPORT_AUTO_SELECT_SIMID);
    }

    public static boolean isSupportAsyncUpdateWallpaper() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SUPPORT_ASYNC_UPDATE_WALLPAPER);
    }

    /// M: add for OP09 @{
    /**
     * M: set OP09 Device storage low status
     * @param low
     */
    public synchronized static void setCTDeviceStorageLowStatus(boolean low) {
        mCTDeviceStorageLow = low;
    }

    /**
     * M: get OP09 Device storage low status
     * @return
     */
    public static boolean getCTDeviceStorageLowStatus() {
        return mCTDeviceStorageLow;
    }

    public static boolean isSupportCBMessage(Context context, int simId) {
        return mMmsConfigPlugin.isSupportCBMessage(context, simId);
    }

    /**
     * the switcher for allow delivery report in roaming status.
     * @param context the Context.
     * @param subId the SubId.
     * @return true: allow to request delivery report. false: forbid request delivery report.
     */
    public static boolean isAllowDRWhenRoaming(Context context, long subId) {
        return mMmsConfigPlugin.isAllowDRWhenRoaming(context, subId);
    }
    /// @}

    /// M: ALPS00837193, query undelivered mms with non-permanent fail ones or not @{
    public static Uri getUndeliveryMmsQueryUri(Uri defaultUri) {
        return mMmsConfigPlugin.getUndeliveryMmsQueryUri(defaultUri);
    }
    /// @}

    /// M: New plugin API @{
    public static void openUrl(Context context, String url) {
        mMmsConfigPlugin.openUrl(context, url);
    }
    /// @}

    /// M: ALPS00956607, not show modify button on recipients editor @{
    public static void setRecipientsEditorOutAtts(EditorInfo outAttrs) {
        mMmsConfigPlugin.setRecipientsEditorOutAtts(outAttrs);
    }
    /// @}

    /*OP01 Features begin... */
    public static boolean isSupportMessagingNotificationProxy() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SUPPORT_MESSAGING_NOTIFICATION_PROXY);
    }

    /// M: CMCC Feature, for attachment enhance
    public static boolean isSupportAttachEnhance() {
        return mMmsFeatureManagerPlugin != null && mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ATTACH_ENHANCE);
    }


    /// M:OP09 Feature, for transaction failed notify.
    public static boolean isTransactionFailedNotifyEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_TRANSACTION_FAILED_NOTIFY);
    }

    /// M:OP09 Feature, for mms cc recipients.
    public static boolean isSupportSendMmsWithCc() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_CC_RECIPIENTS);
    }

    /// M: OP09Feature: the switch for cancel download feature is on or off.
    public static boolean isCancelDownloadEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_CANCEL_DOWNLOAD);
    }

    /// M: OP09Feature: the switch for MMS retry scheduler.
    public static boolean isMmsRetrySchedulerEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_RETRY_SCHEDULER);
    }

    /// M: OP09Feature: the switch for SMS priority.
    public static boolean isSmsPriorityEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SMS_PRIORITY);
    }

    /// M: OP09Feature: add the extended audio type;
    public static void addExtendedAudioType(ArrayList<String> audioType) {
        mMmsConfigPlugin.setExtendedAudioType(audioType);
    }

    /// M: OP09Feature: turn page after fling screen left or right;
    public static boolean enableTurnPageWithFlingScreen() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_PLAY_FILING_TURNPAGE);
    }
    /// M: CMCC Feature, for forward SMS append sender
    public static boolean isAppendSender() {
        return mMmsFeatureManagerPlugin != null && mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SMS_APPEND_SENDER);
    }

    public static boolean isSyncStartPdpEnabled() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_SYNC_START_PDP);
    }

    public static boolean isGminiMultiTransactionEnabled() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_GEMINI_MULTI_TRANSACTION);
    }

    public static boolean isSupportAddTopBottomSlide() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_ADD_TOP_BOTTOM_SLIDE);
    }
    /*OP01 Features end...*/


    /*OP03 Features begin... */

    // Support retrieve status error RETRIEVE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND check
    // to stop send the M-NotifyResp.ind to server for error case RETRIEVE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND.
    // return true: Do not send the Notify Response. false: Send the Notify Response ind to server
    public static boolean isEnableRetrieveStatusErrorCheck() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_ENABLE_RETRIEVE_STATUS_ERROR_CHECK);
    }

    // Is conversation split supported
    public static boolean isConversationSplitSupported() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.CONVERSATION_SPLIT_SUPPORTED);
    }

    // enable Sms Encoding Type.
    public static boolean isEnableSmsEncodingType() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.ENABLED_SMS_ENCODING_TYPE);
    }
    /*OP03 Features end... */


    /// M:OP09 Feature, for replace string.
    public static boolean isStringReplaceEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.STRING_REPLACE_MANAGEMENT);
    }

    /// M: OP09 Feature: for show dual time for received message item.
    public static boolean isShowDualTimeForMsgItemEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SHOW_DUAL_TIME_FOR_MESSAGE_ITEM);
    }

    /// M: OP09 Feature:  support tab setting for mms setting.
    public static boolean isSupportTabSetting() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_TAB_SETTING);
    }

    /// M: OP09Feature: show dual send button for compose.
    public static boolean isDualSendButtonEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_DUAL_SEND_BUTTON);
    }

    /// M: OP09Feature: the switcher for previewing VCard in MMS compose.
    public static boolean isSupportVCardPreview() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_VCARD_PREVIEW);
    }

    /// M: OP09Feature: the switcher for changing the lengthRequired MMS to SMS;
    public static boolean isChangeLengthRequiredMmsToSmsEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.CHANGE_LENGTH_REQUIRED_MMS_TO_SMS);
    }

    /// M: OP09Feature: the switcher for whether the mass text feature is on or not.
    public static boolean isMassTextEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MASS_TEXT_MSG);
    }

    /// M: OP09Feature: the switcher for multi compose
    public static boolean isMultiComposeEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_MULTI_COMPOSE);
    }

    /// M: OP09Feature: the switcher for indicate that the service deal the missed sms is on or off.
    public static boolean isMissedSmsReceiverEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_CANCEL_DOWNLOAD);
    }

    /// M: OP09Feature: the switcher for indicate whether the class zero model is show latest class_zero msg or not;
    public static boolean isClassZeroModelShowLatestEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.CLASS_ZERO_NEW_MODEL_SHOW_LATEST);
    }

    /// M:OP09Feature: the switcher for indicate whether the low memory notification feature is on or not.
    public static boolean isLowMemoryNotifEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_LOW_MEMORY);
    }

    /// M:OP09Feature: wake up screen which has the inserted handset when receive msg.
    public static boolean isWakeupScreenWithHeadsetForNewMsgEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.WAKE_UP_SCREEN_WHEN_RECEIVE_MSG);
    }

    /// M: OP09Feature: add time search condition.
    public static boolean isAdvanceSearchEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.ADVANCE_SEARCH_VIEW);
    }

    /// M: OP09Feature: disable delivery report in roaming.
    public static boolean isDeliveryReportInRoamingEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.DELIEVEEY_REPORT_IN_ROAMING);
    }

    /// M: OP09Feature: show number location.
    public static boolean isNumberLocationEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MMS_NUMBER_LOCATION);
    }

    /// M: OP09Feature: format data and time stamp.
    public static boolean isFormatDateAndTimeStampEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.FORMAT_DATE_AND_TIME);
    }

    /// M: OP09Feature: format notification content.
    public static boolean isFormatNotifContentEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.FORMAT_NOTIFICATION_CONTENT);
    }

    /// M: OP09Feature: read sms from dual model UIM;
    public static boolean isReadSmsFromDualModelUIMEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.READ_SMS_FROM_DUAL_MODEL_UIM);
    }

    /// M: OP09Feature: sent date is used to show and the received date is used to sort.
    public static boolean isShowDateManagementEnable() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SHOW_DATE_MANAGEMENT);
    }

    /// M:OP09Feature: more strict validation for sms addr.
    public static boolean isMoreStrictValidateForSmsAddr() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.MORE_STRICT_VALIDATION_FOR_SMS_ADDRESS);
    }

    /// M: OP09Feautre: show preview for recipient.
    public static boolean isShowPreviewForRecipient() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SHOW_PREVIEW_FOR_RECIPIENT);
    }

    /// M: OP09Feature: show dialog for new SI Msg;
    public static boolean isShowDialogForNewSIMsg() {
        return mMmsFeatureManagerPlugin.isFeatureEnabled(IMmsFeatureManagerExt.SHOW_DIALOG_FOR_NEW_SI_MSG);
    }

    /// M: OP09 Feature: For unsupported Files;
    public static boolean isUnsupportedFilesOn() {
        return mMmsFeatureManagerPlugin
                .isFeatureEnabled(IMmsFeatureManagerExt.MMS_UNSUPPORTED_FILES);
    }

    /// M: Add MmsService configure param @{
    public static Bundle getMmsServiceConfig() {
        // Operator Plugin
        return mMmsConfigPlugin.getMmsServiceConfig();
    }
    /// @}
    
    //add by lipeng for get default sms card for user
    /*public static int getDefaultSMSSlotId(){
    	int mSlotId = -1;
    	List<SimInfoRecord> simList = SimInfoManager.getInsertedSimInfoList(context);
    	//List<SimInfoManager.SimInfoRecord> simList = SimInfoManager.getInsertedSimInfoList(mContext);
    	if(simList!=null && simList.size()>1){
	     	int mMessageSimId = (int)Settings.System.getLong(mContext.getContentResolver(), Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
	        if (mMessageSimId == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK ||
	                (mMessageSimId == Settings.System.SMS_SIM_SETTING_AUTO)) {
	            // always ask, show SIM selection dialog
	        	mSlotId = 0;
	        } else if (mMessageSimId == Settings.System.DEFAULT_SIM_NOT_SET) {

	        	mSlotId = 0;
	        } else {
	        	mSlotId = SimInfoManager.getSlotById(mContext, mMessageSimId);
	        }
    	}
    	Log.d(TAG, "MMS getDefaultSMSSlotId : "+mSlotId);
        return mSlotId;
    }*/
    //end by lipeng
    
    
    
}
