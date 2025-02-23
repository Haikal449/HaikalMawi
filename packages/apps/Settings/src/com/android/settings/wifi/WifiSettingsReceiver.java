package com.android.settings;
 
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
 
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
 
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.telephony.TelephonyManagerEx;
 
import java.util.List;
 

public class WifiSettingsReceiver extends BroadcastReceiver {
    
    private final static String TAG = "WifiSettingsReceiver";
    //static final String CMCC_SSID = "CMCC";
    //static final String CMCC_AUTO_SSID = "CMCC-AUTO";
    private static final String WIFI_AUTO_SSID = "Singtel WiFi";
	private static final String CHT_AUTO_SSID = "CHT Wi-Fi Auto";
    private static final String FET_AUTO_SSID = "FET Wi-Fi Auto";
	private static final String MOVISTAR_AUTO_SSID = "MOVISTAR WIFI";//chenwenshuai
	private static final String VIVO_AUTO_SSID = "VIVO-WIFI";//chenwenshuai
    //private String WIFI_AUTO_SSID_1 = "VIVO-WIFI";
    private static final String PREF_REMIND = "pref_remind";
    private static final String PREF_REMIND_HOTSPOT = "pref_remind_hotspot";
    private static final String PREF_REMIND_CONNECT = "pref_remind_connect";
    private static final String CRYPT_KEEPER = "com.android.settings.CryptKeeper";

    private static final int SIM_CARD_1 = PhoneConstants.SIM_ID_1;
    private static final int SIM_CARD_2 = PhoneConstants.SIM_ID_2;
    private static final int SIM_CARD_UNDEFINED = -1;
    // constant for current sim mode
    private static final int ALL_RADIO_ON = 3;
    private static final int INVALID_NETWORK_ID = -1;
 

    private static final int BUFFER_LENGTH = 40;
    private static final int MNC_SUB_BEG = 3;
    private static final int MNC_SUB_END = 5;
    private static final int MCC_SUB_BEG = 0;
    //remind user if connect to access point
    private static final int WIFI_CONNECT_REMINDER_ALWAYS = 0;
  
    /*to mark if the tcard is insert, set to true only the time SD inserted*/
    private WifiManager mWifiManager;
    private TelephonyManager mTm;
    private TelephonyManagerEx mTelephonyManagerEx;
    private static boolean sHasDisconnect = true;
  
    private int mSimId;
    private int mAutoConnect;
    private String mNumeric;
  
      @Override
      public void onReceive(Context context, Intent intent) {
   
          String stateExtra;
          boolean mIsSIMExist = false;
          boolean isCmccCard = false;
   
          String action = intent.getAction();
          if(!(SystemProperties.get("ro.hq.southeast.wifi.eapsim").equals("1"))){
              return;
          }
          if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
              SharedPreferences sh = context.getSharedPreferences("flight_mode_notify", context.MODE_WORLD_READABLE);
              boolean isEnable = sh.getBoolean(PREF_REMIND, true);
              Log.i(TAG, "flgiht isEnable = " + isEnable);
              if (isEnable) {
                  boolean state = Settings.System.getInt(context.getContentResolver(),
                      Settings.System.AIRPLANE_MODE_ON, 0) != 0;
                  if (state) {
                      ActivityManager amgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                      ComponentName cpname = null;
                      String classname = null;
                      if (amgr.getRunningTasks(1) != null && amgr.getRunningTasks(1).get(0) != null) {
                          cpname = amgr.getRunningTasks(1).get(0).topActivity;
                      }
                      if (cpname != null) {
                          classname = cpname.getClassName();
                          Log.i(TAG, "ClassName:" + classname);
                          if (classname != null && classname.equals(CRYPT_KEEPER)) {
                              Log.i(TAG, "CryptKeeper screen");
                              return;
                          }
                      } 
                      // deleted by liweixin for [CMCC] remove airplanemode confirm dialog
                      /*Intent start = new Intent("com.mediatek.OP01.WIFI_FLIGHT_MODE_NOTIFY");
                      start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                      context.startActivity(start);*/
                  }
              }
              return;
          }
  
