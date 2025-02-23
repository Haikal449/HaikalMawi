/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
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

package com.android.mms.transaction;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.R;
import com.android.mms.ui.ManageSimMessages;
import com.android.mms.ui.SubSelectActivity;
import com.android.mms.ui.SmsPreferenceActivity;

/*[L-migration] Songmin
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.ui.SubSelectActivity;
*/

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Receive Intent.SIM_FULL_ACTION.  Handle notification that SIM is full.
 */
public class SimFullReceiver extends BroadcastReceiver {

    private static final String TAG = "SimFullReceiver";
    // After user view SIM messages of one SIM card, APP should remove this SIM
    // from SIM-FULL notification
    public static final String SIM_FULL_VIEWED_ACTION = "com.android.mms.ui.SIM_FULL_VIEWED";
    // Store SMS full SIM cards in a Set
    private static TreeSet<Integer> sFullSubIdSet = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 1) {

            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            String action = intent.getAction();

            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
	    try {
		subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);	
	    } catch (RuntimeException e) {
		e.printStackTrace();
		return;
	    }

            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                Log.e(TAG, "subId is invalid");
                return;
            }
            if (sFullSubIdSet == null) {
                sFullSubIdSet = new TreeSet<Integer>();
            }
            // prepare full Subs list to refresh SIM full notification
            if (Telephony.Sms.Intents.SIM_FULL_ACTION.equals(action)) {
                // Receive a new SIM full intent, need store it into list
                sFullSubIdSet.add(subId);
            } else if (SIM_FULL_VIEWED_ACTION.equals(action)) {
                // Already viewed by user, need remove it from list
                sFullSubIdSet.remove(subId);
            }

            Log.d(TAG, "onReceive " + action + "  " + subId + " sFullSubIdSet size: "
                    + sFullSubIdSet.size());
            PendingIntent pendingIntent = null;
            nm.cancel(ManageSimMessages.SIM_FULL_NOTIFICATION_ID);
            if (sFullSubIdSet.size() == 0) {
                // All full subs are viewed by user, no need create notification
                return;
            } else if (sFullSubIdSet.size() == 1) {
                // Only 1 full sub need view. Directly view it in
                // ManageSimMessages
                Intent viewSimIntent = new Intent(context, ManageSimMessages.class);
                viewSimIntent.setAction(Intent.ACTION_VIEW);
                viewSimIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                viewSimIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, sFullSubIdSet.first());
                pendingIntent = PendingIntent.getActivity(context, 0, viewSimIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                // More than 1 full subs need view. Need select sub after
                // click the notification
                Intent selectSubIntent = new Intent(context, SubSelectActivity.class);
                selectSubIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                selectSubIntent.putExtra(SmsPreferenceActivity.PREFERENCE_KEY,
                        SmsPreferenceActivity.SMS_MANAGE_SIM_MESSAGES);
                selectSubIntent.putExtra(SmsPreferenceActivity.PREFERENCE_TITLE_ID,
                        R.string.pref_title_manage_sim_messages);

                int[] fullSubArray = getIntArrayFromSet(sFullSubIdSet);
                selectSubIntent.putExtra(SubSelectActivity.EXTRA_APPOINTED_SUBS,
                        fullSubArray);
                pendingIntent = PendingIntent.getActivity(context, 0, selectSubIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification notification = new Notification();
            final Resources res1 = context.getResources();
            notification.icon = R.drawable.notification_sim_alert;
            notification.tickerText = context.getString(R.string.sim_full_title);
            notification.largeIcon = ((BitmapDrawable)res1.getDrawable(R.drawable.mms_icon)).getBitmap();
            if (Telephony.Sms.Intents.SIM_FULL_ACTION.equals(action)) {
                notification.defaults = Notification.DEFAULT_ALL;
            }

            notification.setLatestEventInfo(
                    context, context.getString(R.string.sim_full_title),
                    context.getString(R.string.sim_full_body),
                    pendingIntent);
            nm.notify(ManageSimMessages.SIM_FULL_NOTIFICATION_ID, notification);
        }
    }

    private int[] getIntArrayFromSet(Set<Integer> set) {
        int size = set.size();
        int[] result = new int[size];
        Iterator<Integer> interator = set.iterator();
        for (int i = 0; i < size; i++) {
            result[i] = interator.next();
        }
        return result;
    }
}
