/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.internal.telephony.imsphone;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.telecom.ConferenceParticipant;
import android.telecom.VideoProfile;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneNumberUtils.EccEntry;
import android.telephony.Rlog;
///M: ALPS02037830. @{
import android.telephony.TelephonyManager;
/// @}
import android.telephony.ServiceState;

import com.android.ims.ImsCall;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsConferenceState;
import com.android.ims.ImsConfig;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsServiceClass;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE;
///M: ALPS02037830. @{
import static com.android.internal.telephony.TelephonyProperties.TERMINAL_BASED_CALL_WAITING_DISABLED;
/// @}
import static com.android.internal.telephony.TelephonyProperties.TERMINAL_BASED_CALL_WAITING_ENABLED_OFF;
import com.mediatek.telecom.TelecomManagerEx;

/**
 * {@hide}
 */
public final class ImsPhoneCallTracker extends CallTracker {
    static final String LOG_TAG = "ImsPhoneCallTracker";

    private static final boolean DBG = true;

    private boolean mIsVolteEnabled = false;
    private boolean mIsVtEnabled = false;
    /// M: WFC @{
    private boolean mIsWfcEnabled = false;
    private static final int IMS_CALL_TYPE_LTE = 1;
    private static final int IMS_CALL_TYPE_WIFI = 2;
    ///@}
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ImsManager.ACTION_IMS_INCOMING_CALL)) {
                if (DBG) log("onReceive : incoming call intent");

                if (mImsManager == null) return;

                if (mServiceId < 0) return;

                try {
                    // Network initiated USSD will be treated by mImsUssdListener
                    boolean isUssd = intent.getBooleanExtra(ImsManager.EXTRA_USSD, false);
                    if (isUssd) {
                        if (DBG) log("onReceive : USSD");
                        mUssdSession = mImsManager.takeCall(mServiceId, intent, mImsUssdListener);
                        if (mUssdSession != null) {
                            mUssdSession.accept(ImsCallProfile.CALL_TYPE_VOICE);
                        }
                        return;
                    }

                    // Normal MT call
                    ImsCall imsCall = mImsManager.takeCall(mServiceId, intent, mImsCallListener);
                    ImsPhoneConnection conn = new ImsPhoneConnection(mPhone.getContext(), imsCall,
                            ImsPhoneCallTracker.this, mRingingCall);
                    addConnection(conn);

                    IImsVideoCallProvider imsVideoCallProvider =
                            imsCall.getCallSession().getVideoCallProvider();
                    if (imsVideoCallProvider != null) {
                        ImsVideoCallProviderWrapper imsVideoCallProviderWrapper =
                                new ImsVideoCallProviderWrapper(imsVideoCallProvider);
                        conn.setVideoProvider(imsVideoCallProviderWrapper);
                    }

                    if ((mForegroundCall.getState() != ImsPhoneCall.State.IDLE) ||
                            (mBackgroundCall.getState() != ImsPhoneCall.State.IDLE)) {
                        conn.update(imsCall, ImsPhoneCall.State.WAITING);
                    }

                    mPhone.notifyNewRingingConnection(conn);
                    mPhone.notifyIncomingRing();

                    updatePhoneState();
                    mPhone.notifyPreciseCallStateChanged();
                    /// M: @{
                    if (mIsWfcEnabled) {
                        conn.setCallType(IMS_CALL_TYPE_WIFI);
                    }
                    ///@}

                } catch (ImsException e) {
                    loge("onReceive : exception " + e);
                } catch (RemoteException e) {
                }
            }
        }
    };

    //***** Constants

    static final int MAX_CONNECTIONS = 7;
    static final int MAX_CONNECTIONS_PER_CALL = 5;

    private static final int EVENT_HANGUP_PENDINGMO = 18;
    private static final int EVENT_RESUME_BACKGROUND = 19;
    private static final int EVENT_DIAL_PENDINGMO = 20;

    private static final int TIMEOUT_HANGUP_PENDINGMO = 500;

    //***** Instance Variables
    private ArrayList<ImsPhoneConnection> mConnections = new ArrayList<ImsPhoneConnection>();
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();

    ImsPhoneCall mRingingCall = new ImsPhoneCall(this);
    ImsPhoneCall mForegroundCall = new ImsPhoneCall(this);
    ImsPhoneCall mBackgroundCall = new ImsPhoneCall(this);
    ImsPhoneCall mHandoverCall = new ImsPhoneCall(this);

    private ImsPhoneConnection mPendingMO;
    private int mClirMode = CommandsInterface.CLIR_DEFAULT;
    private Object mSyncHold = new Object();

    private ImsCall mUssdSession = null;
    private Message mPendingUssd = null;

    ImsPhone mPhone;

    private boolean mDesiredMute = false;    // false = mute off
    private boolean mOnHoldToneStarted = false;

    PhoneConstants.State mState = PhoneConstants.State.IDLE;

    private ImsManager mImsManager;
    private int mServiceId = -1;

    private Call.SrvccState mSrvccState = Call.SrvccState.NONE;

    private boolean mIsInEmergencyCall = false;

    private int pendingCallClirMode;
    private int pendingCallVideoState;
    private boolean pendingCallInEcm = false;
    private boolean mSwitchingFgAndBgCalls = false;
    private ImsCall mCallExpectedToResume = null;

    /// M: Redial as ECC @{
    private boolean mDialAsECC = false;
    /// @}

    /// M: ALPS02023277. @{
    private boolean mHasPendingResumeRequest = false;
    /// @}

    //***** Events


    //***** Constructors

    ImsPhoneCallTracker(ImsPhone phone) {
        this.mPhone = phone;

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(ImsManager.ACTION_IMS_INCOMING_CALL);
        mPhone.getContext().registerReceiver(mReceiver, intentfilter);

        /// M: register the indication receiver. @{
        registerIndicationReceiver();
        /// @}

        Thread t = new Thread() {
            public void run() {
                getImsService();
            }
        };
        t.start();
    }

    private PendingIntent createIncomingCallPendingIntent() {
        Intent intent = new Intent(ImsManager.ACTION_IMS_INCOMING_CALL);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void getImsService() {
        if (DBG) log("getImsService");
        mImsManager = ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        try {
            mServiceId = mImsManager.open(ImsServiceClass.MMTEL,
                    createIncomingCallPendingIntent(),
                    mImsConnectionStateListener);

            // Get the ECBM interface and set IMSPhone's listener object for notifications
            getEcbmInterface().setEcbmStateListener(mPhone.mImsEcbmStateListener);
            if (mPhone.isInEcm()) {
                // Call exit ECBM which will invoke onECBMExited
                mPhone.exitEmergencyCallbackMode();
            }
            int mPreferredTtyMode = Settings.Secure.getInt(
                mPhone.getContext().getContentResolver(),
                Settings.Secure.PREFERRED_TTY_MODE,
                Phone.TTY_MODE_OFF);
           mImsManager.setUiTTYMode(mPhone.getContext(), mServiceId, mPreferredTtyMode, null);

        } catch (ImsException e) {
            loge("getImsService: " + e);
            //Leave mImsManager as null, then CallStateException will be thrown when dialing
            mImsManager = null;
        }
    }

    public void dispose() {
        if (DBG) log("dispose");
        mRingingCall.dispose();
        mBackgroundCall.dispose();
        mForegroundCall.dispose();
        mHandoverCall.dispose();

        clearDisconnected();
        mPhone.getContext().unregisterReceiver(mReceiver);
        /// M: It needs to unregister Indication receivers here.  @{
        unregisterIndicationReceiver();

        // close ImsService.
        if (mImsManager != null && mServiceId != -1) {
            try {
                mImsManager.close(mServiceId);
            } catch (ImsException e) {
                loge("getImsService: " + e);
            }
            mServiceId = -1;
            mImsManager = null;
        }
        /// @}
    }

    @Override
    protected void finalize() {
        log("ImsPhoneCallTracker finalized");
    }

    //***** Instance Methods

    //***** Public Methods
    @Override
    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceCallStartedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler h) {
        mVoiceCallStartedRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceCallEndedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler h) {
        mVoiceCallEndedRegistrants.remove(h);
    }

    Connection
    dial(String dialString, int videoState) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        int oirMode = sp.getInt(PhoneBase.CLIR_KEY + mPhone.getPhoneId(), 
            CommandsInterface.CLIR_DEFAULT);
        return dial(dialString, oirMode, videoState);
    }

    /**
     * oirMode is one of the CLIR_ constants
     */
    synchronized Connection
    dial(String dialString, int clirMode, int videoState) throws CallStateException {
        boolean isPhoneInEcmMode = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_INECM_MODE, false);
        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(dialString);

        if (DBG) log("dial clirMode=" + clirMode);

        // note that this triggers call state changed notif
        clearDisconnected();

        if (mImsManager == null) {
            throw new CallStateException("service not available");
        }

        /// M: for ALPS02015368, we should use GSMPhone to dial during SRVCC. @{
        if (mHandoverCall.mConnections.size() > 0) {
            log("SRVCC: there are connections during handover, trigger CSFB!");
            throw new CallStateException(ImsPhone.CS_FALLBACK);
        }
        /// @}

        /// M: for ALPS02062100, MTK feature: reject ringing call for ECC. @{
        if (isEmergencyNumber && mRingingCall.getState().isRinging()) {
            log("hangup the ringing call because this is ECC!");
            hangup(mRingingCall);
        }
        /// @}

        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }

        if (isPhoneInEcmMode && isEmergencyNumber) {
            handleEcmTimer(ImsPhone.CANCEL_ECM_TIMER);
        }

        boolean holdBeforeDial = false;

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
            if (mBackgroundCall.getState() != ImsPhoneCall.State.IDLE) {
                //we should have failed in !canDial() above before we get here
                throw new CallStateException("cannot dial in current state");
            }
            // foreground call is empty for the newly dialed connection
            holdBeforeDial = true;
            switchWaitingOrHoldingAndActive();
        }

        ImsPhoneCall.State fgState = ImsPhoneCall.State.IDLE;
        ImsPhoneCall.State bgState = ImsPhoneCall.State.IDLE;

        mClirMode = clirMode;

        synchronized (mSyncHold) {
            if (holdBeforeDial) {
                fgState = mForegroundCall.getState();
                bgState = mBackgroundCall.getState();

                //holding foreground call failed
                if (fgState == ImsPhoneCall.State.ACTIVE) {
                    throw new CallStateException("cannot dial in current state");
                }

                //holding foreground call succeeded
                if (bgState == ImsPhoneCall.State.HOLDING) {
                    holdBeforeDial = false;
                }
            }

            mPendingMO = new ImsPhoneConnection(mPhone.getContext(),
                    checkForTestEmergencyNumber(dialString), this, mForegroundCall);
        }
        addConnection(mPendingMO);

        /// M: add for debug @{
        log("IMS: dial() holdBeforeDial = " + holdBeforeDial + " isPhoneInEcmMode = " + isPhoneInEcmMode + " isEmergencyNumber = " + isEmergencyNumber);
        /// @}

        if (!holdBeforeDial) {
            if ((!isPhoneInEcmMode) || (isPhoneInEcmMode && isEmergencyNumber)) {
                dialInternal(mPendingMO, clirMode, videoState);
            } else {
                try {
                    getEcbmInterface().exitEmergencyCallbackMode();
                } catch (ImsException e) {
                    e.printStackTrace();
                    throw new CallStateException("service not available");
                }
                mPhone.setOnEcbModeExitResponse(this, EVENT_EXIT_ECM_RESPONSE_CDMA, null);
                pendingCallClirMode = clirMode;
                pendingCallVideoState = videoState;
                pendingCallInEcm = true;
            }
        }

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();

        return mPendingMO;
    }

    private void handleEcmTimer(int action) {
        mPhone.handleTimerInEmergencyCallbackMode(action);
        switch (action) {
            case ImsPhone.CANCEL_ECM_TIMER:
                break;
            case ImsPhone.RESTART_ECM_TIMER:
                break;
            default:
                log("handleEcmTimer, unsupported action " + action);
        }
    }

    private void dialInternal(ImsPhoneConnection conn, int clirMode, int videoState) {
        if (conn == null) {
            return;
        }

        /// M:  For VoLTE enhanced conference call. @{
        if (conn.getConfDialStrings() == null) {
            /// @}
            if (conn.getAddress() == null || conn.getAddress().length() == 0
                    || conn.getAddress().indexOf(PhoneNumberUtils.WILD) >= 0) {
                // Phone number is invalid
                conn.setDisconnectCause(DisconnectCause.INVALID_NUMBER);
                sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
                return;
            }
        }

        // Always unmute when initiating a new call
        setMute(false);
        int serviceType = PhoneNumberUtils.isEmergencyNumber(conn.getAddress()) ?
                ImsCallProfile.SERVICE_TYPE_EMERGENCY : ImsCallProfile.SERVICE_TYPE_NORMAL;

        /// M: @{
        // for some operation's request, we need to dial ECC with ATD. At these cases,
        // we will put the special ECC numbers in specialECCnumber list
        if (serviceType == ImsCallProfile.SERVICE_TYPE_EMERGENCY &&
                PhoneNumberUtils.isSpecialEmergencyNumber(conn.getAddress())) {
            serviceType = ImsCallProfile.SERVICE_TYPE_NORMAL;
        }
        /// @}

        /// M: Redial as ECC @{
        if (mDialAsECC) {
            serviceType = ImsCallProfile.SERVICE_TYPE_EMERGENCY;
            log("Dial as ECC: conn.getAddress(): " + conn.getAddress());
            mDialAsECC = false;
        }
        /// @}
        int callType = ImsCallProfile.getCallTypeFromVideoState(videoState);
        //TODO(vt): Is this sufficient?  At what point do we know the video state of the call?
        conn.setVideoState(videoState);

        try {
            /// M:  For VoLTE enhanced conference call. @{
            String[] callees = null;
            if (conn.getConfDialStrings() != null) {
                ArrayList<String> dialStrings = conn.getConfDialStrings();
                callees = (String[]) dialStrings.toArray(new String[dialStrings.size()]);
            } else {
                /// @}
                callees = new String[] { conn.getAddress() };
            }
            ImsCallProfile profile = mImsManager.createCallProfile(mServiceId,
                    serviceType, callType);
            profile.setCallExtraInt(ImsCallProfile.EXTRA_OIR, clirMode);
            /// M:  For VoLTE enhanced conference call. @{
            if (conn.getConfDialStrings() != null) {
                profile.setCallExtraBoolean(ImsCallProfile.EXTRA_CONFERENCE, true);
            }
            /// @}

            ImsCall imsCall = mImsManager.makeCall(mServiceId, profile,
                    callees, mImsCallListener);
            conn.setImsCall(imsCall);

            IImsVideoCallProvider imsVideoCallProvider =
                    imsCall.getCallSession().getVideoCallProvider();
            if (imsVideoCallProvider != null) {
                ImsVideoCallProviderWrapper imsVideoCallProviderWrapper =
                        new ImsVideoCallProviderWrapper(imsVideoCallProvider);
                conn.setVideoProvider(imsVideoCallProviderWrapper);
            }
            /// M: @{
            if (mIsWfcEnabled) {
                conn.setCallType(IMS_CALL_TYPE_WIFI);
                log("[WFC] setcallTYpe IMS_CALL_TYPE_WIFI ");
            }
            ///@}
        } catch (ImsException e) {
            loge("dialInternal : " + e);
            conn.setDisconnectCause(DisconnectCause.ERROR_UNSPECIFIED);
            sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
        } catch (RemoteException e) {
        }
    }

    /**
     * Accepts a call with the specified video state.  The video state is the video state that the
     * user has agreed upon in the InCall UI.
     *
     * @param videoState The video State
     * @throws CallStateException
     */
    void acceptCall (int videoState) throws CallStateException {
        if (DBG) log("acceptCall");

        if (mForegroundCall.getState().isAlive()
                && mBackgroundCall.getState().isAlive()) {
            throw new CallStateException("cannot accept call");
        }

        if ((mRingingCall.getState() == ImsPhoneCall.State.WAITING)
                && mForegroundCall.getState().isAlive()) {
            setMute(false);
            switchWaitingOrHoldingAndActive();
        } else if (mRingingCall.getState().isRinging()) {
            if (DBG) log("acceptCall: incoming...");
            // Always unmute when answering a new call
            setMute(false);
            try {
                ImsCall imsCall = mRingingCall.getImsCall();
                if (imsCall != null) {
                    imsCall.accept(ImsCallProfile.getCallTypeFromVideoState(videoState));
                } else {
                    throw new CallStateException("no valid ims call");
                }
            } catch (ImsException e) {
                throw new CallStateException("cannot accept call");
            }
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    void
    rejectCall () throws CallStateException {
        if (DBG) log("rejectCall");

        if (mRingingCall.getState().isRinging()) {
            hangup(mRingingCall);
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    void
    switchWaitingOrHoldingAndActive() throws CallStateException {
        if (DBG) log("switchWaitingOrHoldingAndActive");

        if (mRingingCall.getState() == ImsPhoneCall.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }

        if (mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
            ImsCall imsCall = mForegroundCall.getImsCall();
            if (imsCall == null) {
                throw new CallStateException("no ims call");
            }

            // Swap the ImsCalls pointed to by the foreground and background ImsPhoneCalls.
            // If hold or resume later fails, we will swap them back.
            /// M: ALPS02064606. @{
            /// When accept waiting call case, mSwitchingFgAndBgCalls flag won't be reset.
            if (mBackgroundCall.getState().isAlive()) {
                /// @}
                mSwitchingFgAndBgCalls = true;
                mCallExpectedToResume = mBackgroundCall.getImsCall();
            }
            mForegroundCall.switchWith(mBackgroundCall);

            // Hold the foreground call; once the foreground call is held, the background call will
            // be resumed.
            try {
                imsCall.hold();
            } catch (ImsException e) {
                mForegroundCall.switchWith(mBackgroundCall);
                throw new CallStateException(e.getMessage());
            }
        } else if (mBackgroundCall.getState() == ImsPhoneCall.State.HOLDING) {
            resumeWaitingOrHolding();
        }
    }

    void
    conference() {
        if (DBG) log("conference");

        ImsCall fgImsCall = mForegroundCall.getImsCall();
        if (fgImsCall == null) {
            log("conference no foreground ims call");
            return;
        }

        ImsCall bgImsCall = mBackgroundCall.getImsCall();
        if (bgImsCall == null) {
            log("conference no background ims call");
            return;
        }

       // Keep track of the connect time of the earliest call so that it can be set on the
        // {@code ImsConference} when it is created.
        long conferenceConnectTime = Math.min(mForegroundCall.getEarliestConnectTime(),
                mBackgroundCall.getEarliestConnectTime());
        ImsPhoneConnection foregroundConnection = mForegroundCall.getFirstConnection();
        if (foregroundConnection != null) {
            foregroundConnection.setConferenceConnectTime(conferenceConnectTime);
        }


        try {
            fgImsCall.merge(bgImsCall);
        } catch (ImsException e) {
            log("conference " + e.getMessage());
        }
    }

    void
    explicitCallTransfer() {
        //TODO : implement
    }

    void
    clearDisconnected() {
        if (DBG) log("clearDisconnected");

        internalClearDisconnected();

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();
    }

    boolean
    canConference() {
        return mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE
            && mBackgroundCall.getState() == ImsPhoneCall.State.HOLDING
            && !mBackgroundCall.isFull()
            && !mForegroundCall.isFull();
    }

    boolean
    canDial() {
        boolean ret;
        int serviceState = mPhone.getServiceState().getState();
        String disableCall = SystemProperties.get(
                TelephonyProperties.PROPERTY_DISABLE_CALL, "false");

        ret = (serviceState != ServiceState.STATE_POWER_OFF)
            && mPendingMO == null
            && !mRingingCall.isRinging()
            && !disableCall.equals("true")
            && (!mForegroundCall.getState().isAlive()
                    || !mBackgroundCall.getState().isAlive());

        /// M: add for debug @{
        log("IMS: canDial() serviceState = " + serviceState + ", disableCall = " + disableCall + " , mPendingMO = " + mPendingMO + ", mRingingCall.isRinging() = "
            + mRingingCall.isRinging() + ", mForegroundCall.getState().isAlive() = " + mForegroundCall.getState().isAlive() + 
            ", mBackgroundCall.getState().isAlive() = " + mBackgroundCall.getState().isAlive());
        /// @}

        return ret;
    }

    boolean
    canTransfer() {
        return mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE
            && mBackgroundCall.getState() == ImsPhoneCall.State.HOLDING;
    }

    //***** Private Instance Methods

    private void
    internalClearDisconnected() {
        mRingingCall.clearDisconnected();
        mForegroundCall.clearDisconnected();
        mBackgroundCall.clearDisconnected();
        mHandoverCall.clearDisconnected();
    }

    private void
    updatePhoneState() {
        PhoneConstants.State oldState = mState;

        if (mRingingCall.isRinging()) {
            mState = PhoneConstants.State.RINGING;
        } else if (mPendingMO != null ||
                !(mForegroundCall.isIdle() && mBackgroundCall.isIdle())) {
            mState = PhoneConstants.State.OFFHOOK;
        } else {
            mState = PhoneConstants.State.IDLE;
        }

        if (mState == PhoneConstants.State.IDLE && oldState != mState) {
            mVoiceCallEndedRegistrants.notifyRegistrants(
                    new AsyncResult(null, null, null));
        } else if (oldState == PhoneConstants.State.IDLE && oldState != mState) {
            mVoiceCallStartedRegistrants.notifyRegistrants (
                    new AsyncResult(null, null, null));
        }

        if (DBG) log("updatePhoneState oldState=" + oldState + ", newState=" + mState);

        if (mState != oldState) {
            mPhone.notifyPhoneStateChanged();
        }
    }

    private void
    handleRadioNotAvailable() {
        // handlePollCalls will clear out its
        // call list when it gets the CommandException
        // error result from this
        pollCallsWhenSafe();
    }

    private void
    dumpState() {
        List l;

        log("Phone State:" + mState);

        log("Ringing call: " + mRingingCall.toString());

        l = mRingingCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

        log("Foreground call: " + mForegroundCall.toString());

        l = mForegroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

        log("Background call: " + mBackgroundCall.toString());

        l = mBackgroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

    }

    //***** Called from ImsPhone

    void setUiTTYMode(int uiTtyMode, Message onComplete) {
        try {
            mImsManager.setUiTTYMode(mPhone.getContext(), mServiceId, uiTtyMode, onComplete);
        } catch (ImsException e) {
            loge("setTTYMode : " + e);
            mPhone.sendErrorResponse(onComplete, e);
        }
    }

    /*package*/ void setMute(boolean mute) {
        mDesiredMute = mute;
        mForegroundCall.setMute(mute);
    }

    /*package*/ boolean getMute() {
        return mDesiredMute;
    }

    /* package */ void sendDtmf(char c, Message result) {
        if (DBG) log("sendDtmf");

        ImsCall imscall = mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.sendDtmf(c, result);
        }
    }

    /*package*/ void
    startDtmf(char c) {
        if (DBG) log("startDtmf");

        ImsCall imscall = mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.startDtmf(c);
        } else {
            loge("startDtmf : no foreground call");
        }
    }

    /*package*/ void
    stopDtmf() {
        if (DBG) log("stopDtmf");

        ImsCall imscall = mForegroundCall.getImsCall();
        if (imscall != null) {
            imscall.stopDtmf();
        } else {
            loge("stopDtmf : no foreground call");
        }
    }

    //***** Called from ImsPhoneConnection

    /*package*/ void
    hangup (ImsPhoneConnection conn) throws CallStateException {
        if (DBG) log("hangup connection");

        if (conn.getOwner() != this) {
            throw new CallStateException ("ImsPhoneConnection " + conn
                    + "does not belong to ImsPhoneCallTracker " + this);
        }

        hangup(conn.getCall());
    }

    //***** Called from ImsPhoneCall

    /* package */ void
    hangup (ImsPhoneCall call) throws CallStateException {
        if (DBG) log("hangup call");

        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }

        ImsCall imsCall = call.getImsCall();
        boolean rejectCall = false;

        if (call == mRingingCall) {
            if (Phone.DEBUG_PHONE) log("(ringing) hangup incoming");
            rejectCall = true;
        } else if (call == mForegroundCall) {
            if (call.isDialingOrAlerting()) {
                if (Phone.DEBUG_PHONE) {
                    log("(foregnd) hangup dialing or alerting...");
                }
            } else {
                if (Phone.DEBUG_PHONE) {
                    log("(foregnd) hangup foreground");
                }
                //held call will be resumed by onCallTerminated
            }
        } else if (call == mBackgroundCall) {
            if (Phone.DEBUG_PHONE) {
                log("(backgnd) hangup waiting or background");
            }
        } else {
            throw new CallStateException ("ImsPhoneCall " + call +
                    "does not belong to ImsPhoneCallTracker " + this);
        }

        call.onHangupLocal();

        try {
            if (imsCall != null) {
                if (rejectCall) imsCall.reject(ImsReasonInfo.CODE_USER_DECLINE);
                else imsCall.terminate(ImsReasonInfo.CODE_USER_TERMINATED);
            } else if (mPendingMO != null && call == mForegroundCall) {
                // is holding a foreground call
                mPendingMO.update(null, ImsPhoneCall.State.DISCONNECTED);
                mPendingMO.onDisconnect();
                removeConnection(mPendingMO);
                mPendingMO = null;
                updatePhoneState();
                removeMessages(EVENT_DIAL_PENDINGMO);
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }

        mPhone.notifyPreciseCallStateChanged();
    }

    void callEndCleanupHandOverCallIfAny() {
        if (mHandoverCall.mConnections.size() > 0) {
            if (DBG) log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections="
                    + mHandoverCall.mConnections);
            /// M: ALPS01979162 make sure all connections of mHandoverCall are removed from
            /// mConnections to prevent leak @{
            for (Connection conn : mHandoverCall.mConnections) {
                log("SRVCC: remove connection=" + conn);
                removeConnection((ImsPhoneConnection) conn);
            }
            /// @}
            mHandoverCall.mConnections.clear();
            mState = PhoneConstants.State.IDLE;

            /// M: ALPS02192901. @{
            if (mPhone != null && mPhone.mDefaultPhone != null
                    && mPhone.mDefaultPhone.getState() == PhoneConstants.State.IDLE) {
                // If the call is disconnected before GSMPhone poll calls, the phone state of
                // GSMPhone keeps in idle state, so it will not notify phone state changed. In this
                // case, ImsPhone needs to notify by itself.
                log("SRVCC: notify ImsPhone state as idle.");
                mPhone.notifyPhoneStateChanged();
            }
            /// @}
        }
    }

    /// M: @{
    void hangupAll() throws CallStateException {
        if (DBG) {
            log("hangupAll");
        }

        if (mImsManager == null) {
            throw new CallStateException("No ImsManager Instance");
        }

        try {
            mImsManager.hangupAllCall();
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }

        if (!mRingingCall.isIdle()) {
            mRingingCall.onHangupLocal();
        }
        if (!mForegroundCall.isIdle()) {
            mForegroundCall.onHangupLocal();
        }
        if (!mBackgroundCall.isIdle()) {
            mBackgroundCall.onHangupLocal();
        }
    }
    /// @}

    /* package */
    void resumeWaitingOrHolding() throws CallStateException {
        if (DBG) log("resumeWaitingOrHolding");

        try {
            if (mForegroundCall.getState().isAlive()) {
                //resume foreground call after holding background call
                //they were switched before holding
                ImsCall imsCall = mForegroundCall.getImsCall();
                if (imsCall != null) imsCall.resume();
            } else if (mRingingCall.getState() == ImsPhoneCall.State.WAITING) {
                /// M: ALPS02023277. @{
                if (mHasPendingResumeRequest) {
                    log("there is a pending resume background request, ignore accept()!");
                    return;
                }
                /// @}
                //accept waiting call after holding background call
                ImsCall imsCall = mRingingCall.getImsCall();
                if (imsCall != null) imsCall.accept(ImsCallProfile.CALL_TYPE_VOICE);
            } else {
                //Just resume background call.
                //To distinguish resuming call with swapping calls
                //we do not switch calls.here
                //ImsPhoneConnection.update will chnage the parent when completed
                ImsCall imsCall = mBackgroundCall.getImsCall();
                if (imsCall != null) imsCall.resume();
                /// M: ALPS02023277. @{
                mHasPendingResumeRequest = true;
                log("turn on the resuem pending request lock!");
                /// @}
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    /* package */
    void sendUSSD (String ussdString, Message response) {
        if (DBG) log("sendUSSD");

        try {
            if (mUssdSession != null) {
                mUssdSession.sendUssd(ussdString);
                AsyncResult.forMessage(response, null, null);
                response.sendToTarget();
                return;
            }

            String[] callees = new String[] { ussdString };
            ImsCallProfile profile = mImsManager.createCallProfile(mServiceId,
                    ImsCallProfile.SERVICE_TYPE_NORMAL, ImsCallProfile.CALL_TYPE_VOICE);
            profile.setCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING,
                    ImsCallProfile.DIALSTRING_USSD);

            mUssdSession = mImsManager.makeCall(mServiceId, profile,
                    callees, mImsUssdListener);
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            mPhone.sendErrorResponse(response, e);
        }
    }

    /* package */
    void cancelUSSD() {
        if (mUssdSession == null) return;

        try {
            mUssdSession.terminate(ImsReasonInfo.CODE_USER_TERMINATED);
        } catch (ImsException e) {
        }

    }

    private synchronized ImsPhoneConnection findConnection(ImsCall imsCall) {
        for (ImsPhoneConnection conn : mConnections) {
            if (conn.getImsCall() == imsCall) {
                return conn;
            }
        }
        return null;
    }

    private synchronized void removeConnection(ImsPhoneConnection conn) {
        mConnections.remove(conn);
    }

    private synchronized void addConnection(ImsPhoneConnection conn) {
        mConnections.add(conn);
    }

    private void processCallStateChange(ImsCall imsCall, ImsPhoneCall.State state, int cause) {
        if (DBG) log("processCallStateChange " + imsCall + " state=" + state + " cause=" + cause);

        if (imsCall == null) return;

        boolean changed = false;
        ImsPhoneConnection conn = findConnection(imsCall);

        if (conn == null) {
            // TODO : what should be done?
            return;
        }

        changed = conn.update(imsCall, state);

        if (state == ImsPhoneCall.State.DISCONNECTED) {
            changed = conn.onDisconnect(cause) || changed;
            //detach the disconnected connections
            conn.getCall().detach(conn);
            removeConnection(conn);
        }

        if (changed) {
            if (conn.getCall() == mHandoverCall) return;
            updatePhoneState();
            mPhone.notifyPreciseCallStateChanged();
        }
    }

    ///M: WFC @{
    private void processCallTypeChanged(ImsCall imsCall, int rat) {
        ImsPhoneConnection conn = findConnection(imsCall);
        conn.setCallType(rat);
        //rat 1: LTE, rat 2:Wifi
    }
    /// @}
    private int getDisconnectCauseFromReasonInfo(ImsReasonInfo reasonInfo) {
        int cause = DisconnectCause.ERROR_UNSPECIFIED;

        //int type = reasonInfo.getReasonType();
        int code = reasonInfo.getCode();
        switch (code) {
            case ImsReasonInfo.CODE_SIP_BAD_ADDRESS:
            case ImsReasonInfo.CODE_SIP_NOT_REACHABLE:
                return DisconnectCause.NUMBER_UNREACHABLE;

            case ImsReasonInfo.CODE_SIP_BUSY:
                return DisconnectCause.BUSY;

            case ImsReasonInfo.CODE_USER_TERMINATED:
                return DisconnectCause.LOCAL;

            case ImsReasonInfo.CODE_LOCAL_CALL_DECLINE:
                return DisconnectCause.INCOMING_REJECTED;

            case ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE:
                return DisconnectCause.NORMAL;

            case ImsReasonInfo.CODE_SIP_REDIRECTED:
            case ImsReasonInfo.CODE_SIP_BAD_REQUEST:
            case ImsReasonInfo.CODE_SIP_FORBIDDEN:
            case ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE:
            case ImsReasonInfo.CODE_SIP_USER_REJECTED:
            case ImsReasonInfo.CODE_SIP_GLOBAL_ERROR:
                return DisconnectCause.SERVER_ERROR;

            case ImsReasonInfo.CODE_SIP_SERVICE_UNAVAILABLE:
            case ImsReasonInfo.CODE_SIP_NOT_FOUND:
            //case ImsReasonInfo.CODE_SIP_SERVER_ERROR:
                return DisconnectCause.SERVER_UNREACHABLE;

            case ImsReasonInfo.CODE_LOCAL_NETWORK_ROAMING:
            case ImsReasonInfo.CODE_LOCAL_NETWORK_IP_CHANGED:
            case ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN:
            case ImsReasonInfo.CODE_LOCAL_SERVICE_UNAVAILABLE:
            case ImsReasonInfo.CODE_LOCAL_NOT_REGISTERED:
            case ImsReasonInfo.CODE_LOCAL_NETWORK_NO_LTE_COVERAGE:
            case ImsReasonInfo.CODE_LOCAL_NETWORK_NO_SERVICE:
            case ImsReasonInfo.CODE_LOCAL_CALL_VCC_ON_PROGRESSING:
                return DisconnectCause.OUT_OF_SERVICE;

            case ImsReasonInfo.CODE_SIP_REQUEST_TIMEOUT:
            case ImsReasonInfo.CODE_TIMEOUT_1XX_WAITING:
            case ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER:
            case ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER_CALL_UPDATE:
                return DisconnectCause.TIMED_OUT;

            case ImsReasonInfo.CODE_LOCAL_LOW_BATTERY:
            case ImsReasonInfo.CODE_LOCAL_POWER_OFF:
                return DisconnectCause.POWER_OFF;
            /// M: @{
            case ImsReasonInfo.CODE_SIP_REDIRECTED_EMERGENCY:
                return DisconnectCause.IMS_EMERGENCY_REREG;
            /// @}


            case ImsReasonInfo.CODE_SIP_SERVER_ERROR:
                ///M: WFC @{
                if (mIsWfcEnabled) {
                    return DisconnectCause.WFC_WIFI_SIGNAL_LOST;
                } else {
                    return DisconnectCause.SERVER_UNREACHABLE;
                }
            ///M: WFC @{
            case ImsReasonInfo.CODE_SIP_WIFI_SIGNAL_LOST:
                 return DisconnectCause.WFC_WIFI_SIGNAL_LOST;
            case ImsReasonInfo.CODE_SIP_WFC_ISP_PROBLEM:
                 return DisconnectCause.WFC_ISP_PROBLEM;
            case ImsReasonInfo.CODE_SIP_HANDOVER_WIFI_FAIL:
                 return DisconnectCause.WFC_HANDOVER_WIFI_FAIL;
            case ImsReasonInfo.CODE_SIP_HANDOVER_LTE_FAIL:
                 return DisconnectCause.WFC_HANDOVER_LTE_FAIL;

            /// @}
            default:
        }

        return cause;
    }

    /**
     * Listen to the IMS call state change
     */
    private ImsCall.Listener mImsCallListener = new ImsCall.Listener() {
        @Override
        public void onCallProgressing(ImsCall imsCall) {
            if (DBG) log("onCallProgressing");

            mPendingMO = null;
            processCallStateChange(imsCall, ImsPhoneCall.State.ALERTING,
                    DisconnectCause.NOT_DISCONNECTED);
        }

        @Override
        public void onCallStarted(ImsCall imsCall) {
            if (DBG) log("onCallStarted");

            mPendingMO = null;
            processCallStateChange(imsCall, ImsPhoneCall.State.ACTIVE,
                    DisconnectCause.NOT_DISCONNECTED);
        }

        /// M : WFC @{
        @Override
        public void onCallTypeChanged(ImsCall imsCall, int rat) {
            if (DBG) log("onCallStarted");
                processCallTypeChanged(imsCall, rat);
        }
        /// @}

        /**
         * onCallStartFailed will be invoked when:
         * case 1) Dialing fails
         * case 2) Ringing call is disconnected by local or remote user
         */
        @Override
        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("onCallStartFailed reasonCode=" + reasonInfo.getCode());

            if (mPendingMO != null) {
                // To initiate dialing circuit-switched call
                if (reasonInfo.getCode() == ImsReasonInfo.CODE_LOCAL_CALL_CS_RETRY_REQUIRED
                        && mBackgroundCall.getState() == ImsPhoneCall.State.IDLE
                        && mRingingCall.getState() == ImsPhoneCall.State.IDLE) {
                    mForegroundCall.detach(mPendingMO);
                    removeConnection(mPendingMO);
                    mPendingMO.finalize();
                    mPendingMO = null;
                    mPhone.initiateSilentRedial();
                    return;
                } else {
                    int cause = getDisconnectCauseFromReasonInfo(reasonInfo);
                    /// M: Redial as ECC @{
                    if (cause == DisconnectCause.IMS_EMERGENCY_REREG) {
                        mDialAsECC = true;
                    }
                    /// @}
                    mPendingMO = null;
                    processCallStateChange(imsCall, ImsPhoneCall.State.DISCONNECTED, cause);
                }
            }
        }

        @Override
        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("onCallTerminated reasonCode=" + reasonInfo.getCode());

            ImsPhoneCall.State oldState = mForegroundCall.getState();
            int cause = getDisconnectCauseFromReasonInfo(reasonInfo);
            ImsPhoneConnection conn = findConnection(imsCall);
            if (DBG) log("cause = " + cause + " conn = " + conn);

            if (conn != null && conn.isIncoming() && conn.getConnectTime() == 0) {
                // Missed
                if (cause == DisconnectCause.NORMAL) {
                    cause = DisconnectCause.INCOMING_MISSED;
                }
                if (DBG) log("Incoming connection of 0 connect time detected - translated cause = "
                        + cause);

            }

            if (cause == DisconnectCause.NORMAL && conn != null && conn.getImsCall().isMerged()) {
                // Call was terminated while it is merged instead of a remote disconnect.
                cause = DisconnectCause.IMS_MERGED_SUCCESSFULLY;
            }

            /// M: Redial as ECC @{
            if (cause == DisconnectCause.IMS_EMERGENCY_REREG) {
                mDialAsECC = true;
            }
            /// @}

            processCallStateChange(imsCall, ImsPhoneCall.State.DISCONNECTED, cause);
        }

        @Override
        public void onCallHeld(ImsCall imsCall) {
            if (DBG) log("onCallHeld");

            synchronized (mSyncHold) {
                ImsPhoneCall.State oldState = mBackgroundCall.getState();
                processCallStateChange(imsCall, ImsPhoneCall.State.HOLDING,
                        DisconnectCause.NOT_DISCONNECTED);

                // Note: If we're performing a switchWaitingOrHoldingAndActive, the call to
                // processCallStateChange above may have caused the mBackgroundCall and
                // mForegroundCall references below to change meaning.  Watch out for this if you
                // are reading through this code.
                if (oldState == ImsPhoneCall.State.ACTIVE) {
                    // Note: This case comes up when we have just held a call in response to a
                    // switchWaitingOrHoldingAndActive.  We now need to resume the background call.
                    // The EVENT_RESUME_BACKGROUND causes resumeWaitingOrHolding to be called.
                    if ((mForegroundCall.getState() == ImsPhoneCall.State.HOLDING)
                            || (mRingingCall.getState() == ImsPhoneCall.State.WAITING)) {

                            sendEmptyMessage(EVENT_RESUME_BACKGROUND);
                    } else {
                        //when multiple connections belong to background call,
                        //only the first callback reaches here
                        //otherwise the oldState is already HOLDING
                        if (mPendingMO != null) {
                            sendEmptyMessage(EVENT_DIAL_PENDINGMO);
                        }

                        // In this case there will be no call resumed, so we can assume that we
                        // are done switching fg and bg calls now.
                        // This may happen if there is no BG call and we are holding a call so that
                        // we can dial another one.
                        mSwitchingFgAndBgCalls = false;
                    }
                }
            }
        }

        @Override
        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("onCallHoldFailed reasonCode=" + reasonInfo.getCode());

            synchronized (mSyncHold) {
                ImsPhoneCall.State bgState = mBackgroundCall.getState();
                if (reasonInfo.getCode() == ImsReasonInfo.CODE_LOCAL_CALL_TERMINATED) {
                    // disconnected while processing hold
                    /// M: ALPS02147333 Although call is held failed, but we treat it like call
                    /// held succeeded if the call has been terminated @{
                    if ((mForegroundCall.getState() == ImsPhoneCall.State.HOLDING)
                            || (mRingingCall.getState() == ImsPhoneCall.State.WAITING)) {
                        if (DBG) {
                            log("onCallHoldFailed resume background");
                        }
                        sendEmptyMessage(EVENT_RESUME_BACKGROUND);
                    } else {
                        if (mPendingMO != null) {
                            sendEmptyMessage(EVENT_DIAL_PENDINGMO);
                        }
                    }
                    /// @}
                } else if (bgState == ImsPhoneCall.State.ACTIVE) {
                    mForegroundCall.switchWith(mBackgroundCall);

                    if (mPendingMO != null) {
                        mPendingMO.setDisconnectCause(DisconnectCause.ERROR_UNSPECIFIED);
                        sendEmptyMessageDelayed(EVENT_HANGUP_PENDINGMO, TIMEOUT_HANGUP_PENDINGMO);
                    /// M: Notify Telecom to remove pending action by onActionFailed(). @{
                    } else {
                        ImsPhoneConnection conn = findConnection(imsCall);
                        if (conn != null) {
                            if (DBG) log("onCallHoldFailed: notifySuppServiceFailed");
                            conn.notifySuppServiceFailed(getSuppServiceCode(Phone.SuppService.SWITCH));
                        }
                    }
                    /// @}
                }
            }
        }

        @Override
        public void onCallResumed(ImsCall imsCall) {
            if (DBG) log("onCallResumed");

            // If we are the in midst of swapping FG and BG calls and the call we end up resuming
            // is not the one we expected, we likely had a resume failure and we need to swap the
            // FG and BG calls back.
            if (mSwitchingFgAndBgCalls && imsCall != mCallExpectedToResume) {
                mForegroundCall.switchWith(mBackgroundCall);
                mSwitchingFgAndBgCalls = false;
                mCallExpectedToResume = null;
            }

            /// M: ALPS02023277. @{
            mHasPendingResumeRequest = false;
            /// @}

            processCallStateChange(imsCall, ImsPhoneCall.State.ACTIVE,
                    DisconnectCause.NOT_DISCONNECTED);
        }

        @Override
        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) {
                log("onCallResumeFailed");
            }

            // TODO : What should be done?
            // If we are in the midst of swapping the FG and BG calls and we got a resume fail, we
            // need to swap back the FG and BG calls.
            if (mSwitchingFgAndBgCalls && imsCall == mCallExpectedToResume) {
                mForegroundCall.switchWith(mBackgroundCall);
                mCallExpectedToResume = null;
                mSwitchingFgAndBgCalls = false;
            }

            /// M: ALPS02023277. @{
            mHasPendingResumeRequest = false;
            /// @}

            /// M: Notify Telecom to remove pending action by onActionFailed(). @{
            //mPhone.notifySuppServiceFailed(Phone.SuppService.RESUME);
            ImsPhoneConnection conn = findConnection(imsCall);
            if (conn != null) {
                conn.notifySuppServiceFailed(getSuppServiceCode(Phone.SuppService.RESUME));
            }
            /// @}
        }

        @Override
        public void onCallResumeReceived(ImsCall imsCall) {
            if (DBG) log("onCallResumeReceived");

            if (mOnHoldToneStarted) {
                mPhone.stopOnHoldTone();
                mOnHoldToneStarted = false;
            }
        }

        @Override
        public void onCallHoldReceived(ImsCall imsCall) {
            if (DBG) log("onCallHoldReceived");

            ImsPhoneConnection conn = findConnection(imsCall);
            if (conn != null && conn.getState() == ImsPhoneCall.State.ACTIVE) {
                if (!mOnHoldToneStarted && ImsPhoneCall.isLocalTone(imsCall)) {
                    mPhone.startOnHoldTone();
                    mOnHoldToneStarted = true;
                }
            }
        }

        @Override
        public void onCallMerged(ImsCall call, boolean swapCalls) {
            if (DBG) log("onCallMerged");

            /// M: ALPS02090302. @{
            /// During the active call is merging a background call, if user hangs up the active
            /// call before merge completed, the active connection goes to Disconnecting state,
            /// and cause the new ImsConference become Disconnecting state after replacing
            /// the original active connection as its host connection.
            // Apply ACTIVE state directly since the conference is always active after merged.
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null && !conn.getState().isAlive()) {
                conn.update(call, ImsPhoneCall.State.ACTIVE);
            }
            /// @}

            mForegroundCall.merge(mBackgroundCall, mForegroundCall.getState());

            if (swapCalls) {
                try {
                    switchWaitingOrHoldingAndActive();
                } catch (CallStateException e) {
                    if (Phone.DEBUG_PHONE) {
                        loge("Failed swap fg and bg calls on merge exception=" + e);
                    }
                }
            }

            /// M: ALPS02090302. for debug. @{
            if (conn != null) {
                log("the new conference host connection: " + conn);
            }
            /// @}

            updatePhoneState();
            mPhone.notifyPreciseCallStateChanged();
        }

        @Override
        public void onCallMergeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            if (DBG) log("onCallMergeFailed reasonInfo=" + reasonInfo);
            /// M: Notify Telecom to remove pending action by onActionFailed(). @{
            //mPhone.notifySuppServiceFailed(Phone.SuppService.CONFERENCE);
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null) {
                conn.notifySuppServiceFailed(getSuppServiceCode(Phone.SuppService.CONFERENCE));
            }
            /// @}
        }

        /**
         * Called when the state of IMS conference participant(s) has changed.
         *
         * @param call the call object that carries out the IMS call.
         * @param participants the participant(s) and their new state information.
         */
        @Override
        public void onConferenceParticipantsStateChanged(ImsCall call,
                List<ConferenceParticipant> participants) {
            if (DBG) log("onConferenceParticipantsStateChanged");

            ImsPhoneConnection conn = findConnection(call);
            if (conn != null) {
                conn.updateConferenceParticipants(participants);
            }
        }

       @Override
        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
            mPhone.onTtyModeReceived(mode);
        }

        /// M: @{
        @Override
        public void onCallUpdated(ImsCall call) {
            log("onCallUpdated call=" + call);
            ImsCallProfile callProfile = call.getCallProfile();
            if (callProfile != null) {
                String pau = callProfile.getCallExtra(ImsCallProfile.EXTRA_PAU, "");
                log("onCallUpdated pau=" + pau);
                Bundle bundle = new Bundle();
                ImsPhoneConnection conn = findConnection(call);
                if (conn != null) {
                    bundle.putInt("id", conn.getCallId());
                }
                bundle.putString(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD, pau);
                mPhone.notifyCallInfoChanged(bundle);
            }
        }

        @Override
        public void onCallInviteParticipantsRequestDelivered(ImsCall call) {
            if (DBG) {
                log("onCallInviteParticipantsRequestDelivered");
            }

            ImsPhoneConnection conn = findConnection(call);
            if (conn != null) {
                conn.notifyConferenceParticipantsInvited(true);
            }
        }

        @Override
        public void onCallInviteParticipantsRequestFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            if (DBG) {
                log("onCallInviteParticipantsRequestFailed reasonCode=" +
                    reasonInfo.getCode());
            }
            ImsPhoneConnection conn = findConnection(call);
            if (conn != null) {
                conn.notifyConferenceParticipantsInvited(false);
            }
        }
        /// @}
    };

    /**
     * Listen to the IMS call state change
     */
    private ImsCall.Listener mImsUssdListener = new ImsCall.Listener() {
        @Override
        public void onCallStarted(ImsCall imsCall) {
            if (DBG) log("mImsUssdListener onCallStarted");

            if (imsCall == mUssdSession) {
                if (mPendingUssd != null) {
                    AsyncResult.forMessage(mPendingUssd);
                    mPendingUssd.sendToTarget();
                    mPendingUssd = null;
                }
            }
        }

        @Override
        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("mImsUssdListener onCallStartFailed reasonCode=" + reasonInfo.getCode());

            onCallTerminated(imsCall, reasonInfo);
        }

        @Override
        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo reasonInfo) {
            if (DBG) log("mImsUssdListener onCallTerminated reasonCode=" + reasonInfo.getCode());

            if (imsCall == mUssdSession) {
                mUssdSession = null;
                if (mPendingUssd != null) {
                    CommandException ex =
                            new CommandException(CommandException.Error.GENERIC_FAILURE);
                    AsyncResult.forMessage(mPendingUssd, null, ex);
                    mPendingUssd.sendToTarget();
                    mPendingUssd = null;
                }
            }
            imsCall.close();
        }

        @Override
        public void onCallUssdMessageReceived(ImsCall call,
                int mode, String ussdMessage) {
            if (DBG) log("mImsUssdListener onCallUssdMessageReceived mode=" + mode);

            int ussdMode = -1;

            switch(mode) {
                case ImsCall.USSD_MODE_REQUEST:
                    ussdMode = CommandsInterface.USSD_MODE_REQUEST;
                    break;

                case ImsCall.USSD_MODE_NOTIFY:
                    ussdMode = CommandsInterface.USSD_MODE_NOTIFY;
                    break;
            }

            mPhone.onIncomingUSSD(ussdMode, ussdMessage);
        }
    };

    /**
     * Listen to the IMS service state change
     *
     */
    private ImsConnectionStateListener mImsConnectionStateListener =
        new ImsConnectionStateListener() {
        @Override
        public void onImsConnected() {
            if (DBG) log("onImsConnected");
            mPhone.setServiceState(ServiceState.STATE_IN_SERVICE);
            broadcastImsStateChange(ServiceState.STATE_IN_SERVICE, 0);
        }

        @Override
        public void onImsDisconnected() {
            if (DBG) log("onImsDisconnected");
            mPhone.setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
            mPhone.setImsRegistered(false);
        }

        @Override
        public void onImsResumed() {
            if (DBG) log("onImsResumed");
            mPhone.setServiceState(ServiceState.STATE_IN_SERVICE);
        }

        @Override
        public void onImsSuspended() {
            if (DBG) log("onImsSuspended");
            mPhone.setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
        }

        @Override
        public void onFeatureCapabilityChanged(int serviceClass,
                int[] enabledFeatures, int[] disabledFeatures) {
            if (serviceClass == ImsServiceClass.MMTEL) {
                if (enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE) {
                    mIsVolteEnabled = true;
                }
                if (enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE) {
                    mIsVtEnabled = true;
                }
                ///M: @{
                if (enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI) {
                    mIsWfcEnabled = true;
                }
                /// @}
                if (disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE) {
                    mIsVolteEnabled = false;
                }
                if (disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE) {
                    mIsVtEnabled = false;
                }
                ///M: @{
                if (disabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] ==
                        ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI) {
                    mIsWfcEnabled = false;
                }
                ///@}
            }
            if (DBG) log("onFeatureCapabilityChanged, mIsVolteEnabled = " +  mIsVolteEnabled
                    + " mIsVtEnabled = " + mIsVtEnabled);
            log("[WFC] mIsWfcEnabled=" + mIsWfcEnabled);
            broadcastImscapabilitiesChange(enabledFeatures, disabledFeatures);
        }

        @Override
        public void onImsDisconnectedWithCause(int cause) {
            if (DBG) log("onImsDisconnectedWithCause:" + cause);
            broadcastImsStateChange(ServiceState.STATE_OUT_OF_SERVICE, cause);
        }
    };

    /* package */
    ImsUtInterface getUtInterface() throws ImsException {
        if (mImsManager == null) {
            throw new ImsException("no ims manager", ImsReasonInfo.CODE_UNSPECIFIED);
        }

        ImsUtInterface ut = mImsManager.getSupplementaryServiceConfiguration(mServiceId);
        return ut;
    }

    private void transferHandoverConnections(ImsPhoneCall call) {
        if (call.mConnections != null) {
            for (Connection c : call.mConnections) {
                c.mPreHandoverState = call.mState;
                log ("Connection state before handover is " + c.getStateBeforeHandover());

                /// M: for conference SRVCC. @{
                c.mPreMultipartyState = c.isMultiparty();
                log("SRVCC: Connection isMultiparty before handover is " + c.isMultiparty());
                /// @}
            }
        }
        if (mHandoverCall.mConnections == null ) {
            mHandoverCall.mConnections = call.mConnections;
        } else { // Multi-call SRVCC
            mHandoverCall.mConnections.addAll(call.mConnections);
        }
        if (mHandoverCall.mConnections != null) {
            if (call.getImsCall() != null) {
                call.getImsCall().close();
            }
            for (Connection c : mHandoverCall.mConnections) {
                ((ImsPhoneConnection)c).changeParent(mHandoverCall);
                ((ImsPhoneConnection)c).releaseWakeLock();
            }
        }
        if (call.getState().isAlive()) {
            log ("Call is alive and state is " + call.mState);
            mHandoverCall.mState = call.mState;
        }
        call.mConnections.clear();
        call.mState = ImsPhoneCall.State.IDLE;
    }

    /* package */
    void notifySrvccState(Call.SrvccState state) {
        if (DBG) log("notifySrvccState state=" + state);

        mSrvccState = state;

        if (mSrvccState == Call.SrvccState.COMPLETED) {
            transferHandoverConnections(mForegroundCall);
            transferHandoverConnections(mBackgroundCall);
            transferHandoverConnections(mRingingCall);
            /// M: ALPS02015368 mPendingMO should be cleared when fake SRVCC/bSRVCC happens,
            /// or dial function will fail @{
            if (mPendingMO != null) {
                log("SRVCC: reset mPendingMO");
                removeConnection(mPendingMO);
                mPendingMO = null;
            }
            /// @}
        }
    }

    //****** Overridden from Handler

    @Override
    public void
    handleMessage (Message msg) {
        AsyncResult ar;
        if (DBG) log("handleMessage what=" + msg.what);

        switch (msg.what) {
            case EVENT_HANGUP_PENDINGMO:
                if (mPendingMO != null) {
                    mPendingMO.onDisconnect();
                    removeConnection(mPendingMO);
                    mPendingMO = null;
                }

                updatePhoneState();
                mPhone.notifyPreciseCallStateChanged();
                break;
            case EVENT_RESUME_BACKGROUND:
                try {
                    resumeWaitingOrHolding();
                } catch (CallStateException e) {
                    if (Phone.DEBUG_PHONE) {
                        loge("handleMessage EVENT_RESUME_BACKGROUND exception=" + e);
                    }
                }
                break;
            case EVENT_DIAL_PENDINGMO:
                dialInternal(mPendingMO, mClirMode, VideoProfile.VideoState.AUDIO_ONLY);
                break;

            case EVENT_EXIT_ECM_RESPONSE_CDMA:
                // no matter the result, we still do the same here
                if (pendingCallInEcm) {
                    dialInternal(mPendingMO, pendingCallClirMode, pendingCallVideoState);
                    pendingCallInEcm = false;
                }
                mPhone.unsetOnEcbModeExitResponse(this);
                break;
        }
    }

    @Override
    protected void log(String msg) {
        Rlog.d(LOG_TAG, "[ImsPhoneCallTracker] " + msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[ImsPhoneCallTracker] " + msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ImsPhoneCallTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mVoiceCallEndedRegistrants=" + mVoiceCallEndedRegistrants);
        pw.println(" mVoiceCallStartedRegistrants=" + mVoiceCallStartedRegistrants);
        pw.println(" mRingingCall=" + mRingingCall);
        pw.println(" mForegroundCall=" + mForegroundCall);
        pw.println(" mBackgroundCall=" + mBackgroundCall);
        pw.println(" mHandoverCall=" + mHandoverCall);
        pw.println(" mPendingMO=" + mPendingMO);
        //pw.println(" mHangupPendingMO=" + mHangupPendingMO);
        pw.println(" mPhone=" + mPhone);
        pw.println(" mDesiredMute=" + mDesiredMute);
        pw.println(" mState=" + mState);
    }

    @Override
    protected void handlePollCalls(AsyncResult ar) {
    }

    /* package */
    ImsEcbm getEcbmInterface() throws ImsException {
        if (mImsManager == null) {
            throw new ImsException("no ims manager", ImsReasonInfo.CODE_UNSPECIFIED);
        }

        ImsEcbm ecbm = mImsManager.getEcbmInterface(mServiceId);
        return ecbm;
    }

    public boolean isInEmergencyCall() {
        return mIsInEmergencyCall;
    }

    public boolean isVolteEnabled() {
        return mIsVolteEnabled;
    }

    public boolean isVtEnabled() {
        return mIsVtEnabled;
    }

     /**
        * notify upper application about ims registration information via intent.
        *
        * @param state  the registration state.
        *
        */
    private void broadcastImsStateChange(int state, int errorCode) {
        if (DBG) log("broadcastImsStateChange state= " + state + " errorCode= " + errorCode);
        Intent intent = new Intent(ImsManager.ACTION_IMS_STATE_CHANGED);
        intent.putExtra(ImsManager.EXTRA_IMS_REG_STATE_KEY, state);
        if (state != ServiceState.STATE_IN_SERVICE && errorCode > 0) {
            intent.putExtra(ImsManager.EXTRA_IMS_REG_ERROR_KEY, errorCode);
        }
        intent.putExtra(ImsManager.EXTRA_PHONE_ID, mPhone.getPhoneId());
        mPhone.getContext().sendBroadcast(intent);
    }

     /**
        * notify upper application ims feature capability information via intent.
        *
        * @param enabledFeatures Enabled Feature array
        * @param disabledFeatures disabled feature array
        */
    private void broadcastImscapabilitiesChange(int[] enabledFeatures, int[] disabledFeatures) {
        if (DBG) log("broadcastImscapabilitiesChange");
        Intent intent = new Intent(ImsManager.ACTION_IMS_CAPABILITIES_CHANGED);
        intent.putExtra(ImsManager.EXTRA_IMS_ENABLE_CAP_KEY, enabledFeatures);
        intent.putExtra(ImsManager.EXTRA_IMS_DISABLE_CAP_KEY, disabledFeatures);
        intent.putExtra(ImsManager.EXTRA_PHONE_ID, mPhone.getPhoneId());
        mPhone.getContext().sendBroadcast(intent);

    }

    @Override
    public PhoneConstants.State getState() {
        return mState;
    }

    /// M: For ACTION_IMS_INCOMING_CALL_INDICATION, mIndicationReceiver is 
    /// responsible to handle it @{

    private BroadcastReceiver mIndicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ImsManager.ACTION_IMS_INCOMING_CALL_INDICATION)) {
                if (DBG) log("onReceive : indication call intent");
                
                if (mImsManager == null) {
                    if (DBG) log("no ims manager");
                    return;
                }
                
                boolean isAllow = true; /// default value is always allowed to take call
                int serviceId = intent.getIntExtra(ImsManager.EXTRA_SERVICE_ID, -1);

                String callWaitingSetting = TelephonyManager.getTelephonyProperty(
                    mPhone.getPhoneId(),
                    PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                    TERMINAL_BASED_CALL_WAITING_DISABLED);

                if (callWaitingSetting.equals(
                        TERMINAL_BASED_CALL_WAITING_ENABLED_OFF) == true
                        && mForegroundCall != null
                        && mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
                    log("PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE = "
                            + "TERMINAL_BASED_CALL_WAITING_ENABLED_OFF."
                            + " Reject the call as UDUB ");
                    isAllow = false;
                }

                // Now we handle ECC/MT conflict issue.
                if (isEccExist()) {
                    log("there is an ECC call, dis-allow this incoming call!");
                    isAllow = false;
                }

                if (DBG) log("setCallIndication : serviceId = " + serviceId
                        + ", intent = " + intent 
                        + ", isAllow = " + isAllow);
                try {
                    mImsManager.setCallIndication(mServiceId, intent, isAllow);
                } catch (ImsException e) {
                    loge("setCallIndication ImsException " + e);
                }
            }
        }
    };
    
    private void registerIndicationReceiver() {
        if (DBG) log("registerIndicationReceiver");
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(ImsManager.ACTION_IMS_INCOMING_CALL_INDICATION);
        mPhone.getContext().registerReceiver(mIndicationReceiver, intentfilter);
        
    }
    private void unregisterIndicationReceiver() {
        if (DBG) log("unregisterIndicationReceiver");
        mPhone.getContext().unregisterReceiver(mIndicationReceiver);        
    }

    /**
     * for ALPS01987345, workaround for Modem, block incoming call when ECC exist.
     * @return true if there is ECC.
     */
    private boolean isEccExist() {
        ImsPhoneCall[] allCalls = {mForegroundCall, mBackgroundCall, mRingingCall, mHandoverCall};

        for (int i = 0; i < allCalls.length; i++) {
            if (!allCalls[i].getState().isAlive()) {
                continue;
            }

            ImsCall imsCall = allCalls[i].getImsCall();
            if (imsCall == null) {
                continue;
            }

            ImsCallProfile callProfile = imsCall.getCallProfile();
            if (callProfile != null &&
                    callProfile.mServiceType == ImsCallProfile.SERVICE_TYPE_EMERGENCY) {
                return true;
            }
        }

        log("isEccExist(): no ECC!");
        return false;
    }
    /// @}
    
    /// M: For VoLTE enhanced conference call. @{
    Connection
    dial(List<String> numbers, int videoState) throws CallStateException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        int oirMode = sp.getInt(PhoneBase.CLIR_KEY, CommandsInterface.CLIR_DEFAULT);
        return dial(numbers, oirMode, videoState);
    }

    synchronized Connection
    dial(List<String> numbers, int clirMode, int videoState) throws CallStateException {
        if (DBG) {
            log("dial clirMode=" + clirMode);
        }

        // note that this triggers call state changed notif
        clearDisconnected();

        if (mImsManager == null) {
            throw new CallStateException("service not available");
        }

        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }

        boolean holdBeforeDial = false;

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (mForegroundCall.getState() == ImsPhoneCall.State.ACTIVE) {
            if (mBackgroundCall.getState() != ImsPhoneCall.State.IDLE) {
                //we should have failed in !canDial() above before we get here
                throw new CallStateException("cannot dial in current state");
            }
            // foreground call is empty for the newly dialed connection
            holdBeforeDial = true;
            switchWaitingOrHoldingAndActive();
        }

        ImsPhoneCall.State fgState = ImsPhoneCall.State.IDLE;
        ImsPhoneCall.State bgState = ImsPhoneCall.State.IDLE;

        mClirMode = clirMode;

        synchronized (mSyncHold) {
            if (holdBeforeDial) {
                fgState = mForegroundCall.getState();
                bgState = mBackgroundCall.getState();

                //holding foreground call failed
                if (fgState == ImsPhoneCall.State.ACTIVE) {
                    throw new CallStateException("cannot dial in current state");
                }

                //holding foreground call succeeded
                if (bgState == ImsPhoneCall.State.HOLDING) {
                    holdBeforeDial = false;
                }
            }

            // Create IMS conference host connection
            mPendingMO = new ImsPhoneConnection(mPhone.getContext(), "",
                ImsPhoneCallTracker.this, mForegroundCall);

            ArrayList<String> dialStrings = new ArrayList<String>();
            for (String str : numbers) {
                dialStrings.add(PhoneNumberUtils.extractNetworkPortionAlt(str));
            }
            mPendingMO.setConfDialStrings(dialStrings);
        }
        addConnection(mPendingMO);

        if (!holdBeforeDial) {
            dialInternal(mPendingMO, clirMode, videoState);
        }

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();

        return mPendingMO;
    }
    /// @}

    /// M: @{
    private int getSuppServiceCode(Phone.SuppService service) {
        int actionCode = 0;
        switch (service) {
            case SWITCH:
                actionCode = 1;
                break;
            case CONFERENCE:
                actionCode = 4;
                break;
            case RESUME:
                actionCode = 7;
                break;
            default:
                break;
        }
        return actionCode;
    }
    /// @}
}