          if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
              NetworkInfo netInfo =
                  (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
              NetworkInfo.DetailedState dState = netInfo.getDetailedState();
  
              if (dState == NetworkInfo.DetailedState.DISCONNECTED) {
                  sHasDisconnect = true;
                  Log.i(TAG, "wifi disconnected state");
              } else if (sHasDisconnect
                          && dState == NetworkInfo.DetailedState.CONNECTED) {
                  sHasDisconnect = false;
                  WifiInfo wifiInfo =
                      (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                  Log.i(TAG, "wifi connected state");
                  if (wifiInfo != null) {
                      Log.i(TAG, "wifi info");
                      int networkId = wifiInfo.getNetworkId();
                      if (networkId == INVALID_NETWORK_ID) {
                          Log.i(TAG, "can't get networkId");
                          return;
                      }
  
                      String ssid = removeDoubleQuotes(wifiInfo.getSSID());
					  Log.i(TAG, "ylf[WifiSettingsReceiver]_ssid="+ssid);
                      if (WIFI_AUTO_SSID.equals(ssid) || CHT_AUTO_SSID.equals(ssid) || FET_AUTO_SSID.equals(ssid)
					  	  || MOVISTAR_AUTO_SSID.equals(ssid) || VIVO_AUTO_SSID.equals(ssid)) {
                      //if (WIFI_AUTO_SSID.equals(ssid)) {
                          WifiManager wifiManager =
                              (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                          final List<WifiConfiguration> configs
                              = wifiManager.getConfiguredNetworks();
                          boolean isCMCC = false;
                          if (configs != null) {
                              for (WifiConfiguration config : configs) {
                                  if (config != null
                                      && networkId == config.networkId
                                      && (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                                         || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X))) {
                                      isCMCC = true;
                                      break;
                                  }
                              }
                          }
  
                          if (!isCMCC) {
                              return;
                          }
  
                          SharedPreferences sh = context.getSharedPreferences(
                                  "wifi_connect_notify",
                                  context.MODE_WORLD_READABLE);
                          boolean isEnable = sh.getBoolean(PREF_REMIND_CONNECT, true);
                          int value = Settings.System.getInt(context.getContentResolver(),
                                  Settings.System.WIFI_CONNECT_REMINDER,
                                  WIFI_CONNECT_REMINDER_ALWAYS);
                          Log.i(TAG, "wifi connect remind = " + isEnable + value);
                          if (isEnable && value == WIFI_CONNECT_REMINDER_ALWAYS) {
                              /*Intent start = new Intent(context, WifiConnectNotifyDialog.class);
                              start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                              context.startActivity(start);*/
                          }
                      }
                  }
              }
              return;
          }
  
          mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
          mTelephonyManagerEx = TelephonyManagerEx.getDefault();
  
          if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
              int state = intent.getIntExtra(
                      WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
              Log.i(TAG, "state = " + state);
              if (state == WifiManager.WIFI_AP_STATE_ENABLED) {
                  boolean isDataOn = mTm.getDataEnabled();
                  Log.i(TAG, "isDataOn = " + isDataOn);
  
                  if (!isDataOn) {
                     SharedPreferences sh = context.getSharedPreferences("wifi_hotspot_enabled_notify", context.MODE_WORLD_READABLE);
                      boolean isEnable = sh.getBoolean(PREF_REMIND_HOTSPOT, true);
                      Log.i(TAG, "wifi hotspot remind = " + isEnable);
                      if (isEnable) {
                          /*Intent start = new Intent(context, WifiHotspotNotifyDialog.class);
                          start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                          context.startActivity(start);*/
                      }
                  }
              }
              return;
          }
  
          mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
          Log.i(TAG, "onReceive() action = " + action);
  
          mAutoConnect = Settings.System.getInt(context.getContentResolver(),
              Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
          final int wifiState = mWifiManager.getWifiState();
          Log.i(TAG, "onReceive() wifiState = " + wifiState);
          /*if it is because wifi enabled it wifi update the CMCC-AUTO config
              about SIM slot AND IMSI*/
          if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
              && (wifiState == WifiManager.WIFI_STATE_ENABLED)
              && SystemProperties.get("ro.mtk_eap_sim_aka").equals("1"))
          {
              final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
              Log.i(TAG, "onReceive()  WIFI_STATE_ENABLED configs = " + configs);
              if (configs != null)
              {
                  for (WifiConfiguration config : configs)
                  {
                      Log.i(TAG, "onReceive() SSID = " + config.SSID + "simSlot = " + config.simSlot);
                      Log.i(TAG, "onReceive() config = " + config.toString());
                      if (((WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (CHT_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (FET_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (MOVISTAR_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (VIVO_AUTO_SSID.equals(removeDoubleQuotes(config.SSID))))
					 /* if (WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID))*/
                          && (config.toString().contains("eap SIM") || config.toString().contains("eap AKA")))
                      {
                          Log.i(TAG, "onReceive() find CMCC-AUTO");
                          /*the first time it try to set the CMCC-AUTO config*/
                          if (config.simSlot == null)
                          {
                              int slotId = SIM_CARD_1;
                              Log.i(TAG, "onReceive() first time update");
                              if (SystemProperties.get("ro.mtk_gemini_support").equals("1"))
                              {
                                  Log.i(TAG, "onReceive() dual sim load");
                                  /*if ((!mTm.hasIccCard(SIM_CARD_1) || !checkCmccCard(SIM_CARD_1,config.SSID))
                                         && (mTm.hasIccCard(SIM_CARD_2) && checkCmccCard(SIM_CARD_2,config.SSID))) {
                                     slotId = SIM_CARD_2;
                                 }*/
                                 /*HQ_yulifeng add for eap-sim first update,20151017,b*/
                                 if(mTm.hasIccCard(SIM_CARD_1) && checkCmccCard(SIM_CARD_1,config.SSID)){
                                     updateCmccConfig(slotId, config, mWifiManager);
                                 }else if(mTm.hasIccCard(SIM_CARD_2) && checkCmccCard(SIM_CARD_2,config.SSID)){
                                     updateCmccConfig(slotId, config, mWifiManager);
                                 }else{
                                     continue;
                                 }
                              }
                              else{
                                  if(mTm.hasIccCard(SIM_CARD_1) && checkCmccCard(SIM_CARD_1,config.SSID)){
                                      updateCmccConfig(slotId, config, mWifiManager);
                                  }else{
                                      continue;
                                  }
                              }
                           }
                           /*HQ_yulifeng add for eap-sim first update,20151017,e*/
                           else
                           {
                              /*only to update the right simslot*/
                             if (SystemProperties.get("ro.mtk_gemini_support").equals("1"))
                              {
                                  int curSlot = SIM_CARD_1;
                                  int otherSlot = SIM_CARD_2;
                                  Log.i(TAG, "onReceive() dual sim load, not first time");
                                  if (config.simSlot.equals(addQuote("1"))) {
                                      curSlot = SIM_CARD_2;
                                      otherSlot = SIM_CARD_1;
                                      Log.i(TAG, "onReceive() default is sim2");
                                  }
 
                                  int slotId = curSlot;
                                  if (mTm.hasIccCard(curSlot)
                                     && !(config.imsi.equals(makeNAI(getSubscriberId(curSlot), "SIM"))
                                         || config.imsi.equals(makeNAI(mTm.getSubscriberId(curSlot), "AKA")))) {
                                      Log.i(TAG, "onReceive() sim inserted, but imsi change");
                                      if (!checkCmccCard(curSlot,config.SSID)
                                          && (mTm.hasIccCard(otherSlot) && checkCmccCard(otherSlot,config.SSID))) {
                                          slotId = otherSlot;
  
                                      }
                                      updateCmccConfig(slotId, config, mWifiManager);
                                  } else if (!mTm.hasIccCard(curSlot)) {
                                    Log.i(TAG, "onReceive() last sim is NOT INSERT");
                                      if (mTm.hasIccCard(otherSlot) && checkCmccCard(otherSlot,config.SSID)) {
                                          slotId = otherSlot;
                                     }
                                      updateCmccConfig(slotId, config, mWifiManager);
                                }
                              }
                              else
                              {
                                  if (mTm.hasIccCard(SIM_CARD_1)
                                      && !(config.imsi.equals(makeNAI(getSubscriberId(SIM_CARD_1), "SIM"))
                                        || config.imsi.equals(makeNAI(mTm.getSubscriberId(SIM_CARD_1), "AKA")))) {
                                      Log.i(TAG, "onReceive() imsi change");
                                      updateCmccConfig(SIM_CARD_1, config, mWifiManager);
                                  }
                              }
                          }
                          break;
                      }
                  }
              }
              return;
          }
  
  
          /*if the sim card insert or plug out, and the wifi is enabled, need update the CMCC-AUTO config
              about SIM slot and IMSI*/
          if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)
              && (wifiState == WifiManager.WIFI_STATE_ENABLED)
              && SystemProperties.get("ro.mtk_eap_sim_aka").equals("1"))
          {
              stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
              int mSimId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
  
              /*if it is in the state of bootup done or sim card inserted*/
              final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
              Log.i(TAG, "onReceive() stateExtra = " + stateExtra + "; simid = " + mSimId);
              if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra)
                  && (configs != null))
              {
                  Log.i(TAG, "onReceive() INTENT_VALUE_ICC_LOADED");
                  for (WifiConfiguration config : configs)
                  {
                      Log.i(TAG, "onReceive() SSID = " + config.SSID);
                      if (((WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (CHT_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (FET_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (MOVISTAR_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (VIVO_AUTO_SSID.equals(removeDoubleQuotes(config.SSID))))
					 /* if ((WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))*/
                          && (config.toString().contains("eap SIM") || config.toString().contains("eap AKA")))
                      {
                          Log.i(TAG, "onReceive() siminserted, old imsi = " + config.imsi + "; simSlot = " + config.simSlot);
                          if (SystemProperties.get("ro.mtk_gemini_support").equals("1"))
                          {
                              if (config.simSlot == null) {
                                  if (checkCmccCard(mSimId,config.SSID)) {
                                      Log.i(TAG, "onReceive() first CMCC CARD");
                                      updateCmccConfig(mSimId, config, mWifiManager);
                                  }
                              } else {
                                  if (((mSimId == SIM_CARD_1) && (addQuote("0").equals(config.simSlot)))
                                      || ((mSimId == SIM_CARD_2) && (addQuote("1").equals(config.simSlot))))
                                  {
                                      Log.i(TAG, "onReceive() insert the same SIM card ");
                                      if (checkCmccCard(mSimId,config.SSID)
                                          && !(config.imsi.equals(makeNAI(getSubscriberId(mSimId), "SIM"))
                                              || config.imsi.equals(makeNAI(mTm.getSubscriberId(mSimId), "AKA"))))
                                      {
                                          updateCmccConfig(mSimId, config, mWifiManager);
                                      }
                                  }
                                  else
                                  {
                                      Log.i(TAG, "onReceive() sim plugin insert sim != config.simSlot");
                                      int loadSlot = SIM_CARD_1;
                                      int otherSlot = SIM_CARD_2;
  
                                      if (mSimId == SIM_CARD_2)
                                      {
                                          loadSlot = SIM_CARD_2;
                                          otherSlot = SIM_CARD_1;
                                      }
  
                                      if ((!mTm.hasIccCard(otherSlot) || !checkCmccCard(otherSlot,config.SSID))
                                          && checkCmccCard(loadSlot,config.SSID)) {
                                          Log.i(TAG, "onReceive() the other sim isn't insert or cmcc card, new insert sim is cmcc card");
                                          updateCmccConfig(loadSlot, config, mWifiManager);
                                      }
                                  }
                              }
                          } else {
                              updateCmccConfig(SIM_CARD_1, config, mWifiManager);
                          }
                          break;
                      }
                  }
                  return;
              }
  
              /*if it is in the state of sim card plugout*/
              if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)
                  && (configs != null))
              {
                  Log.i(TAG, "onReceive() [sim plugout] configs = " + configs);
                  for (WifiConfiguration config : configs)
                  {
                      Log.i(TAG, "onReceive() [sim plugout] SSID = " + config.SSID);
                      if (((WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (CHT_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (FET_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (MOVISTAR_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))
					  	  || (VIVO_AUTO_SSID.equals(removeDoubleQuotes(config.SSID))))
					 /* if ((WIFI_AUTO_SSID.equals(removeDoubleQuotes(config.SSID)))*/
                          && (config.toString().contains("eap SIM") || config.toString().contains("eap AKA")))
                      {
                          if (config.simSlot != null)
                          {
                              Log.i(TAG, "onReceive() CMCC-AUTO configs = " + config.toString());
                              if (SystemProperties.get("ro.mtk_gemini_support").equals("1"))
                              {
                                  /*if the sim is beening used, it should update the config*/
                                  if ((mSimId == SIM_CARD_1 && config.simSlot.equals(addQuote("0")))
                                      || (mSimId == SIM_CARD_2 && config.simSlot.equals(addQuote("1"))))
                                  {
                                      Log.i(TAG, "onReceive() the sim used plug out");
                                      int otherSlot = SIM_CARD_2;
                                      if (mSimId == SIM_CARD_2)
                                      {
                                          otherSlot = SIM_CARD_1;
                                      }
  
                                      if (mTm.hasIccCard(otherSlot) && checkCmccCard(otherSlot,config.SSID)) {
                                          Log.i(TAG, "onReceive() the other sim is cmcc card");
                                          updateCmccConfig(otherSlot, config, mWifiManager);
                                      }
                                  }
                              }
                              else
                              {
                                  updateCmccConfig(SIM_CARD_1, config, mWifiManager);
                              }
                          }
                          break;
                      }
                  }
                  return;
              }
          }
      }
  
       /**
         * M: make NAI
         * @param imsi eapMethod
         * @return the string of NAI
         */
      private static String makeNAI(String imsi, String eapMethod) {
          // airplane mode & select wrong sim slot
          if (imsi == null) {
                return addQuote("error");
          }
  
          StringBuffer NAI = new StringBuffer(BUFFER_LENGTH);
          // s = sb.append("a = ").append(a).append("!").toString();
          System.out.println("".length());
  
          if (eapMethod.equals("SIM")) {
                NAI.append("1");
          } else if (eapMethod.equals("AKA")) {
                NAI.append("1");
          }
  
  
          // add imsi
          NAI.append(imsi);
          NAI.append("@wlan.mnc");
          // add mnc
          NAI.append("0");
          NAI.append(imsi.substring(MNC_SUB_BEG, MNC_SUB_END));
          NAI.append(".mcc");
          // add mcc
          NAI.append(imsi.substring(MCC_SUB_BEG, MNC_SUB_BEG));
  
          // NAI.append(imsi.substring(5));
          NAI.append(".3gppnetwork.org");
          Log.i(TAG, NAI.toString());
          Log.i(TAG, "\"" + NAI.toString() + "\"");
          return addQuote(NAI.toString());
      }
  
        /**
         * M: add quote for strings
         * @param string
         * @return add quote to the string
         */
      private static String addQuote(String s) {
          return "\"" + s + "\"";
      }
  
      private String removeDoubleQuotes(String string) {
          if (string != null) {
              int length = string.length();
              if ((length > 1) && (string.charAt(0) == '"')
                  && (string.charAt(length - 1) == '"')) {
                  return string.substring(1, length - 1);
              }
          }
          return string;
      }
  
      private boolean checkCmccCard(int simId) {
          boolean isCmccCard = false;
          int subId = getSubIdBySlot(simId);
          mNumeric = mTm.getSimOperator(subId);
          //isCmccCard = mNumeric.equals("52501") || mNumeric.equals("52502") || mNumeric.equals("46692") || mNumeric.equals("46601");
          isCmccCard = mNumeric.equals("52501") || mNumeric.equals("52502") || mNumeric.equals("46692") || mNumeric.equals("46601");
          //isCmccCard = mNumeric.equals("46000") || mNumeric.equals("46002");
          return isCmccCard;
      }

      //HQ_yulifeng add for check card by simId \ ssid
	  private boolean checkCmccCard(int simId,String ssid) {
          int subId = getSubIdBySlot(simId);
          mNumeric = mTm.getSimOperator(subId);
		  if(("52501".equals(mNumeric)||"52502".equals(mNumeric))&& WIFI_AUTO_SSID.equals(removeDoubleQuotes(ssid))) {
              Log.d(TAG, "ylf_checkCmccCard = true,CMCC coming,ssid = "+ssid);
              return true;
          }
		  if(("46692".equals(mNumeric))&& CHT_AUTO_SSID.equals(removeDoubleQuotes(ssid))) {
              Log.d(TAG, "ylf_checkCmccCard = true,CHT Wi-Fi Auto coming,ssid = "+ssid);
              return true;
          }
		  if(("46601".equals(mNumeric))&& FET_AUTO_SSID.equals(removeDoubleQuotes(ssid))) {
              Log.d(TAG, "ylf_checkCmccCard = true,FET Wi-Fi Auto coming,ssid = "+ssid);
              return true;
          }
		  if(("74000".equals(mNumeric)||"33403".equals(mNumeric)||"71204".equals(mNumeric)
		  	||"70604".equals(mNumeric)||"70403".equals(mNumeric)||"710300".equals(mNumeric)
		  	||"714020".equals(mNumeric)||"71606".equals(mNumeric)||"73002".equals(mNumeric)
		  	||"74807".equals(mNumeric)||"72207".equals(mNumeric)||"732123".equals(mNumeric)
		  	||"73404".equals(mNumeric))&& MOVISTAR_AUTO_SSID.equals(removeDoubleQuotes(ssid))) {
              Log.d(TAG, "ylf_checkCmccCard = true,FET Wi-Fi Auto coming,ssid = "+ssid);
              return true;
          }
          return false;
      }
  
      private void updateCmccConfig(int simslot, WifiConfiguration config, WifiManager mWifiManager) {
          String simtype = null;
          simtype = getIccCardType(simslot);
          Log.i(TAG, " updateCmccConfig() simslot = " + simslot);
  
          if (simtype != null) {
              if (simslot == SIM_CARD_1) {
                  config.simSlot = addQuote("0");
              } else if (simslot == SIM_CARD_2) {
                  config.simSlot = addQuote("1");
              }
              config.pcsc = addQuote("rild");
              if (config.toString().contains("eap SIM")) {
                  config.imsi = makeNAI(getSubscriberId(simslot), "SIM");
                  Log.i(TAG, " updateCmccConfig() SIM imsi = " + config.imsi + ";simSlot = " + config.simSlot);
                  updateConfig(config);
              } else if (config.toString().contains("eap AKA")) {
                  config.imsi = makeNAI(getSubscriberId(simslot), "AKA");
                  Log.i(TAG, "updateCmccConfig() AKA imsi = " + config.imsi + ";simSlot = " + config.simSlot);
                  updateConfig(config);
              }
          }
      }
  
      private void reConnectCmccAuto() {
          Log.i(TAG, "reConnectCmccAuto()");
          if (mAutoConnect != Settings.System.WIFI_CONNECT_TYPE_AUTO) {
              Log.i(TAG, "reConnectCmccAuto() auto connnect is off");
              return;
          }
  
          List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
          if (configs == null) {
              return;
          }
          int totalSize = configs.size();
          WifiConfiguration highPriorutyAp = null;
          if (totalSize > 0) {
              highPriorutyAp = configs.get(0);
          }
  
          for (int i = 1; i < totalSize; i++) {
              WifiConfiguration curAp = configs.get(i);
              if (highPriorutyAp == null) {
                   highPriorutyAp = curAp;
              } else if (curAp == null) {
                    continue;
              } else if (curAp.priority > highPriorutyAp.priority) {
                  highPriorutyAp = curAp;
              }
          }
  
          if (highPriorutyAp != null) {
              Log.i(TAG, "config.ssid=" + highPriorutyAp.SSID
                          + ",priority=" + highPriorutyAp.priority);
              String highSSID = (highPriorutyAp.SSID == null ? ""
                      : removeDoubleQuotes(highPriorutyAp.SSID));
              if ((WIFI_AUTO_SSID.equals(highSSID)||CHT_AUTO_SSID.equals(highSSID)||FET_AUTO_SSID.equals(highSSID)
			  	   ||MOVISTAR_AUTO_SSID.equals(highSSID)||VIVO_AUTO_SSID.equals(highSSID)) && highPriorutyAp.networkId != -1) {
              //if ((WIFI_AUTO_SSID.equals(highSSID))&& highPriorutyAp.networkId != -1) {
                  mWifiManager.connect(highPriorutyAp.networkId, null);
                  Log.i(TAG, "reConnectCmccAuto() done");
              }
          }
      }
  
      private int getSubIdBySlot(int slot) {
          int [] subId = SubscriptionManager.getSubId(slot);
          Log.i(TAG, "getSubIdBySlot, simId " + slot +
                  "subId " + ((subId != null) ? subId[0] : "invalid!"));
          return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubId();
      }
  
      private String getIccCardType(int slot) {
          String simtype = null;
          int subId = getSubIdBySlot(slot);
          simtype = mTelephonyManagerEx.getIccCardType(subId);
          return simtype;
      }
  
      private String getSubscriberId(int slot) {
          String subscriber = null;
          int subId = getSubIdBySlot(slot);
          subscriber = mTm.getSubscriberId(subId);
          return subscriber;
      }
  
      private void updateConfig(WifiConfiguration config) {
          Log.i(TAG, "updateConfig()");
          if (config == null) {
              return;
          }
          WifiConfiguration newConfig = new WifiConfiguration();
          newConfig.networkId = config.networkId;
          newConfig.priority = -1;
          newConfig.imsi = config.imsi;
          newConfig.simSlot = config.simSlot;
          newConfig.pcsc = config.pcsc;
          newConfig.enterpriseConfig.setAnonymousIdentity("");
          //newConfig.anonymous_identity.setValue("");
         mWifiManager.updateNetwork(newConfig);
 
        reConnectCmccAuto();
     }
 
 }
