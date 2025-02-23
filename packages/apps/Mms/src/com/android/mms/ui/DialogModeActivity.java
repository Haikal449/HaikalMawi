/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package com.android.mms.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AndroidException;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.ui.MsgContentSlideView.MsgContentSlideListener;
import com.android.mms.ui.MsgNumSlideview.MsgNumBarSlideListener;
import com.android.mms.ui.SubSelectDialog.SubClickAndDismissListener;
import com.android.mms.util.AddressUtils;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;

import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.mediatek.internal.telephony.CellConnMgr;
import com.android.mms.util.MessageResource;
import com.mediatek.common.MPlugin;
import com.android.mms.util.MmsContentType;
import com.android.mms.util.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.DefaultMmsCancelDownloadExt;
import com.mediatek.mms.ext.DefaultMmsComposeExt;
import com.mediatek.mms.ext.DefaultMmsMessageListItemExt;
import com.mediatek.mms.ext.IMmsAskIfDownloadExt;
import com.mediatek.mms.ext.IMmsAskIfDownloadHost;
import com.mediatek.mms.ext.IMmsCancelDownloadExt;
import com.mediatek.mms.ext.IMmsComposeExt;
import com.mediatek.mms.ext.IMmsDialogModeExt;
import com.mediatek.mms.ext.IMmsMessageListItemExt;
import com.mediatek.mms.ext.IMmsUtilsExt;
import com.mediatek.mms.ext.DefaultMmsDialogModeExt;
import com.mediatek.mms.ext.ViewOnClickListener;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.mms.transaction.MessagingNotification;



/// M: add for ipmessage
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.ProgressBar;

import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.DownloadAttachStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageSendMode;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageType;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpImageMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.message.IpTextMessage;
import com.mediatek.mms.ipmessage.message.IpVCalendarMessage;
import com.mediatek.mms.ipmessage.message.IpVCardMessage;
import com.mediatek.mms.ipmessage.message.IpVideoMessage;
import com.mediatek.mms.ipmessage.message.IpVoiceMessage;

import android.view.KeyEvent;
import android.content.res.Configuration;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import com.mediatek.internal.telephony.DefaultSmsSimSettings;
import java.util.Locale;
/** M:
 * Dialog mode
 */
