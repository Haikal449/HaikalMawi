/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

import com.android.internal.telephony.cat.bip.OtherAddress;
import com.android.internal.telephony.cat.bip.TransportProtocol;
import com.android.internal.telephony.cat.bip.BipUtils;
import com.android.internal.telephony.cat.bip.ChannelStatus;
import com.android.internal.telephony.cat.bip.BearerDesc;

/**
 * Class used to pass CAT messages from telephony to application. Application
 * should call getXXX() to get commands's specific values.
 *
 */
public class CatCmdMessage implements Parcelable {
    // members
    CommandDetails mCmdDet;
    private TextMessage mTextMsg;
    private Menu mMenu;
    private Input mInput;
    private BrowserSettings mBrowserSettings = null;
    private ToneSettings mToneSettings = null;
    private CallSettings mCallSettings = null;
    private SetupEventListSettings mSetupEventListSettings = null;

    //yanqing add for stkappname
    public Menu myMenu;

    // Add by Huibin Mao MTK80229
    // ICS Migration start
    // parameters for BIP
    public BearerDesc mBearerDesc = null;
    public int mBufferSize = 0;
    public OtherAddress mLocalAddress = null;
    public TransportProtocol mTransportProtocol = null;
    public OtherAddress mDataDestinationAddress = null;

    public String mApn = null;
    public String mLogin = null;
    public String mPwd = null;

    public int mChannelDataLength = 0;
    public int mRemainingDataLength = 0;
    public byte[] mChannelData = null;

    public ChannelStatus mChannelStatusData = null;

    public int mCloseCid = 0;
    public int mSendDataCid = 0;
    public int mReceiveDataCid = 0;
    public boolean mCloseBackToTcpListen = false;
    public int mSendMode = 0;
    public List<ChannelStatus> mChannelStatusList = null;

    public int mInfoType = 0;
    public String mDestAddress = null;

    // ICS Migration end

    /*
     * Container for Launch Browser command settings.
     */
    public class BrowserSettings {
        public String url;
        public LaunchBrowserMode mode;
    }

    /*
     * Container for Call Setup command settings.
     */
    public class CallSettings {
        public TextMessage confirmMsg;
        public TextMessage callMsg;
    }

    public class SetupEventListSettings {
        public int[] eventList;
    }

    public final class SetupEventListConstants {
        // Event values in SETUP_EVENT_LIST Proactive Command as per ETSI 102.223
        public static final int USER_ACTIVITY_EVENT          = 0x04;
        public static final int IDLE_SCREEN_AVAILABLE_EVENT  = 0x05;
        public static final int LANGUAGE_SELECTION_EVENT     = 0x07;
        public static final int BROWSER_TERMINATION_EVENT    = 0x08;
        public static final int BROWSING_STATUS_EVENT        = 0x0F;
    }

    public final class BrowserTerminationCauses {
        public static final int USER_TERMINATION             = 0x00;
        public static final int ERROR_TERMINATION            = 0x01;
    }

    CatCmdMessage(CommandParams cmdParams) {
        mCmdDet = cmdParams.mCmdDet;
        switch(getCmdType()) {
        case SET_UP_MENU:
        case SELECT_ITEM:
            mMenu = ((SelectItemParams) cmdParams).mMenu;
            //yanqing add for stkappname
            myMenu = mMenu;
            break;
        case DISPLAY_TEXT:
        case SET_UP_IDLE_MODE_TEXT:
        case SEND_DTMF:
        case SEND_SMS:
        case SEND_SS:
        case SEND_USSD:
            mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
            break;
        case GET_INPUT:
        case GET_INKEY:
            mInput = ((GetInputParams) cmdParams).mInput;
            break;
        case LAUNCH_BROWSER:
            mTextMsg = ((LaunchBrowserParams) cmdParams).mConfirmMsg;
            mBrowserSettings = new BrowserSettings();
            mBrowserSettings.url = ((LaunchBrowserParams) cmdParams).mUrl;
            mBrowserSettings.mode = ((LaunchBrowserParams) cmdParams).mMode;
            break;
        case PLAY_TONE:
            PlayToneParams params = (PlayToneParams) cmdParams;
            mToneSettings = params.mSettings;
            mTextMsg = params.mTextMsg;
            break;
        case GET_CHANNEL_STATUS:
            mTextMsg = ((GetChannelStatusParams) cmdParams).textMsg;
            break;
        case SET_UP_CALL:
            mCallSettings = new CallSettings();
            mCallSettings.confirmMsg = ((CallSetupParams) cmdParams).mConfirmMsg;
            mCallSettings.callMsg = ((CallSetupParams) cmdParams).mCallMsg;
            break;
        case OPEN_CHANNEL:
            mBearerDesc = ((OpenChannelParams) cmdParams).bearerDesc;
            mBufferSize = ((OpenChannelParams) cmdParams).bufferSize;
            mLocalAddress = ((OpenChannelParams) cmdParams).localAddress;
            mTransportProtocol = ((OpenChannelParams) cmdParams).transportProtocol;
            mDataDestinationAddress = ((OpenChannelParams) cmdParams).dataDestinationAddress;
            mTextMsg = ((OpenChannelParams) cmdParams).textMsg;

            if (mBearerDesc != null) {
                if (mBearerDesc.bearerType == BipUtils.BEARER_TYPE_GPRS ||
                    mBearerDesc.bearerType == BipUtils.BEARER_TYPE_DEFAULT ||
                    mBearerDesc.bearerType == BipUtils.BEARER_TYPE_EUTRAN) {
                    mApn = ((OpenChannelParams) cmdParams).gprsParams.accessPointName;
                    mLogin = ((OpenChannelParams) cmdParams).gprsParams.userLogin;
                    mPwd = ((OpenChannelParams) cmdParams).gprsParams.userPwd;
                }
            } else {
                CatLog.d("[BIP]", "Invalid BearerDesc object");
            }
            break;
        case CLOSE_CHANNEL:
            mTextMsg = ((CloseChannelParams) cmdParams).textMsg;
            mCloseCid = ((CloseChannelParams) cmdParams).mCloseCid;
            mCloseBackToTcpListen = ((CloseChannelParams) cmdParams).mBackToTcpListen;
            break;
        case RECEIVE_DATA:
            mTextMsg = ((ReceiveDataParams) cmdParams).textMsg;
            mChannelDataLength = ((ReceiveDataParams) cmdParams).channelDataLength;
            mReceiveDataCid = ((ReceiveDataParams) cmdParams).mReceiveDataCid;
            break;
        case SEND_DATA:
            mTextMsg = ((SendDataParams) cmdParams).textMsg;
            mChannelData = ((SendDataParams) cmdParams).channelData;
            mSendDataCid = ((SendDataParams) cmdParams).mSendDataCid;
            mSendMode = ((SendDataParams) cmdParams).mSendMode;
            break;
        case REFRESH:
            mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
            break;
        case SET_UP_EVENT_LIST:
        /* L-MR1
            mSetupEventListSettings = new SetupEventListSettings();
            mSetupEventListSettings.eventList = ((SetEventListParams) cmdParams).mEventInfo;
            */
            break;
        case PROVIDE_LOCAL_INFORMATION:
            break;
        case CALLCTRL_RSP_MSG:
            mTextMsg = ((CallCtrlBySimParams) cmdParams).mTextMsg;
            mInfoType = ((CallCtrlBySimParams) cmdParams).mInfoType;
            mDestAddress = ((CallCtrlBySimParams) cmdParams).mDestAddress;
            break;
        default:
            break;
        }
    }

