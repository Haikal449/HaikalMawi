/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
 * Copyright (C) 2006 The Android Open Source Project
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

package com.mediatek.phone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import android.net.LinkProperties;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;


import com.android.phone.PhoneGlobals;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;

import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.dataconnection.DcTrackerBase;

import com.mediatek.internal.telephony.BtSimapOperResponse;
import java.util.ArrayList;

import java.util.Iterator;
import android.os.Messenger;
import android.os.IBinder;

// VOLTE
import com.mediatek.internal.telephony.DedicateBearerProperties;
import com.mediatek.internal.telephony.DefaultBearerConfig;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.QosStatus;

import com.mediatek.internal.telephony.RadioManager;

import com.mediatek.internal.telephony.TftStatus;

import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.telephony.ExternalSimManager;


/**
 * Implementation of the ITelephony interface.
 */
public class PhoneInterfaceManagerEx extends ITelephonyEx.Stub {

    private static final String LOG_TAG = "PhoneInterfaceManagerEx";
    private static final boolean DBG = true;

    /** The singleton instance. */
    private static PhoneInterfaceManagerEx sInstance;

    PhoneGlobals mApp;
    Phone mPhone;

    MainThreadHandler mMainThreadHandler;

    // Query SIM phonebook Adn stroage info thread
    private QueryAdnInfoThread mAdnInfoThread = null;

    // SIM authenthication thread
    private SimAuth mSimAuthThread = null;

    /* SMS Center Address start*/
    private static final int CMD_HANDLE_GET_SCA = 11;
    private static final int CMD_GET_SCA_DONE = 12;
    private static final int CMD_HANDLE_SET_SCA = 13;
    private static final int CMD_SET_SCA_DONE = 14;
    /* SMS Center Address end*/

    // M: [LTE][Low Power][UL traffic shaping] Start
    private static final int CMD_SET_LTE_ACCESS_STRATUM_STATE = 35;
    private static final int EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE = 36;
    private static final int CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE = 37;
    private static final int EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE = 38;
    // M: [LTE][Low Power][UL traffic shaping] End

    private static final String[] PROPERTY_RIL_TEST_SIM = {
        "gsm.sim.ril.testsim",
        "gsm.sim.ril.testsim.2",
        "gsm.sim.ril.testsim.3",
        "gsm.sim.ril.testsim.4",
    };

