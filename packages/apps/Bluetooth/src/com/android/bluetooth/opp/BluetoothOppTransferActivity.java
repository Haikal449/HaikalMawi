/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (c) 2008-2009, Motorola, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Motorola, Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.bluetooth.opp;

import com.android.bluetooth.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.database.ContentObserver;
import android.widget.ProgressBar;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import java.io.File;

import android.app.NotificationManager;
import android.text.format.Formatter;

/**
 * Handle all transfer related dialogs: -Ongoing transfer -Receiving one file
 * dialog -Sending one file dialog -sending multiple files dialog -Complete
 * transfer -receive -receive success, will trigger corresponding handler
 * -receive fail dialog -send -send success dialog -send fail dialog -Other
 * dialogs - - DIALOG_RECEIVE_ONGOING will transition to
 * DIALOG_RECEIVE_COMPLETE_SUCCESS or DIALOG_RECEIVE_COMPLETE_FAIL
 * DIALOG_SEND_ONGOING will transition to DIALOG_SEND_COMPLETE_SUCCESS or
 * DIALOG_SEND_COMPLETE_FAIL
 */
public class BluetoothOppTransferActivity extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "[Bluetooth.OPP]BluetoothOppTransferActivity";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    private Uri mUri;

    // ongoing transfer-0 complete transfer-1
    boolean mIsComplete;

    private BluetoothOppTransferInfo mTransInfo;

    private ProgressBar mProgressTransfer;

    private TextView mPercentView;

    private AlertController.AlertParams mPara;

    private View mView = null;

    private TextView mLine1View, mLine2View, mLine3View, mLine5View;

    private int mWhichDialog;

    private BluetoothAdapter mAdapter;

    // Dialogs definition:
    // Receive progress dialog
    public static final int DIALOG_RECEIVE_ONGOING = 0;

    // Receive complete and success dialog
    public static final int DIALOG_RECEIVE_COMPLETE_SUCCESS = 1;

    // Receive complete and fail dialog: will display some fail reason
    public static final int DIALOG_RECEIVE_COMPLETE_FAIL = 2;

    // Send progress dialog
    public static final int DIALOG_SEND_ONGOING = 3;

    // Send complete and success dialog
    public static final int DIALOG_SEND_COMPLETE_SUCCESS = 4;

    // Send complete and fail dialog: will let user retry
    public static final int DIALOG_SEND_COMPLETE_FAIL = 5;

    // Send complete and fail dialog, but current BT is off: don't retry
    public static final int DIALOG_SEND_COMPLETE_FAIL_OFF_BT = 6;

    /** Observer to get notified when the content observer's data changes */
    private BluetoothTransferContentObserver mObserver;

    private boolean mIsFileValid = true;

    // do not update button during activity creating, only update when db
    // changes after activity created
    private boolean mNeedUpdateButton = false;

    private BluetoothOppObexSession mObexSession;

    private BluetoothOppNotification mNotifier;

    private class BluetoothTransferContentObserver extends ContentObserver {
        public BluetoothTransferContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (V) Log.v(TAG, "received db changes.");
            mNeedUpdateButton = true;
            updateProgressbar();
        }
    }


    private static final int UPDATE_TEXTVIEW = 100;

    public class UpdataProgress implements UpdateProgressCb {
        public UpdataProgress() {

        }
        @Override
        public void updateProgress(int totalBytes, int currentBytes) {
            Log.d(TAG, "updateProgress ++, totalBytes = " + totalBytes + " currentBytes = " + currentBytes);

            if (totalBytes == 0) {
                // if Max and progress both equal 0, the progress display 100%.
                // Below is to fix it.
                mProgressTransfer.setMax(100);
            } else {
                mProgressTransfer.setMax(totalBytes);
            }

            mProgressTransfer.setProgress(currentBytes);

            Object[] params = new Object[] {totalBytes, currentBytes};
            Message msg = new Message();
            msg.what = UPDATE_TEXTVIEW;
            msg.obj = params;
            mHandler.sendMessage(msg);
        }
    }

    private UpdataProgress mUp = new UpdataProgress();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case UPDATE_TEXTVIEW:
                    Object[] param = (Object[]) msg.obj;
                    mPercentView.setText(BluetoothOppUtility.formatProgressText((Integer) param[0],
                        (Integer) param[1]));
                    break;

                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mUri = intent.getData();
        if (V)Log.v(TAG, "onCreate::mUri = " + mUri);

        mTransInfo = new BluetoothOppTransferInfo();
        mTransInfo = BluetoothOppUtility.queryRecord(this, mUri);
        if (mTransInfo == null) {
            if (V) Log.e(TAG, "Error: Can not get data from db");
            finish();
            return;
        }

        Log.d(TAG, "onCreate:: direction = " + mTransInfo.mDirection
                + " status = " + mTransInfo.mStatus
                + " filename = " + mTransInfo.mFileName
                + " fileuri = " + mTransInfo.mFileUri);

        if (mTransInfo.mDirection == BluetoothShare.DIRECTION_OUTBOUND & mTransInfo.mFileUri != null) {
            Uri uri = Uri.parse(mTransInfo.mFileUri);
            String filePath = BluetoothOppUtility.getFilePath(BluetoothOppTransferActivity.this, BluetoothOppUtility.originalUri(uri));
            Log.d(TAG, "filepath = " + filePath);
            mIsFileValid = (filePath == null) ? false : (new File(filePath)).exists();
            if (ContactsContract.AUTHORITY.equals(uri.getAuthority())) {
                Log.d(TAG, "contact");
                if (BluetoothOppUtility.contactValid(BluetoothOppTransferActivity.this, uri)) {
                    mIsFileValid = true;
                } else {
                    mIsFileValid = false;
                }
            }
            Log.d(TAG, "mIsFileValid = " + mIsFileValid);
        } else {
            //no use, just set a default value.
            mIsFileValid = false;
        }

        mIsComplete = BluetoothShare.isStatusCompleted(mTransInfo.mStatus);

        displayWhichDialog();

        // update progress bar for ongoing transfer
        if (!mIsComplete) {
            mObserver = new BluetoothTransferContentObserver();
            getContentResolver().registerContentObserver(BluetoothShare.CONTENT_URI, true,
                    mObserver);
        }

        if (mWhichDialog != DIALOG_SEND_ONGOING && mWhichDialog != DIALOG_RECEIVE_ONGOING
                && mWhichDialog != DIALOG_SEND_COMPLETE_FAIL_OFF_BT) {
            // set this record to INVISIBLE
            BluetoothOppUtility.updateVisibilityToHidden(this, mUri);
        }

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mObexSession = BluetoothOppTransfer.mSession;
        // Set up the "dialog"
        setUpDialog();

        mNotifier = new BluetoothOppNotification(this);
    }

    @Override
    protected void onDestroy() {
        if (D) Log.d(TAG, "onDestroy()");

        if (mObserver != null) {
            getContentResolver().unregisterContentObserver(mObserver);
        }
        super.onDestroy();
    }

    private void displayWhichDialog() {
        int direction = mTransInfo.mDirection;
        boolean isSuccess = BluetoothShare.isStatusSuccess(mTransInfo.mStatus);
        boolean isComplete = BluetoothShare.isStatusCompleted(mTransInfo.mStatus);

        if (direction == BluetoothShare.DIRECTION_INBOUND) {
            if (isComplete == true) {
                if (isSuccess == true) {
                    // should not go here
                    mWhichDialog = DIALOG_RECEIVE_COMPLETE_SUCCESS;
                } else if (isSuccess == false) {
                    mWhichDialog = DIALOG_RECEIVE_COMPLETE_FAIL;
                }
            } else if (isComplete == false) {
                mWhichDialog = DIALOG_RECEIVE_ONGOING;
            }
        } else if (direction == BluetoothShare.DIRECTION_OUTBOUND) {
            if (isComplete == true) {
                if (isSuccess == true) {
                    mWhichDialog = DIALOG_SEND_COMPLETE_SUCCESS;

                } else if (isSuccess == false) {
                    if (!BluetoothOppManager.getInstance(this).isEnabled()) {
                        mWhichDialog = DIALOG_SEND_COMPLETE_FAIL_OFF_BT;
                    } else {
                        mWhichDialog = DIALOG_SEND_COMPLETE_FAIL;
                    }
                }
            } else if (isComplete == false) {
                mWhichDialog = DIALOG_SEND_ONGOING;
            }
        }

        if (V) Log.v(TAG, " WhichDialog/dir/isComplete/failOrSuccess" + mWhichDialog + direction
                    + isComplete + isSuccess);
    }

    private void setUpDialog() {
        // final AlertController.AlertParams p = mAlertParams;
        mPara = mAlertParams;
        mPara.mTitle = getString(R.string.download_title);

        if ((mWhichDialog == DIALOG_RECEIVE_ONGOING) || (mWhichDialog == DIALOG_SEND_ONGOING)) {
            mPara.mPositiveButtonText = getString(R.string.download_ok);
            mPara.mPositiveButtonListener = this;
            mPara.mNegativeButtonText = getString(R.string.download_cancel);
            mPara.mNegativeButtonListener = this;
        } else if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_SUCCESS) {
            mPara.mPositiveButtonText = getString(R.string.download_succ_ok);
            mPara.mPositiveButtonListener = this;
        } else if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_FAIL) {
            mPara.mIconAttrId = android.R.attr.alertDialogIcon;
            mPara.mPositiveButtonText = getString(R.string.download_fail_ok);
            mPara.mPositiveButtonListener = this;
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_SUCCESS) {
            mPara.mPositiveButtonText = getString(R.string.upload_succ_ok);
            mPara.mPositiveButtonListener = this;
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL) {
            mPara.mIconAttrId = android.R.attr.alertDialogIcon;
            if (mIsFileValid) {
                mPara.mPositiveButtonText = getString(R.string.upload_fail_ok);
                mPara.mPositiveButtonListener = this;
            }
            mPara.mNegativeButtonText = getString(R.string.upload_fail_cancel);
            mPara.mNegativeButtonListener = this;
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL_OFF_BT) {
            mPara.mPositiveButtonText = getString(R.string.transfer_clear_dlg_title);
            mPara.mPositiveButtonListener = this;
            mPara.mNegativeButtonText = getString(R.string.upload_fail_cancel);
            mPara.mNegativeButtonListener = this;
        }
        mPara.mView = createView();
        setupAlert();
    }

    private View createView() {

        mView = getLayoutInflater().inflate(R.layout.file_transfer, null);

        mProgressTransfer = (ProgressBar)mView.findViewById(R.id.progress_transfer);
        mPercentView = (TextView)mView.findViewById(R.id.progress_percent);

        customizeViewContent();

        // no need update button when activity creating
        mNeedUpdateButton = false;
        updateProgressbar();

        return mView;
    }

    /**
     * customize the content of view
     */
    private void customizeViewContent() {
        String tmp;

        if (mWhichDialog == DIALOG_RECEIVE_ONGOING
                || mWhichDialog == DIALOG_RECEIVE_COMPLETE_SUCCESS) {
            mLine1View = (TextView)mView.findViewById(R.id.line1_view);
            tmp = getString(R.string.download_line1, mTransInfo.mDeviceName);
            mLine1View.setText(tmp);
            mLine2View = (TextView)mView.findViewById(R.id.line2_view);
            tmp = getString(R.string.download_line2, mTransInfo.mFileName);
            mLine2View.setText(tmp);
            mLine3View = (TextView)mView.findViewById(R.id.line3_view);
            tmp = getString(R.string.download_line3, Formatter.formatFileSize(this,
                    mTransInfo.mTotalBytes));
            mLine3View.setText(tmp);
            mLine5View = (TextView)mView.findViewById(R.id.line5_view);
            if (mWhichDialog == DIALOG_RECEIVE_ONGOING) {
                tmp = getString(R.string.download_line5);
            } else if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_SUCCESS) {
                tmp = getString(R.string.download_succ_line5);
            }
            mLine5View.setText(tmp);
        } else if (mWhichDialog == DIALOG_SEND_ONGOING
                || mWhichDialog == DIALOG_SEND_COMPLETE_SUCCESS) {
            mLine1View = (TextView)mView.findViewById(R.id.line1_view);
            tmp = getString(R.string.upload_line1, mTransInfo.mDeviceName);
            mLine1View.setText(tmp);
            mLine2View = (TextView)mView.findViewById(R.id.line2_view);
            tmp = getString(R.string.download_line2, mTransInfo.mFileName);
            mLine2View.setText(tmp);
            mLine3View = (TextView)mView.findViewById(R.id.line3_view);
            tmp = getString(R.string.upload_line3, mTransInfo.mFileType, Formatter.formatFileSize(
                    this, mTransInfo.mTotalBytes));
            mLine3View.setText(tmp);
            mLine5View = (TextView)mView.findViewById(R.id.line5_view);
            if (mWhichDialog == DIALOG_SEND_ONGOING) {
                tmp = getString(R.string.upload_line5);
            } else if (mWhichDialog == DIALOG_SEND_COMPLETE_SUCCESS) {
                tmp = getString(R.string.upload_succ_line5);
            }
            mLine5View.setText(tmp);
        } else if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_FAIL) {
            if (mTransInfo.mStatus == BluetoothShare.STATUS_ERROR_SDCARD_FULL) {
                mLine1View = (TextView)mView.findViewById(R.id.line1_view);
                tmp = getString(R.string.bt_sm_2_1, mTransInfo.mDeviceName);
                mLine1View.setText(tmp);
                mLine2View = (TextView)mView.findViewById(R.id.line2_view);
                tmp = getString(R.string.download_fail_line2, mTransInfo.mFileName);
                mLine2View.setText(tmp);
                mLine3View = (TextView)mView.findViewById(R.id.line3_view);
                tmp = getString(R.string.bt_sm_2_2, Formatter.formatFileSize(this,
                        mTransInfo.mTotalBytes));
                mLine3View.setText(tmp);
            } else {
                mLine1View = (TextView)mView.findViewById(R.id.line1_view);
                tmp = getString(R.string.download_fail_line1);
                mLine1View.setText(tmp);
                mLine2View = (TextView)mView.findViewById(R.id.line2_view);
                tmp = getString(R.string.download_fail_line2, mTransInfo.mFileName);
                mLine2View.setText(tmp);
                mLine3View = (TextView)mView.findViewById(R.id.line3_view);
                tmp = getString(R.string.download_fail_line3, BluetoothOppUtility
                        .getStatusDescription(this, mTransInfo.mStatus, mTransInfo.mDeviceName));
                mLine3View.setText(tmp);
            }
            mLine5View = (TextView)mView.findViewById(R.id.line5_view);
            mLine5View.setVisibility(View.GONE);
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL) {
            mLine1View = (TextView)mView.findViewById(R.id.line1_view);
            tmp = getString(R.string.upload_fail_line1, mTransInfo.mDeviceName);
            mLine1View.setText(tmp);
            mLine2View = (TextView)mView.findViewById(R.id.line2_view);
            tmp = getString(R.string.upload_fail_line1_2, mTransInfo.mFileName);
            mLine2View.setText(tmp);
            mLine3View = (TextView)mView.findViewById(R.id.line3_view);
            tmp = getString(R.string.download_fail_line3, BluetoothOppUtility.getStatusDescription(
                    this, mTransInfo.mStatus, mTransInfo.mDeviceName));
            mLine3View.setText(tmp);
            mLine5View = (TextView)mView.findViewById(R.id.line5_view);
            mLine5View.setVisibility(View.GONE);
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL_OFF_BT) {
            mLine1View = (TextView) mView.findViewById(R.id.line1_view);
            tmp = getString(R.string.upload_fail_line1, mTransInfo.mDeviceName);
            mLine1View.setText(tmp);
            mLine2View = (TextView) mView.findViewById(R.id.line2_view);
            tmp = getString(R.string.upload_fail_line1_2, mTransInfo.mFileName);
            mLine2View.setText(tmp);
            mLine3View = (TextView) mView.findViewById(R.id.line3_view);
            tmp = getString(R.string.download_fail_line3, BluetoothOppUtility.getStatusDescription(
                    this, mTransInfo.mStatus, mTransInfo.mDeviceName));
            mLine3View.setText(tmp);
            mLine5View = (TextView) mView.findViewById(R.id.line5_view);
            mLine5View.setText(R.string.notify_need_on_BT_before_retry);
        }

        if (BluetoothShare.isStatusError(mTransInfo.mStatus)) {
            mProgressTransfer.setVisibility(View.GONE);
            mPercentView.setVisibility(View.GONE);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_SUCCESS) {
                    // "Open" - open receive file
                    BluetoothOppUtility.openReceivedFile(this, mTransInfo.mFileName,
                            mTransInfo.mFileType, mTransInfo.mTimeStamp, mUri);

                    // make current transfer "hidden"
                    BluetoothOppUtility.updateVisibilityToHidden(this, mUri);

                    // clear correspondent notification item
                    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                            .cancel(mTransInfo.mID);
                } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL) {
                    // "try again"

                    // retry the failed transfer
                    new Thread(new Runnable() {

                        public void run() {
                            // make current transfer "hidden"
                            BluetoothOppUtility.updateVisibilityToHidden(BluetoothOppTransferActivity.this, mUri);

                            // clear correspondent notification item
                            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                                    .cancel(mTransInfo.mID);
                            
                            Uri uri = BluetoothOppUtility.originalUri(Uri.parse(mTransInfo.mFileUri));
                            BluetoothOppSendFileInfo sendFileInfo =
                                BluetoothOppSendFileInfo.generateFileInfo(BluetoothOppTransferActivity.this, uri, mTransInfo.mFileType);
                            uri = BluetoothOppUtility.generateUri(uri, sendFileInfo);
                            BluetoothOppUtility.putSendFileInfo(uri, sendFileInfo);
                            mTransInfo.mFileUri = uri.toString();
                            BluetoothOppUtility.retryTransfer(BluetoothOppTransferActivity.this, mTransInfo);

                        }
                    }).start();

                    BluetoothDevice remoteDevice = mAdapter.getRemoteDevice(mTransInfo.mDestAddr);

                    // Display toast message
                    Toast.makeText(
                            this,
                            this.getString(R.string.bt_toast_4, BluetoothOppManager.getInstance(
                                    this).getDeviceName(remoteDevice)), Toast.LENGTH_SHORT)
                            .show();

                } else if (mWhichDialog == DIALOG_SEND_COMPLETE_SUCCESS) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, mUri);
                    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                            .cancel(mTransInfo.mID);
                } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL_OFF_BT) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, mUri);
                    updateNotificationWhenBtDisabled();
                }
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                if (mWhichDialog == DIALOG_RECEIVE_ONGOING || mWhichDialog == DIALOG_SEND_ONGOING) {
                    // "Stop" button
                    this.getContentResolver().delete(mUri, null, null);

                    String msg = "";
                    if (mWhichDialog == DIALOG_RECEIVE_ONGOING) {
                        msg = getString(R.string.bt_toast_3, mTransInfo.mDeviceName);
                    } else if (mWhichDialog == DIALOG_SEND_ONGOING) {
                        msg = getString(R.string.bt_toast_6, mTransInfo.mDeviceName);
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "cancel id = " + mTransInfo.mID);
                    /** M: [ALPS01902046] make sure mObexSession not null @{ */
                    if (mObexSession != null) {
                        mObexSession.notifyStopTaskId(mTransInfo.mID);
                    }
                    /** @} */
                    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                            .cancel(mTransInfo.mID);
                } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, mUri);
                }
                break;
        }
        finish();
    }

    /**
     * Update progress bar per data got from content provider
     */
    private void updateProgressbar() {
        Log.i(TAG, "updateProgressbar ++");
        mTransInfo = BluetoothOppUtility.queryRecord(this, mUri);
        if (mTransInfo == null) {
            if (V) Log.e(TAG, "Error: Can not get data from db");
            return;
        }

        if (BluetoothShare.isStatusCompleted(mTransInfo.mStatus)) {
            Log.i(TAG, "updateProgressbar::complete");
            if (mObexSession != null) {
                mObexSession.unRegisterCb(mUp);
            }
        }

        if (mTransInfo.mTotalBytes == 0) {
            // if Max and progress both equal 0, the progress display 100%.
            // Below is to fix it.
            mProgressTransfer.setMax(100);
        } else {
            mProgressTransfer.setMax(mTransInfo.mTotalBytes);
        }

        Log.i(TAG, "currentByetes in db = " + mTransInfo.mCurrentBytes + " totalByte = " + mTransInfo.mTotalBytes);

        int currentBytes = 0;
        if (mObexSession != null) {
            currentBytes = mObexSession.getCurrentByte();
        }
        Log.i(TAG, "getCurrentBytes in progress = " + currentBytes);
        /// M: [ALPS01902044]Use mTransInfo.mCurrentBytes to adjust if a file is being transfered
        if (mTransInfo.mCurrentBytes > 0 && !BluetoothShare.isStatusCompleted(mTransInfo.mStatus)) {
             mProgressTransfer.setProgress(currentBytes);
             mPercentView.setText(BluetoothOppUtility.formatProgressText(mTransInfo.mTotalBytes,
                currentBytes));
        } else {
             mProgressTransfer.setProgress(mTransInfo.mCurrentBytes);
             mPercentView.setText(BluetoothOppUtility.formatProgressText(mTransInfo.mTotalBytes,
                mTransInfo.mCurrentBytes));
        }

        // Handle the case when DIALOG_RECEIVE_ONGOING evolve to
        // DIALOG_RECEIVE_COMPLETE_SUCCESS/DIALOG_RECEIVE_COMPLETE_FAIL
        // Handle the case when DIALOG_SEND_ONGOING evolve to
        // DIALOG_SEND_COMPLETE_SUCCESS/DIALOG_SEND_COMPLETE_FAIL
        if (!mIsComplete && BluetoothShare.isStatusCompleted(mTransInfo.mStatus)
                && mNeedUpdateButton) {
            displayWhichDialog();
            updateButton();
            customizeViewContent();
        }
    }

    /**
     * Update button when one transfer goto complete from ongoing
     */
    private void updateButton() {
        if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_SUCCESS) {
            mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
            mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    getString(R.string.download_succ_ok));
        } else if (mWhichDialog == DIALOG_RECEIVE_COMPLETE_FAIL) {
            mAlert.setIcon(mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
            mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    getString(R.string.download_fail_ok));
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_SUCCESS) {
            mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
            mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    getString(R.string.upload_succ_ok));
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL) {
            mAlert.setIcon(mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    getString(R.string.upload_fail_ok));
            mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setText(
                    getString(R.string.upload_fail_cancel));
        } else if (mWhichDialog == DIALOG_SEND_COMPLETE_FAIL_OFF_BT) {
            mAlert.setIcon(mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    getString(R.string.transfer_clear_dlg_title));
            mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setText(
                    getString(R.string.upload_fail_cancel));
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart ++");
	//HQ_zhangteng added for HQ01382936 at 2015-09-17
        if (mObexSession != null && mUp != null &&mTransInfo != null&& !BluetoothShare.isStatusCompleted(mTransInfo.mStatus)) {
            mObexSession.registerCb(mUp);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop ++");
        if (mObexSession != null) {
            mObexSession.unRegisterCb(mUp);
        }
    }

    public interface UpdateProgressCb {
        void updateProgress(int totalBytes, int currentBytes);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged ++");
    }

    private void updateNotificationWhenBtDisabled() {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (!adapter.isEnabled()) {
        if (V) Log.v(TAG, "Bluetooth is not enabled, update notification manually.");
        mNotifier.updateNotification();
    }
}

}
