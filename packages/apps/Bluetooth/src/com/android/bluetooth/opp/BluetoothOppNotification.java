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

import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import java.util.HashMap;

/**
 * This class handles the updating of the Notification Manager for the cases
 * where there is an ongoing transfer, incoming transfer need confirm and
 * complete (successful or failed) transfer.
 */
class BluetoothOppNotification {
    private static final String TAG = "[Bluetooth.OPP]BluetoothOppNotification";
    private static final boolean V = Constants.VERBOSE;

    static final String status = "(" + BluetoothShare.STATUS + " == '192'" + ")";

    static final String visible = "(" + BluetoothShare.VISIBILITY + " IS NULL OR "
            + BluetoothShare.VISIBILITY + " == '" + BluetoothShare.VISIBILITY_VISIBLE + "'" + ")";

    static final String confirm = "(" + BluetoothShare.USER_CONFIRMATION + " == '"
            + BluetoothShare.USER_CONFIRMATION_CONFIRMED + "' OR "
            + BluetoothShare.USER_CONFIRMATION + " == '"
            + BluetoothShare.USER_CONFIRMATION_AUTO_CONFIRMED  + "' OR "
            + BluetoothShare.USER_CONFIRMATION + " == '"
            + BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED + "'" + ")";

    static final String not_through_handover = "(" + BluetoothShare.USER_CONFIRMATION + " != '"
            + BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED + "'" + ")";

    static final String WHERE_RUNNING = status + " AND " + visible + " AND " + confirm;

    static final String WHERE_COMPLETED = BluetoothShare.STATUS + " >= '200' AND " + visible +
            " AND " + not_through_handover; // Don't show handover-initiated transfers

    private static final String WHERE_COMPLETED_OUTBOUND = WHERE_COMPLETED + " AND " + "("
            + BluetoothShare.DIRECTION + " == " + BluetoothShare.DIRECTION_OUTBOUND + ")";

    private static final String WHERE_COMPLETED_INBOUND = WHERE_COMPLETED + " AND " + "("
            + BluetoothShare.DIRECTION + " == " + BluetoothShare.DIRECTION_INBOUND + ")";

    static final String WHERE_CONFIRM_PENDING = BluetoothShare.USER_CONFIRMATION + " == '"
            + BluetoothShare.USER_CONFIRMATION_PENDING + "'" + " AND " + visible;

    public NotificationManager mNotificationMgr;

    private static Context mContext;

    private HashMap<String, NotificationItem> mNotifications;

    private NotificationUpdateThread mUpdateNotificationThread;

    private int mPendingUpdate = 0;

    private static final int NOTIFICATION_ID_OUTBOUND = -1000005;

    private static final int NOTIFICATION_ID_INBOUND = -1000006;

    private boolean mUpdateCompleteNotification = true;

    private static int mActiveNotificationId = 0;

    /// M: [ALPS01905841] Add a variable to record the last cleaned notification@{
    private static int mNotCleanedNotificationId = 0;

    private static int mPreparedToCleanedNotificationId = 0;
    /**
     * This inner class is used to describe some properties for one transfer.
     */
    public static class NotificationItem {
        int id; // This first field _id in db;

        int direction; // to indicate sending or receiving

        int totalCurrent = 0; // current transfer bytes

        int totalTotal = 0; // total bytes for current transfer

        long timeStamp = 0; // Database time stamp. Used for sorting ongoing transfers.

        String description; // the text above progress bar

        boolean handoverInitiated = false; // transfer initiated by connection handover (eg NFC)

        /// M: [Carrier] Stands for the handover transfer carrier(nfc or hotnot)
        String carrier = null;

        String destination; // destination associated with this transfer
    }

    /**
     * Constructor
     *
     * @param ctx The context to use to obtain access to the Notification
     *            Service
     */
    BluetoothOppNotification(Context ctx) {
        mContext = ctx;
        mNotificationMgr = (NotificationManager)mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifications = new HashMap<String, NotificationItem>();
    }

    /**
     * Update the notification ui.
     */
    public void updateNotification() {
        synchronized (BluetoothOppNotification.this) {
            mPendingUpdate++;
            if (mPendingUpdate > 1) {
                if (V) Log.v(TAG, "update too frequent, put in queue");
                return;
            }
            if (!mHandler.hasMessages(NOTIFY)) {
                if (V) Log.v(TAG, "send message");
                mHandler.sendMessage(mHandler.obtainMessage(NOTIFY));
            }
        }
    }