    /**
     * Initialize the singleton PhoneInterfaceManagerEx instance.
     * This is only done once, at startup, from PhoneGlobals.onCreate().
     */
    /* package */
    public static PhoneInterfaceManagerEx init(PhoneGlobals app, Phone phone) {
        synchronized (PhoneInterfaceManagerEx.class) {
            if (sInstance == null) {
                sInstance = new PhoneInterfaceManagerEx(app, phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private PhoneInterfaceManagerEx(PhoneGlobals app, Phone phone) {
        mApp = app;
        mPhone = phone;
        mMainThreadHandler = new MainThreadHandler();
        publish();

        ExternalSimManager.getDefault(mPhone.getContext());
    }

    private void publish() {
        if (DBG) log("publish: " + this);

        ServiceManager.addService("phoneEx", this);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, "[PhoneIntfMgrEx] " + msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, "[PhoneIntfMgrEx] " + msg);
    }

    /**
     * A request object for use with {@link MainThreadHandler}. Requesters should wait() on the
     * request after sending. The main thread will notify the request when it is complete.
     */
    private static final class MainThreadRequest {
        /** The argument to use for the request */
        public Object argument;
        /** The result of the request that is run on the main thread */
        public Object result;
        public Object argument2;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }

        public MainThreadRequest(Object argument, Object argument2) {
            this.argument = argument;
            this.argument2 = argument2;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone process. Since many
     * of the Phone calls are not thread safe this is needed to shuttle the requests from the
     * inbound binder threads to the main thread in the phone process.  The Binder thread
     * may provide a {@link MainThreadRequest} object in the msg.obj field that they are waiting
     * on, which will be notified when the operation completes and will contain the result of the
     * request.
     *
     * <p>If a MainThreadRequest object is provided in the msg.obj field,
     * note that request.result must be set to something non-null for the calling thread to
     * unblock.
     */
    private final class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request;
            Message onCompleted;
            AsyncResult ar;
            int subId;
            int phoneId;

            switch (msg.what) {
                case CMD_HANDLE_GET_SCA:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(CMD_GET_SCA_DONE, request);

                    if (request.argument == null) {
                        // no argument, ignore
                        log("[sca get sc address but no argument");
                    } else {
                        subId = (Integer) request.argument;
                        getPhone(subId).getSmscAddress(onCompleted);
                    }
                    break;

                case CMD_GET_SCA_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;

                    Bundle result = new Bundle();
                    if (ar.exception == null && ar.result != null) {
                        log("[sca get result" + ar.result);
                        result.putByte(TelephonyManagerEx.GET_SC_ADDRESS_KEY_RESULT,
                                TelephonyManagerEx.ERROR_CODE_NO_ERROR);
                        result.putCharSequence(TelephonyManagerEx.GET_SC_ADDRESS_KEY_ADDRESS,
                                (String) ar.result);
                    } else {
                        log("[sca Fail to get sc address");
                        // Currently modem will return generic error without specific error cause,
                        // So we treat all exception as the same error cause.
                        result.putByte(TelephonyManagerEx.GET_SC_ADDRESS_KEY_RESULT,
                                TelephonyManagerEx.ERROR_CODE_GENERIC_ERROR);
                        result.putCharSequence(TelephonyManagerEx.GET_SC_ADDRESS_KEY_ADDRESS, "");
                    }
                    request.result = result;

                    synchronized (request) {
                        log("[sca notify sleep thread");
                        request.notifyAll();
                    }
                    break;

                case CMD_HANDLE_SET_SCA:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(CMD_SET_SCA_DONE, request);

                    ScAddress sca = (ScAddress) request.argument;
                    if (sca.mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        // invalid subscription ignore
                        log("[sca invalid subscription");
                    } else {
                        getPhone(sca.mSubId).setSmscAddress(sca.mAddress, onCompleted);
                    }
                    break;

                case CMD_SET_SCA_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception != null) {
                        Log.d(LOG_TAG, "[sca Fail: set sc address");
                        request.result = new Boolean(false);
                    } else {
                        Log.d(LOG_TAG, "[sca Done: set sc address");
                        request.result = new Boolean(true);
                    }

                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                // M: [LTE][Low Power][UL traffic shaping] Start
                case CMD_SET_LTE_ACCESS_STRATUM_STATE:
                    request = (MainThreadRequest) msg.obj;
                    boolean enabled = ((Boolean) request.argument).booleanValue();
                    phoneId = ((Integer) request.argument2).intValue();
                    if (DBG) {
                        log("CMD_SET_LTE_ACCESS_STRATUM_STATE: enabled " + enabled
                                + "phoneId " + phoneId);
                    }
                    mPhone = PhoneFactory.getPhone(phoneId);
                    if (mPhone == null) {
                        loge("setLteAccessStratumReport: No MainPhone");
                        request.result = new Boolean(false);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        PhoneBase phoneBase = (PhoneBase)(((PhoneProxy)mPhone).getActivePhone());
                        DcTrackerBase dcTracker = phoneBase.mDcTracker;
                        onCompleted = obtainMessage(EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE,
                                request);
                        dcTracker.onSetLteAccessStratumReport((Boolean) enabled, onCompleted);
                    }
                    break;

                case EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE:
                    if (DBG) log("EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE");
                    handleNullReturnEvent(msg, "setLteAccessStratumReport");
                    break;

                case CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE:
                    request = (MainThreadRequest) msg.obj;
                    int state = ((Integer) request.argument).intValue();
                    phoneId = ((Integer) request.argument2).intValue();
                    if (DBG) {
                        log("CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE: state " + state
                                + "phoneId " + phoneId);
                    }
                    mPhone = PhoneFactory.getPhone(phoneId);
                    if (mPhone == null) {
                        loge("setLteUplinkDataTransfer: No MainPhone");
                        request.result = new Boolean(false);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        PhoneBase phoneBase = (PhoneBase)(((PhoneProxy)mPhone).getActivePhone());
                        DcTrackerBase dcTracker = phoneBase.mDcTracker;
                        onCompleted = obtainMessage(EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE,
                                request);
                        dcTracker.onSetLteUplinkDataTransfer((Integer) state, onCompleted);
                    }
                    break;

                case EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE:
                    if (DBG) log("EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE");
                    handleNullReturnEvent(msg, "setLteUplinkDataTransfer");
                    break;
                // M: [LTE][Low Power][UL traffic shaping] End

                default:
                    break;
            }
        }

        private void handleNullReturnEvent(Message msg, String command) {
            AsyncResult ar = (AsyncResult) msg.obj;
            MainThreadRequest request = (MainThreadRequest) ar.userObj;
            if (ar.exception == null) {
                request.result = new Boolean(true);
            } else {
                request.result = new Boolean(false);
                if (ar.exception instanceof CommandException) {
                    loge(command + ": CommandException: " + ar.exception);
                } else {
                    loge(command + ": Unknown exception");
                }
            }
            synchronized (request) {
                request.notifyAll();
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see sendRequestAsync
     */
    private Object sendRequest(int command, Object argument) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

   /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see #sendRequestAsync
     */
    private Object sendRequest(int command, Object argument, Object argument2) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument, argument2);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

    private static Phone getPhone(int subId) {
        // FIXME: getPhone by subId
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return PhoneFactory.getPhone(
                ((phoneId < 0) ? SubscriptionManager.DEFAULT_PHONE_INDEX : phoneId));
    }

    private static Phone getPhoneUsingPhoneId(int phoneId) {
        return PhoneFactory.getPhone(phoneId);
    }

    private int getSubIdBySlot(int slot) {
        int [] subIds = SubscriptionManager.getSubId(slot);
        int subId = ((subIds == null) ? SubscriptionManager.getDefaultSubId() : subIds[0]);
        if (DBG) log("getSubIdBySlot, simId " + slot + "subId " + subId);
        return subId;
    }

    private class UnlockSim extends Thread {

        /* Query network lock start */

        // Verify network lock result.
        public static final int VERIFY_RESULT_PASS = 0;
        public static final int VERIFY_INCORRECT_PASSWORD = 1;
        public static final int VERIFY_RESULT_EXCEPTION = 2;

        // Total network lock count.
        public static final int NETWORK_LOCK_TOTAL_COUNT = 5;
        public static final String QUERY_SIMME_LOCK_RESULT = "com.mediatek.phone.QUERY_SIMME_LOCK_RESULT";
        public static final String SIMME_LOCK_LEFT_COUNT = "com.mediatek.phone.SIMME_LOCK_LEFT_COUNT";

        /* Query network lock end */


        private final IccCard mSimCard;

        private boolean mDone = false;
        private boolean mResult = false;

        // For replies from SimCard interface
        private Handler mHandler;

        private static final int QUERY_NETWORK_STATUS_COMPLETE = 100;
        private static final int SET_NETWORK_LOCK_COMPLETE = 101;

        private int mVerifyResult = -1;
        private int mSIMMELockRetryCount = -1;

        public UnlockSim(IccCard simCard) {
            mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (UnlockSim.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case QUERY_NETWORK_STATUS_COMPLETE:
                                synchronized (UnlockSim.this) {
                                    int [] LockState = (int []) ar.result;
                                    if (ar.exception != null) { //Query exception occurs
                                        log("Query network lock fail");
                                        mResult = false;
                                        mDone = true;
                                    } else {
                                        mSIMMELockRetryCount = LockState[2];
                                        log("[SIMQUERY] Category = " + LockState[0]
                                            + " ,Network status =" + LockState[1]
                                            + " ,Retry count = " + LockState[2]);

                                        mDone = true;
                                        mResult = true;
                                        UnlockSim.this.notifyAll();
                                    }
                                }
                                break;
                            case SET_NETWORK_LOCK_COMPLETE:
                                log("SUPPLY_NETWORK_LOCK_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    if ((ar.exception != null) &&
                                           (ar.exception instanceof CommandException)) {
                                        log("ar.exception " + ar.exception);
                                        if (((CommandException) ar.exception).getCommandError()
                                            == CommandException.Error.PASSWORD_INCORRECT) {
                                            mVerifyResult = VERIFY_INCORRECT_PASSWORD;
                                       } else {
                                            mVerifyResult = VERIFY_RESULT_EXCEPTION;
                                       }
                                    } else {
                                        mVerifyResult = VERIFY_RESULT_PASS;
                                    }
                                    mDone = true;
                                    UnlockSim.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                UnlockSim.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized Bundle queryNetworkLock(int category) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log("Enter queryNetworkLock");
            Message callback = Message.obtain(mHandler, QUERY_NETWORK_STATUS_COMPLETE);
            mSimCard.queryIccNetworkLock(category, callback);

            while (!mDone) {
                try {
                    log("wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(QUERY_SIMME_LOCK_RESULT, mResult);
            bundle.putInt(SIMME_LOCK_LEFT_COUNT, mSIMMELockRetryCount);

            log("done");
            return bundle;
        }

        synchronized int supplyNetworkLock(String strPasswd) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log("Enter supplyNetworkLock");
            Message callback = Message.obtain(mHandler, SET_NETWORK_LOCK_COMPLETE);
            mSimCard.supplyNetworkDepersonalization(strPasswd, callback);

            while (!mDone) {
                try {
                    log("wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }

            log("done");
            return mVerifyResult;
        }
    }

    public Bundle queryNetworkLock(int subId, int category) {
        final UnlockSim queryNetworkLockState;

        log("queryNetworkLock");

        queryNetworkLockState = new UnlockSim(getPhone(subId).getIccCard());
        queryNetworkLockState.start();

        return queryNetworkLockState.queryNetworkLock(category);
    }

    public int supplyNetworkDepersonalization(int subId, String strPasswd) {
        final UnlockSim supplyNetworkLock;

        log("supplyNetworkDepersonalization");

        supplyNetworkLock = new UnlockSim(getPhone(subId).getIccCard());
        supplyNetworkLock.start();

        return supplyNetworkLock.supplyNetworkLock(strPasswd);
    }

    /**
     * Modem SML change feature.
     * This function will query the SIM state of the given slot. And broadcast
     * ACTION_UNLOCK_SIM_LOCK if the SIM state is in network lock.
     *
     * @param subId: Indicate which sub to query
     * @param needIntent: The caller can deside to broadcast ACTION_UNLOCK_SIM_LOCK or not
     *                    in this time, because some APs will receive this intent (eg. Keyguard).
     *                    That can avoid this intent to effect other AP.
     */
    public void repollIccStateForNetworkLock(int subId, boolean needIntent) {
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            getPhone(subId).getIccCard().repollIccStateForModemSmlChangeFeatrue(needIntent);
        } else {
            log("Not Support in Single SIM.");
        }
    }

    private static class SetMsisdn extends Thread {
        private int mSubId;
        private Phone myPhone;
        private boolean mDone = false;
        private int mResult = 0;
        private Handler mHandler;

        private static final String DEFAULT_ALPHATAG = "Default Tag";
        private static final int CMD_SET_MSISDN_COMPLETE = 100;


        public SetMsisdn(Phone myP, int subId) {
            mSubId = subId;
            myPhone = myP;
        }


        @Override
        public void run() {
            Looper.prepare();
            synchronized (SetMsisdn.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case CMD_SET_MSISDN_COMPLETE:
                                synchronized (SetMsisdn.this) {
                                    if (ar.exception != null) { //Query exception occurs
                                        Log.e(LOG_TAG, "Set msisdn fail");
                                        mDone = true;
                                        mResult = 0;
                                    } else {
                                        Log.d(LOG_TAG, "Set msisdn success");
                                        mDone = true;
                                        mResult = 1;
                                    }
                                    SetMsisdn.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                SetMsisdn.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized int setLine1Number(String alphaTag, String number) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "Enter setLine1Number");
            Message callback = Message.obtain(mHandler, CMD_SET_MSISDN_COMPLETE);
            String myTag = alphaTag;

            myTag = myPhone.getLine1AlphaTag();

            if (myTag == null || myTag.equals("")) {
                myTag = DEFAULT_ALPHATAG;
            }

            Log.d(LOG_TAG, "sub = " + mSubId + ", Tag = " + myTag + " ,number = " + number);

            myPhone.setLine1Number(myTag, number, callback);


            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "done");
            return mResult;
        }
    }

    //@Override
    public int setLine1Number(int subId, String alphaTag, String number) {
        if (DBG) log("setLine1NumberUsingSubId, subId " + subId);
        if (number == null) {
            loge("number = null");
            return 0;
        }
        if (subId <= 0) {
            loge("Error subId: " + subId);
            return 0;
        }

        final SetMsisdn setMsisdn;

        setMsisdn = new SetMsisdn(getPhone(subId), subId);
        setMsisdn.start();

        return setMsisdn.setLine1Number(alphaTag, number);
    }

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    //@Override
    public boolean isFdnEnabled(int subId) {
        log("isFdnEnabled  subId=" + subId);

        if (subId <= 0) {
            loge("Error subId: " + subId);
            return false;
        }

        /* We will rollback the temporary solution after SubscriptionManager merge to L1 */
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getIccCard().getIccFdnEnabled();
        } else {
            return false;
        }
    }

    //@Override
    public String getIccCardType(int subId) {
        if (DBG) log("getIccCardType  subId=" + subId);

        Phone phone = getPhone(subId);
        if (phone == null) {
            if (DBG) log("getIccCardType(): phone is null");
            return "";
        }

        return phone.getIccCard().getIccCardType();
    }

    /**
     * Get SVLTE CardType by slot ID.
     *
     * @param slotId for card slot ID.
     * @return int card type index.
     */
    //@Override
    public int getSvlteCardType(int slotId) {
        if (DBG) {
            log("getSvlteCardType(): slotId=" + slotId);
        }
/*
        Phone phone = getPhone(slotId);
        if (phone == null) {
            if (DBG) {
                log("getSvlteCardType(): phone is null");
            }
            return 0;
        }

        return phone.getIccCard().getSvlteCardType();
*/

        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            UiccCard uiccCard = UiccController.getInstance().getUiccCard(slotId);
            if (uiccCard != null) {
                return uiccCard.getSvlteCardType();
            } else {
                if (DBG) {
                    log("getSvlteCardType(): uiccCard is null");
                }
                return 0;
            }
        } else {
            if (DBG) {
                log("getSvlteCardType(): Non CdmaLteDcSupport.");
            }
            return 0;
        }
    }

    //@Override
    public boolean isAppTypeSupported(int slotId, int appType) {
        if (DBG) log("isAppTypeSupported  slotId=" + slotId);

        UiccCard uiccCard = UiccController.getInstance().getUiccCard(slotId);
        if (uiccCard == null) {
            if (DBG) log("isAppTypeSupported(): uiccCard is null");
            return false;
        }

        return ((uiccCard.getApplicationByType(appType) == null) ?  false : true);
    }

    //@Override
    public boolean isTestIccCard(int slotId) {
        String mTestCard = null;

        mTestCard = SystemProperties.get(PROPERTY_RIL_TEST_SIM[slotId], "");
        if (DBG) log("isTestIccCard(): slot id =" + slotId + ", iccType = " + mTestCard);
        return (mTestCard != null && mTestCard.equals("1"));
    }

    /**
     * Gemini
     * Returns the alphabetic name of current registered operator.
     * <p>
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use {@link #getPhoneType()} to determine if
     * on a CDMA network).
     */
    @Deprecated
    public String getNetworkOperatorNameGemini(int slotId) {
        int subId = getSubIdBySlot(slotId);
        if (DBG) log("Deprecated! getNetworkOperatorNameGemini simId = " + slotId + " ,sub = " + subId);
        return getNetworkOperatorNameUsingSub(subId);
    }

    public String getNetworkOperatorNameUsingSub(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        String prop = TelephonyManager.getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ALPHA, "");
        if (DBG) log("getNetworkOperatorNameUsingSub sub = " + subId + " ,prop = " + prop);
        return prop;
    }

    /**
     * Gemini
     * Returns the numeric name (MCC+MNC) of current registered operator.
     * <p>
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use {@link #getPhoneType()} to determine if
     * on a CDMA network).
     */
    @Deprecated
    public String getNetworkOperatorGemini(int slotId) {
        int subId = getSubIdBySlot(slotId);
        if (DBG) log("Deprecated! getNetworkOperatorGemini simId = " + slotId + " ,sub = " + subId);
        return getNetworkOperatorUsingSub(subId);
    }

    public String getNetworkOperatorUsingSub(int subId) {
    	  int phoneId = SubscriptionManager.getPhoneId(subId);
        String prop = TelephonyManager.getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, "");
        if (DBG) log("getNetworkOperatorUsingSub sub = " + subId + " ,prop = " + prop);
        return prop;
    }

    /* BT SIM operation begin */
    public int btSimapConnectSIM(int simId,  BtSimapOperResponse btRsp) {
        Log.d(LOG_TAG, "btSimapConnectSIM, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
            Log.e(LOG_TAG, "btSimapDisconnectSIM btPhone is null");
            return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        sendBtSapTh.setBtOperResponse(btRsp);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
          sendBtSapTh.start();
        }
        int ret = sendBtSapTh.btSimapConnectSIM(simId);
        Log.d(LOG_TAG, "btSimapConnectSIM ret is " + ret + " btRsp.curType " + btRsp.getCurType()
         + " suptype " + btRsp.getSupportType() + " atr " + btRsp.getAtrString());
        return ret;
    }

    public int btSimapDisconnectSIM() {
        int simId = UiccController.getInstance().getBtConnectedSimId();
        Log.d(LOG_TAG, "btSimapDisconnectSIM, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
           Log.e(LOG_TAG, "btSimapDisconnectSIM btPhone is null");
           return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
           sendBtSapTh.start();
        }
        return sendBtSapTh.btSimapDisconnectSIM();
    }

    public int btSimapApduRequest(int type, String cmdAPDU,  BtSimapOperResponse btRsp) {
        int simId = UiccController.getInstance().getBtConnectedSimId();
        Log.d(LOG_TAG, "btSimapApduRequest, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
          Log.e(LOG_TAG, "btSimapApduRequest btPhone is null");
          return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        sendBtSapTh.setBtOperResponse(btRsp);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
           sendBtSapTh.start();
        }
        return sendBtSapTh.btSimapApduRequest(type, cmdAPDU);
    }

    public int btSimapResetSIM(int type,  BtSimapOperResponse btRsp) {
        int simId = UiccController.getInstance().getBtConnectedSimId();
        Log.d(LOG_TAG, "btSimapResetSIM, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
          Log.e(LOG_TAG, "btSimapResetSIM btPhone is null");
          return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        sendBtSapTh.setBtOperResponse(btRsp);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
           sendBtSapTh.start();
        }
        return sendBtSapTh.btSimapResetSIM(type);
    }

    public int btSimapPowerOnSIM(int type,  BtSimapOperResponse btRsp) {
        int simId = UiccController.getInstance().getBtConnectedSimId();
        Log.d(LOG_TAG, "btSimapPowerOnSIM, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
          Log.e(LOG_TAG, "btSimapPowerOnSIM btPhone is null");
          return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        sendBtSapTh.setBtOperResponse(btRsp);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
           sendBtSapTh.start();
        }
        return sendBtSapTh.btSimapPowerOnSIM(type);
    }

    public int btSimapPowerOffSIM() {
        int simId = UiccController.getInstance().getBtConnectedSimId();
        Log.d(LOG_TAG, "btSimapPowerOffSIM, simId " + simId);
        Phone btPhone = getPhoneUsingPhoneId(simId);
        if (btPhone == null) {
           Log.e(LOG_TAG, "btSimapPowerOffSIM btPhone is null");
           return -1;
        }
        final SendBtSimapProfile sendBtSapTh = SendBtSimapProfile.getInstance(btPhone);
        if (sendBtSapTh.getState() == Thread.State.NEW) {
            sendBtSapTh.start();
        }
        return sendBtSapTh.btSimapPowerOffSIM();
    }

    private static class SendBtSimapProfile extends Thread {
        private Phone mBtSapPhone;
        private boolean mDone = false;
        private String mStrResult = null;
        private ArrayList mResult;
        private int mRet = 1;
        private BtSimapOperResponse mBtRsp;
        private Handler mHandler;

        private static SendBtSimapProfile sInstance;
        static final Object sInstSync = new Object();
        // For async handler to identify request type
        private static final int BTSAP_CONNECT_COMPLETE = 300;
        private static final int BTSAP_DISCONNECT_COMPLETE = 301;
        private static final int BTSAP_POWERON_COMPLETE = 302;
        private static final int BTSAP_POWEROFF_COMPLETE = 303;
        private static final int BTSAP_RESETSIM_COMPLETE = 304;
        private static final int BTSAP_TRANSFER_APDU_COMPLETE = 305;

        public static SendBtSimapProfile getInstance(Phone phone) {
            synchronized (sInstSync) {
                if (sInstance == null) {
                    sInstance = new SendBtSimapProfile(phone);
                }
            }
            return sInstance;
        }
        private SendBtSimapProfile(Phone phone) {
            mBtSapPhone = phone;
            mBtRsp = null;
        }


        public void setBtOperResponse(BtSimapOperResponse btRsp) {
            mBtRsp = btRsp;
        }

        private Phone getPhone(int subId) {
            // FIXME: getPhone by subId
            return null;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (SendBtSimapProfile.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case BTSAP_CONNECT_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_CONNECT_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }
                                        Log.e(LOG_TAG, "Exception BTSAP_CONNECT, Exception:" + ar.exception);
                                    } else {
                                        mStrResult = (String) (ar.result);
                                        Log.d(LOG_TAG, "BTSAP_CONNECT_COMPLETE  mStrResult " + mStrResult);
                                        String[] splited = mStrResult.split(",");

                                        try {
                                            mBtRsp.setCurType(Integer.parseInt(splited[0].trim()));
                                            mBtRsp.setSupportType(Integer.parseInt(splited[1].trim()));
                                            mBtRsp.setAtrString(splited[2]);
                                            Log.d(LOG_TAG, "BTSAP_CONNECT_COMPLETE curType " + mBtRsp.getCurType() + " SupType " + mBtRsp.getSupportType() + " ATR " + mBtRsp.getAtrString());
                                        } catch (NumberFormatException e) {
                                            Log.e(LOG_TAG, "NumberFormatException");
                                        }

                                        mRet = 0;
                                        //log("BTSAP_CONNECT_COMPLETE curType " + (String)(mResult.get(0)) + " SupType " + (String)(mResult.get(1)) + " ATR " + (String)(mResult.get(2)));
                                    }

                                    //log("BTSAP_CONNECT_COMPLETE curType " + mBtRsp.getCurType() + " SupType " + mBtRsp.getSupportType() + " ATR " + mBtRsp.getAtrString());
                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                            case BTSAP_DISCONNECT_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_DISCONNECT_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }
                                        Log.e(LOG_TAG, "Exception BTSAP_DISCONNECT, Exception:" + ar.exception);
                                    } else {
                                        mRet = 0;
                                    }
                                    Log.d(LOG_TAG, "BTSAP_DISCONNECT_COMPLETE result is " + mRet);
                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                            case BTSAP_POWERON_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_POWERON_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }
                                        loge("Exception POWERON_COMPLETE, Exception:" + ar.exception);
                                    } else {
                                        mStrResult = (String) (ar.result);
                                        Log.d(LOG_TAG, "BTSAP_POWERON_COMPLETE  mStrResult " + mStrResult);
                                        String[] splited = mStrResult.split(",");

                                        try {
                                            mBtRsp.setCurType(Integer.parseInt(splited[0].trim()));
                                            mBtRsp.setAtrString(splited[1]);
                                            Log.d(LOG_TAG, "BTSAP_POWERON_COMPLETE curType " + mBtRsp.getCurType() + " ATR " + mBtRsp.getAtrString());
                                        } catch (NumberFormatException e) {
                                            Log.e(LOG_TAG, "NumberFormatException");
                                        }
                                        mRet = 0;
                                    }

                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                            case BTSAP_POWEROFF_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_POWEROFF_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }
                                        Log.e(LOG_TAG, "Exception BTSAP_POWEROFF, Exception:" + ar.exception);
                                    } else {
                                        mRet = 0;
                                    }
                                    Log.d(LOG_TAG, "BTSAP_POWEROFF_COMPLETE result is " + mRet);
                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                            case BTSAP_RESETSIM_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_RESETSIM_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }
                                        loge("Exception BTSAP_RESETSIM, Exception:" + ar.exception);
                                    } else {
                                        mStrResult = (String) (ar.result);
                                        Log.d(LOG_TAG, "BTSAP_RESETSIM_COMPLETE  mStrResult " + mStrResult);
                                        String[] splited = mStrResult.split(",");

                                        try {
                                            mBtRsp.setCurType(Integer.parseInt(splited[0].trim()));
                                            mBtRsp.setAtrString(splited[1]);
                                            Log.d(LOG_TAG, "BTSAP_RESETSIM_COMPLETE curType " + mBtRsp.getCurType() + " ATR " + mBtRsp.getAtrString());
                                        } catch (NumberFormatException e) {
                                            Log.e(LOG_TAG, "NumberFormatException");
                                        }
                                        mRet = 0;
                                    }

                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                            case BTSAP_TRANSFER_APDU_COMPLETE:
                                Log.d(LOG_TAG, "BTSAP_TRANSFER_APDU_COMPLETE");
                                synchronized (SendBtSimapProfile.this) {
                                    if (ar.exception != null) {
                                        CommandException ce = (CommandException) ar.exception;
                                        if (ce.getCommandError() == CommandException.Error.BT_SAP_CARD_REMOVED) {
                                            mRet = 4;
                                        } else if (ce.getCommandError() == CommandException.Error.BT_SAP_NOT_ACCESSIBLE) {
                                            mRet = 2;
                                        } else {
                                            mRet = 1;
                                        }

                                        Log.e(LOG_TAG, "Exception BTSAP_TRANSFER_APDU, Exception:" + ar.exception);
                                    } else {
                                        mBtRsp.setApduString((String) (ar.result));
                                        Log.d(LOG_TAG, "BTSAP_TRANSFER_APDU_COMPLETE result is " + mBtRsp.getApduString());
                                        mRet = 0;
                                    }

                                    mDone = true;
                                    SendBtSimapProfile.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                SendBtSimapProfile.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized int btSimapConnectSIM(int simId) {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_CONNECT_COMPLETE);
            mBtSapPhone.sendBtSimProfile(0, 0, null, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "done");
            if (mRet == 0) {
                // parse result
                UiccController.getInstance().setBtConnectedSimId(simId);
                Log.d(LOG_TAG, "synchronized btSimapConnectSIM connect Sim is "
                            + UiccController.getInstance().getBtConnectedSimId());
                Log.d(LOG_TAG, "btSimapConnectSIM curType " + mBtRsp.getCurType() + " SupType "
                        + mBtRsp.getSupportType() + " ATR " + mBtRsp.getAtrString());
            } else {
                ret = mRet;
            }

            Log.d(LOG_TAG, "synchronized btSimapConnectSIM ret " + ret);
            return ret;
        }

        synchronized int btSimapDisconnectSIM() {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "synchronized btSimapDisconnectSIM");
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_DISCONNECT_COMPLETE);
            final int slotId = UiccController.getInstance().getBtConnectedSimId();
            // TODO: Wait for GeminiUtils ready
            /*
            if (!GeminiUtils.isValidSlot(slotId)) {
                ret = 7; // No sim has been connected
                return ret;
            }
            */
            mBtSapPhone.sendBtSimProfile(1, 0, null, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            if (mRet == 0) {
                UiccController.getInstance().setBtConnectedSimId(-1);
            }
            ret = mRet;
            Log.d(LOG_TAG, "synchronized btSimapDisconnectSIM ret " + ret);
            return ret;
        }

        synchronized int btSimapResetSIM(int type) {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_RESETSIM_COMPLETE);

            final int slotId = UiccController.getInstance().getBtConnectedSimId();
            // TODO: Wait for GeminiUtils ready
            /*
            if (!GeminiUtils.isValidSlot(slotId)) {
                ret = 7; // No sim has been connected
                return ret;
            }
            */
            mBtSapPhone.sendBtSimProfile(4, type, null, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            if (mRet == 0)  {
                Log.d(LOG_TAG, "btSimapResetSIM curType " + mBtRsp.getCurType() + " ATR " + mBtRsp.getAtrString());
            } else {
                ret = mRet;
            }

            Log.d(LOG_TAG, "synchronized btSimapResetSIM ret " + ret);
            return ret;
        }

        synchronized int btSimapPowerOnSIM(int type)  {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_POWERON_COMPLETE);

            final int slotId = UiccController.getInstance().getBtConnectedSimId();
            // TODO: Wait for GeminiUtils ready
            /*
            if (!GeminiUtils.isValidSlot(slotId)) {
                ret = 7; // No sim has been connected
                return ret;
            }
            */
            mBtSapPhone.sendBtSimProfile(2, type, null, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            if (mRet == 0)  {
                Log.d(LOG_TAG, "btSimapPowerOnSIM curType " + mBtRsp.getCurType() + " ATR " + mBtRsp.getAtrString());
            } else {
            ret = mRet;
            }
            Log.d(LOG_TAG, "synchronized btSimapPowerOnSIM ret " + ret);
            return ret;
        }

        synchronized int btSimapPowerOffSIM() {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_POWEROFF_COMPLETE);

            final int slotId = UiccController.getInstance().getBtConnectedSimId();
            // TODO: Wait for GeminiUtils ready
            /*
            if (!GeminiUtils.isValidSlot(slotId)) {
                ret = 7; // No sim has been connected
                return ret;
            }
            */
            mBtSapPhone.sendBtSimProfile(3, 0, null, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            ret = mRet;
            Log.d(LOG_TAG, "synchronized btSimapPowerOffSIM ret " + ret);
            return ret;
        }

        synchronized int btSimapApduRequest(int type, String cmdAPDU) {
            int ret = 0;
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mDone = false;
            Message callback = Message.obtain(mHandler, BTSAP_TRANSFER_APDU_COMPLETE);

            final int slotId = UiccController.getInstance().getBtConnectedSimId();
            // TODO: Wait for GeminiUtils ready
            /*
            if (!GeminiUtils.isValidSlot(slotId)) {
                ret = 7; // No sim has been connected
                return ret;
            }
            */
            Log.d(LOG_TAG, "btSimapApduRequest start " + type + ", mBtSapPhone " + mBtSapPhone);
            mBtSapPhone.sendBtSimProfile(5, type, cmdAPDU, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            if (mRet == 0)  {
                Log.d(LOG_TAG, "btSimapApduRequest APDU " + mBtRsp.getApduString());
            } else {
                ret = mRet;
            }

            Log.d(LOG_TAG, "synchronized btSimapApduRequest ret " + ret);
            return ret;
        }
    }
    /* BT SIM operation end */

    // MVNO-API START
    public String getMvnoMatchType(int subId) {
        String type = getPhone(subId).getMvnoMatchType();
        if (DBG) log("getMvnoMatchTypeUsingSub sub = " + subId + " ,vMailAlphaTag = " + type);
        return type;
    }

    public String getMvnoPattern(int subId, String type) {
        String pattern = getPhone(subId).getMvnoPattern(type);
        if (DBG) log("getMvnoPatternUsingSub sub = " + subId + " ,vMailAlphaTag = " + pattern);
        return pattern;
    }
    // MVNO-API END

    /**
     * Make sure the caller has the READ_PRIVILEGED_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforcePrivilegedPhoneStatePermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
                null);
    }

    /**
     * Request to run AKA authenitcation on UICC card by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteRand random challenge in byte array
     * @param byteAutn authenication token in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simAkaAuthentication(int slotId, int family, byte[] byteRand, byte[] byteAutn) {
        enforcePrivilegedPhoneStatePermission();

        if (mSimAuthThread == null) {
            log("simAkaAuthentication new thread");
            mSimAuthThread = new SimAuth(mPhone);
            mSimAuthThread.start();
        } else {
            log("simAkaAuthentication thread has been created.");
        }

        String strRand = "";
        String strAutn = "";
        log("simAkaAuthentication session is " + family + " simId " + slotId);

        if (byteRand != null && byteRand.length > 0) {
            strRand = IccUtils.bytesToHexString(byteRand).substring(0, byteRand.length * 2);
        }

        if (byteAutn != null && byteAutn.length > 0) {
            strAutn = IccUtils.bytesToHexString(byteAutn).substring(0, byteAutn.length * 2);
        }
        log("simAkaAuthentication strRand is " + strRand + " strAutn " + strAutn);

        return mSimAuthThread.doGeneralSimAuth(slotId, family, 0, 0, strRand, strAutn);
    }

    /**
     * Request to run GBA authenitcation (Bootstrapping Mode)on UICC card
     * by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteRand random challenge in byte array
     * @param byteAutn authenication token in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simGbaAuthBootStrapMode(int slotId, int family, byte[] byteRand, byte[] byteAutn) {
        enforcePrivilegedPhoneStatePermission();

        if (mSimAuthThread == null) {
            log("simGbaAuthBootStrapMode new thread");
            mSimAuthThread = new SimAuth(mPhone);
            mSimAuthThread.start();
        } else {
            log("simGbaAuthBootStrapMode thread has been created.");
        }

        String strRand = "";
        String strAutn = "";
        log("simGbaAuthBootStrapMode session is " + family + " simId " + slotId);

        if (byteRand != null && byteRand.length > 0) {
            strRand = IccUtils.bytesToHexString(byteRand).substring(0, byteRand.length * 2);
        }

        if (byteAutn != null && byteAutn.length > 0) {
            strAutn = IccUtils.bytesToHexString(byteAutn).substring(0, byteAutn.length * 2);
        }
        log("simGbaAuthBootStrapMode strRand is " + strRand + " strAutn " + strAutn);

        return mSimAuthThread.doGeneralSimAuth(slotId, family, 1, 0xDD, strRand, strAutn);
    }

    /**
     * Request to run GBA authenitcation (NAF Derivation Mode)on UICC card
     * by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteNafId network application function id in byte array
     * @param byteImpi IMS private user identity in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simGbaAuthNafMode(int slotId, int family, byte[] byteNafId, byte[] byteImpi) {
        enforcePrivilegedPhoneStatePermission();

        if (mSimAuthThread == null) {
            log("simGbaAuthNafMode new thread");
            mSimAuthThread = new SimAuth(mPhone);
            mSimAuthThread.start();
        } else {
            log("simGbaAuthNafMode thread has been created.");
        }

        String strNafId = "";
        String strImpi = "";
        log("simGbaAuthNafMode session is " + family + " simId " + slotId);

        if (byteNafId != null && byteNafId.length > 0) {
            strNafId = IccUtils.bytesToHexString(byteNafId).substring(0, byteNafId.length * 2);
        }

        /* ISIM GBA NAF mode parameter should be NAF_ID.
         * USIM GAB NAF mode parameter should be NAF_ID + IMPI
         * If getIccApplicationChannel got 0, mean that ISIM not support */
        if (UiccController.getInstance().getIccApplicationChannel(slotId, family) == 0) {
            log("simGbaAuthNafMode ISIM not support.");
            if (byteImpi != null && byteImpi.length > 0) {
                strImpi = IccUtils.bytesToHexString(byteImpi).substring(0, byteImpi.length * 2);
            }
        }
        log("simGbaAuthNafMode NAF ID is " + strNafId + " IMPI " + strImpi);

        return mSimAuthThread.doGeneralSimAuth(slotId, family, 1, 0xDE, strNafId, strImpi);
    }

    /**
     * Since MTK keyguard has dismiss feature, we need to retrigger unlock event
     * when user try to access the SIM card.
     *
     * @param subId inidicated subscription
     *
     * @return true represent broadcast a unlock intent to notify keyguard
     *         false represent current state is not LOCKED state. No need to retrigger.
     *
     */
    public boolean broadcastIccUnlockIntent(int subId) {
        int state = TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotId(subId));

        log("[broadcastIccUnlockIntent] subId:" + subId + " state: " + state);

        String lockedReasion = "";

        switch (state) {
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN;
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK;
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                switch (getPhone(subId).getIccCard().getNetworkPersoType()) {
                    case PERSOSUBSTATE_SIM_NETWORK:
                        lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_NETWORK;
                        break;
                    case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                        lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_NETWORK_SUBSET;
                        break;
                    case PERSOSUBSTATE_SIM_CORPORATE:
                        lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_CORPORATE;
                        break;
                    case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                        lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_SERVICE_PROVIDER;
                        break;
                    case PERSOSUBSTATE_SIM_SIM:
                        lockedReasion = IccCardConstants.INTENT_VALUE_LOCKED_SIM;
                        break;
                    default:
                        lockedReasion = IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
                }
                break;
            default:
                return false;
        }

        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);//ACTION_UNLOCK_SIM_LOCK);

        intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                         IccCardConstants.INTENT_VALUE_ICC_LOCKED);
        intent.putExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON, lockedReasion);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, SubscriptionManager.getPhoneId(subId));
        log("[broadcastIccUnlockIntent] Broadcasting intent ACTION_UNLOCK_SIM_LOCK "
            + " reason " + state + " for slotId : " + SubscriptionManager.getSlotId(subId));

        mApp.sendBroadcastAsUser(intent, UserHandle.ALL);

        return true;
    }

    /**
     * Query if the radio is turned off by user.
     *
     * @param subId inidicated subscription
     *
     * @return true radio is turned off by user.
     *         false radio isn't turned off by user.
     *
     */
    public boolean isRadioOffBySimManagement(int subId) {
        boolean result = true;
        try {
            Context otherAppsContext = mApp.createPackageContext(
                    "com.android.phone", Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences mIccidPreference =
                    otherAppsContext.getSharedPreferences("RADIO_STATUS", 0);

            Context context = mPhone.getContext();
            SubscriptionInfo subInfo =
                    SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
            if ((subInfo != null) && (mIccidPreference != null)) {
                log("[isRadioOffBySimManagement]SharedPreferences: "
                        + mIccidPreference.getAll().size() + ", IccId: " + subInfo.getIccId());
                result = mIccidPreference.contains(subInfo.getIccId());
            }
            log("[isRadioOffBySimManagement]result: " + result);
        } catch (NameNotFoundException e) {
            log("Fail to create com.android.phone createPackageContext");
        }
        return result;
    }

    // SIM switch
    /**
     * Get current phone capability
     *
     * @return the capability of phone. (@see PhoneConstants)
     */
    public int getPhoneCapability(int phoneId) {
        //return PhoneConstants.CAPABILITY_34G;
        return 0;
    }

    /**
     * Set capability to phones
     *
     * @param phoneId phones want to change capability
     * @param capability new capability for each phone
     */
    public void setPhoneCapability(int[] phoneId, int[] capability) {

    }

    /**
     * To config SIM swap mode(for dsda).
     *
     * @return true if config SIM Swap mode successful, or return false
     */
    public boolean configSimSwap(boolean toSwapped) {
        return true;
    }

    /**
     * To check SIM is swapped or not(for dsda).
     *
     * @return true if swapped, or return false
     */
    public boolean isSimSwapped() {
        return false;
    }

    /**
     * To Check if Capability Switch Manual Control Mode Enabled.
     *
     * @return true if Capability Switch manual control mode is enabled, else false;
     */
    public boolean isCapSwitchManualEnabled() {
        return true;
    }

    /**
     * Get item list that will be displayed on manual switch setting
     *
     * @return String[] contains items
     */
    public String[] getCapSwitchManualList() {
        return null;
    }

    public String getDefaultLocatedPlmn() { 
       return PhoneFactory.getDefaultPhone().getLocatedPlmn(); 
    } 


  /**
     * To get located PLMN from sepcified SIM modem  protocol
     * Returns current located PLMN string(ex: "46000") or null if not availble (ex: in flight mode or no signal area or this SIM is turned off)
     * @param subId Indicate which SIM subscription to query
     */
    public String getLocatedPlmn(int subId) {
        return getPhone(subId).getLocatedPlmn();
    }

   /**
     * Check if phone is hiding network temporary out of service state.
     * @param subId Indicate which SIM subscription to query
     * @return if phone is hiding network temporary out of service state.
    */
    public int getNetworkHideState(int subId) {
        return getPhone(subId).getNetworkHideState();
    }

   /**
     * Get the network service state for specified SIM.
     * @param subId Indicate which SIM subscription to query
     * @return service state.
     */
    public Bundle getServiceState(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            Bundle data = new Bundle();
            phone.getServiceState().fillInNotifierBundle(data);
            return data;
        } else {
            log("Can't not get phone");
            return null;
        }
    }

    /**
     * Helper thread to turn async call to {@link #SimAuthentication} into
     * a synchronous one.
     */
    private static class SimAuth extends Thread {
        private Phone mTargetPhone;
        private boolean mDone = false;
        private IccIoResult mResponse = null;

        // For replies from SimCard interface
        private Handler mHandler;

        // For async handler to identify request type
        private static final int SIM_AUTH_GENERAL_COMPLETE = 300;

        public SimAuth(Phone phone) {
            mTargetPhone = phone;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (SimAuth.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case SIM_AUTH_GENERAL_COMPLETE:
                                log("SIM_AUTH_GENERAL_COMPLETE");
                                synchronized (SimAuth.this) {
                                    if (ar.exception != null) {
                                        log("SIM Auth Fail");
                                        mResponse = (IccIoResult) (ar.result);
                                    } else {
                                        mResponse = (IccIoResult) (ar.result);
                                    }
                                    log("SIM_AUTH_GENERAL_COMPLETE result is " + mResponse);
                                    mDone = true;
                                    SimAuth.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                SimAuth.this.notifyAll();
            }
            Looper.loop();
        }

        byte[] doGeneralSimAuth(int slotId, int family, int mode, int tag,
                String strRand, String strAutn) {
           synchronized (SimAuth.this) {
                while (mHandler == null) {
                    try {
                        SimAuth.this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                mDone = false;
                mResponse = null;

                Message callback = Message.obtain(mHandler, SIM_AUTH_GENERAL_COMPLETE);

                int sessionId = UiccController.getInstance().getIccApplicationChannel(slotId, family);
                log("family = " + family + ", sessionId = " + sessionId);

                int[] subId = SubscriptionManager.getSubId(slotId);
                if (subId == null) {
                    log("slotId = " + slotId + ", subId is invalid.");
                    return null;
                } else {
                    getPhone(subId[0]).doGeneralSimAuthentication(sessionId, mode, tag, strRand, strAutn, callback);
                }

                while (!mDone) {
                    try {
                        log("wait for done");
                        SimAuth.this.wait();
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
                int len = 0;
                byte[] result = null;

                if (mResponse != null) {
                    // 2 bytes for sw1 and sw2
                    len = 2 + ((mResponse.payload == null) ? 0 : mResponse.payload.length);
                    result = new byte[len];

                    if (mResponse.payload != null) {
                        System.arraycopy(mResponse.payload, 0, result, 0, mResponse.payload.length);
                    }

                    result[len - 1] = (byte) mResponse.sw2;
                    result[len - 2] = (byte) mResponse.sw1;

                    // TODO: Should use IccUtils.bytesToHexString to print log info.
                    //for (int i = 0; i < len ; i++) {
                    //    log("Result = " + result[i]);
                    //}
                    //log("Result = " + new String(result));
                } else {
                    log("mResponse is null.");
                }

                log("done");
                return result;
            }
        }
    }

   /**
    * This function is used to get SIM phonebook storage information
    * by sim id.
    *
    * @param simId Indicate which sim(slot) to query
    * @return int[] which incated the storage info
    *         int[0]; // # of remaining entries
    *         int[1]; // # of total entries
    *         int[2]; // # max length of number
    *         int[3]; // # max length of alpha id
    *
    */
    public int[] getAdnStorageInfo(int subId) {
        Log.d(LOG_TAG, "getAdnStorageInfo " + subId);

        if (SubscriptionManager.isValidSubscriptionId(subId) == true) {
            if (mAdnInfoThread == null) {
                Log.d(LOG_TAG, "getAdnStorageInfo new thread ");
                mAdnInfoThread  = new QueryAdnInfoThread(subId);
                mAdnInfoThread.start();
            } else {
                mAdnInfoThread.setSubId(subId);
                Log.d(LOG_TAG, "getAdnStorageInfo old thread ");
            }
            return mAdnInfoThread.GetAdnStorageInfo();
        } else {
            Log.d(LOG_TAG, "getAdnStorageInfo subId is invalid.");
            int[] recordSize;
            recordSize = new int[4];
            recordSize[0] = 0; // # of remaining entries
            recordSize[1] = 0; // # of total entries
            recordSize[2] = 0; // # max length of number
            recordSize[3] = 0; // # max length of alpha id
            return recordSize;
        }
    }

    private static class QueryAdnInfoThread extends Thread {

        private int mSubId;
        private boolean mDone = false;
        private int[] recordSize;

        private Handler mHandler;

        // For async handler to identify request type
        private static final int EVENT_QUERY_PHB_ADN_INFO = 100;

        public QueryAdnInfoThread(int subId) {
            mSubId = subId;
        }
        public void setSubId(int subId) {
            mSubId = subId;
            mDone = false;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (QueryAdnInfoThread.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;

                        switch (msg.what) {
                            case EVENT_QUERY_PHB_ADN_INFO:
                                Log.d(LOG_TAG, "EVENT_QUERY_PHB_ADN_INFO");
                                synchronized (QueryAdnInfoThread.this) {
                                    mDone = true;
                                    int[] info = (int[]) (ar.result);
                                    if (info != null) {
                                        recordSize = new int[4];
                                        recordSize[0] = info[0]; // # of remaining entries
                                        recordSize[1] = info[1]; // # of total entries
                                        recordSize[2] = info[2]; // # max length of number
                                        recordSize[3] = info[3]; // # max length of alpha id
                                        Log.d(LOG_TAG, "recordSize[0]=" + recordSize[0] + ",recordSize[1]=" + recordSize[1] +
                                                         "recordSize[2]=" + recordSize[2] + ",recordSize[3]=" + recordSize[3]);
                                    }
                                    else {
                                        recordSize = new int[4];
                                        recordSize[0] = 0; // # of remaining entries
                                        recordSize[1] = 0; // # of total entries
                                        recordSize[2] = 0; // # max length of number
                                        recordSize[3] = 0; // # max length of alpha id
                                    }
                                    QueryAdnInfoThread.this.notifyAll();

                                }
                                break;
                            }
                      }
                };
                QueryAdnInfoThread.this.notifyAll();
            }
            Looper.loop();
        }

        public int[] GetAdnStorageInfo() {
            synchronized (QueryAdnInfoThread.this) {
                while (mHandler == null) {
                    try {
                        QueryAdnInfoThread.this.wait();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Message response = Message.obtain(mHandler, EVENT_QUERY_PHB_ADN_INFO);

                getPhone(mSubId).queryPhbStorageInfo(RILConstants.PHB_ADN, response);

                while (!mDone) {
                    try {
                        Log.d(LOG_TAG, "wait for done");
                        QueryAdnInfoThread.this.wait();
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
                Log.d(LOG_TAG, "done");
                return recordSize;
            }
        }
    }

   /**
    * This function is used to check if the SIM phonebook is ready
    * by sim id.
    *
    * @param simId Indicate which sim(slot) to query
    * @return true if phone book is ready.
    *
    */
    public boolean isPhbReady(int subId) {
        String strPhbReady = "false";
        String strAllSimState = "";
        String strCurSimState = "";
        boolean isSimLocked = false;
        int phoneId = SubscriptionManager.getPhoneId(subId);
        int slotId = SubscriptionManager.getSlotId(subId);

        if (SubscriptionManager.isValidSlotId(slotId) == true) {
            strAllSimState = SystemProperties.get(TelephonyProperties.PROPERTY_SIM_STATE);

            if ((strAllSimState != null) && (strAllSimState.length() > 0)) {
                String values[] = strAllSimState.split(",");
                if ((phoneId >= 0) && (phoneId < values.length) && (values[phoneId] != null)) {
                    strCurSimState = values[phoneId];
                }
            }

            isSimLocked = (strCurSimState.equals("NETWORK_LOCKED") || strCurSimState.equals("PIN_REQUIRED")); //In PUK_REQUIRED state, phb can be accessed.

            if (PhoneConstants.SIM_ID_2 == slotId) {
                strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.2", "false");
            } else if (PhoneConstants.SIM_ID_3 == slotId) {
                strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.3", "false");
            } else if (PhoneConstants.SIM_ID_4 == slotId) {
                strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.4", "false");
            } else {
                strPhbReady = SystemProperties.get("gsm.sim.ril.phbready", "false");
            }
        }

        log("[isPhbReady] subId:" + subId + ", slotId: " + slotId + ", isPhbReady: " + strPhbReady + ",strSimState: " + strAllSimState);

        return (strPhbReady.equals("true") && !isSimLocked);
    }

    public boolean isAirplanemodeAvailableNow() {
        return mApp.isAllowAirplaneModeChange();
    }

    // SMS parts
    private class ScAddress {
        public String mAddress;
        public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public ScAddress(int subId, String addr) {
            mAddress = addr;
            mSubId = subId;
        }
    }

    /**
     * Get service center address
     *
     * @param subId subscription identity
     *
     * @return service message center address
     */
    public Bundle getScAddressUsingSubId(int subId) {
        log("getScAddressUsingSubId, subId: " + subId);

        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            log("no corresponding phone id");
            return null;
        }

        Bundle result = (Bundle) sendRequest(CMD_HANDLE_GET_SCA, subId);

        log("getScAddressUsingSubId: exit with " + result.toString());

        return result;
    }

    /**
     * Set service message center address
     *
     * @param subId subscription identity
     * @param address service message center addressto be set
     *
     * @return true for success, false for failure
     */
    public boolean setScAddressUsingSubId(int subId, String address) {
        log("setScAddressUsingSubId, subId: " + subId);

        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            log("no corresponding phone id");
            return false;
        }

        ScAddress scAddress = new ScAddress(subId, address);

        Boolean result = (Boolean) sendRequest(CMD_HANDLE_SET_SCA, scAddress);

        log("setScAddressUsingSubId: exit with " + result.booleanValue());
        return result.booleanValue();
    }
    // SMS part end

    // VOLTE
    /**
    * This function will check if the input cid is dedicate bearer.
    *
    * @param cid for checking is dedicate bearer or not
    * @param phoneId for getting the current using phone
    * @return boolean return dedicated bearer or not
    *                true: is a dedicated bearer for input cid
    *                false: not a dedicated bearer for input cid
    */
    public boolean isDedicateBearer(int cid, int phoneId) {
        return PhoneFactory.getPhone(phoneId).isDedicateBearer(cid);
    }

    /**
    * This function will disable Dedicate bearer.
    * @param reason for indicating what reason for disabling dedicate bearer
    * @param ddcid for indicating which dedicate beare cide need to be disable
    * @param phoneId for getting the current using phone
    * @return int return ddcid of disable dedicated bearer
    *            -1: some thing wrong
    */
    public int disableDedicateBearer(String reason, int ddcid, int phoneId) {
        return PhoneFactory.getPhone(phoneId).disableDedicateBearer(reason, ddcid);
    }

    /**
    * This function will enable Dedicate bearer.
    * <p>
    * @param apnType input apnType for enable dedicate bearer
    * @param signalingFlag boolean value for indicating signaling or not
    * @param qosStatus input qosStatus info
    * @param tftStatus input tftStatus info
    * @param phoneId for getting the current using phone
    * @return int return ddcid of enable dedicated bearer
    *            -1: some thing wrong
    */
    public int enableDedicateBearer(String apnType, boolean signalingFlag, QosStatus qosStatus,
                            TftStatus tftStatus, int phoneId) {
        return PhoneFactory.getPhone(phoneId).enableDedicateBearer(apnType, signalingFlag
                                , qosStatus, tftStatus);
    }

    /**
    * This function will abort Dedicate bearer.
    * @param reason for indicating what reason for abort enable dedicate bearer
    * @param ddcid for indicating which dedicate beare cide need to be abort
    * @param phoneId for getting the current using phone
    * @return int return ddcid of abort dedicated bearer
    *            -1: some thing wrong
    */
    public int abortEnableDedicateBearer(String reason, int ddcid, int phoneId) {
        return PhoneFactory.getPhone(phoneId).abortEnableDedicateBearer(reason, ddcid);
    }

    /**
     * This function will modify Dedicate bearer.
     *
     * @param cid for indicating which dedicate cid to modify
     * @param qosStatus input qosStatus for modify
     * @param tftStatus input tftStatus for modify
     * @param phoneId for getting the current using phone
     * @return int: return ddcid of modify dedicated bearer
     *            -1: some thing wrong
     */
    public int modifyDedicateBearer(int cid, QosStatus qosStatus, TftStatus tftStatus
                        , int phoneId) {
        return PhoneFactory.getPhone(phoneId).modifyDedicateBearer(cid, qosStatus, tftStatus);
    }

    /**
     * This function will set Default Bearer Config for apnContext.
     *
     * @param apnType for indicating which apnType to set default bearer config
     * @param defaultBearerConfig config of default bearer config to be set
     * @param phoneId for getting the current using phone
     * @return int: return success or not
     *            0: set default bearer config successfully
     */
    public int setDefaultBearerConfig(String apnType, DefaultBearerConfig defaultBearerConfig
                        , int phoneId) {
        log("setDefaultBearerConfig: apnType: " + apnType + " defaultBearerConfig: "
            + defaultBearerConfig);
        return PhoneFactory.getPhone(phoneId).setDefaultBearerConfig(apnType, defaultBearerConfig);
    }

    /**
     * This function will get Default Bearer properties for apn type.
     *
     * @param apnType input apn type for get the mapping default bearer properties
     * @param phoneId for getting the current using phone
     * @return DedicateBearerProperties return the default beare properties for input apn type
     *                             return null if something wrong
     *
     */
    public DedicateBearerProperties getDefaultBearerProperties(String apnType, int phoneId) {
        return PhoneFactory.getPhone(phoneId).getDefaultBearerProperties(apnType);
    }

    /**
     * This function will get DcFailCause with int format.
     *
     * @param apnType for geting which last error of apnType
     * @param phoneId for getting the current using phone
     * @return int: return int failCause value
     */
    public int getLastDataConnectionFailCause(String apnType, int phoneId) {
        DcFailCause failCause = PhoneFactory.getPhone(phoneId).
                                    getLastDataConnectionFailCause(apnType);
        return failCause.getErrorCode();
    }

    /**
     * This function will get deactivate cids.
     *
     * @param apnType for getting which apnType deactivate cid array
     * @param phoneId for getting the current using phone
     * @return int []: int array about cids which is(are) deactivated
     */
    public int [] getDeactivateCidArray(String apnType, int phoneId) {
        return PhoneFactory.getPhone(phoneId).getDeactivateCidArray(apnType);
    }

    /**
     * This function will get link properties of input apn type.
     *
     * @param apnType input apn type for geting link properties
     * @param phoneId for getting the current using phone
     * @return LinkProperties: return correspondent link properties with input apn type
     */
    public LinkProperties getLinkProperties(String apnType, int phoneId) {
        return PhoneFactory.getPhone(phoneId).getLinkProperties(apnType);
    }

    /**
     * This function will do pcscf Discovery.
     *
     * @param apnType input apn type for geting pcscf
     * @param cid input cid
     * @param phoneId for getting the current using phone
     * @param onComplete for response event while pcscf discovery done
     * @return int: return 0: OK, -1: failed
     */
    public int pcscfDiscovery(String apnType, int cid, int phoneId,
                        Message onComplete) {
        return PhoneFactory.getPhone(phoneId).pcscfDiscovery(apnType, cid, onComplete);
    }

    /**
     * Set phone radio type and access technology.
     *
     * @param rafs an RadioAccessFamily array to indicate all phone's
     *        new radio access family. The length of RadioAccessFamily
     *        must equal to phone count.
     * @return true if start setPhoneRat successfully.
     */
    @Override
    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        boolean ret = true;
        try {
            ProxyController.getInstance().setRadioCapability(rafs);
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "setRadioCapability: Runtime Exception");
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }

    /**
     * Check if under capability switching.
     *
     * @return true if switching
     */
    public boolean isCapabilitySwitching() {
        return ProxyController.getInstance().isCapabilitySwitching();
    }

    /// M: [C2K] Switch SVLTE RAT mode. @{
    /**
     * Switch SVLTE RAT mode.
     * @param mode the RAT mode.
     */
    public void switchSvlteRatMode(int mode) {
        SvlteRatController.getInstance().setSvlteRatMode(mode, null);
    }

    /**
     * Set SVLTE RAT mode.
     * @param mode the RAT mode.
     * @param subId subscription ID to be queried
     */
    public void setSvlteRatMode(int mode, int subId) {
        int phoneId = SvlteUtils.getSlotIdbySubId(subId);
        if (PhoneFactory.getPhone(phoneId) instanceof SvltePhoneProxy) {
            SvltePhoneProxy svltePhoneProxy = (SvltePhoneProxy) PhoneFactory
                    .getPhone(phoneId);
            svltePhoneProxy.getSvlteRatController().setSvlteRatMode(mode, null);
        }
    }

    /// M: [SVLTE] Get the ServiceState for Svlte. @{
    /**
     * This function will get the ServiceState for Svlte.
     * @param subId for getting the current ServiceState for Svlte.
     * @return service state.
     *
     * @hide
     */
    public Bundle getSvlteServiceState(int subId) {
        if (getPhone(subId).getSvlteServiceState() != null) {
            Bundle data = new Bundle();
            getPhone(subId).getSvlteServiceState().fillInNotifierBundle(data);
            return data;
        }
        return null;
    }

    /**
     * Switch SVLTE RAT mode.
     * @param mode the RAT mode.
     */
    public void switchRadioTechnology(int networkType) {
        SvlteRatController.getInstance().setRadioTechnology(networkType, null);
    }

    /**
     * Set SVLTE Radio Technology.
     * @param networkType the networktype want to switch.
     * @param subId subscription ID to be queried
     */
    public void setRadioTechnology(int networkType, int subId) {
        int phoneId = SvlteUtils.getSlotIdbySubId(subId);
        if (PhoneFactory.getPhone(phoneId) instanceof SvltePhoneProxy) {
            SvltePhoneProxy svltePhoneProxy = (SvltePhoneProxy) PhoneFactory
                    .getPhone(phoneId);
            svltePhoneProxy.getSvlteRatController().setRadioTechnology(
                    networkType, null);
        }
    }
    /// @}

    public void setTrmForPhone(int phoneId, int mode) {
        CommandsInterface ci;
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            ci = ((PhoneBase) (((PhoneProxy) phone).getActivePhone())).mCi;
            log("setTrmForPhone phoneId: " + phoneId + " mode:" + mode);
            ci.setTrm(mode, null);
        } else {
            log("phone is null");
        }
    }

    /**
     * Get subscriber Id of LTE phone.
     * @param subId the subId of CDMAPhone
     * @return The subscriber Id of LTE phone.
     *
     * @hide
     */
    public String getSubscriberIdForLteDcPhone(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        log("getSubscriberIdForLteDcPhone, subId:" + subId + ", phoneId:" + phoneId);
        String imsi = null;
        try {
            UiccController uc = UiccController.getInstance();
            IccRecords records = uc.getIccRecords(phoneId, UiccController.APP_FAM_3GPP);
            if (records != null) {
                imsi = records.getIMSI();
            } else {
                log("getSubscriberIdForLteDcPhone, 3gpp records is null");
            }
        } catch (RuntimeException e) {
            log("getSubscriberIdForLteDcPhone, UiccController is not ready");
        }
		/* HQ_qianwei 2015-10-12 modified for HQ01433591 begin */
        if (Build.TYPE.equals("eng")) {
                 	 log("getSubscriberIdForLteDcPhone, imsi of 3gpp:" + imsi);
        	}
		/* HQ_qianwei 2015-10-12 modified for HQ01433591 ends */
        return imsi;
    }

    /**
     * For C2K get SVLTE imei.
     * @param slotId slot id
     * @return svlte imei.
     *
     * @hide
     */
    public String getSvlteImei(int slotId) {
        log("getSvlteImei slotId = " + slotId);
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            log("svlte featureoption is true getSvlteImei slotId = " + slotId);
            if (PhoneFactory.getPhone(slotId) instanceof SvltePhoneProxy) {
                SvltePhoneProxy svltePhoneProxy = (SvltePhoneProxy) PhoneFactory.getPhone(slotId);
                return svltePhoneProxy.getLtePhone().getImei();
            }
        }
        log("svlte featureoption is false getSvlteImei slotId = " + slotId);
        return TelephonyManager.getDefault().getImei();
    }

    /**
     * For C2K get SVLTE meid.
     * @param slotId slot id
     * @return svlte meid.
     *
     * @hide
     */
    public String getSvlteMeid(int slotId) {
        int cdmaSlotId = SvlteModeController.getCdmaSocketSlotId();
        log("getSvlteMeid slotId = " + slotId + "  cdmaSlotId = " + cdmaSlotId);
        if (cdmaSlotId == slotId) {
            if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
                log("svlte featureoption is true getSvlteMeid slotId = " + slotId);
                if (PhoneFactory.getPhone(slotId) instanceof SvltePhoneProxy) {
                    SvltePhoneProxy svltePhoneProxy =
                        (SvltePhoneProxy) PhoneFactory.getPhone(slotId);
                    return svltePhoneProxy.getNLtePhone().getDeviceId();
                }
            }
        }
        log("svlte featureoption is false getSvlteMeid slotId = " + slotId);
        return TelephonyManager.getDefault().getDeviceId(slotId);
    }

    /**
     * For C2K Get the airplane mode allow change or not.
     */
    public boolean isAllowAirplaneModeChange() {
        return isAirplanemodeAvailableNow();
    }

    /**
     * Return the sim card if in home network.
     *
     * @param subId subscription ID to be queried.
     * @return true if in home network.
     * @hide
     */
    public boolean isInHomeNetwork(int subId) {
        final int phoneId = SvlteUtils.getSlotIdbySubId(subId);
        boolean isInHomeNetwork = false;
        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone instanceof PhoneProxy) {
            final PhoneProxy phoneProxy = ((PhoneProxy) phone);
            ServiceState serviceState = phoneProxy.getSvlteServiceState();
            if (serviceState == null) {
                serviceState = phoneProxy.getServiceState();
            }
            if (serviceState != null) {
                isInHomeNetwork = inSameCountry(phoneId, serviceState.getVoiceOperatorNumeric());
            }
        }
        log("isInHomeNetwork, subId=" + subId + " ,phoneId=" + phoneId
                + " ,isInHomeNetwork=" + isInHomeNetwork);
        return isInHomeNetwork;
    }

    /**
     * Check ISO country by MCC to see if phone is roaming in same registered country.
     *
     * @param phoneId for which phone inSameCountry is returned
     * @param operatorNumeric registered operator numeric
     * @return true if in same country.
     * @hide
     */
    private static final boolean inSameCountry(int phoneId, String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || (operatorNumeric.length() < 5)
                || (!TextUtils.isDigitsOnly(operatorNumeric))) {
            // Not a valid network
            log("inSameCountry, Not a valid network"
                    + ", phoneId=" + phoneId + ", operatorNumeric=" + operatorNumeric);
            return true;
        }

        final String homeNumeric = getHomeOperatorNumeric(phoneId);
        if (TextUtils.isEmpty(homeNumeric) || (homeNumeric.length() < 5)
                || (!TextUtils.isDigitsOnly(homeNumeric))) {
            // Not a valid SIM MCC
            log("inSameCountry, Not a valid SIM MCC"
                    + ", phoneId=" + phoneId + ", homeNumeric=" + homeNumeric);
            return true;
        }

        boolean inSameCountry = true;
        final String networkMCC = operatorNumeric.substring(0, 3);
        final String homeMCC = homeNumeric.substring(0, 3);
        final String networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
        final String homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
        log("inSameCountry, phoneId=" + phoneId
                + ", homeMCC=" + homeMCC
                + ", networkMCC=" + networkMCC
                + ", homeCountry=" + homeCountry
                + ", networkCountry=" + networkCountry);
        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            // Not a valid country
            return true;
        }
        inSameCountry = homeCountry.equals(networkCountry);
        if (inSameCountry) {
            return inSameCountry;
        }
        // special same country cases
        if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
            inSameCountry = true;
        } else if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
            inSameCountry = true;
        } else if ("cn".equals(homeCountry) && "mo".equals(networkCountry)) {
            inSameCountry = true;
        }

        log("inSameCountry, phoneId=" + phoneId + ", inSameCountry=" + inSameCountry);
        return inSameCountry;
    }

    /**
     * Returns the Service Provider Name (SPN).
     *
     * @param phoneId for which HomeOperatorNumeric is returned
     * @return the Service Provider Name (SPN)
     * @hide
     */
    private static final String getHomeOperatorNumeric(int phoneId) {
        String numeric = TelephonyManager.getDefault().getSimOperatorNumericForPhone(phoneId);
        if (TextUtils.isEmpty(numeric)) {
            numeric = SystemProperties.get("ro.cdma.home.operator.numeric", "");
        }
        log("getHomeOperatorNumeric, phoneId=" + phoneId + ", numeric=" + numeric);
        return numeric;
    }

    // M: [LTE][Low Power][UL traffic shaping] Start
    public boolean setLteAccessStratumReport(boolean enabled) {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || phoneId != dataPhoneId) {
            loge("setLteAccessStratumReport incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("setLteAccessStratumReport: MainPhoneId and dataPhoneId aren't the same");
                }
            }
            return false;
        }
        if (DBG) log("setLteAccessStratumReport: enabled = " + enabled);
        Boolean success = (Boolean) sendRequest(CMD_SET_LTE_ACCESS_STRATUM_STATE,
                new Boolean(enabled), new Integer(phoneId));
        if (DBG) log("setLteAccessStratumReport: success = " + success);
        return success;

    }

    public boolean setLteUplinkDataTransfer(boolean isOn, int timeMillis) {
        int state = 1;
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || phoneId != dataPhoneId) {
            loge("setLteUplinkDataTransfer incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("setLteUplinkDataTransfer: MainPhoneId and dataPhoneId aren't the same");
                }
            }
            return false;
        }
        if (DBG) {
            log("setLteUplinkDataTransfer: isOn = " + isOn
                    + ", Tclose timer = " + (timeMillis/1000));
        }
        if (!isOn) state = (timeMillis/1000) << 16 | 0;
        Boolean success = (Boolean) sendRequest(CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE,
                new Integer(state), new Integer(phoneId));
        if (DBG) log("setLteUplinkDataTransfer: success = " + success);
        return success;
    }

    public String getLteAccessStratumState() {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        String state = PhoneConstants.LTE_ACCESS_STRATUM_STATE_UNKNOWN;
        if (phone == null || phoneId != dataPhoneId) {
            loge("getLteAccessStratumState incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("getLteAccessStratumState: MainPhoneId and dataPhoneId aren't the same");
                }
            }
        } else {
            PhoneBase phoneBase = (PhoneBase)(((PhoneProxy)phone).getActivePhone());
            DcTrackerBase dcTracker = phoneBase.mDcTracker;
            state = dcTracker.getLteAccessStratumState();
        }
        if (DBG) log("getLteAccessStratumState: " + state);
        return state;
    }

    public boolean isSharedDefaultApn() {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        boolean isSharedDefaultApn = false;
        if (phone == null || phoneId != dataPhoneId) {
            loge("isSharedDefaultApn incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) loge("isSharedDefaultApn: MainPhoneId and dataPhoneId aren't the same");
            }
        } else {
            PhoneBase phoneBase = (PhoneBase)(((PhoneProxy)phone).getActivePhone());
            DcTrackerBase dcTracker = phoneBase.mDcTracker;
            isSharedDefaultApn = dcTracker.isSharedDefaultApn();
        }
        if (DBG) log("isSharedDefaultApn: " + isSharedDefaultApn);
        return isSharedDefaultApn;
    }
    // M: [LTE][Low Power][UL traffic shaping] End
}
