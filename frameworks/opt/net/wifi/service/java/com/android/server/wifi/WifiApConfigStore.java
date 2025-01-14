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

package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.android.internal.util.AsyncChannel;
import com.android.internal.R;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.os.SystemClock;
import com.mediatek.common.MPlugin;
import com.mediatek.common.wifi.IWifiFwkExt;
import java.util.Random;
import android.os.SystemProperties;
import android.provider.Settings;//chenwenshuai for HQ01559367
//add by wanghui for al813 L01 L21 wifi-ap begin
import android.telephony.TelephonyManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
//add by wanghui for al813 L01 L21 wifi-ap end




/**
 * Provides API to the WifiStateMachine for doing read/write access
 * to soft access point configuration
 */
class WifiApConfigStore extends StateMachine {

    private Context mContext;
    private static final String TAG = "WifiApConfigStore";

    private static final String AP_CONFIG_FILE = Environment.getDataDirectory() +
        "/misc/wifi/softap.conf";
    //add by wanghui for al813 L01 L21 wifi-ap begin
    private static final String ACTION = "android.intent.action.SIM_STATE_CHANGED";  
    private IntentFilter filter;
    //add by wanghui for al813 L01 L21 wifi-ap end

    private static final int AP_CONFIG_FILE_VERSION = 1;
	private static final String WIFI_AP_CUSTOM = "wifi_ap_custom";//chenwenshuai for HQ01559367

    private State mDefaultState = new DefaultState();
    private State mInactiveState = new InactiveState();
    private State mActiveState = new ActiveState();

    private WifiConfiguration mWifiApConfig = null;
    private AsyncChannel mReplyChannel = new AsyncChannel();

    WifiApConfigStore(Context context, Handler target) {
        super(TAG, target.getLooper());

        mContext = context;
        addState(mDefaultState);
            addState(mInactiveState, mDefaultState);
            addState(mActiveState, mDefaultState);

        setInitialState(mInactiveState);
		//add by wanghui for al813 wifi-ap begin
       //sim 
        filter = new IntentFilter();  
        filter.addAction(ACTION);  
        mContext.registerReceiver(receiver, filter);  
        //add by wanghui for al813 wifi-ap end
    }

    public static WifiApConfigStore makeWifiApConfigStore(Context context, Handler target) {
        WifiApConfigStore s = new WifiApConfigStore(context, target);
        s.start();
        return s;
    }

	//add by wanghui for al813 wifi-ap begin
    private BroadcastReceiver receiver =new BroadcastReceiver(){
         @Override  
        public void onReceive(Context context, Intent intent){
        		String needCusWifiAp = Settings.System.getString(context.getContentResolver(),WIFI_AP_CUSTOM);
               if(intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED") && !"true".equals(needCusWifiAp)){                       
                       setDefaultApConfiguration();
                       Log.d("wanghui","setDef");
					   String imei = ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
					   if(imei != null && !imei.equals("")){
					   		Settings.System.putString(context.getContentResolver(),WIFI_AP_CUSTOM, "true");
					   }
               }
       }
    };  
    //add by wanghui for al813 wifi-ap end
    class DefaultState extends State {
        public boolean processMessage(Message message) {
            switch (message.what) {
                case WifiStateMachine.CMD_SET_AP_CONFIG:
                case WifiStateMachine.CMD_SET_AP_CONFIG_COMPLETED:
                    Log.e(TAG, "Unexpected message: " + message);
                    break;
                case WifiStateMachine.CMD_REQUEST_AP_CONFIG:
                    mReplyChannel.replyToMessage(message,
                            WifiStateMachine.CMD_RESPONSE_AP_CONFIG, mWifiApConfig);
                    break;
                default:
                    Log.e(TAG, "Failed to handle " + message);
                    break;
            }
            return HANDLED;
        }
    }