public class DialogModeActivity extends Activity implements
    MsgNumBarSlideListener,
    MsgContentSlideListener,
    OnClickListener,
    SlideViewInterface,
    INotificationsListener,
    MessageStatusListener,
 IMmsAskIfDownloadHost, SubClickAndDismissListener {

    private int mCurUriIdx;
    private Uri mCurUri;
    //private MsgNumSlideview mMsgNumBar;
    private ImageButton mLeftArrow;
    private ImageButton mRightArrow;
    private TextView mMsgNumText;
    private TextView mSender;
    //private MsgContentSlideView mContentLayout;
    private TextView mSmsContentText;
    private TextView mRecvTime;
    private TextView mSubName;
    private ImageView mContactImage;
    private View mMmsView;
    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
    private View mMmsAttachView;
    private ImageView mMmsAttachImageView;
    private TextView mAttachName;
    private TextView mAttachSize;
    /// @}
    private ImageView mMmsImageView;
    private ImageButton mMmsPlayButton;
    private EditText mReplyEditor;
    private ImageButton mSendButton;
    private TextView mTextCounter;
    private Button mCloseBtn;
    private Button mDeleteBtn;
	private Button mDialBtn;//HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 
    /// M: add for OP09 to show SubIcon
    private ImageView mSubIcon;
    //private ImageButton mCloseBtn;
    private AsyncDialog mAsyncDialog;   // Used for background tasks.
    private Cursor mCursor;
    //private boolean mWaitingForSubActivity;
    private DialogModeReceiver mReceiver;
    private boolean mContentViewSet;
    private AlertDialog mSubSelectDialog;
    private int mAssociatedSubId;
    private int mSelectedSubId;
    private WorkingMessage mWorkingMessage;
    private boolean mSendingMessage;
    private boolean mWaitingForSendMessage;
    private int mSubCount;
    private int[] mSubIdList;
    private static Drawable sDefaultContactImage;

    private int mPage = MmsConfig.getSmsToMmsTextThreshold();

    private static final String TAG = "Mms/DialogMode";
    private static final int SMS_ID = 0;
    private static final int SMS_TID = 1;
    private static final int SMS_ADDR = 2;
    private static final int SMS_DATE = 3;
    private static final int SMS_READ = 4;
    private static final int SMS_BODY = 5;
    private static final int SMS_SUB = 6;
    private static final int SMS_TYPE = 7;
    private static final String TYPE_MMS = "mms";
    private static final String TYPE_SMS = "sms";
    private static final int REQUEST_CODE_ECM_EXIT_DIALOG = 107;
    private static final String EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String SELECT_TYPE = "Select_type";
    private static final int SIM_SELECT_FOR_SEND_MSG = 1;
    /// M: see this time out use place comment about it's usage.
    private static final int DRAW_OVER_TIME_OUT = 100;

    private static final String UNREAD_SELECTION = "(read=0 OR seen=0)";

    private static final int FLAG_HOMEKEY_DISPATCHED = 0X80000000;
    /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
    //private final ArrayList<Uri> mUris;
    private ArrayList<Uri> mUris;
    private ArrayList<String> mReplyContent;

    /// M: Memebers for OP09. @{
    private ImageButton mButtonSmall;
    private ImageButton mButtonBig;
    private int[] mButtonSlotIds;
    private int[] mButtonColors;
    private Drawable[] mButtonDrawable;
    private int mSendSubIdForOp09 = -1;
    private boolean mHasRegisterReceiver = false;
    private Button mMmsDownloadBtn;

    private IMmsDialogModeExt  mMmsDialogModePlugin;
    private IMmsUtilsExt mMmsUtilsPlugin;
    private IMmsAskIfDownloadExt mAskIfDownloadPlugin;

    private IMmsComposeExt mMmsComposePlugin;
    private IMmsCancelDownloadExt mMmsCancelDownloadPlugin;
    private IMmsMessageListItemExt mMmsMessageListItemPlugin;
    /// @}


    Runnable mResetMessageRunnable = new Runnable() {
        public void run() {
            MmsLog.d(TAG, "mResetMessageRunnable.run");
            resetMessage();
        }
    };

    Runnable mMessageSentRunnable = new Runnable() {
        public void run() {
            MmsLog.d(TAG, "mMessageSentRunnable.run");
            String body = getString(R.string.strOk);
            MmsLog.d(TAG, "string=" + body);
            Toast.makeText(getApplicationContext(), body, Toast.LENGTH_SHORT).show();
        }
    };

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            return;
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            MmsLog.d(TAG, "mTextEditorWatcher.onTextChanged");
            // mWorkingMessage.setText(s);
            updateSendButtonState();
            updateCounter(s, start, before, count);
            if (mReplyContent.size() != 0) {
                mReplyContent.set(mCurUriIdx, s.toString());
            }
        }

        public void afterTextChanged(Editable s) {
            return;
        }
    };

    /// M:
    private ArrayList<Uri> mReadedUris;
    /** M: this variable is used for a special issue.
     *  when use click mms's play button, MmsPlayerActivity will be started, and close this activity.
     *  but the invocation is async, so this activity's finish will be call first.
     *  when framework started MmsPlayerActivity it will check the invoking activity,
     *  because this activity is finishing, the new created activity will not put in front.
     *  so add this flag to control finish activity in onStop.
     */
    private boolean mNeedFinish = false;

    /// M: add for ipmessage
    private TextView mGroupSender;
    private final ArrayList<Uri> mIpMessageUris;
    private TextView mBodyTextView;
    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    private TextView mGroupMmsSender;
    private boolean mIsGroupMms;
    /// M: add for image and video
    private View mIpImageView; // ip_image
    private ImageView mImageContent; // image_content
    private View mIpImageSizeBg; // image_size_bg
    private ImageView mActionButton; // action_btn
    private TextView mContentSize; // content_size
    private ProgressBar mImageDownloadProgressBar; // image_downLoad_progress
    private View mCaptionSeparator; // caption_separator
    private TextView mCaption; // text_caption
    private ImageView mMediaPlayView;
    /// M: add for audio
    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info
    private ProgressBar mAudioDownloadProgressBar; // audio_downLoad_progress
    /// M: add for vCard
    private View mIpVCardView;
    private ImageView mVCardIcon;
    private TextView mVCardInfo;
    /// M: add for vcalendar
    private View mIpVCalendarView;
    private TextView mVCalendarInfo;
    private int[] mParams = new int[4];
    private boolean mReachMax = true;

    public DialogModeActivity() {
        mUris = new ArrayList<Uri>();
        mReplyContent = new ArrayList<String>();
        mReadedUris = new ArrayList<Uri>();
        mIpMessageUris = new ArrayList<Uri>();
        mCurUriIdx = 0;
        mCurUri = null;
        mCursor = null;
        mMmsView = null;
        mMmsImageView = null;
        mMmsPlayButton = null;
        //mCellMgr = null;
        mReceiver = null;
        mContentViewSet = false;
        mWorkingMessage = null;
        //mWaitingForSubActivity = false;
        /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
        mMmsAttachView = null;
        mMmsAttachImageView = null;
        mAttachName = null;
        mAttachSize = null;
        /// @}

        mAskIfDownloadPlugin = (IMmsAskIfDownloadExt) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_ASK_IF_DOWNLOAD);
        mAskIfDownloadPlugin.init(this);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /// M: do not finish activity when touch outside
        setFinishOnTouchOutside(false);
        //getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED,FLAG_HOMEKEY_DISPATCHED);

        MmsLog.d(TAG, "DialogModeActivity.onCreate");
        initPlugin(this);
        MmsLog.d(TAG, "at Home");
        if(getIntent() == null) {
            return;
        }
		try{
			addNewUri(getIntent());
		}catch(java.lang.NullPointerException e){
			e.printStackTrace();
		}
        if (loadCurMsg() != null) {
            initDialogView();
            setDialogView();
            resetMessage();
        }
        
        registerReceiver();
        /// M: add for ipmessage, notification listener
        IpMessageUtils.addIpMsgNotificationListeners(this, this);
        /// @}
    }

    @Override
    protected void onResume() {
        super.onResume();
        MmsLog.d(TAG, "DialogModeActivity.onResume()");
        /** M: mReplyEditor.getLineCount() is not right when it is invoked during onCreate.
         *  in tablet, rotate screen will restore text in EditText, and trigger onTextChanged,
         *  in the triggered onTextChanged the getLineCount() is still not right.
         *  even in onResume, getLineCount() is not ensured right.
         *  the function is right after the layout is drawn. But drawn over no callback.
         *  so the work around is to check the line count after a millseconds later.
         *  If the feature hide count when only one line is removed, this code can remove.
         */
        Handler tempHandler = new Handler();
        tempHandler.postDelayed(new Runnable() {
            public void run() {
                if (MmsConfig.isDualSendButtonEnable()) {
                    return;
                }
                if (mTextCounter != null && mReplyEditor != null && mReplyEditor.getLineCount() > 1) {
                    /// M: the content is set right in onTextChanged. It is just not visible.
                    mTextCounter.setVisibility(View.VISIBLE);
                }
            }
        }, DRAW_OVER_TIME_OUT);
    }

    private void registerReceiver() {
        MmsLog.d(TAG, "DialogModeActivity.registerReceiver");
        /// M: fix bug ALPS00517135, update sim state dynamically;
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mSimReceiver, intentFilter);
        mHasRegisterReceiver = true;
        if (mReceiver != null) {
            return;
        }
        mReceiver = new DialogModeReceiver();
        IntentFilter filter = new IntentFilter("com.android.mms.dialogmode.VIEWED");
        registerReceiver(mReceiver, filter);
    }

    private void unRegisterReceiver() {
        MmsLog.d(TAG, "DialogModeActivity.unRegisterReceiver");
        /// M: fix bug ALPS00517135, update sim state dynamically
        if (mHasRegisterReceiver) {
            mHasRegisterReceiver = false;
            unregisterReceiver(mSimReceiver);
            mSimReceiver = null;
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }
    @Override
    protected void onDestroy() {
        MmsLog.d(TAG, "DialogModeActivity.onDestroy");
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        unRegisterReceiver();
        IpMessageUtils.removeIpMsgNotificationListeners(this, this);
        /// M: update widget
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        MmsLog.d(TAG, "DialogModeActivity.onSaveInstanceState");
        PowerManager powerManager = (PowerManager) getSystemService(Service.POWER_SERVICE);
        if (!powerManager.isScreenOn()) {
            MmsLog.d(TAG, "onSaveInstanceState not ScreenOn");
            return;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        MmsLog.d(TAG, "DialogModeActivity.onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);
        String newString = intent.getStringExtra("com.android.mms.transaction.new_msg_uri");
        if (mCurUri != null && newString != null) {
            if (!newString.equals(mCurUri.toString())) {
                addNewUri(intent);
            } else {
                MmsLog.d(TAG,"DialogModeActivity.new Uri = current Uri , don't add NewUri ");
            }
        }
        loadCurMsg();
        initDialogView();
        setDialogView();
//        if (mCellMgr == null) {
//            mCellMgr = new CellConnMgr();
//            mCellMgr.register(getApplication());
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MmsLog.d(TAG, "DialogModeActivity.onActivityResult, requestCode=" + requestCode
            + ", resultCode=" + resultCode + ", data=" + data);
        if (resultCode != RESULT_OK) {
            MmsLog.d(TAG, "fail due to resultCode=" + resultCode);
            return;
        }

        if (requestCode == REQUEST_CODE_ECM_EXIT_DIALOG) {
                boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
                MmsLog.d(TAG, "REQUEST_CODE_ECM_EXIT_DIALOG, mode=" + outOfEmergencyMode);
                if (outOfEmergencyMode) {
                    sendMessage(false);
                }
        } else {
                MmsLog.d(TAG, "bail due to unknown requestCode=" + requestCode);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        MmsLog.d(TAG, "DialogModeActivity.startActivityForResult");
        super.startActivityForResult(intent, requestCode);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged is called");
        super.onConfigurationChanged(newConfig);

    }
    private AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }
    private void resetMessage() {
        MmsLog.d(TAG, "DialogModeActivity.resetMessage");

        // We have to remove the text change listener while the text editor gets cleared and
        // we subsequently turn the message back into SMS. When the listener is listening while
        // doing the clearing, it's fighting to update its counts and itself try and turn
        // the message one way or the other.
        mReplyEditor.removeTextChangedListener(mTextEditorWatcher);
        // Clear the text box.
        TextKeyListener.clear(mReplyEditor.getText());

        if (mWorkingMessage != null) {
            MmsLog.d(TAG, "clear working message");
            mWorkingMessage = null;
        }
        updateSendButtonState();
        mReplyEditor.addTextChangedListener(mTextEditorWatcher);
        //mReplyEditor.setText("");
        if (mReplyContent.size() != 0) {
            String content = mReplyContent.get(mCurUriIdx);
            mReplyEditor.setText(content);
		} else {
            mReplyEditor.setText("");
		}
        mSendingMessage = false;
    }
    public void onPreMessageSent() {
        MmsLog.d(TAG, "DialogModeActivity.onPreMessageSent");
        runOnUiThread(mResetMessageRunnable);
    }

    public void onMessageSent() {
        MmsLog.d(TAG, "DialogModeActivity.onMessageSent");
        mWaitingForSendMessage = false;
        runOnUiThread(mMessageSentRunnable);
    }

    public void onProtocolChanged(boolean mms, boolean needToast) {
        MmsLog.d(TAG, "DialogModeActivity.onProtocolChanged");
    }

    public void onAttachmentChanged() {
        MmsLog.d(TAG, "DialogModeActivity.onAttachmentChanged");
    }

    public void onPreMmsSent() {
        MmsLog.d(TAG, "DialogModeActivity.onPreMmsSent");
    }

    public void onMaxPendingMessagesReached() {
        MmsLog.d(TAG, "DialogModeActivity.onMaxPendingMessagesReached");
    }

    public void onAttachmentError(int error) {
        MmsLog.d(TAG, "DialogModeActivity.onAttachmentError");
    }

    private void addNewUri(Intent intent) {
        if (intent == null) {
            return;
        }
        String newString = intent.getStringExtra("com.android.mms.transaction.new_msg_uri");
        MmsLog.d(TAG, "DialogModeActivity.addNewUri, new uri=" + newString);

	Uri newUri = null;
	try {
		newUri = Uri.parse(newString);
	} catch (Exception e) {
		e.printStackTrace();
		return;
	}
if (newUri == null) {
            return;
        }	
        
        mUris.add(mUris.size(), newUri);
        mReplyContent.add(mReplyContent.size(), null);
        mCurUriIdx = mUris.size() - 1;
        MmsLog.d(TAG, "new index=" + mCurUriIdx);
        if (intent.getBooleanExtra("ipmessage", false)) {
            MmsLog.d(TAG, "receiver a ipmessage,uri:" + newUri.toString());
            mIpMessageUris.add(newUri);
        }
    }

    private void initDialogView() {
        MmsLog.d(TAG, "DialogModeActivity.initDialogView");
        if (mContentViewSet) {
            MmsLog.d(TAG, "have init");
            return;
        }
        setContentView(R.layout.msg_dlg_activity);
        mContentViewSet = true;
        updateSubInfoList();
        mLeftArrow = (ImageButton) findViewById(R.id.previous);
        mLeftArrow.setOnClickListener(this);
        mRightArrow = (ImageButton) findViewById(R.id.next);
        mRightArrow.setOnClickListener(this);
        mMsgNumText = (TextView) findViewById(R.id.msg_counter);
        mSender = (TextView) findViewById(R.id.recepient_name);
        Typeface tf = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        if (tf != null) {
            mSender.setTypeface(tf);
        }
        MsgContentSlideView contentLayout;
        contentLayout = (MsgContentSlideView) findViewById(R.id.content_scroll_view);
        contentLayout.registerFlingListener(this);
        mGroupSender = (TextView) findViewById(R.id.group_sender);
        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        mGroupMmsSender = (TextView) findViewById(R.id.group_mms_sender);
        mSmsContentText = (TextView) findViewById(R.id.msg_content);
        mBodyTextView = mSmsContentText;
        if (tf != null) {
            mSmsContentText.setTypeface(tf);
        }
        mRecvTime = (TextView) findViewById(R.id.msg_recv_timer);
        if (tf != null) {
            mRecvTime.setTypeface(tf);
        }

        LinearLayout simInfo = (LinearLayout) findViewById(R.id.sim_info_linear);
        simInfo.setVisibility(View.VISIBLE);

        mSubName = (TextView) findViewById(R.id.sub_name);
        mSubName.setVisibility(View.VISIBLE);
        if (tf != null) {
        	mSubName.setTypeface(tf);
        }
        mContactImage = (ImageView) findViewById(R.id.contact_img);
        sDefaultContactImage = getApplicationContext().getResources().getDrawable(
            R.drawable.ic_contact_picture);
        mReplyEditor = (EditText) findViewById(R.id.embedded_reply_text_editor);
        mReplyEditor.addTextChangedListener(mTextEditorWatcher);
        mReplyEditor
                .setFilters(new InputFilter[] {new TextLengthFilter(MmsConfig.getMaxTextLimit())});
        /// M: if ipmessage is enabled. show another hint
        if (MmsConfig.isServiceEnabled(this)) {
            mReplyEditor.setHint(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_type_to_compose_text));
        }
        mSendButton = (ImageButton) findViewById(R.id.reply_send_button);
         //add by quanwei for HQ01625403 begin
        String language = Locale.getDefault().getLanguage();
        if(language.equals("fa")){
        	mSendButton.setImageResource(R.drawable.send_btn_drawable_fa);
        }
        //add by quanwei for HQ01625403 end
        mSendButton.setOnClickListener(this);
        mTextCounter = (TextView) findViewById(R.id.text_counter);
        mCloseBtn = (Button) findViewById(R.id.mark_as_read_btn);
        mCloseBtn.setOnClickListener(this);
        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDeleteBtn.setOnClickListener(this);
		/*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 begin*/
        mDialBtn = (Button) findViewById(R.id.dial_number_btn);
		if(mDialBtn!=null){
        	mDialBtn.setOnClickListener(this);
		}
		/*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 end*/
        //mReplyEditor.setText("");
        MmsLog.d(TAG, "mCurUriIdx" + mCurUriIdx);
        if (mReplyContent.size() != 0) {
            String content = mReplyContent.get(mCurUriIdx);
            mReplyEditor.setText(content);
            MmsLog.d(TAG, "content" + content);
        }
        /// M: add for ipmessage
        /// M: add for image and video
        mIpImageView = (View) findViewById(R.id.ip_image);
        mImageContent = (ImageView) findViewById(R.id.image_content);
        mIpImageSizeBg = (View) findViewById(R.id.image_size_bg);
        mActionButton = (ImageView) findViewById(R.id.action_btn);
        mContentSize = (TextView) findViewById(R.id.content_size);
        mImageDownloadProgressBar = (ProgressBar) findViewById(R.id.image_downLoad_progress);
        mCaptionSeparator = (View) findViewById(R.id.caption_separator);
        mCaption = (TextView) findViewById(R.id.text_caption);
        mMediaPlayView = (ImageView) findViewById(R.id.video_media_play);
        /// M: add for audio
        mIpAudioView = (View) findViewById(R.id.ip_audio);
        mAudioIcon = (ImageView) findViewById(R.id.ip_audio_icon);
        mAudioInfo = (TextView) findViewById(R.id.audio_info);
        mAudioDownloadProgressBar = (ProgressBar) findViewById(R.id.audio_downLoad_progress);
        /// M: add for vCard
        mIpVCardView = (View) findViewById(R.id.ip_vcard);
        mVCardIcon = (ImageView) findViewById(R.id.ip_vcard_icon);
        mVCardInfo = (TextView) findViewById(R.id.vcard_info);
        /// M: add for vCalendar
        mIpVCalendarView = (View) findViewById(R.id.ip_vcalendar);
        mVCalendarInfo = (TextView) findViewById(R.id.vcalendar_info);

        /// M: For OP09
//        mMmsDialogModePlugin.initCTSendButton();
        mMmsComposePlugin.initDualSendBtnForDialogMode(this, (LinearLayout) this.findViewById(R.id.button_and_counter),
            (TextView) findViewById(R.id.ct_text_dlg_counter));
    }
    private void setDialogView() {
        MmsLog.d(TAG, "DialogModeActivity.setDialogView");
        // Msg count bar
        int msgNum = mUris.size();
        String langage = Locale.getDefault().getLanguage();//add by lipeng for number display
        MmsLog.d(TAG, "msgNum" + msgNum);
        if (msgNum <= 1) {
            mLeftArrow.setVisibility(View.INVISIBLE);
            mRightArrow.setVisibility(View.INVISIBLE);
            mMsgNumText.setVisibility(View.INVISIBLE);
        } else {
            mLeftArrow.setVisibility(View.VISIBLE);
            mRightArrow.setVisibility(View.VISIBLE);
            mMsgNumText.setVisibility(View.VISIBLE);
            StringBuilder msgNumStrBuilder = new StringBuilder("");
            msgNumStrBuilder.append(mCurUriIdx + 1);
            msgNumStrBuilder.append('/');
            msgNumStrBuilder.append(msgNum);
            String msgNumStr = msgNumStrBuilder.toString();
            MmsLog.d(TAG, "msgNumStr" + msgNumStr);
            mMsgNumText.setText(msgNumStr);
        }
        if (mReplyContent.size() != 0) {
            String content = mReplyContent.get(mCurUriIdx);
            mReplyEditor.setText(content);
        }
        if(MessageUtils.isInteger(getSenderString())&&(langage.startsWith("ar")||langage.startsWith("fa")||langage.startsWith("iw"))){
               mSender.setText('\u202D' + getSenderString() + '\u202C');
       }else{
               mSender.setText(getSenderString());             
       }
        mSmsContentText.setText(getSmsContent());
        mRecvTime.setText(getReceivedTime());
        /// M: For OP09 DualTime Feature;@{
        if (MmsConfig.isShowDualTimeForMsgItemEnable()) {
            mRecvTime.setText(getSentTime());
            mMmsMessageListItemPlugin.setDualTime(getApplicationContext(), true, getCurrentSubId(),
                (TextView) findViewById(R.id.send_time_txt),
                (LinearLayout) findViewById(R.id.double_time_layout), getReceivedTime());
        }
        /// @}
        /// M: add for op09 to change the recvtime paddingtop
        mMmsDialogModePlugin.setRecvtimerViewToFitBigIcon(mRecvTime);
        // Add for joyn message
        String subStatus = IpMessageUtils.getResourceManager(getApplicationContext()).getSimStatus();
//        CharSequence simCharSequence = MessageUtils.getSubInfo(this, getCurrentSubId());
//        String simStatus = IpMessageUtils.getResourceManager(this).getSimStatus();
//        if (simStatus != null) {
//            simStatus = "   " + simStatus + "  ";
//            ((SpannableStringBuilder) simCharSequence).replace(0, simCharSequence.length(), simStatus);
//        }
//      mSimName.setText(simCharSequence);
        if(null != subStatus){
        	setSubIconAndLabel(subStatus);
        }else{
        	setSubIconAndLabel(null);
        }
        /// M: For OP09
		//HQ_zhangjing 2015-10-23 modified for CQ HQ01456261
        setSimTypeDrawable(this, getCurrentSubId(), (ImageView) findViewById(
        R.id.sub_icon), mSubName);
        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        if (mIsGroupMms) {
            mContactImage.setImageDrawable(sDefaultContactImage);
        } else {
            Drawable image = getContactImage();
            if (image != null) {
                mContactImage.setImageDrawable(image);
            }
        }
        // add for ipmessage
        if (isCurIpMessage()) {
            showIpMessage();
            // change send button to blue
            updateSendButtonState();
            return;
        } else {
            // hide ipmessage views and show sms view.
            mGroupSender.setVisibility(View.GONE);
            mIpImageView.setVisibility(View.GONE);
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
            /// M: add for ipmessage, hide audio or vcard view
            mIpAudioView.setVisibility(View.GONE);
            mIpVCardView.setVisibility(View.GONE);
            mIpVCalendarView.setVisibility(View.GONE);
            mBodyTextView.setVisibility(View.VISIBLE);
            updateSendButtonState();
        }
        if (isCurSMS()) {
            if (mMmsView != null) {
                MmsLog.d(TAG, "Hide MMS views");
                mMmsView.setVisibility(View.GONE);
            }
            if (mMmsAttachView != null) {
                MmsLog.d(TAG, "Hide MMS vcard or vcalendar views");
                mMmsAttachView.setVisibility(View.GONE);
            }
            if (mMmsDownloadBtn != null) {
                mMmsDownloadBtn.setVisibility(View.GONE);
            }
        } else {
           MmsLog.d(TAG, "a MMS");
           loadMmsView();
        }
    }
    private void setSubIconAndLabel(String subName) {
    	MmsLog.d(TAG,"enter setSubIconAndLabel sunId="+getCurrentSubId());
        SubscriptionInfo subInfo = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfo(getCurrentSubId());
    	 MmsLog.d(TAG,"subInfo="+subInfo);
    	 if(null != subInfo){
             mSubName.setTextColor(subInfo.getIconTint());
    		 if(null != subName){
    			mSubName.setText(subName);
   	         }else{
                mSubName.setText(subInfo.getDisplayName().toString());
   	         }
    	 }else{
    		 MmsLog.d(TAG,"subInfo is null ");
    		 mSubName.setVisibility(View.GONE);
            // if(null != subName){
            // mSubName.setText(subName);
            // }
    	 }
    }
    public void onSlideToPrev() {
        int msgNum = mUris.size();
        MmsLog.d(TAG, "DialogModeActivity.onSlideToPrev, msgNum=" + msgNum);
        MmsLog.d(TAG, "DialogModeActivity.onSlideToPrev, mCurUriIdx=" + mCurUriIdx);
        if (msgNum <= 1) {
            return;
        }
        if (mCurUriIdx == 0) {
            return;
        }
        if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
            MmsLog.d(TAG, "DialogModeActivity.onSlideToPrev, mCurUri=" + mCurUri.toString());
            mReadedUris.add(mCurUri);
        }
        mCurUriIdx--;
        loadCurMsg();
        setDialogView();
    }

    public void onSlideToNext() {
        int msgNum = mUris.size();
        MmsLog.d(TAG, "DialogModeActivity.onSlideToNext, msgNum=" + msgNum);
        MmsLog.d(TAG, "DialogModeActivity.onSlideToNext, mCurUriIdx=" + mCurUriIdx);
        if (msgNum <= 1) {
            return;
        }
        if (mCurUriIdx == (msgNum - 1)) {
            return;
        }
        if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
            MmsLog.d(TAG, "DialogModeActivity.onSlideToNext, mCurUri=" + mCurUri.toString());
            mReadedUris.add(mCurUri);
        }
        mCurUriIdx++;
        loadCurMsg();
        setDialogView();
    }

    private boolean isCurSMS() {
        MmsLog.d(TAG, "DialogModeActivity.isCurSMS");
        if (judgeParameterIsOk() == false) {
            return false;
        }
        mCurUri = (Uri) mUris.get(mCurUriIdx);
        String type = mCurUri.getAuthority();
        MmsLog.d(TAG, "type=" + type);
        if (type.equals(TYPE_SMS)) {
            return true;
        }
        return false;
    }

    private Cursor loadCurMsg() {
        MmsLog.d(TAG, "DialogModeActivity.loadCurMsg, idx=" + mCurUriIdx);
        if (judgeParameterIsOk() == false) {
            return null;
        }
        mCurUri = (Uri) mUris.get(mCurUriIdx);
        MmsLog.d(TAG, "uri=" + mCurUri.toString());
        String projection[];
        if (isCurSMS()) {
            projection = new String[] {"_id", "thread_id", "address", "date", "read", "body",
                "sub_id", "date_sent"};
        } else {
            projection = new String[] {"_id", "thread_id", "null as address", "date", "read",
                "sub", "sub_id", "m_type", "date_sent"};
        }
        if (mCursor != null) {
            mCursor.close();
        }
        Cursor cursor = getContentResolver().query(mCurUri, projection, UNREAD_SELECTION, null,
                null);
        if (cursor == null) {
            MmsLog.d(TAG, "no msg found");
            mCursor = null;
            // finish();
            return null;
        }
        /// M: this is a invalid uri, load the next.
        if (cursor.getCount() == 0) {
            cursor.close();
            mUris.remove(mCurUriIdx);
            mReplyContent.remove(mCurUriIdx);
            if (mUris.size() > 0) {
                return loadCurMsg();
            }
            // finish();
            return null;
        }

        mCursor = cursor;
        return cursor;
    }
    private boolean judgeParameterIsOk() {
        if (mUris.size() <= 0 || mCurUriIdx >= mUris.size() || (Uri) mUris.get(mCurUriIdx) == null) {
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = null;
            mCurUri = null;
            mCurUriIdx = 0;
            finish();
            return false;
        } else {
            return true;
        }
    }
    private void removeCurMsg() {
        MmsLog.d(TAG, "DialogModeActivity.removeCurMsg");
        if (judgeParameterIsOk() == false) {
            return;
        }
        mCurUri = (Uri) mUris.get(mCurUriIdx);
        MmsLog.d(TAG, "uri=" + mCurUri.toString());
        MmsLog.d(TAG, "mCurUriIdx=" + mCurUriIdx);
        mUris.remove(mCurUriIdx);
        mReplyContent.remove(mCurUriIdx);
        if (mCurUriIdx != 0) {
            mCurUriIdx--;
        }
        if (mUris.isEmpty()) {
            MmsLog.d(TAG, "no msg");
            finish();
            return;
        }
        loadCurMsg();
        setDialogView();
    }
    private String getSenderString() {
        MmsLog.d(TAG, "DialogModeActivity.getSenderString");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return "";
        }
        if (mCursor.moveToFirst()) {
            if (isCurSMS()) {
                if (isCurGroupIpMessage()) {
                    return getCurGroupIpMessageName();
                }
                // Add for joyn
                if (isCurJoynConvergedIpMessage()) {
                    return getCurJoynIpMessageName();
                }
                String recipientIds = mCursor.getString(SMS_ADDR);
                ContactList recipients;
                recipients = ContactList.getByNumbers(recipientIds, false, true);
                // MmsLog.d(TAG, "recipients=" + recipientIds);
                if (Build.TYPE.equals("eng")) {
                    MmsLog.d(TAG, "recipients=" + recipients.formatNames(", "));
                }
                return recipients.formatNames(", ");
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return "";
                }
                ContactList recipients = conv.getRecipients();
                /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
                mIsGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(DialogModeActivity.this)
                                            && recipients.size() > 1;
                if (Build.TYPE.equals("eng")) {
                    MmsLog.d(TAG, "recipients=" + recipients.formatNames(", "));
                }
                return recipients.formatNames(", ");
            }
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getSenderNumber() {
        MmsLog.d(TAG, "DialogModeActivity.getSenderNumber");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return "";
        }
        if (mCursor.moveToFirst()) {
            if (isCurSMS()) {
                if (isCurGroupIpMessage()) {
                    return getCurGroupIpMessageNumber();
                }
                String addr = mCursor.getString(SMS_ADDR);
                MmsLog.d(TAG, "addr=" + addr);
                return addr;
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return "";
                }
                ContactList recipients = conv.getRecipients();
                String[] numbers = recipients.getNumbers();

                if (numbers != null && numbers.length != 0) {
                    MmsLog.d(TAG, "number0=" + numbers[0]);
                    return numbers[0];
                } else {
                    MmsLog.d(TAG, "empty number");
                    return "";
                }
            }
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getSmsContent() {
        MmsLog.d(TAG, "DialogModeActivity.getSmsContent");
        String langage = Locale.getDefault().getLanguage();//add by lipeng for number display
        if (!isCurSMS() || mCursor == null) {
            return "";
        }
        if (mCursor.moveToFirst()) {
            String content = mCursor.getString(SMS_BODY);
            if (Build.TYPE.equals("eng")) {
                MmsLog.d(TAG, "content=" + content);
            }
			// add by lipeng for number display
			if (content != null && MessageUtils.isInteger(content)&&(langage.startsWith("ar")||langage.startsWith("fa")||langage.startsWith("iw"))) {
				content = "\u202D" + content + "\u202C";
			}
			// end by lipeng
            return content;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getReceivedTime() {
        MmsLog.d(TAG, "DialogModeActivity.getReceivedTime");
        StringBuilder builder = new StringBuilder("");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return builder.toString();
        }
        if (mCursor.moveToFirst()) {
            long date = mCursor.getLong(SMS_DATE);
            String strDate;
            if (isCurSMS()) {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date);
            } else {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date * 1000L);
            }
            MmsLog.d(TAG, "date=" + strDate);
            builder.append(strDate);
            return builder.toString();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return builder.toString();
        }
    }

    private String getSentTime() {
        MmsLog.d(TAG, "DialogModeActivity.getReceivedTime");
        StringBuilder builder = new StringBuilder("");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return builder.toString();
        }
        if (mCursor.moveToFirst()) {
            long date = mCursor.getLong(mCursor.getColumnCount() - 1);
            String strDate;
            if (isCurSMS()) {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date);
            } else {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date * 1000L);
            }
            MmsLog.d(TAG, "sentDate=" + strDate);
            builder.append(strDate);
            return builder.toString();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return builder.toString();
        }
    }

    private int getCurrentSubId() {
        int subId = -1;
        if (mCursor == null) {
            return subId;
        }
        if (mCursor.moveToFirst()) {
            subId = mCursor.getInt(SMS_SUB);
        }
        MmsLog.d(TAG, "getCurrentSubId:" + subId);
        return subId;
    }

    private String getSIMName() {
        MmsLog.d(TAG, "DialogModeActivity.getSIMName");
        StringBuilder builder = new StringBuilder("");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return builder.toString();
        }
        if (mCursor.moveToFirst()) {
            int subId = mCursor.getInt(SMS_SUB);
            MmsLog.d(TAG, "subId=" + subId);
            SubscriptionInfo subInfo = SubscriptionManager.from(MmsApp.getApplication())
                    .getActiveSubscriptionInfo(subId);
            builder.append(subInfo.getDisplayName().toString());
            return builder.toString();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return builder.toString();
        }
    }

    private int getSIMColor() {
        MmsLog.d(TAG, "DialogModeActivity.getSIMColor");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return MessageResource.drawable.sim_background_locked;
        }
        if (mCursor.moveToFirst()) {
            int subId = mCursor.getInt(SMS_SUB);
            MmsLog.d(TAG, "subId =" + subId);
            SubscriptionInfo subInfo = SubscriptionManager.from(MmsApp.getApplication())
                    .getActiveSubscriptionInfo(subId);
            //MmsLog.d(TAG, "color=" + subInfo.getSimBackgroundLightRes());
            //MmsLog.d(TAG, "color=" + subInfo.getColor());
            //return subInfo.getSimBackgroundLightRes();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            //return EncapsulatedR.drawable.sim_background_locked;
        }
        return 1;
    }

    /// M: add for ipmessage,fix bug ALPS01608034 @{
    private Handler contactImageHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what > 0) {
                ContactList recipients = getConversation().getRecipients();
                if (recipients == null || recipients.size() == 0) {
                    return;
                }
                Contact contact = recipients.get(0);
                if (contact == null) {
                    return;
                }
                Drawable image = contact.getGroupAvatar();
                if (image != null && mContactImage != null) {
                    mContactImage.setImageDrawable(image);
                }
            }
        }
    };
    /// @}

    private Drawable getContactImage() {
        MmsLog.d(TAG, "DialogModeActivity.getContactImage");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return sDefaultContactImage;
        }
        if (mCursor.moveToFirst()) {
            ContactList recipients;
            boolean isGroup = false;
            if (isCurGroupIpMessage()) {
                isGroup = true;
                recipients = getConversation().getRecipients();
            } else if (isCurSMS()) {
                String recipientIds = mCursor.getString(SMS_ADDR);
                recipients = ContactList.getByNumbers(recipientIds, false, true);
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return sDefaultContactImage;
                }
                recipients = conv.getRecipients();
                if (recipients == null) {
                    return sDefaultContactImage;
                }
            }
            if (recipients == null || recipients.size() == 0) {
                return sDefaultContactImage;
            }
            Contact contact = recipients.get(0);
            if (contact == null) {
                MmsLog.d(TAG, "no contact");
                return sDefaultContactImage;
            }
            if (isGroup) {
                return contact.getGroupAvatar(contactImageHandler, getApplicationContext(), getThreadId());
            }
            return contact.getAvatar(getApplicationContext(), sDefaultContactImage, getThreadId());
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return sDefaultContactImage;
        }
    }

    private long getThreadId() {
        MmsLog.d(TAG, "DialogModeActivity.getThreadId");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return -1;
        }
        if (mCursor.moveToFirst()) {
            long tid = mCursor.getLong(SMS_TID);
            MmsLog.d(TAG, "tid=" + tid);
            return tid;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return -1;
        }
    }

    private Conversation getConversation() {
        MmsLog.d(TAG, "DialogModeActivity.getConversation");
        long tid = getThreadId();
        if (tid < 0) {
            MmsLog.d(TAG, "invalid tid");
            return null;
        }
        MmsLog.d(TAG, "tid=" + tid);
        Conversation conv = Conversation.get(this, tid, true); // new Conversation(this, tid, true);
        if (conv == null) {
            MmsLog.d(TAG, "conv null");
            return null;
        }
        return conv;
    }

    // Implements OnClickListener
    public void onClick(View v) {
        MmsLog.d(TAG, "DialogModeActivity.onClick");
        if (v == null) {
            return;
        }
        if (v.equals(mSmsContentText)) {
            MmsLog.d(TAG, "Clicent content view");
            openThread(getThreadId());
        } else if (v.equals(mMmsPlayButton)) { // PLay MMS
            MmsLog.d(TAG, "View this MMS");
            MessageUtils.viewMmsMessageAttachment(this, mCurUri, null, getAsyncDialog());
            if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
                mReadedUris.add(mCurUri);
            }
            markAsRead(mReadedUris);
            /// M: see this variable's note
            mNeedFinish = true;
        } else if (v.equals(mSendButton)) {
            MmsLog.d(TAG, "Send SMS");
            sendReplySms();
        } else if (v.equals(mCloseBtn)) {
            // change the mark as read button to close button.
            MmsLog.d(TAG, "mark readbtn");
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
                MmsLog.d(TAG, "mCurUri" + mCurUri.toString());
                mReadedUris.add(mCurUri);
            }
            finish();
        } else if (v == mDeleteBtn) {
            // change the delete button to view button.
            MmsLog.d(TAG, "view the message thread");
            markAsSeen(mReadedUris);
            if (isCurGroupIpMessage()) {
                /// M: open group chat
                openIpMsgThread(getThreadId());
            } else {
                openThread(getThreadId());
            }
        } else if (v == mLeftArrow) {
            onSlideToPrev();
        } else if (v == mRightArrow) {
            onSlideToNext();
        } else if (v == mMmsDownloadBtn) { // OP09 Feature
            MmsLog.d(TAG, "    mMmsDownloadBtn: onClick()");
            /// M: For OP09 @{
            if (MmsConfig.isCancelDownloadEnable()) {
                mMmsCancelDownloadPlugin.markStateExt(mCurUri,
                    IMmsCancelDownloadExt.STATE_DOWNLOADING);
            }
            /// @}
            final Context context = this;
            // Start the TransactionService to download.
            Intent intent = new Intent(context, TransactionService.class);
            intent.putExtra(TransactionBundle.URI, mCurUri.toString());
            intent.putExtra(TransactionBundle.TRANSACTION_TYPE, Transaction.RETRIEVE_TRANSACTION);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, getSimId());
            startService(intent);
            // Launch the specific ComposeMessageActivity.
            Intent clickIntent = ComposeMessageActivity.createIntent(context, getThreadId());
            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(clickIntent);
		/*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 begin*/
        }else if( v == mDialBtn ){
        	dialRecipient();
		/*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 end*/
        }
    }

    /*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 begin*/
    private void dialRecipient() {
		String number = getSenderNumber();
		if (number != null &&  number.length() != 0) {
            Intent dialIntent ;
			dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(dialIntent);
        }
    }
    /*HQ_zhangjing 2015-08-31 modified for CQ HQ01340274 begin*/

    private void openThread(long threadId) {
        MmsLog.d(TAG, "DialogModeActivity.openThread " + threadId);
        if (MmsConfig.getMmsDirMode()) {
            MmsLog.d(TAG, "go to inbox");
            Intent it = new Intent(this, FolderViewList.class);
            it.putExtra("floderview_key", FolderViewList.OPTION_INBOX);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(it);
        } else {
            if (threadId < 0) {
                return;
            }
            /// M: Fix CR : ALPS01011718 @{
            Intent intent = ComposeMessageActivity.createIntent(this, threadId);
            intent.putExtra("finish", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            /// @}
            startActivity(intent);
        }
        finish();
    }

    private void loadMmsView() {
        MmsLog.d(TAG, "DialogModeActivity.loadMmsView ");
        if (mMmsView == null) {
            MmsLog.d(TAG, "set Mms views visible");
            mMmsView = findViewById(R.id.msg_dlg_mms_view);
            mMmsImageView = (ImageView) findViewById(R.id.msg_dlg_image_view);
            mMmsPlayButton = (ImageButton) findViewById(R.id.msg_dlg_play_slideshow_button);
            mMmsPlayButton.setVisibility(View.VISIBLE);
        }
        mMmsImageView.setVisibility(View.GONE);
        if (mMmsAttachView == null) {
            mMmsAttachView = findViewById(R.id.vca_dlg_image_view);
            mMmsAttachImageView = (ImageView) findViewById(R.id.vca_image_view);
            mAttachName = (TextView) findViewById(R.id.file_attachment_name_info);
            mAttachSize = (TextView) findViewById(R.id.file_attachment_size_info);
        }
        loadMmsContents();
    }

    private void sendReplySms() {
        MmsLog.d(TAG, "DialogModeActivity.sendReplySms");
        /// M: if this is a group ipmessage, just reply with the card it from.
        if (isCurGroupIpMessage() || isCurJoynConvergedIpMessage()) {
            String body = mReplyEditor.getText().toString();
            int subId = mCursor.getInt(SMS_SUB);
            String to = getSenderNumber();
            MmsLog.d(TAG, "sendIpTextMessage[group], to:" + to + ",subId:" + subId + ",body:" + body);
            sendIpTextMessage(body, subId, to);
            mCurUri = (Uri) mUris.get(mCurUriIdx);
            markAsRead(mCurUri);
            return;
        } else {
            simSelection();
        }
    }

    private void simSelection() {
        MmsLog.d(TAG, "DialogModeActivity.simSelection");
        MmsLog.d(TAG, "mSelectedSubId = " + mSelectedSubId);
        if (mSendSubIdForOp09 > 0 && MmsConfig.isDualSendButtonEnable()) {
            mSelectedSubId = mSendSubIdForOp09;
            mSendSubIdForOp09 = -1;
            MmsLog.d(TAG, "send msg from mSendSubIdForOp09 = " + mSelectedSubId);
            confirmSendMessageIfNeeded();
        } else if (mSubCount == 1) {
            MmsLog.d(TAG, "mSubCount == 1");
            mSelectedSubId = mSubIdList[0];
            confirmSendMessageIfNeeded();
        } else {
            String number = getSenderNumber();
            if (number == null || number.length() == 0) {
                mAssociatedSubId = -1;
            } else {
                mAssociatedSubId = getContactSIM(number);
            }
            MmsLog.d(TAG, "mAssociatedSubId=" + mAssociatedSubId);
            int messageSubId = SubscriptionManager.getDefaultSmsSubId();
            MmsLog.d(TAG, "messageSimId=" + messageSubId);
            Intent intent = new Intent();
            intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SEND_MSG);
            if (messageSubId == DefaultSmsSimSettings.ASK_USER_SUB_ID) {
                // always ask, show SIM selection dialog
                showSubSelectedDialog(false, intent);
                updateSendButtonState();
            } else if (messageSubId == Settings.System.DEFAULT_SIM_NOT_SET) {
                if (mAssociatedSubId == -1) {
                    showSubSelectedDialog(false, intent);
                    updateSendButtonState();
                } else {
                    mSelectedSubId = mAssociatedSubId;
                    confirmSendMessageIfNeeded();
                }
            } else if (messageSubId == Settings.System.SMS_SIM_SETTING_AUTO
                    && MmsConfig.isSupportAutoSelectSubId()) {
                long currentSubId = getCurrentSubId();
                for (int subid : mSubIdList) {
                    if (subid == currentSubId) {
                        mSelectedSubId = subid;
                        break;
                    }
                }
                if (mSelectedSubId == currentSubId) {
                    confirmSendMessageIfNeeded();
                } else {
                    showSubSelectedDialog(false, intent);
                    updateSendButtonState();
                }
            } else {
                if (!MessageUtils.isSmsSubIdActive(this, messageSubId)) {
                    showSubSelectedDialog(false, intent);
                    updateSendButtonState();
                } else {
                    mSelectedSubId = messageSubId;
                    confirmSendMessageIfNeeded();
                }
            }
        }

        MmsLog.d(TAG, "mSelectedSubId = " + mSelectedSubId);
    }

    private int getContactSIM(String number) {
        MmsLog.d(TAG, "DialogModeActivity.getContactSIM, " + number);

        int subId = -1;
        String formatNumber = MessageUtils.formatNumber(number, this);
        String TrimFormatNumber = formatNumber;
        if (formatNumber != null) {
            TrimFormatNumber = formatNumber.replace(" ", "");
        }
        Cursor associateSIMCursor = getApplicationContext().getContentResolver().query(
                Data.CONTENT_URI,
                new String[] {
                    ContactsContract.Data.SIM_ASSOCIATION_ID
                },
                Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND ("
                        + Data.DATA1 + "=?" + " OR " + Data.DATA1 + "=?" + " OR " + Data.DATA4
                        + "=?" + ") AND (" + ContactsContract.Data.SIM_ASSOCIATION_ID
                        + "!= -1)", new String[] {
                        number, formatNumber, TrimFormatNumber
                }, null);
        try {
            if (null == associateSIMCursor) {
                MmsLog.i(TAG, " queryContactInfo : associateSIMCursor is null");
            } else {
                MmsLog.i(TAG, " queryContactInfo : associateSIMCursor is not null. Count["
                    + associateSIMCursor.getCount() + "]");
            }
            if ((null != associateSIMCursor) && (associateSIMCursor.getCount() > 0)) {
                associateSIMCursor.moveToFirst();
                // Get only one record is OK
                subId = (Integer) associateSIMCursor.getInt(0);
            } else {
                subId = -1;
            }
            MmsLog.d(TAG, "subId=" + subId);
        } finally {
            if (associateSIMCursor != null) {
                associateSIMCursor.close();
            }
        }
        return subId;
    }
    private void showSubSelectedDialog(boolean overridePref ,Intent intent) {
        SubSelectDialog subSelectDialog = new SubSelectDialog(this, this);
        mSubSelectDialog = subSelectDialog.showSubSelectedDialog(true, null, intent);
    }

    @Override
    public void onDialogClick(int subId, Intent intent) {
        mSelectedSubId = subId;
        updateSendButtonState();
        if (intent.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SEND_MSG) {
            confirmSendMessageIfNeeded();
        }
    }

    @Override
    public void onDialogDismiss() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onCancelClick() {
        // TODO Auto-generated method stub
    }

    private void confirmSendMessageIfNeeded() {
        MmsLog.d(TAG, "DialogModeActivity.confirmSendMessageIfNeeded");
        checkConditionsAndSendMessage(true);
    }

    private void checkConditionsAndSendMessage(final boolean bCheckEcmMode) {
        MmsLog.d(TAG, "DialogModeActivity.checkConditionsAndSendMessage");
        if (mSelectedSubId <= 0) {
            mSelectedSubId = SubscriptionManager.getDefaultSubId();
        }
        // add CellConnMgr feature
        final CellConnMgr cellConnMgr = new CellConnMgr(getApplicationContext());
        final int state = cellConnMgr.getCurrentState(mSelectedSubId, CellConnMgr.STATE_FLIGHT_MODE
            | CellConnMgr.STATE_SIM_LOCKED | CellConnMgr.STATE_RADIO_OFF);
        MmsLog.d(TAG,"CellConnMgr, state is " + state);
        if (((state & CellConnMgr.STATE_FLIGHT_MODE) == CellConnMgr.STATE_FLIGHT_MODE ) ||
            ((state & CellConnMgr.STATE_RADIO_OFF) == CellConnMgr.STATE_RADIO_OFF ) ||
            ((state & (CellConnMgr.STATE_FLIGHT_MODE | CellConnMgr.STATE_RADIO_OFF))  
                  == (CellConnMgr.STATE_FLIGHT_MODE | CellConnMgr.STATE_RADIO_OFF)))  {
            final ArrayList<String> stringArray = cellConnMgr.getStringUsingState(mSelectedSubId,
                    state);
            MmsLog.d(TAG, "CellConnMgr, stringArray length is " + stringArray.size());
            if (stringArray.size() == 4) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(stringArray.get(0));
                builder.setMessage(stringArray.get(1));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                updateSendButtonState();
                            }
                        });
                builder.show();
            }
        }  else if((state & CellConnMgr.STATE_SIM_LOCKED) == CellConnMgr.STATE_SIM_LOCKED ){
            final ArrayList<String> stringArray = cellConnMgr.getStringUsingState(mSelectedSubId, state);
            MmsLog.d(TAG,"CellConnMgr, stringArray length is " + stringArray.size());
            if(stringArray.size() == 4){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(stringArray.get(0));
                builder.setCancelable(true);
                builder.setMessage(stringArray.get(1));
                builder.setPositiveButton(stringArray.get(2), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                            cellConnMgr.handleRequest(mSelectedSubId, state);
                            updateSendButtonState();
                    }
                });
                builder.setNegativeButton(stringArray.get(3), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        updateSendButtonState();
                }
                });
                builder.show();
            }
        } else {
            sendMessage(bCheckEcmMode);
        }
    }

    private void sendMessage(boolean bCheckEcmMode) {
        MmsLog.d(TAG, "DialogModeActivity.sendMessage," + bCheckEcmMode);

        if (bCheckEcmMode) {
            MmsLog.d(TAG, "bCheckEcmMode=" + bCheckEcmMode);

            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    MmsLog.d(TAG, "show notice to block others");
                    startActivityForResult(new Intent(
                            TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    MmsLog.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }
        /*
         * ContactList contactList = isRecipientsEditorVisible() ? mRecipientsEditor.constructContactsFromInput(false) :
         * getRecipients(); mDebugRecipients = contactList.serialize();
         */
        //need check by xun isServiceEnabled
        /// M: add for ipmessage
//        if (isCurIpMessage() && MmsConfig.isServiceEnabled(this, mSelectedSubId)) {
//            String body = mReplyEditor.getText().toString();
//            long subId = mSelectedSubId;
//            String to = getSenderNumber();
//            MmsLog.d(TAG, "sendIpTextMessage, to:" + to + ",simId:" + subId + ",body:" + body);
//            sendIpTextMessage(body, subId, to);
//            mCurUri = (Uri) mUris.get(mCurUriIdx);
//            markAsRead(mCurUri);
//            return;
//        }

        if (!mSendingMessage) {
            /*
             * if (LogTag.SEVERE_WARNING) { String sendingRecipients = mConversation.getRecipients().serialize(); if
             * (!sendingRecipients.equals(mDebugRecipients)) { String workingRecipients =
             * mWorkingMessage.getWorkingRecipients(); if (!mDebugRecipients.equals(workingRecipients)) {
             * LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.sendMessage" + " recipients in window: \"" +
             * mDebugRecipients + "\" differ from recipients from conv: \"" + sendingRecipients +
             * "\" and working recipients: " + workingRecipients, this); } } sanityCheckConversation(); }
             */

            // send can change the recipients. Make sure we remove the listeners first and then add
            // them back once the recipient list has settled.
            // removeRecipientsListeners();
            MmsLog.d(TAG, "new working message");
            mWorkingMessage = WorkingMessage.createEmpty(this, this);
            // mWorkingMessage.setMessageStatusListener(this);
            mWorkingMessage.setConversation(getConversation());
            mWorkingMessage.setText(mReplyEditor.getText());
            MmsLog.d(TAG, "mSelectedSubId = " + mSelectedSubId);
            mWorkingMessage.send("", mSelectedSubId);
            mSendingMessage = true;
            mWaitingForSendMessage = true;
            mCurUri = (Uri) mUris.get(mCurUriIdx);
            markAsRead(mCurUri);
        }
    }

    private void loadMmsContents() {
        MmsLog.d(TAG, "DialogModeActivity.loadMmsContents");
        String langage = Locale.getDefault().getLanguage();//add by lipeng for number display
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return;
        }

        if (!mCursor.moveToFirst()) {
            MmsLog.d(TAG, "moveToFirst fail");
            return;
        }

        MmsLog.d(TAG, "cursor ok");
            // check msg type
            int type = mCursor.getInt(SMS_TYPE);
            MmsLog.d(TAG, "type=" + type);

            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == type) {
                MmsLog.d(TAG, "mms nofity");
                String content;
                content = getNotificationContentString(mCurUri);
			// add by lipeng for number display
			if (content != null && MessageUtils.isInteger(content)&&(langage.startsWith("ar")||langage.startsWith("fa")||langage.startsWith("iw"))) {
				content = "\u202D" + content + "\u202C";
			}
			// end by lipeng
                mSmsContentText.setText(content);
                if (mMmsAttachView != null) {
                    mMmsAttachView.setVisibility(View.GONE);
                }
                if (mMmsView != null) {
                    mMmsView.setVisibility(View.GONE);
                }
                /// M: For OP09 Feature. @{
                if (MmsConfig.isShowDualTimeForMsgItemEnable()) {
                    mRecvTime.setText("");
                    mMmsMessageListItemPlugin.hideDualTimePanel(
                        (TextView) findViewById(R.id.send_time_txt),
                        (LinearLayout) findViewById(R.id.double_time_layout));
                }
                mAskIfDownloadPlugin.showMmsDownloadBtn();
                /// @}
                return;
            /// M: For OP09 feature, hide the download button. @{
            } else {
                if (MmsConfig.isCancelDownloadEnable() && mMmsDownloadBtn != null) {
                    mMmsDownloadBtn.setVisibility(View.GONE);
                }
            /// @}
            }

            // get MMS pdu
            PduPersister p = PduPersister.getPduPersister(this);
            MultimediaMessagePdu msg;
            SlideshowModel slideshow = null;

            try {
                msg = (MultimediaMessagePdu) p.load(mCurUri);
            } catch (MmsException e) {
                MmsLog.d(TAG, e.toString());
                msg = null;
            }

            if (msg == null) {
                MmsLog.d(TAG, "msg null");
                return;
            }

            // get slideshow
        try {
            slideshow = SlideshowModel.createFromPduBody(this, msg.getBody());
        } catch (MmsException e) {
            slideshow = null;
            e.printStackTrace();
        }
            if (slideshow == null) {
                MmsLog.d(TAG, "loadMmsContents(); slideshow null");
            } else {
                MmsLog.d(TAG, "loadMmsContents(); slideshow ok");
            }

            // set Mms content text
            EncodedStringValue subObj = msg.getSubject();
            String subject = null;

            if (subObj != null) {
                subject = subObj.getString();
                MmsLog.d(TAG, "sub=" + subject);
            }

            SpannableStringBuilder buf = new SpannableStringBuilder();
            boolean hasSubject = false;

            // init set a empty string
            buf.append("");

            // add subject
            if ((subject != null) && (subject.length() > 0)) {
                hasSubject = true;
                buf.append(TextUtils.replace(getResources().getString(R.string.inline_subject),
                    new String[] {"%s"}, new CharSequence[] {subject}));
                buf.replace(0, buf.length(), buf);
            }

            MmsLog.d(TAG, "with sub=" + buf.toString());

        if (slideshow == null) {
            MmsLog.d(TAG, "slideshow null");
            mMmsView.setVisibility(View.GONE);
            mMmsAttachView.setVisibility(View.GONE);
            if (buf.length() == 0) {
                mSmsContentText.setText("        ");
            }
        } else {
                // append first text to content
                SlideModel slide = slideshow.get(0);
                String body;

                if ((slide != null) && slide.hasText()) {
                    TextModel tm = slide.getText();
                    body = tm.getText();
				// add lipeng for number display
				if (body != null && MessageUtils.isInteger(body)&&(langage.startsWith("ar")||langage.startsWith("fa")||langage.startsWith("iw"))) {
					body = "\u202D" + body + "\u202C";
				}
				// end by lipeng
                    MmsLog.d(TAG, "body=" + body);

                    if (hasSubject) {
                        buf.append(" - ");
                    }
                    buf.append(body);
                } else { // First slide no text
                    if (!hasSubject) {
                        buf.append("        ");
                    }
                }
                MmsLog.d(TAG, "with cont=" + buf.toString());
                mSmsContentText.setText(buf);
                mSmsContentText.setVisibility(View.VISIBLE);

                // Set Mms play button
                boolean needPresent = false;
                for (int i = 0; i < slideshow.size(); i++) {
                    MmsLog.d(TAG, "check slide" + i);
                    slide = slideshow.get(i);
                    if (slide.hasImage() || slide.hasVideo() || slide.hasAudio()) {
                        MmsLog.d(TAG, "found");
                        needPresent = true;
                        break;
                    }
                }

                if (!needPresent) {
                if (slideshow.size() > 1) {
                        needPresent = true;
                }
            }
                if (needPresent) {
                        MmsLog.d(TAG, "present slidehsow");
                        Presenter presenter = PresenterFactory.getPresenter(
                            "MmsThumbnailPresenter", this, this, slideshow);
                        presenter.present(null);
                        mMmsPlayButton.setOnClickListener(this);
                        mMmsAttachView.setVisibility(View.GONE);
                        setSmsContentTextView();
                } else {
                    MmsLog.d(TAG, "no media");
                    mMmsView.setVisibility(View.GONE);
                    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
                    setVcardOrVcalendar(slideshow);
                }
        }

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms @{
        if (mIsGroupMms) {
            mGroupMmsSender.setVisibility(View.VISIBLE);
            String name = interpretFrom(msg.getFrom(), mCurUri) + ":";
            mGroupMmsSender.setText(name);
        } else {
            mGroupMmsSender.setVisibility(View.GONE);
        }
        /// @}
    }

    private String getNotificationContentString(Uri uri) {
        MmsLog.d(TAG, "DialogModeActivity.getNotificationContentString");

        PduPersister p = PduPersister.getPduPersister(this);
        NotificationInd msg;

        try {
            msg = (NotificationInd) p.load(mCurUri);
            /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms @{
            if (mIsGroupMms) {
                mGroupMmsSender.setVisibility(View.VISIBLE);
                String name = interpretFrom(msg.getFrom(), mCurUri) + ":";
                mGroupMmsSender.setText(name);
            } else {
                mGroupMmsSender.setVisibility(View.GONE);
            }
            /// @}
        } catch (MmsException e) {
            MmsLog.d(TAG, e.toString());
            return "";
        }
        if (msg == null) {
            MmsLog.d(TAG, "msg null");
            return "";
        }

        String msgSizeText = this.getString(R.string.message_size_label)
            + String.valueOf((msg.getMessageSize() + 1023) / 1024)
            + this.getString(R.string.kilobyte);

        String timestamp = this.getString(R.string.expire_on, MessageUtils.formatTimeStampString(
            this, msg.getExpiry() * 1000L));

        String ret = msgSizeText + "\r\n" + timestamp;
        MmsLog.d(TAG, "ret=" + ret);

        /// M: add for OP09. @{
        if (MmsConfig.isFormatNotifContentEnable()) {
            ret = mMmsDialogModePlugin.getNotificationContentString(this.getString(R.string.from_label) + (getSenderNumber()), this
                    .getString(R.string.subject_label)
                + (msg.getSubject() != null ? msg.getSubject().getString() : ""), msgSizeText, timestamp);
        }
        /// @}

        return ret;
    }

    // SlideshowModel mSlideshow;
    private SlideshowModel getSlideShow() {
        MmsLog.d(TAG, "DialogModeActivity.getSlideShow ");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return null;
        }

        if (mCursor.moveToFirst()) {
            MmsLog.d(TAG, "cursor ok");

                PduPersister p = PduPersister.getPduPersister(this);
                int type = mCursor.getInt(SMS_TYPE);
                MmsLog.d(TAG, "type=" + type);

                if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == type) {
                    MmsLog.d(TAG, "mms nofity");
                    return null;
                }

                MultimediaMessagePdu msg;
                try {
                    msg = (MultimediaMessagePdu) p.load(mCurUri);
                } catch (MmsException e) {
                    MmsLog.d(TAG, e.toString());
                e.printStackTrace();
                    msg = null;
                }

                if (msg != null) {
                SlideshowModel slideshow;
                try {
                    slideshow = SlideshowModel
                            .createFromPduBody(this, msg.getBody());
                } catch (MmsException e) {
                    MmsLog.d(TAG, e.toString());
                    e.printStackTrace();
                    slideshow = null;
                }
                    if (slideshow == null) {
                        MmsLog.d(TAG, "getSlideShow(); slideshow null");
                    } else {
                        MmsLog.d(TAG, "getSlideShow(); slideshow ok");
                    }
                    return slideshow;
                }
                MmsLog.d(TAG, "msg null");

            return null;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return null;
        }
    }

    @Override
    public void startAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.startAudio");
    }

    @Override
    public void startVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.startVideo");
    }

    @Override
    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setAudio");
    }

    @Override
    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setTextVisibility");
    }

    @Override
    public void setText(String name, String text) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setText");
    }

    @Override
    public void setImage(String name, Bitmap bitmap) {
        MmsLog.d(TAG, "DialogModeActivity.setImage " + name);
        // inflateMmsView();

        try {
            Bitmap image = bitmap;
            if (null == image) {
                MmsLog.d(TAG, "bitmap null");
                image = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
            }
            MmsLog.d(TAG, "set bitmap to mMmsImageView");
            mMmsImageView.setImageBitmap(image);
            mMmsImageView.setVisibility(View.VISIBLE);
            mMmsView.setVisibility(View.VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage: out of memory:" + e.toString());
        }
    }

    @Override
    public void setImage(Uri mUri) {
        MmsLog.d(TAG, "DialogModeActivity.setImage(uri) ");
        try {
            Bitmap bitmap = null;
            if (null == mUri) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
            } else {
                //String mScheme = mUri.getScheme();
                InputStream mInputStream = null;
                try {
                    mInputStream = getApplicationContext().getContentResolver().openInputStream(
                        mUri);
                    if (mInputStream != null) {
                        bitmap = BitmapFactory.decodeStream(mInputStream);
                    }
                } catch (FileNotFoundException e) {
                    bitmap = null;
                } finally {
                    if (mInputStream != null) {
                        mInputStream.close();
                    }
                }
            }
                setImage("", bitmap);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage(Uri): out of memory: ", e);
        } catch (Exception e) {
            MmsLog.d(TAG, "setImage(uri) error." + e);
        }
    }

    @Override
    public void reset() {
        MmsLog.d(TAG, "DialogModeActivity.reset");

        if (mMmsView != null) {
            mMmsView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setVisibility");
        mMmsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void pauseAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.pauseAudio");

    }

    @Override
    public void pauseVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.pauseVideo");

    }

    @Override
    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.seekAudio");

    }

    @Override
    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.seekVideo");

    }

    @Override
    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setVideoVisibility");
    }

    @Override
    public void stopAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.stopAudio");
    }

    @Override
    public void stopVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.stopVideo");
    }

    @Override
    public void setVideo(String name, Uri video) {
        MmsLog.d(TAG, "DialogModeActivity.setVideo");
        // inflateMmsView();

        try {
            Bitmap bitmap = VideoAttachmentView.createVideoThumbnail(this, video);
            if (null == bitmap) {
                MmsLog.d(TAG, "bitmap null");
                bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_video);
            }
            MmsLog.d(TAG, "set bitmap to mMmsImageView");
            mMmsImageView.setImageBitmap(bitmap);
            mMmsImageView.setVisibility(View.VISIBLE);
            mMmsView.setVisibility(View.VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage: out of memory:" + e.toString());
        }
    }

    @Override
    public void setVideoThumbnail(String name, Bitmap bitmap) {
        MmsLog.d(TAG, "setVideoThumbnail");
    }

    @Override
    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setImageRegionFit");
    }

    @Override
    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setImageVisibility");
        mMmsView.setVisibility(View.VISIBLE);
    }

    @Override
    public int getWidth() {
        MmsLog.d(TAG, "DialogModeActivity.getWidth" + mMmsImageView.getWidth());
        return mMmsImageView.getWidth();
    }

    @Override
    public int getHeight() {
        MmsLog.d(TAG, "DialogModeActivity.getHeight" + mMmsImageView.getHeight());
        return mMmsImageView.getHeight();
    }

    private void updateSendButtonState() {
        boolean enable = false;
        int len = 0;
        if (mReplyEditor != null) {
            len = mReplyEditor.getText().toString().length();
        }
        MmsLog.d(TAG, "DialogModeActivity.updateSendButtonState(): len = " + len);

        if (mSendButton != null) {
            if (len > 0) {
                MmsLog.d(TAG, "updateSendButtonState(): mSubCount = " + mSubCount);

                /** M: MTK Encapsulation ITelephony */
                // ITelephony phone = ITelephony.Stub
                //        .asInterface(ServiceManager.checkService("phone"));
                ITelephony sTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                //TelephonyService phone = EncapsulatedTelephonyService.getInstance();
                if (sTelephony != null) {
                    if (isAnySimInsert()) { // check SIM state
                        enable = true;
                    }
                }
            }

            // View sendButton = showSmsOrMmsSendButton(mWorkingMessage.requiresMms());
            mSendButton.setEnabled(enable);
            mSendButton.setFocusable(enable);
            /// M: add for ipmessage
			/*HQ_zhangjing 2015-10-23 modified for CQ HQ01456261 begin*/
            /*if (enable) {
                if (isCurIpMessage()) {
                    mSendButton.setImageResource(R.drawable.ic_send_ipmsg);
                } else {
                    mSendButton.setImageResource(R.drawable.ic_send_ipmsg);
                }
            } else {
                mSendButton.setImageResource(R.drawable.ic_send_sms_unsend);
            }*/
			/*HQ_zhangjing 2015-10-23 modified for CQ HQ01456261 edn */
        }
        ///mMmsDialogModePlugin.updateCTSendButtonStatue(enable);
        mMmsComposePlugin.updateDualSendButtonStatue(enable, false);
    }

    private boolean isAnySimInsert() {
        MmsLog.d(TAG, "DialogModeActivity.isAnySimInsert,mSubCount=" + mSubCount);
        if (mSubCount > 0) {
            return true;
        }
        return false;
    }

       private void updateSubInfoList() {
           MmsLog.d(TAG, "updateSubInfoList mSubCount = " + mSubCount);
    	   mSubIdList = SubscriptionManager.from(this).getActiveSubscriptionIdList();
    	   if (mSubIdList != null ) {
               MmsLog.d(TAG, "updateSubInfoList is not null");
    		   mSubCount = mSubIdList.length;
    		   } else {
                   MmsLog.d(TAG, "updateSubInfoList is null");
    			   mSubCount = 0;
    		   }
    }

    private void updateCounter(CharSequence text, int start, int before, int count) {
        MmsLog.d(TAG, "DialogModeActivity.updateCounter");

        //int[] params = null;
        //params = SmsMessage.calculateLength(text, false);
        MmsLog.d(TAG, "mParams[0]" + mParams[0]);
        MmsLog.d(TAG, "mParams[1]" + mParams[1]);
        MmsLog.d(TAG, "mParams[2]" + mParams[2]);
        MmsLog.d(TAG, "mParams[3]" + mParams[3]);
        /*
         * SmsMessage.calculateLength returns an int[4] with: int[0] being the number of SMS's required, int[1] the
         * number of code units used, int[2] is the number of code units remaining until the next message. int[3] is the
         * encoding type that should be used for the message.
         */
        if (mReachMax == true) {
            mParams = SmsMessage.calculateLength(text, false);
            mReachMax = false;

        }
        int msgCount = mParams[0];

        int remainingInCurrentMessage = mParams[2];
        //int unitesUsed = params[1];

        // mWorkingMessage.setLengthRequiresMms(
        // msgCount >= MmsConfig.getSmsToMmsTextThreshold(), true);
        // Show the counter
        // Update the remaining characters and number of messages required.
        // if (mWorkingMessage.requiresMms()) {
        // mTextCounter.setVisibility(View.GONE);
        // } else {
        // mTextCounter.setVisibility(View.VISIBLE);
        // }
        if (MmsConfig.isDualSendButtonEnable()) {
            mMmsComposePlugin.updateNewTextCounter(mReplyEditor.getLineCount(), false, remainingInCurrentMessage,
                msgCount);
            return;
        }
        /// M: add in OP09 project for common feature. @{
        if (mReplyEditor.getLineCount() <= 1) {
            mTextCounter.setVisibility(View.INVISIBLE);
        } else {
            mTextCounter.setVisibility(View.VISIBLE);
        }
        /// @}
        String counterText = remainingInCurrentMessage + "/" + msgCount;
        MmsLog.d(TAG, "counterText=" + counterText);
        mTextCounter.setText(counterText);
        // m1
    }
    /**
    * This filter will constrain edits not to make the length of the text
    * greater than the specified length.
    */
    class TextLengthFilter extends InputFilter.LengthFilter {
        private final Toast mExceedMessageSizeToast;
        private final int mMaxLength;
        public TextLengthFilter(int max) {
            super(max);
            mMaxLength = max;
            mExceedMessageSizeToast = Toast.makeText(DialogModeActivity.this,
                R.string.exceed_message_size_limitation, Toast.LENGTH_SHORT);
        }
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
        int dstart, int dend) {
            /// M: re-compute max sms number count
            String text = "";
            String destString = dest.toString();
            String headString = destString.substring(0, dstart);
            if (headString != null) {
                text += headString;
            }
            String middleString = source.toString().substring(start, end);
            if (middleString != null) {
                text += middleString;
            }
            String tailString = destString.substring(dend);
            if (tailString != null) {
                text += tailString;
            }
            int page = mPage - 1;
            int maxLength = mMaxLength;
            // this function is for MO SMS

            MmsLog.d(TAG, "filter is called");
//            TextEncodingDetails ted = SmsManager.isImsSmsSupported() ?
//                com.android.internal.telephony.cdma.SmsMessage.calculateLength(text, false) :
//                com.android.internal.telephony.gsm.SmsMessage.calculateLength(text, false);
//                TextEncodingDetails ted = com.android.internal.telephony.gsm.SmsMessage.calculateLength(text, false);
            mParams  = SmsMessage.calculateLength(text, false);
            MmsLog.d(TAG, "mParams[0]" + mParams[0]);
            MmsLog.d(TAG, "mParams[1]" + mParams[1]);
            MmsLog.d(TAG, "mParams[2]" + mParams[2]);
            MmsLog.d(TAG, "mParams[3]" + mParams[3]);
            ArrayList<String> list = SmsMessage.fragmentText(text);
            if (list != null && list.size() > page) {
                maxLength = 0;
                for (int i = 0; i < page; i++) {
                    maxLength += list.get(i).length();
                }
                MmsLog.d(TAG, "get maxLength:" + maxLength);
            }
            MmsLog.d(TAG, "maxLength:" + maxLength);
            int keep = maxLength - (dest.length() - (dend - dstart));
            if (keep < (end - start)) {
                InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mExceedMessageSizeToast.show();
                if (mInputMethodManager != null) {
                    mInputMethodManager.restartInput(DialogModeActivity.this.getWindow().getCurrentFocus());
                }
                mReachMax = true;
            }
            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    --keep;
                    if (keep == start) {
                        return "";
                    }
                }
                return source.subSequence(start, keep);
            }
        }
    }
    private void markAsRead(final Uri uri) {
        MmsLog.d(TAG, "DialogModeActivity.markAsRead, " + uri.toString());

        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                    null, null);           
                MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                        MessagingNotification.THREAD_NONE, false,uri);
            }
        }).start();
        removeCurMsg();
    }

    private void markAsRead(final ArrayList<Uri> uris) {
        final Object[] uriArray = uris.toArray();
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                for (Object uriObject : uriArray) {
                    Uri uri = (Uri) uriObject;
                    MmsLog.d(TAG, "markasread a:" + uri.toString());
                    SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                            null, null);
                    MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                            MessagingNotification.THREAD_NONE, false,uri);
                }
        }}).start();
    }

    private void markAsSeen(final ArrayList<Uri> uris) {
        final Object[] uriArray = uris.toArray();
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("seen", 1);
                for (Object uriObject : uriArray) {
                    Uri uri = (Uri) uriObject;
                    MmsLog.d(TAG, "markasseen a:" + uri.toString());
                    SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                            null, null);
                }
                ///M: [ALPS01486266] [KK][MT6592][SGLTE][SMS][Must Resolve]after view new message,still show unread message notification
                ///because composeMessageActivity also update notification
                //MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                        //MessagingNotification.THREAD_NONE, false);
            }
        }).start();
    }

    public class DialogModeReceiver extends BroadcastReceiver {

        private static final String MSG_VIEWED_ACTION = "com.android.mms.dialogmode.VIEWED";

        public void onReceive(Context context, Intent intent) {
            MmsLog.d(TAG, "DialogModeActivity.DialogModeReceiver.onReceive");

            // TODO Auto-generated method stub
            if (intent != null) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                MmsLog.d(TAG, "action=" + action);
                DialogModeActivity.this.finish();
            }
        }
    }

    /// M: add for ipmessage
    private boolean isCurIpMessage() {
        boolean result = false;
        if (mUris.size() <= 0) {
            mCurUri = null;
            mCurUriIdx = 0;
            return false;
        }
        mCurUri = (Uri) mUris.get(mCurUriIdx);
        if (mCurUri != null) {
            result = mIpMessageUris.contains(mCurUri);
            MmsLog.d(TAG, "check uri:" + mCurUri.toString());
        }
        MmsLog.d(TAG, "result:" + result);
        return result;
    }

    private long getCurIpMessageId() {
        long id = 0;
        if (mUris.size() <= 0) {
            mCurUri = null;
            mCurUriIdx = 0;
            return id;
        }
        mCurUri = (Uri) mUris.get(mCurUriIdx);
        if (mCurUri != null) {
            id = Long.parseLong(mCurUri.getLastPathSegment());
        } else {
            MmsLog.w(TAG, "mCurUri is null!");
        }
        MmsLog.d(TAG, "id:" + id);
        return id;
    }

    private void showIpMessage() {
        if (mMmsView != null) {
            MmsLog.d(TAG, "Hide MMS views");
            mMmsView.setVisibility(View.GONE);
        }
        if (isCurGroupIpMessage() && mCursor != null) {
            String name = mCursor.getString(SMS_ADDR);
            MmsLog.d(TAG, "group sender address:" + name);
            name = IpMessageUtils.getContactManager(this).getNameByNumber(name) + ":";
            MmsLog.d(TAG, "group sender name:" + name);
            mGroupSender.setText(name);
            mGroupSender.setVisibility(View.VISIBLE);
        } else {
            mGroupSender.setVisibility(View.GONE);
        }
        long id = getCurIpMessageId();
        IpMessage ipMessage = IpMessageUtils.getMessageManager(this).getIpMsgInfo(id);
        MmsLog.d(TAG, "showIpMessage. id:" + id + ",type:" + ipMessage.getType());
        switch (ipMessage.getType()) {
        case IpMessageType.TEXT:
            setIpTextItem((IpTextMessage) ipMessage);
            break;
        case IpMessageType.PICTURE:
            setIpImageItem((IpImageMessage) ipMessage);
            break;
        case IpMessageType.VOICE:
            setIpVoiceItem((IpVoiceMessage) ipMessage);
            break;
        case IpMessageType.VCARD:
            setIpVCardItem((IpVCardMessage) ipMessage);
            break;
        case IpMessageType.VIDEO:
            setIpVideoItem((IpVideoMessage) ipMessage);
            break;
        case IpMessageType.CALENDAR:
            setIpVCalendarItem((IpVCalendarMessage) ipMessage);
            break;
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            MmsLog.w(TAG, "Unknown IP message type. type = " + ipMessage.getType());
            break;
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
            /// M: group chat type
            MmsLog.w(TAG, "Group IP message type. type = " + ipMessage.getType());
            break;
        default:
            MmsLog.w(TAG, "Error IP message type. type = " + ipMessage.getType());
            break;
        }
    }

    private void setIpTextItem(IpTextMessage textMessage) {
        MmsLog.d(TAG, "setIpTextItem()");
        String langage = Locale.getDefault().getLanguage();//add by lipeng for number display
        if (TextUtils.isEmpty(textMessage.getBody())) {
            MmsLog.w(TAG, "setIpTextItem(): No message content!");
            return;
        }

        mIpImageView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        CharSequence formattedMessage = formatMessage(textMessage.getBody());
        mIpImageView.setVisibility(View.GONE);
		// add by lipeng for number display
		if (MessageUtils.isInteger(formattedMessage + "")&&(langage.startsWith("ar")||langage.startsWith("fa")||langage.startsWith("iw"))) {
			formattedMessage = "\u202D" + formattedMessage + "\u202C";
		}// end by lipeng
        mBodyTextView.setText(formattedMessage);
        mBodyTextView.setVisibility(View.VISIBLE);
    }

    private CharSequence formatMessage(String body) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        buf.append(body);
        return buf;
    }

    private void updateIpMessageImageOrVideoView(IpAttachMessage message, long msgId) {
        mActionButton.setVisibility(View.VISIBLE);
        mActionButton.setImageResource(R.drawable.ipmsg_chat_download_selector);

        if (null != mImageDownloadProgressBar) {
            mImageDownloadProgressBar.setVisibility(View.GONE);
            mActionButton.setVisibility(View.GONE);
        }
        mContentSize.setVisibility(View.VISIBLE);
        mContentSize.setText(IpMessageUtils.formatFileSize(message.getSize()));
    }

    private void setIpImageItem(IpImageMessage imageMessage) {
        MmsLog.d(TAG, "setIpImageItem()");
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.INVISIBLE);
        final long msgId = getCurIpMessageId();

        if (!setPicView(imageMessage.getThumbPath())) {
            setPicView(imageMessage.getPath());
        }
        mIpImageSizeBg.setBackgroundDrawable(null);
        mActionButton.setVisibility(View.GONE);
        if (null != mImageDownloadProgressBar) {
            mImageDownloadProgressBar.setVisibility(View.GONE);
        }
        mActionButton.setClickable(false);
        mContentSize.setVisibility(View.GONE);

        if (TextUtils.isEmpty(imageMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            mCaption.setText(imageMessage.getCaption());
        }

        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
    }

    private void setIpVoiceItem(IpVoiceMessage voiceMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVoiceItem(): message Id = " + msgId);

        if (null != mAudioDownloadProgressBar) {
            mAudioDownloadProgressBar.setVisibility(View.GONE);
        }
        mAudioInfo.setVisibility(View.VISIBLE);
        mAudioInfo.setText(IpMessageUtils.formatAudioTime(voiceMessage.getDuration()));

        if (TextUtils.isEmpty(voiceMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            mCaption.setText(voiceMessage.getCaption());
        }

        /// M: add for ipmessage, show audio view
        mIpAudioView.setVisibility(View.VISIBLE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide image view or video view
        mIpImageView.setVisibility(View.GONE);
    }

    private void setIpVCardItem(IpVCardMessage vCardMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVCardItem(): message Id = " + msgId);
        String name = vCardMessage.getName();
        if (name != null && name.lastIndexOf(".") != -1) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        mVCardInfo.setText(name);
        mIpVCardView.setVisibility(View.VISIBLE);
        mIpAudioView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide image view or video view
        mIpImageView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
    }

    private void setIpVCalendarItem(IpVCalendarMessage vCalendarMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVCalendarItem(): message Id = " + msgId);
        String summary = vCalendarMessage.getSummary();
        if (summary != null && summary.lastIndexOf(".") != -1) {
            summary = summary.substring(0, summary.lastIndexOf("."));
        }
        mVCalendarInfo.setText(summary);
        mIpVCalendarView.setVisibility(View.VISIBLE);
        mBodyTextView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
    }

    private void setIpVideoItem(IpVideoMessage videoMessage) {
        final long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVideoItem(): message Id = " + msgId);
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);

        if (!setPicView(videoMessage.getThumbPath())) {
            setVideoView(videoMessage.getPath());
        }
        mIpImageSizeBg.setBackgroundDrawable(null);
        mActionButton.setVisibility(View.GONE);
        if (null != mImageDownloadProgressBar) {
            mImageDownloadProgressBar.setVisibility(View.GONE);
        }
        mActionButton.setClickable(false);
        mContentSize.setVisibility(View.GONE);

        if (TextUtils.isEmpty(videoMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            mCaption.setText(videoMessage.getCaption());
        }

        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
    }

    private boolean setPicView(String filePath) {
        MmsLog.d(TAG, "setPicView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        mIpImageSizeBg.setVisibility(View.GONE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        options.inJustDecodeBounds = false;

        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / 500);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(filePath, options);

        int mWidth = getResources().getDimensionPixelOffset(R.dimen.img_minwidth);
        MmsLog.d(TAG, "setPicView(): before set layout IpImageSizeBg.width = " + mIpImageSizeBg.getWidth());
        if (bitmap != null) {
            bitmap = IpMessageUtils.resizeImage(bitmap, mWidth, bitmap.getHeight() * mWidth / bitmap.getWidth(), true);
            mImageContent.setImageBitmap(bitmap);
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            mIpImageSizeBg.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setVideoView(String filePath) {
        MmsLog.d(TAG, "setVideoView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        mMediaPlayView.setVisibility(View.VISIBLE);
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MICRO_KIND);
        if (null != bitmap) {
            mImageContent.setImageBitmap(bitmap);
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            mIpImageSizeBg.setVisibility(View.GONE);
            return false;
        }
    }

    private void sendIpTextMessage(String body, int subId, String to) {
        final IpTextMessage ipMessage = new IpTextMessage();
        ipMessage.setBody(body);
        ipMessage.setType(IpMessageType.TEXT);
        //[yueqiang] check with xun
        //ipMessage.setSimId(subId);
        ipMessage.setTo(to);
        int sendModeType = IpMessageSendMode.AUTO;
        final int sendMode = sendModeType;
        onPreMessageSent();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MmsLog.d(TAG, "sendIpTextMessage(): calling API: saveIpMsg().");
                int ret = -1;
                ret = IpMessageUtils.getMessageManager(DialogModeActivity.this).saveIpMsg(ipMessage, sendMode);
                if (ret == -1) {
                    MmsLog.w(TAG, "sendIpTextMessage failed! ");
                } else {
                    // success.
                    onMessageSent();
                }
            }
        }).start();
    }

    private boolean isCurGroupIpMessage() {
        boolean result = false;
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String number = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + number);
                if (number != null && number.startsWith(IpMessageConsts.GROUP_START)) {
                    // this is group message
                    result = true;
                }
            }
        }
        MmsLog.d(TAG, "is group message:" + result);
        return result;
    }

    private boolean isCurJoynConvergedIpMessage() {
        boolean result = false;
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String number = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + number);
                if (number != null && number.startsWith(IpMessageConsts.JOYN_START)) {
                    // this is joyn converged message
                    result = true;
                }
            }
        }
        MmsLog.d(TAG, "is converaged message:" + result);
        return result;
    }

    private String getCurGroupIpMessageNumber() {
        String number = "";
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String num = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + num);
                if (num != null && num.startsWith(IpMessageConsts.GROUP_START)) {
                    // this is group message
                    number = num;
                }
            }
        }
        MmsLog.d(TAG, "group message number:" + number);
        return number;
    }

    private String getCurGroupIpMessageName() {
        String name = "";
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String nam = conv.getRecipients().get(0).getName();
                MmsLog.d(TAG, "name:" + nam);
                if (nam != null) {
                    // this is group message name
                    name = IpMessageUtils.getContactManager(this).getNameByThreadId(getThreadId());
                }
            }
        }
        MmsLog.d(TAG, "group message name:" + name);
        return name;
    }

    private String getCurJoynIpMessageName() {
        String name = "";
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String num = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + num);
                if (num != null) {
                    // this is joyn message name
                    name = IpMessageUtils.getContactManager(this).getNameByNumber(num);
                }
            }
        }
        MmsLog.d(TAG, "joyn message name:" + name);
        return name;
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(TAG, "DialogModeActivity, notificationReceived: intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (isFinishing()) {
            MmsLog.d(TAG, "activity is being destroied, ignore notification.");
            return;
        }
        MmsLog.d(TAG, "DialogModeActivity, action:" + action);
        if (!isCurIpMessage()) {
            MmsLog.d(TAG, "showing one is not ipmessage, ignore action.");
            return;
        }
        long msgId = 0L;
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION:
        case IpMessageUtils.IPMSG_IP_MESSAGE_STATUS_ACTION:
            try {
                if (IpMessageUtils.getActionTypeByAction(action) == IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION) {
                    msgId = intent.getLongExtra(DownloadAttachStatus.DOWNLOAD_MSG_ID, 0);
                } else {
                    msgId = intent.getLongExtra(IpMessageStatus.IP_MESSAGE_ID, 0);
                }
                if (getCurIpMessageId() != msgId) {
                    MmsLog.d(TAG, "current ipmessage is not this:" + msgId + ",current:" + getCurIpMessageId());
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showIpMessage(); //just refresh all element.
                    }
                });
            } catch (NullPointerException e) {
                // TODO: handle exception
                MmsLog.d(TAG, "catch a NullPointerExcpetion?");
            }
            break;
        default :
            MmsLog.d(TAG, "DialogModeActivity. ignore notification.");
            return;
        }
    }

    private void openIpMsgThread(final long threadId) {
        Intent intent = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
        intent.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
        IpMessageUtils.startRemoteActivity(this, intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MmsLog.d(TAG, "onStop()");
        if (mSubSelectDialog != null && mSubSelectDialog.isShowing()) {
            mSubSelectDialog.dismiss();
        }
        if (mNeedFinish) {
            mNeedFinish = false;
            finish();
        }

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
        case KeyEvent.KEYCODE_HOME:
            onClick(mCloseBtn);
            break;
        default:
            break;
    }
    return super.onKeyDown(keyCode, event);

}
    @Override
    public void onBackPressed() {
        /// M: take press back just as press close button
        onClick(mCloseBtn);
    }

    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
    private void setVcardOrVcalendar(SlideshowModel slideshow) {
        if (slideshow == null || slideshow.getAttachFiles() == null
                || slideshow.getAttachFiles().size() == 0) {
               if (mMmsAttachView != null) {
                    mMmsAttachView.setVisibility(View.GONE);
                }
                if (mMmsView != null) {
                    mMmsView.setVisibility(View.GONE);
                }
            return;
        }
        FileAttachmentModel attach = slideshow.getAttachFiles().get(0);
        String contentType = attach.getContentType();
        String src = attach.getSrc();
        long size = attach.getAttachSize();
        if (contentType.equalsIgnoreCase(MmsContentType.TEXT_VCARD)
                || contentType.equalsIgnoreCase(MmsContentType.TEXT_VCALENDAR)) {
            mMmsAttachView.setVisibility(View.VISIBLE);
            MmsLog.d(TAG, "set vcard or vcarlendar to mMmsImageView");
            String nameText = "";
            if (contentType.equalsIgnoreCase(MmsContentType.TEXT_VCARD)) {
                mMmsAttachImageView.setImageResource(R.drawable.ic_vcard_attach);
                nameText = getResources().getString(R.string.file_attachment_vcard_name, src);
            } else {
                mMmsAttachImageView.setImageResource(R.drawable.ic_vcalendar_attach);
                nameText = getResources().getString(R.string.file_attachment_vcalendar_name, src);
            }
            mAttachName.setText(nameText);
            mAttachSize.setText(MessageUtils.getHumanReadableSize(size));
            setSmsContentTextView();
        }
    }

    private void setSmsContentTextView() {
        if (mSmsContentText != null) {
            CharSequence contentString = mSmsContentText.getText();
            if (contentString == null || contentString.toString().trim().length() == 0) {
                mSmsContentText.setVisibility(View.GONE);
            }
        }
    }
    /// @}

    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    private String interpretFrom(EncodedStringValue from, Uri messageUri) {
        String address;
        if (from != null) {
            address = from.getString();
        } else {
            address = AddressUtils.getFrom(DialogModeActivity.this, messageUri);
        }
        String contact = TextUtils.isEmpty(address) ?
                            DialogModeActivity.this.getString(android.R.string.unknownName)
                            : Contact.get(address, false).getName();
        return contact;
    }

    /// M:For OP09
    private int getSimId() {
        int subId = 0;
        if (mCursor.moveToFirst()) {
            subId = mCursor.getInt(SMS_SUB);
            MmsLog.d(TAG, "subId=" + subId);
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
        }
        return subId;
    }

    // Add for OP09 Feature: Ask if download MMS immediately when not auto download.
    public void showMmsDownloadBtn() {
        mMmsDownloadBtn = (Button) findViewById(R.id.msg_dlg_mms_download_button);
        mMmsDownloadBtn.setVisibility(View.VISIBLE);
        mMmsDownloadBtn.setOnClickListener(this);
    }

    /// M: Init Plugin
    private void initPlugin(Context context) {
        mMmsDialogModePlugin = (IMmsDialogModeExt) MPlugin.createInstance(
                IMmsDialogModeExt.class.getName(),context);
        MmsLog.d(TAG, "operator mMmsDialogModePlugin = " + mMmsDialogModePlugin);
        if(mMmsDialogModePlugin == null){
            mMmsDialogModePlugin = new DefaultMmsDialogModeExt(context);
            MmsLog.d(TAG, "default mMmsDialogModePlugin = " + mMmsDialogModePlugin);
        }

         mMmsComposePlugin = (IMmsComposeExt) MPlugin.createInstance(
                IMmsComposeExt.class.getName(),context);
            MmsLog.d(TAG, "operator mMmsComposePlugin = " + mMmsComposePlugin);
        if(mMmsComposePlugin == null){
            mMmsComposePlugin = new DefaultMmsComposeExt(context);
            MmsLog.d(TAG, "default mMmsComposePlugin = " + mMmsComposePlugin);
        }

        mMmsCancelDownloadPlugin = (IMmsCancelDownloadExt) MPlugin.createInstance(
            IMmsCancelDownloadExt.class.getName(), context);
        MmsLog.d(TAG, "operator IMmsCancelDownloadExt = " + mMmsCancelDownloadPlugin);
        if (mMmsCancelDownloadPlugin == null) {
            mMmsCancelDownloadPlugin = new DefaultMmsCancelDownloadExt(context);
            MmsLog.d(TAG, "default IMmsCancelDownloadExt = " + mMmsCancelDownloadPlugin);
        }

        mMmsUtilsPlugin = (IMmsUtilsExt) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MESSAGE_UTILS);

        mMmsMessageListItemPlugin = (IMmsMessageListItemExt) MPlugin.createInstance(
            IMmsMessageListItemExt.class.getName(), context);
        MmsLog.d(TAG, "operator IMmsMessageListItemExt = " + mMmsMessageListItemPlugin);
        if (mMmsMessageListItemPlugin == null) {
            mMmsMessageListItemPlugin = new DefaultMmsMessageListItemExt(context);
            MmsLog.d(TAG, "default IMmsMessageListItemExt = " + mMmsMessageListItemPlugin);
        }
    }

    /// M: OP09 Feature: DualSendButton; the button listener;
    ViewOnClickListener mDualButtonClickListener = new ViewOnClickListener() {

        @Override
        public void onClick(View arg0) {
            simSelection();
        }

        @Override
        public void setSelectedSubId(int subId) {
            mSendSubIdForOp09 = subId;
        }
    };

    /// M: fix bug ALPS00517135, update sim state dynamically. @{
    private Handler mSimHanlder = new Handler();
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MmsLog.d(TAG, "onReceive action = " + action);
            if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (mSubSelectDialog != null && mSubSelectDialog.isShowing()) {
                    mSubSelectDialog.dismiss();
                }
                mMmsComposePlugin.setDualSendButtonType(mDualButtonClickListener);
                updateSubInfoList();
                updateSendButtonState();
                MessageUtils.subInfoMap.clear();
            }
        }
    };
    /// @}
	/*HQ_zhangjing 2015-10-23 modified for CQ HQ01456261 begin*/
    private void setSimTypeDrawable(Context context, int subId, ImageView imageView,
            TextView textView) {
        if (context == null || imageView == null) {
            return;
        }
        textView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        Drawable simTypeDraw = null;
		int slotId = SubscriptionManager.getSlotId( subId );
        if ( slotId == 0) {
            simTypeDraw = getResources().getDrawable(R.drawable.icon_card_sim1);
        } else if( slotId == 1 ){
            simTypeDraw = getResources().getDrawable(R.drawable.icon_card_sim2);
        }else {
            simTypeDraw = getResources().getDrawable(R.drawable.icon_card_sim_none);
        }
        if (imageView != null) {
            imageView.setImageDrawable(simTypeDraw);
        }
		
    }
	/*HQ_zhangjing 2015-10-23 modified for CQ HQ01456261 end*/
	
}