    public void clearIncomingConfirmNotifycation() {
        Log.d(TAG, "clearIncomingConfirmNotifycation ++");
        synchronized (BluetoothOppNotification.this) {
            mHandler.sendMessage(mHandler.obtainMessage(CLEAR_INCOMING_NOTIFY));
        }
    }

    private static final int NOTIFY = 0;
    private static final int CLEAR_INCOMING_NOTIFY = 1;
    // Use 1 second timer to limit notification frequency.
    // 1. On the first notification, create the update thread.
    //    Buffer other updates.
    // 2. Update thread will clear mPendingUpdate.
    // 3. Handler sends a delayed message to self
    // 4. Handler checks if there are any more updates after 1 second.
    // 5. If there is an update, update it else stop.
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY:
                    synchronized (BluetoothOppNotification.this) {
                        if (mPendingUpdate > 0 && mUpdateNotificationThread == null) {
                            if (V) Log.v(TAG, "new notify threadi!");
                            mUpdateNotificationThread = new NotificationUpdateThread();
                            mUpdateNotificationThread.start();
                            if (V) Log.v(TAG, "send delay message");
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(NOTIFY), 1000);
                        } else if (mPendingUpdate > 0) {
                            if (V) Log.v(TAG, "previous thread is not finished yet");
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(NOTIFY), 1000);
                        }
                        break;
                    }
                case CLEAR_INCOMING_NOTIFY:
                    clearIncomingNotify();
                    break;
              }
         }
    };

    private class NotificationUpdateThread extends Thread {

        public NotificationUpdateThread() {
            super("Notification Update Thread");
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            synchronized (BluetoothOppNotification.this) {
                if (mUpdateNotificationThread != this) {
                    throw new IllegalStateException(
                            "multiple UpdateThreads in BluetoothOppNotification");
                }
                mPendingUpdate = 0;
            }
            updateActiveNotification();
            updateCompletedNotification();
            updateIncomingFileConfirmNotification();
            synchronized (BluetoothOppNotification.this) {
                mUpdateNotificationThread = null;
            }
        }
    }

    private void updateActiveNotification() {
        if (V)Log.v(TAG, "updateActiveNotification ++");
        // Active transfers
        Cursor cursor = mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null,
                WHERE_RUNNING, null, BluetoothShare._ID);
        if (cursor == null) {
            return;
        }

        // If there is active transfers, then no need to update completed transfer
        // notifications
        if (cursor.getCount() > 0) {
            mUpdateCompleteNotification = false;
        } else {
            mUpdateCompleteNotification = true;
        }
        if (V) Log.v(TAG, "mUpdateCompleteNotification = " + mUpdateCompleteNotification);

        // Collate the notifications
        final int timestampIndex = cursor.getColumnIndexOrThrow(BluetoothShare.TIMESTAMP);
        final int directionIndex = cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION);
        final int idIndex = cursor.getColumnIndexOrThrow(BluetoothShare._ID);
        final int totalBytesIndex = cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES);
        final int currentBytesIndex = cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES);
        final int dataIndex = cursor.getColumnIndexOrThrow(BluetoothShare._DATA);
        final int filenameHintIndex = cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT);
        final int confirmIndex = cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION);
        final int destinationIndex = cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION);
        /// M:  [Carrier] get carrier column index
        final int carrierIndex = cursor.getColumnIndexOrThrow(BluetoothShare.CARRIER_NAME);

        mNotifications.clear();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long timeStamp = cursor.getLong(timestampIndex);
            int dir = cursor.getInt(directionIndex);
            int id = cursor.getInt(idIndex);
            int total = cursor.getInt(totalBytesIndex);
            int current = cursor.getInt(currentBytesIndex);
            int confirmation = cursor.getInt(confirmIndex);
            
            /// M:  [Carrier] get carrier name for currunt row
            String carrierName = cursor.getString(carrierIndex);

            String destination = cursor.getString(destinationIndex);
            String fileName = cursor.getString(dataIndex);
            if (fileName == null) {
                fileName = cursor.getString(filenameHintIndex);
            }
            if (fileName == null) {
                fileName = mContext.getString(R.string.unknown_file);
            }

            String batchID = Long.toString(timeStamp);

            // sending objects in one batch has same timeStamp
            if (mNotifications.containsKey(batchID)) {
                // NOTE: currently no such case
                // Batch sending case
            } else {
                NotificationItem item = new NotificationItem();
                /// M: [Carrier] get carrier name
                item.carrier = carrierName;
                
                item.timeStamp = timeStamp;
                item.id = id;
                item.direction = dir;
                if (item.direction == BluetoothShare.DIRECTION_OUTBOUND) {
                    item.description = mContext.getString(R.string.notification_sending, fileName);
                } else if (item.direction == BluetoothShare.DIRECTION_INBOUND) {
                    item.description = mContext
                            .getString(R.string.notification_receiving, fileName);
                } else {
                    if (V) Log.v(TAG, "mDirection ERROR!");
                }
                item.totalCurrent = current;
                item.totalTotal = total;
                item.handoverInitiated =
                        confirmation == BluetoothShare.USER_CONFIRMATION_HANDOVER_CONFIRMED;
                item.destination = destination;
                mNotifications.put(batchID, item);

                if (V) Log.v(TAG, "ID=" + item.id + "; batchID=" + batchID + "; totoalCurrent"
                            + item.totalCurrent + "; totalTotal=" + item.totalTotal);
            }
        }
        cursor.close();

        // Add the notifications
        for (NotificationItem item : mNotifications.values()) {
            if (item.handoverInitiated) {
                float progress = 0;
                if (item.totalTotal == -1) {
                    progress = -1;
                } else {
                    progress = (float)item.totalCurrent / item.totalTotal;
                }

                // Let NFC service deal with notifications for this transfer
                Intent intent = new Intent(Constants.ACTION_BT_OPP_TRANSFER_PROGRESS);
                if (item.direction == BluetoothShare.DIRECTION_INBOUND) {
                    intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION,
                            Constants.DIRECTION_BLUETOOTH_INCOMING);
                } else {
                    intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION,
                            Constants.DIRECTION_BLUETOOTH_OUTGOING);
                }
                intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_ID, item.id);
                intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_PROGRESS, progress);
                intent.putExtra(Constants.EXTRA_BT_OPP_ADDRESS, item.destination);
                /// M: [Carrier] send broadcast with carrier name extra
                intent.putExtra(Constants.EXTRA_INVOKE_CARRIER, item.carrier);
                mContext.sendBroadcast(intent, Constants.HANDOVER_STATUS_PERMISSION);
                continue;
            }
            // Build the notification object
            // TODO: split description into two rows with filename in second row
            Notification.Builder b = new Notification.Builder(mContext);
            b.setColor(mContext.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color));
            b.setContentTitle(item.description);
            b.setContentInfo(
                    BluetoothOppUtility.formatProgressText(item.totalTotal, item.totalCurrent));
            b.setProgress(item.totalTotal, item.totalCurrent, item.totalTotal == -1);
            b.setWhen(item.timeStamp);
            if (item.direction == BluetoothShare.DIRECTION_OUTBOUND) {
                b.setSmallIcon(android.R.drawable.stat_sys_upload);
            } else if (item.direction == BluetoothShare.DIRECTION_INBOUND) {
                b.setSmallIcon(android.R.drawable.stat_sys_download);
            } else {
                if (V) Log.v(TAG, "mDirection ERROR!");
            }
            b.setOngoing(true);

            Intent intent = new Intent(Constants.ACTION_LIST);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            intent.setDataAndNormalize(Uri.parse(BluetoothShare.CONTENT_URI + "/" + item.id));

            b.setContentIntent(PendingIntent.getBroadcast(mContext, 0, intent, 0));
            mNotificationMgr.notify(item.id, b.getNotification());

            /// M: [ALPS01905841] Prevent the notification clean up operation is skipped due to update request is handled
            /// each second. Use the mCleanedNotificationId to make the clean request get run @{
            if (mActiveNotificationId != item.id) {
                mUpdateCompleteNotification = true;
            } else {
                mNotCleanedNotificationId = mActiveNotificationId;
                if(mPreparedToCleanedNotificationId != mNotCleanedNotificationId){
                     if (V) Log.v(TAG, "remove MissID= " + mPreparedToCleanedNotificationId);
                     mNotificationMgr.cancel(mPreparedToCleanedNotificationId);
                }
                if (V) Log.v(TAG, "keep cancel_id= " + mNotCleanedNotificationId);
            }
            mActiveNotificationId = item.id;
            /// @}
        }
    }

    public void updateCompletedNotification() {
        String title;
        String caption;
        long timeStamp = 0;
        int outboundSuccNumber = 0;
        int outboundFailNumber = 0;
        int outboundNum;
        int inboundNum;
        int inboundSuccNumber = 0;
        int inboundFailNumber = 0;
        Intent intent;

        // If there is active transfer, no need to update complete transfer
        // notification
        if (!mUpdateCompleteNotification) {
            if (V) Log.v(TAG, "No need to update complete notification");
            return;
        }

        // After merge complete notifications to 2 notifications, there is no
        // chance to update the active notifications to complete notifications
        // as before. So need cancel the active notification after the active
        // transfer becomes complete.
        if (mNotificationMgr != null && mActiveNotificationId != 0) {
	    if (V) Log.v(TAG, "start to cancel_id= " + mNotCleanedNotificationId);
            mNotificationMgr.cancel(mNotCleanedNotificationId);
            /// M: [ALPS01905841] Update the value of  mCleanedNotificationId to record the
            /// last cleaned active notification @{
            mNotCleanedNotificationId = mActiveNotificationId;
            mPreparedToCleanedNotificationId = mNotCleanedNotificationId;	
            if (V) Log.v(TAG, "next will be cancel_id= " + mNotCleanedNotificationId);
            /// @}
            if (V) Log.v(TAG, "ongoing transfer notification was removed");
        }

        // Creating outbound notification
        Cursor cursor;
        try {
            cursor = mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null, WHERE_COMPLETED_OUTBOUND, null,
                    BluetoothShare.TIMESTAMP + " DESC");
        if (cursor == null) {
            return;
        }

        final int timestampIndex = cursor.getColumnIndexOrThrow(BluetoothShare.TIMESTAMP);
        final int statusIndex = cursor.getColumnIndexOrThrow(BluetoothShare.STATUS);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            if (cursor.isFirst()) {
                // Display the time for the latest transfer
                timeStamp = cursor.getLong(timestampIndex);
            }
            int status = cursor.getInt(statusIndex);

            if (BluetoothShare.isStatusError(status)) {
                outboundFailNumber++;
            } else {
                outboundSuccNumber++;
            }
        }
        if (V) Log.v(TAG, "outbound: succ-" + outboundSuccNumber + "  fail-" + outboundFailNumber);
        cursor.close();

        outboundNum = outboundSuccNumber + outboundFailNumber;
        // create the outbound notification
        if (outboundNum > 0) {
            Notification outNoti = new Notification();
            outNoti.icon = android.R.drawable.stat_sys_upload_done;
            title = mContext.getString(R.string.outbound_noti_title);
            caption = mContext.getString(R.string.noti_caption, outboundSuccNumber,
                    outboundFailNumber);
            intent = new Intent(Constants.ACTION_OPEN_OUTBOUND_TRANSFER);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            outNoti.color = mContext.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color);
            outNoti.setLatestEventInfo(mContext, title, caption, PendingIntent.getBroadcast(
                    mContext, 0, intent, 0));
            intent = new Intent(Constants.ACTION_COMPLETE_HIDE);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            outNoti.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            outNoti.when = timeStamp;
            mNotificationMgr.notify(NOTIFICATION_ID_OUTBOUND, outNoti);
        } else {
            if (mNotificationMgr != null) {
                mNotificationMgr.cancel(NOTIFICATION_ID_OUTBOUND);
                if (V) Log.v(TAG, "outbound notification was removed.");
            }
        }

        // Creating inbound notification
        cursor = mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null,
                WHERE_COMPLETED_INBOUND, null, BluetoothShare.TIMESTAMP + " DESC");
        if (cursor == null) {
            return;
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            if (cursor.isFirst()) {
                // Display the time for the latest transfer
                timeStamp = cursor.getLong(timestampIndex);
            }
            int status = cursor.getInt(statusIndex);

            if (BluetoothShare.isStatusError(status)) {
                inboundFailNumber++;
            } else {
                inboundSuccNumber++;
            }
        }
        if (V) Log.v(TAG, "inbound: succ-" + inboundSuccNumber + "  fail-" + inboundFailNumber);
        cursor.close();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "updateCompletedNotification:: update error");
            e.printStackTrace();
        }

        inboundNum = inboundSuccNumber + inboundFailNumber;
        // create the inbound notification
        if (inboundNum > 0) {
            Notification inNoti = new Notification();
            inNoti.icon = android.R.drawable.stat_sys_download_done;
            title = mContext.getString(R.string.inbound_noti_title);
            caption = mContext.getString(R.string.noti_caption, inboundSuccNumber,
                    inboundFailNumber);
            intent = new Intent(Constants.ACTION_OPEN_INBOUND_TRANSFER);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            inNoti.color = mContext.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color);
            inNoti.setLatestEventInfo(mContext, title, caption, PendingIntent.getBroadcast(
                    mContext, 0, intent, 0));
            intent = new Intent(Constants.ACTION_COMPLETE_HIDE);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            inNoti.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            inNoti.when = timeStamp;
			//add by HQ_zhouguo at 20150829 start
			inNoti.ledARGB = 0xFF00FF00;  
			inNoti.ledOnMS = 500;
            inNoti.ledOffMS = 2000;
			inNoti.flags = Notification.FLAG_SHOW_LIGHTS;
			//add by HQ_zhouguo at 20150829 end
            mNotificationMgr.notify(NOTIFICATION_ID_INBOUND, inNoti);
        } else {
            if (mNotificationMgr != null) {
                mNotificationMgr.cancel(NOTIFICATION_ID_INBOUND);
                if (V) Log.v(TAG, "inbound notification was removed.");
            }
        }
    }

    private void updateIncomingFileConfirmNotification() {
        Cursor cursor = mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null,
                WHERE_CONFIRM_PENDING, null, BluetoothShare._ID);

        if (cursor == null) {
            return;
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            CharSequence title =
                    mContext.getText(R.string.incoming_file_confirm_Notification_title);
            CharSequence caption = mContext
                    .getText(R.string.incoming_file_confirm_Notification_caption);
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare._ID));
            long timeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TIMESTAMP));
            Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + id);

            Notification n = new Notification();
            n.icon = R.drawable.bt_incomming_file_notification;
            n.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
            n.flags |= Notification.FLAG_ONGOING_EVENT;
            n.defaults = Notification.DEFAULT_SOUND;
            n.tickerText = title;

            Intent intent = new Intent(Constants.ACTION_INCOMING_FILE_CONFIRM);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            intent.setDataAndNormalize(contentUri);

            n.when = timeStamp;
            n.color = mContext.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color);
            n.setLatestEventInfo(mContext, title, caption, PendingIntent.getBroadcast(mContext, 0,
                    intent, 0));

            intent = new Intent(Constants.ACTION_HIDE);
            intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            intent.setDataAndNormalize(contentUri);
            n.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);

            mNotificationMgr.notify(id, n);
        }
        cursor.close();
    }

    private void clearIncomingNotify() {
        Log.d(TAG, "clearIncomingNotify ++ ");
        Cursor cursor = mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null,
                WHERE_CONFIRM_PENDING, null, BluetoothShare._ID);

        if (cursor == null) {
            return;
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            CharSequence title =
                    mContext.getText(R.string.incoming_file_confirm_Notification_title);
            CharSequence caption = mContext
                    .getText(R.string.incoming_file_confirm_Notification_caption);
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare._ID));

            mNotificationMgr.cancel(id);
        }
        cursor.close();
    }

    public static Notification getOppOngoingNotification(NotificationItem item) {
        Log.i(TAG, "getOppOngoingNotification ++");
        Notification.Builder b = new Notification.Builder(mContext);
        b.setContentTitle(item.description);
        b.setContentInfo(BluetoothOppUtility.formatProgressText(item.totalTotal, item.totalCurrent));
        b.setProgress(item.totalTotal, item.totalCurrent, item.totalTotal == -1);
        //b.setWhen(item.timeStamp);
        b.setWhen(System.currentTimeMillis());
        if (item.direction == BluetoothShare.DIRECTION_OUTBOUND) {
            b.setSmallIcon(android.R.drawable.stat_sys_upload);
        } else if (item.direction == BluetoothShare.DIRECTION_INBOUND) {
            b.setSmallIcon(android.R.drawable.stat_sys_download);
        } else {
            if (V)
                Log.v(TAG, "mDirection ERROR!");
        }
        b.setOngoing(true);

        Intent intent = new Intent(Constants.ACTION_LIST);
        intent.setClassName(Constants.THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
        intent.setDataAndNormalize(Uri.parse(BluetoothShare.CONTENT_URI + "/" + item.id));

        b.setContentIntent(PendingIntent.getBroadcast(mContext, 0, intent, 0));
        /*mNotificationMgr.notify(item.id, b.getNotification());*/

        mActiveNotificationId = item.id;

        return b.getNotification();
    }
}
