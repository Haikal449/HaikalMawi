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

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.widget.Toast;


/**
 * Receives and handles: system broadcasts; Intents from other applications;
 * Intents from OppService; Intents from modules in Opp application layer.
 */
public class BluetoothOppReceiver extends BroadcastReceiver {
    private static final String TAG = "[Bluetooth.OPP]BluetoothOppReceiver";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "onReceive::action = " + action);
	//HQ_caoxuhao add for red line start
	if(action == null)	return;
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	//HQ_caoxuhao add for red line end
            if (BluetoothAdapter.STATE_ON == intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                if (V) Log.v(TAG, "Received BLUETOOTH_STATE_CHANGED_ACTION, BLUETOOTH_STATE_ON");
                context.startService(new Intent(context, BluetoothOppService.class));

                // If this is within a sending process, continue the handle
                // logic to display device picker dialog.
                synchronized (this) {
                    if (BluetoothOppManager.getInstance(context).mSendingFlag) {
                        // reset the flags
                        BluetoothOppManager.getInstance(context).mSendingFlag = false;

                        Intent in1 = new Intent(BluetoothDevicePicker.ACTION_LAUNCH);
                        in1.putExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false);
                        in1.putExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE,
                                BluetoothDevicePicker.FILTER_TYPE_TRANSFER);
                        in1.putExtra(BluetoothDevicePicker.EXTRA_LAUNCH_PACKAGE,
                                Constants.THIS_PACKAGE_NAME);
                        in1.putExtra(BluetoothDevicePicker.EXTRA_LAUNCH_CLASS,
                                BluetoothOppReceiver.class.getName());

                        in1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(in1);
                    }
                }
            }
        } else if (action.equals(BluetoothDevicePicker.ACTION_DEVICE_SELECTED)) {
            BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(context);

            if (!mOppManager.isEnabled()) {
                if (D) Log.i(TAG, "BT off, return");
                return;
            }

            final BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (V) Log.v(TAG, "Received BT device selected intent, bt device: " + remoteDevice);

            // Insert transfer session record to database
            mOppManager.startTransfer(remoteDevice);

            // Display toast message
            String deviceName = remoteDevice.getName();
            String toastMsg;
            int batchSize = mOppManager.getBatchSize();
            if (mOppManager.mMultipleFlag) {
                toastMsg = context.getString(R.string.bt_toast_5, Integer.toString(batchSize),
                        deviceName);
            } else {
                toastMsg = context.getString(R.string.bt_toast_4, deviceName);
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
        } else if (action.equals(Constants.ACTION_INCOMING_FILE_CONFIRM)) {
            if (V) Log.v(TAG, "Receiver ACTION_INCOMING_FILE_CONFIRM");

            Uri uri = intent.getData();
            Intent in = new Intent(context, BluetoothOppIncomingFileConfirmActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.setDataAndNormalize(uri);
            context.startActivity(in);

            NotificationManager notMgr = (NotificationManager)context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (notMgr != null) {
                notMgr.cancel((int)ContentUris.parseId(intent.getData()));
                if (V) Log.v(TAG, "notMgr.cancel called");
            }
        } else if (action.equals(BluetoothShare.INCOMING_FILE_CONFIRMATION_REQUEST_ACTION)) {
            if (V) Log.v(TAG, "Receiver INCOMING_FILE_NOTIFICATION");
			/*add by lihaizhou for HQ01351870 by begin */
			Uri uri = intent.getData();
			Log.v("lihaizhou", "uri"+uri);
            Intent in = new Intent(context, BluetoothOppIncomingFileConfirmActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.setDataAndNormalize(uri);
            context.startActivity(in);
			/*add by lihaizhou for HQ01351870 by end*/
            Toast.makeText(context, context.getString(R.string.incoming_file_toast_msg),
                    Toast.LENGTH_SHORT).show();

        } else if (action.equals(Constants.ACTION_OPEN) || action.equals(Constants.ACTION_LIST)) {
            if (V) {
                if (action.equals(Constants.ACTION_OPEN)) {
                    Log.v(TAG, "Receiver open for " + intent.getData());
                } else {
                    Log.v(TAG, "Receiver list for " + intent.getData());
                }
            }

            BluetoothOppTransferInfo transInfo = new BluetoothOppTransferInfo();
            Uri uri = intent.getData();
            transInfo = BluetoothOppUtility.queryRecord(context, uri);
            if (transInfo == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }

            if (transInfo.mDirection == BluetoothShare.DIRECTION_INBOUND
                    && BluetoothShare.isStatusSuccess(transInfo.mStatus)) {
                // if received file successfully, open this file
                BluetoothOppUtility.openReceivedFile(context, transInfo.mFileName,
                        transInfo.mFileType, transInfo.mTimeStamp, uri);
                BluetoothOppUtility.updateVisibilityToHidden(context, uri);
            } else {
                Intent in = new Intent(context, BluetoothOppTransferActivity.class);
                in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                in.setDataAndNormalize(uri);
                context.startActivity(in);
            }

            NotificationManager notMgr = (NotificationManager)context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (notMgr != null) {
                notMgr.cancel((int)ContentUris.parseId(intent.getData()));
                if (V) Log.v(TAG, "notMgr.cancel called");
                }
        } else if (action.equals(Constants.ACTION_OPEN_OUTBOUND_TRANSFER)) {
            if (V) Log.v(TAG, "Received ACTION_OPEN_OUTBOUND_TRANSFER.");

            Intent in = new Intent(context, BluetoothOppTransferHistory.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("direction", BluetoothShare.DIRECTION_OUTBOUND);
            context.startActivity(in);
        } else if (action.equals(Constants.ACTION_OPEN_INBOUND_TRANSFER)) {
            if (V) Log.v(TAG, "Received ACTION_OPEN_INBOUND_TRANSFER.");

            Intent in = new Intent(context, BluetoothOppTransferHistory.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("direction", BluetoothShare.DIRECTION_INBOUND);
            context.startActivity(in);
        } else if (action.equals(Constants.ACTION_OPEN_RECEIVED_FILES)) {
            if (V) Log.v(TAG, "Received ACTION_OPEN_RECEIVED_FILES.");

            Intent in = new Intent(context, BluetoothOppTransferHistory.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("direction", BluetoothShare.DIRECTION_INBOUND);
            in.putExtra(Constants.EXTRA_SHOW_ALL_FILES, true);
            context.startActivity(in);
        } else if (action.equals(Constants.ACTION_HIDE)) {
            if (V) Log.v(TAG, "Receiver hide for " + intent.getData());
            Cursor cursor = context.getContentResolver().query(intent.getData(), null, null, null,
                    null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int statusColumn = cursor.getColumnIndexOrThrow(BluetoothShare.STATUS);
                    int status = cursor.getInt(statusColumn);
                    int visibilityColumn = cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY);
                    int visibility = cursor.getInt(visibilityColumn);
                    int userConfirmationColumn = cursor
                            .getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION);
                    int userConfirmation = cursor.getInt(userConfirmationColumn);
                    if (((userConfirmation == BluetoothShare.USER_CONFIRMATION_PENDING))
                            && visibility == BluetoothShare.VISIBILITY_VISIBLE) {
                        ContentValues values = new ContentValues();
                        values.put(BluetoothShare.VISIBILITY, BluetoothShare.VISIBILITY_HIDDEN);
                        context.getContentResolver().update(intent.getData(), values, null, null);
                        if (V) Log.v(TAG, "Action_hide received and db updated");
                        }
                }
                cursor.close();
            }
        } else if (action.equals(Constants.ACTION_COMPLETE_HIDE)) {
            if (V) Log.v(TAG, "Receiver ACTION_COMPLETE_HIDE");
            ContentValues updateValues = new ContentValues();           
            updateValues.put(BluetoothShare.VISIBILITY, BluetoothShare.VISIBILITY_HIDDEN);
            context.getContentResolver().update(BluetoothShare.CONTENT_URI, updateValues,
                   BluetoothOppNotification.WHERE_COMPLETED, null);
        } else if (action.equals(BluetoothShare.TRANSFER_COMPLETED_ACTION)) {
            if (V) Log.v(TAG, "Receiver Transfer Complete Intent for " + intent.getData());

            String toastMsg = null;
            BluetoothOppTransferInfo transInfo = new BluetoothOppTransferInfo();
            transInfo = BluetoothOppUtility.queryRecord(context, intent.getData());
            if (transInfo == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            /*if (transInfo.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
        Uri tempUri = Uri.parse(transInfo.mFileUri);
                Log.i(TAG, "testfornfc::fileName = " + BluetoothOppUtility.getFilePath(context, BluetoothOppUtility.originalUri(tempUri)));
            } else {
                Log.i(TAG, "testfornfc::fileName = " + transInfo.mFileName);
            }*/


            if (transInfo.mHandoverInitiated) {
                // Deal with handover-initiated transfers separately
                Intent handoverIntent = new Intent(Constants.ACTION_BT_OPP_TRANSFER_DONE);
                if (transInfo.mDirection == BluetoothShare.DIRECTION_INBOUND) {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION,
                            Constants.DIRECTION_BLUETOOTH_INCOMING);
                } else {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION,
                            Constants.DIRECTION_BLUETOOTH_OUTGOING);
                }
                handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_ID, transInfo.mID);
                handoverIntent.putExtra(Constants.EXTRA_BT_OPP_ADDRESS, transInfo.mDestAddr);
                /// M: [Carrier] Send broadcast with carrier name
                handoverIntent.putExtra(Constants.EXTRA_INVOKE_CARRIER, transInfo.mCarrierName);

                if (BluetoothShare.isStatusSuccess(transInfo.mStatus)) {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_STATUS,
                            Constants.HANDOVER_TRANSFER_STATUS_SUCCESS);
                    if (transInfo.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
            String filePath = BluetoothOppUtility.getFilePath(context,BluetoothOppUtility.originalUri(Uri.parse(transInfo.mFileUri)));
                        handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_URI, filePath);
                    } else {
                        handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_URI, transInfo.mFileName);
                    }
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_MIMETYPE,
                            transInfo.mFileType);
                } else {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_STATUS,
                            Constants.HANDOVER_TRANSFER_STATUS_FAILURE);
                }
                context.sendBroadcast(handoverIntent, Constants.HANDOVER_STATUS_PERMISSION);
                return;
            }

            if (BluetoothShare.isStatusSuccess(transInfo.mStatus)) {
                if (transInfo.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
                    toastMsg = context.getString(R.string.notification_sent, transInfo.mFileName);
                } else if (transInfo.mDirection == BluetoothShare.DIRECTION_INBOUND) {
                    toastMsg = context.getString(R.string.notification_received,
                            transInfo.mFileName);
                }

            } else if (BluetoothShare.isStatusError(transInfo.mStatus)) {
                if (transInfo.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
                    toastMsg = context.getString(R.string.notification_sent_fail,
                            transInfo.mFileName);
                } else if (transInfo.mDirection == BluetoothShare.DIRECTION_INBOUND) {
                    toastMsg = context.getString(R.string.download_fail_line1);
                }
            }
            if (V) Log.v(TAG, "Toast msg == " + toastMsg);
            if (toastMsg != null) {
                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
            }
        } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
            Log.i(TAG, "received test Intent.ACTION_MEDIA_EJECT");
            Cursor cursor = context.getContentResolver().query(BluetoothShare.CONTENT_URI,
                    new String[] {BluetoothShare._DATA, BluetoothShare.URI, BluetoothShare.DIRECTION},
                    "status == 192",
                    null, null);
            if (cursor == null) {
                Log.d(TAG, "cursor == null !");
                return;
            }
            int direction = -1;
            String filePath = null;
            Uri uri = null;
            String root = null;
            String commonRoot = "/storage/sdcardx";
            Log.i(TAG, "count = " + cursor.getCount());
            if (cursor.moveToFirst()) {
                Log.i(TAG, "cursor != null, cursor.count = " + cursor.getCount());
                direction = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
                String uriStr = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.URI));
                Log.d(TAG, "uriStr = " + uriStr);

                if (direction == BluetoothShare.DIRECTION_INBOUND) {
                    filePath = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare._DATA));
                    root = filePath.substring(0, commonRoot.length());
                } else if (direction == BluetoothShare.DIRECTION_OUTBOUND) {
                    uri = Uri.parse(uriStr);
                    if ("content".equals(uri.getScheme())) {
                        String[] projection = new String[] {MediaColumns.DATA};
                        Cursor cursor1 = context.getContentResolver().query(BluetoothOppUtility.originalUri(uri), projection, null, null, null);
                        if (cursor1 == null) {
                            return;
                        }
                        if (cursor1.moveToFirst()) {
                            filePath = cursor1.getString(cursor.getColumnIndexOrThrow(MediaColumns.DATA));
                        }
                        cursor1.close();
                        if (filePath == null) {
                            return;
                        }
                        root = filePath.substring(0, commonRoot.length());
                    } else if ("file".equals(uri.getScheme())) {
                        filePath = uriStr;
                        root = filePath.substring("file:///".length() - 1, "file:///".length() + commonRoot.length() - 1);
                    } else {
                        Log.d(TAG, "other type, do not process");
                        return;
                    }
                }
            } else {
                cursor.close();
                Log.d(TAG, "there is no running task");
                return;
            }
            cursor.close();
            Log.d(TAG, "direction = " + direction + " filepath = " + filePath + " root = " + root);
            if (direction == -1 || filePath == null) {
                return;
            }

            Uri path = intent.getData();
            Log.d(TAG, "path = " + path);
            BluetoothOppService oppService = BluetoothOppService.getInstance();
            if (path != null && path.toString().contains(root) && oppService != null) {
                Log.i(TAG, "sdcard is removed, stop task");

                if (direction == BluetoothShare.DIRECTION_INBOUND) {
                    if (oppService.mServerTransfer != null) {
                        oppService.mServerTransfer.stop();
                    }
                } else if (direction == BluetoothShare.DIRECTION_OUTBOUND) {
                    if (oppService.mTransfer != null) {
                        oppService.mTransfer.stop();
                    }
                }
                oppService.mBatchs.clear();
            }
        }
    }
}