    class InactiveState extends State {
        public boolean processMessage(Message message) {
            switch (message.what) {
                case WifiStateMachine.CMD_SET_AP_CONFIG:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (config.SSID != null) {
                        mWifiApConfig = (WifiConfiguration) message.obj;
                        transitionTo(mActiveState);
                    } else {
                        Log.e(TAG, "Try to setup AP config without SSID: " + message);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class ActiveState extends State {
        public void enter() {
            new Thread(new Runnable() {
                public void run() {
                    writeApConfiguration(mWifiApConfig);
                    sendMessage(WifiStateMachine.CMD_SET_AP_CONFIG_COMPLETED);
                }
            }).start();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                //TODO: have feedback to the user when we do this
                //to indicate the write is currently in progress
                case WifiStateMachine.CMD_SET_AP_CONFIG:
                    deferMessage(message);
                    break;
                case WifiStateMachine.CMD_SET_AP_CONFIG_COMPLETED:
                    transitionTo(mInactiveState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    void loadApConfiguration() {
        DataInputStream in = null;
        try {
            WifiConfiguration config = new WifiConfiguration();
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(
                            AP_CONFIG_FILE)));

            int version = in.readInt();
            if (version != 1) {
                Log.e(TAG, "Bad version on hotspot configuration file, set defaults");
                setDefaultApConfiguration();
                return;
            }
            config.SSID = in.readUTF();
            int authType = in.readInt();
            config.allowedKeyManagement.set(authType);
            if (authType != KeyMgmt.NONE) {
                config.preSharedKey = in.readUTF();
            }
            config.channel = in.readInt();
            config.channelWidth = in.readInt();
            mWifiApConfig = config;
        } catch (IOException ignore) {
            setDefaultApConfiguration();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }

    Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    private void writeApConfiguration(final WifiConfiguration config) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(
                        new FileOutputStream(AP_CONFIG_FILE)));

            out.writeInt(AP_CONFIG_FILE_VERSION);
            out.writeUTF(config.SSID);
            int authType = config.getAuthType();
            out.writeInt(authType);
            if(authType != KeyMgmt.NONE) {
                out.writeUTF(config.preSharedKey);
            }
            out.writeInt(config.channel);
            out.writeInt(config.channelWidth);
        } catch (IOException e) {
            Log.e(TAG, "Error writing hotspot configuration" + e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }
    }

    /* Generate a default WPA2 based configuration with a random password.
       We are changing the Wifi Ap configuration storage from secure settings to a
       flat file accessible only by the system. A WPA2 based default configuration
       will keep the device secure after the update */
    private void setDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
	    //add by wanghui for al813 wifi-ap begin
        TelephonyManager tm = (TelephonyManager)mContext.getSystemService(mContext.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        //add by wanghui for al813 wifi-ap end
        IWifiFwkExt wifiFwkExt = MPlugin.createInstance(IWifiFwkExt.class.getName(), mContext);
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            if (wifiFwkExt != null) {
                config.SSID = wifiFwkExt.getApDefaultSsid();
            } else {
                config.SSID = mContext.getString(R.string.wifi_tether_configure_ssid_default);
            }
        } else {
            config.SSID = com.mediatek.custom.CustomProperties.getString(
                        com.mediatek.custom.CustomProperties.MODULE_WLAN,
                        com.mediatek.custom.CustomProperties.SSID,
                        mContext.getString(R.string.wifi_tether_configure_ssid_default));
            if (wifiFwkExt != null && wifiFwkExt.needRandomSsid()) {
                Random random = new Random(SystemClock.elapsedRealtime());
                config.SSID = config.SSID + random.nextInt(1000);
                Log.d(TAG, "setDefaultApConfiguration, SSID:" + config.SSID); 
            }
        }
	String projetcStr = SystemProperties.get("ro.product.name", "");
	/* SW_GLOBAL_COMMON_043, yulifeng, 20150809, wifiap,HQ01301811,b*/	
	 if(SystemProperties.get("ro.hq.wifi.ap.name.pwd").equals("1")) {
                 Random random = new Random();
                 String srandom = Integer.toString(random.nextInt(9000)+1000);
                 if(projetcStr.equals("TAG-L01")) {
				 	if(imei != null){
                       mContext.unregisterReceiver(receiver);
                      String  imeilastfourNumber =imei.substring(imei.length()-4,imei.length());
                       srandom = imeilastfourNumber;
                    }
                         config.SSID = "HUAWEI TAG-L01_" + srandom;
                 } else if(projetcStr.equals("TAG-L21")) {
                 		if(imei != null){
                       mContext.unregisterReceiver(receiver);
                      String  imeilastfourNumber =imei.substring(imei.length()-4,imei.length());
                       srandom = imeilastfourNumber;
                    }
                         config.SSID = "HUAWEI_TAG-L21_" + srandom;//add by xiemin for HQ02057384
                 } else {
                         //modify by yulifeng for wifi ap name HQ01395154 20150930
                         String productStr = SystemProperties.get("ro.product.name", ""); 
                         Log.d("yulifeng","{WifiApConfigStore]_productStr:"+productStr);
                         config.SSID = "HUAWEI " + productStr + "_" +srandom;
                         //end
                 }
	 }
	 /* SW_GLOBAL_COMMON_043, yulifeng, 20150809, wifiap,HQ01301811,e*/
        config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
        String randomUUID = UUID.randomUUID().toString();      
	/* SW_GLOBAL_COMMON_043, yulifeng, 20150809, wifiap,HQ01301811,b*/
	if(SystemProperties.get("ro.hq.wifi.ap.name.pwd").equals("1")){
		//first 8 chars for key
		config.preSharedKey = randomUUID.substring(0, 8);
	}
	else{
		//first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        	config.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9,13);
	}
	/* SW_GLOBAL_COMMON_043, yulifeng, 20150809, wifiap,HQ01301811,e*/
        sendMessage(WifiStateMachine.CMD_SET_AP_CONFIG, config);
    }
}