    public CatCmdMessage(Parcel in) {
        mCmdDet = in.readParcelable(null);
        mTextMsg = in.readParcelable(null);
        mMenu = in.readParcelable(null);
        mInput = in.readParcelable(null);
        switch (getCmdType()) {
        case LAUNCH_BROWSER:
            mBrowserSettings = new BrowserSettings();
            mBrowserSettings.url = in.readString();
            mBrowserSettings.mode = LaunchBrowserMode.values()[in.readInt()];
            break;
        case PLAY_TONE:
            mToneSettings = in.readParcelable(null);
            break;
        case SET_UP_CALL:
            mCallSettings = new CallSettings();
            mCallSettings.confirmMsg = in.readParcelable(null);
            mCallSettings.callMsg = in.readParcelable(null);
            break;
        case SET_UP_EVENT_LIST:
/* L-MR1
            mSetupEventListSettings = new SetupEventListSettings();
            int length = in.readInt();
            mSetupEventListSettings.eventList = new int[length];
            for (int i = 0; i < length; i++) {
                mSetupEventListSettings.eventList[i] = in.readInt();
            }
*/
            break;
        // Add by Huibin Mao MTK80229
        // ICS Migration start
        case OPEN_CHANNEL:
            mBearerDesc = in.readParcelable(null);
            break;
        // ICS Migration end
        default:
            break;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mCmdDet, 0);
        dest.writeParcelable(mTextMsg, 0);
        dest.writeParcelable(mMenu, 0);
        dest.writeParcelable(mInput, 0);
        switch(getCmdType()) {
        case LAUNCH_BROWSER:
            dest.writeString(mBrowserSettings.url);
            dest.writeInt(mBrowserSettings.mode.ordinal());
            break;
        case PLAY_TONE:
            dest.writeParcelable(mToneSettings, 0);
            break;
        case SET_UP_CALL:
            dest.writeParcelable(mCallSettings.confirmMsg, 0);
            dest.writeParcelable(mCallSettings.callMsg, 0);
            break;
        case SET_UP_EVENT_LIST:
            //L-MR1
            //dest.writeIntArray(mSetupEventListSettings.eventList);
            break;
        // Add by Huibin Mao MTK80229
        // ICS Migration start
        case OPEN_CHANNEL:
            dest.writeParcelable(mBearerDesc, 0);
            break;
         // ICS Migration end
        default:
            break;
        }
    }

    public static final Parcelable.Creator<CatCmdMessage> CREATOR = new Parcelable.Creator<CatCmdMessage>() {
        @Override
        public CatCmdMessage createFromParcel(Parcel in) {
            return new CatCmdMessage(in);
        }

        @Override
        public CatCmdMessage[] newArray(int size) {
            return new CatCmdMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    // Add by Huibin Mao MTK80229
    // ICS Migration start
    /* external API to be used by application */
    /**
     * Return command qualifier
     * @internal
     */
    public int getCmdQualifier() {
        return mCmdDet.commandQualifier;
    }

    // ICS Migration end

    public AppInterface.CommandType getCmdType() {
        return AppInterface.CommandType.fromInt(mCmdDet.typeOfCommand);
    }

    public Menu getMenu() {
        return mMenu;
    }

    public Input geInput() {
        return mInput;
    }

    public TextMessage geTextMessage() {
        return mTextMsg;
    }

    public BrowserSettings getBrowserSettings() {
        return mBrowserSettings;
    }

    public ToneSettings getToneSettings() {
        return mToneSettings;
    }

    public CallSettings getCallSettings() {
        return mCallSettings;
    }

    public SetupEventListSettings getSetEventList() {
        return mSetupEventListSettings;
    }
    /**
     * Return bearer description
     * @internal
     */
    public BearerDesc getBearerDesc() {
        return mBearerDesc;
    }
}
