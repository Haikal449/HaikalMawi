/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.launcher2;

import android.os.Handler;

import com.mediatek.launcher2.ext.LauncherLog;

public class Alarm implements Runnable{
    private static final String TAG = "Alarm";

    // if we reach this time and the alarm hasn't been cancelled, call the listener
    private long mAlarmTriggerTime;

    // if we've scheduled a call to run() (ie called mHandler.postDelayed), this variable is true.
    // We use this to avoid having multiple pending callbacks
    private boolean mWaitingForCallback;

    private Handler mHandler;
    private OnAlarmListener mAlarmListener;
    private boolean mAlarmPending = false;

    public Alarm() {
        mHandler = new Handler();
    }

    public void setOnAlarmListener(OnAlarmListener alarmListener) {
        mAlarmListener = alarmListener;
    }

    // Sets the alarm to go off in a certain number of milliseconds. If the alarm is already set,
    // it's overwritten and only the new alarm setting is used
    public void setAlarm(long millisecondsInFuture) {
        long currentTime = System.currentTimeMillis();
        mAlarmPending = true;
        mAlarmTriggerTime = currentTime + millisecondsInFuture;
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "setAlarm: currentTime = " + currentTime + ", millisecondsInFuture = "
                    + millisecondsInFuture + ", mAlarmListener = " + mAlarmListener);
        }
        if (!mWaitingForCallback) {
            mHandler.postDelayed(this, mAlarmTriggerTime - currentTime);
            mWaitingForCallback = true;
        }
    }

    public void cancelAlarm() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "Cancel alarm here: mAlarmListener = " + mAlarmListener);
        }

        mAlarmTriggerTime = 0;
        mAlarmPending = false;
    }

    // this is called when our timer runs out
    public void run() {
        mWaitingForCallback = false;
        if (mAlarmTriggerTime != 0) {
            long currentTime = System.currentTimeMillis();

            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "run: mAlarmTriggerTime = " + mAlarmTriggerTime + ", currentTime = "
                        + currentTime + ", mAlarmListener = " + mAlarmListener);
            }

            if (mAlarmTriggerTime > currentTime) {
                // We still need to wait some time to trigger spring loaded mode--
                // post a new callback
                mHandler.postDelayed(this, Math.max(0, mAlarmTriggerTime - currentTime));
                mWaitingForCallback = true;
            } else {
                mAlarmPending = false;
                if (mAlarmListener != null) {
                    mAlarmListener.onAlarm(this);
                }
            }
        }
    }

    public boolean alarmPending() {
        return mAlarmPending;
    }
}

interface OnAlarmListener {
    public void onAlarm(Alarm alarm);
}
