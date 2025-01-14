/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.net.wifi.BatchedScanSettings;
import android.net.wifi.RttManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiScanner;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.SystemClock;
import android.text.TextUtils;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.util.LocalLog;
import android.util.Log;
import android.os.SystemProperties;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native calls for bring up/shut down of the supplicant daemon and for
 * sending requests to the supplicant daemon
 *
 * waitForEvent() is called on the monitor thread for events. All other methods
 * must be serialized from the framework.
 *
 * {@hide}
 */
public class WifiNative {

    private static boolean DBG = false;
    private final String mTAG;
    private static final int DEFAULT_GROUP_OWNER_INTENT     = 6;

    static final int BLUETOOTH_COEXISTENCE_MODE_ENABLED     = 0;
    static final int BLUETOOTH_COEXISTENCE_MODE_DISABLED    = 1;
    static final int BLUETOOTH_COEXISTENCE_MODE_SENSE       = 2;

    static final int SCAN_WITHOUT_CONNECTION_SETUP          = 1;
    static final int SCAN_WITH_CONNECTION_SETUP             = 2;

    ///M: for gbk ssid  @{
    private static final String SSID_STR = "ssid=";
    private static final String DELIMITER_STR = "====";
    private static final String END_STR = "####";
    ArrayList<AccessPoint> mIsGBKMapping = new ArrayList<AccessPoint>();
    ///@}

    // Hold this lock before calling supplicant - it is required to
    // mutually exclude access from Wifi and P2p state machines
    static final Object mLock = new Object();

    public final String mInterfaceName;
    public final String mInterfacePrefix;

    private boolean mSuspendOptEnabled = false;

    /* Register native functions */

    static {
        /* Native functions are defined in libwifi-service.so */
        System.loadLibrary("wifi-service");
        registerNatives();
    }

    private static native int registerNatives();

    public native static boolean loadDriver();

    public native static boolean isDriverLoaded();

    public native static boolean unloadDriver();

    public native static boolean startSupplicant(boolean p2pSupported);

    /* Sends a kill signal to supplicant. To be used when we have lost connection
       or when the supplicant is hung */
    public native static boolean killSupplicant(boolean p2pSupported);

    private native boolean connectToSupplicantNative();

    private native void closeSupplicantConnectionNative();

    /**
     * Wait for the supplicant to send an event, returning the event string.
     * @return the event string sent by the supplicant.
     */
    ///M: modify
    private native String waitForEventNative(String interfaceName);

    private native boolean doBooleanCommandNative(String command);

    private native int doIntCommandNative(String command);

    private native String doStringCommandNative(String command);

    ///M: for gbk ssid @{
    private native byte[] doByteArrayCommandNative(String command);

    private native byte[] waitForEventInByteArrayNative(String interfaceName);
    ///@}

///M: add
    private boolean mDisconnectCalled = false;

///M: ALPS02164902 switch of UTF8-like GBK encoding
    public boolean mUtf8LikeGbkEncoding = true;

    public WifiNative(String interfaceName) {
        mInterfaceName = interfaceName;
        mTAG = "WifiNative-" + interfaceName;
        if (!interfaceName.equals("p2p0")) {
            mInterfacePrefix = "IFNAME=" + interfaceName + " ";
        } else {
            // commands for p2p0 interface don't need prefix
            mInterfacePrefix = "";
        }
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            //DBG = true;
	      DBG = false;//chenwenshuai modify for HQ01545923 at 2015-12-03
        } else {
            DBG = false;
        }
    }

    private static final LocalLog mLocalLog = new LocalLog(1024);

    // hold mLock before accessing mCmdIdLock
    private static int sCmdId;

    public LocalLog getLocalLog() {
        return mLocalLog;
    }

    private static int getNewCmdIdLocked() {
        return sCmdId++;
    }

    private void localLog(String s) {
        if (mLocalLog != null)
            mLocalLog.log(mInterfaceName + ": " + s);
    }

    public boolean connectToSupplicant() {
        synchronized(mLock) {
            localLog(mInterfacePrefix + "connectToSupplicant");
            return connectToSupplicantNative();
        }
    }

    public void closeSupplicantConnection() {
        synchronized(mLock) {
            localLog(mInterfacePrefix + "closeSupplicantConnection");
            closeSupplicantConnectionNative();
        }
    }

    public String waitForEvent() {
        // No synchronization necessary .. it is implemented in WifiMonitor
        ///M: modify for gbk ssid @{
        //return waitForEventNative(mInterfaceName);
        byte[] event = waitForEventInByteArrayNative(mInterfaceName);
        String ret = parseSsidInByteArray(event);
        return ret;
        //@}
    }

    private boolean doBooleanCommand(String command) {
        if (DBG) Log.d(mTAG, "doBoolean: " + command);
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + mInterfacePrefix + command;
            boolean result = doBooleanCommandNative(mInterfacePrefix + command);
            localLog(toLog + " -> " + result);
            if (DBG) Log.d(mTAG, command + ": returned " + result);
            return result;
        }
    }

    private int doIntCommand(String command) {
        if (DBG) Log.d(mTAG, "doInt: " + command);
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + mInterfacePrefix + command;
            int result = doIntCommandNative(mInterfacePrefix + command);
            localLog(toLog + " -> " + result);
            if (DBG) Log.d(mTAG, "   returned " + result);
            return result;
        }
    }

    private String doStringCommand(String command) {
        if (DBG) {
            //GET_NETWORK commands flood the logs
            if (!command.startsWith("GET_NETWORK")) {
                Log.d(mTAG, "doString: [" + command + "]");
            }
        }
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + mInterfacePrefix + command;
            String result = doStringCommandNative(mInterfacePrefix + command);
            if (result == null) {
                if (DBG) Log.d(mTAG, "doStringCommandNative no result");
            } else {
                if (!command.startsWith("STATUS-")) {
                    localLog(toLog + " -> " + result);
                }
                if (DBG) Log.d(mTAG, "   returned " + result.replace("\n", " "));
            }
            return result;
        }
    }

    private String doStringCommandWithoutLogging(String command) {
        if (DBG) {
            //GET_NETWORK commands flood the logs
            if (!command.startsWith("GET_NETWORK")) {
                Log.d(mTAG, "doString: [" + command + "]");
            }
        }
        synchronized (mLock) {
            return doStringCommandNative(mInterfacePrefix + command);
        }
    }

    ///M: for gbk ssid
    private byte[] doByteArrayCommand(String command) {
        if(DBG) Log.d(mTAG, "doByteArray: " + command);
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + mInterfacePrefix + command;
            return doByteArrayCommandNative(mInterfacePrefix + command);
        }
    }

    public boolean ping() {
        String pong = doStringCommand("PING");
        return (pong != null && pong.equals("PONG"));
    }

    public void setSupplicantLogLevel(String level) {
        doStringCommand("LOG_LEVEL " + level);
    }

    public String getFreqCapability() {
        return doStringCommand("GET_CAPABILITY freq");
    }

    public boolean scan(int type, String freqList) {
		//add by yanzewen for cmcc WIFI-OTA test
		if(SystemProperties.get("sys.wlan_scan_test").equals("1")){
			return false;
		}
        else if (type == SCAN_WITHOUT_CONNECTION_SETUP) {
            if (freqList == null) return doBooleanCommand("SCAN TYPE=ONLY");
            else return doBooleanCommand("SCAN TYPE=ONLY freq=" + freqList);
        } else if (type == SCAN_WITH_CONNECTION_SETUP) {
            if (freqList == null) return doBooleanCommand("SCAN");
            else return doBooleanCommand("SCAN freq=" + freqList);
        } else {
            throw new IllegalArgumentException("Invalid scan type");
        }
    }

    /* Does a graceful shutdown of supplicant. Is a common stop function for both p2p and sta.
     *
     * Note that underneath we use a harsh-sounding "terminate" supplicant command
     * for a graceful stop and a mild-sounding "stop" interface
     * to kill the process
     */
    public boolean stopSupplicant() {
        return doBooleanCommand("TERMINATE");
    }

    public String listNetworks() {
        ///M: for gbk ssid @{
        //return doStringCommand("LIST_NETWORKS");
        byte[] byteArray = doByteArrayCommand("LIST_NETWORKS");
        return parseSsidInByteArray(byteArray);
        ///@}
    }

    public String listNetworks(int last_id) {
        ///M: for gbk ssid @{
        //return doStringCommand("LIST_NETWORKS LAST_ID=" + last_id);
        byte[] byteArray = doByteArrayCommand("LIST_NETWORKS LAST_ID=" + last_id);
        return parseSsidInByteArray(byteArray);
        ///@}
    }

    public int addNetwork() {
        Log.d(mTAG, "addNetwork, mInterfaceName = " + mInterfaceName);
        if (mInterfaceName.equals("p2p0")) {
            return doIntCommandNative("IFNAME=" + mInterfaceName + " " + "ADD_NETWORK");
        }
        return doIntCommand("ADD_NETWORK");
    }

    public boolean setNetworkVariable(int netId, String name, String value) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) return false;
        if(mInterfaceName.equals("p2p0")){
            String prefix = "IFNAME=" + mInterfaceName + " ";
	    return doBooleanCommand(prefix + "SET_NETWORK " + netId + " " + name + " " + value);
        }
        return doBooleanCommand("SET_NETWORK " + netId + " " + name + " " + value);
    }

    ///M: for gbk ssid
    public boolean setNetworkVariable(int netId, String name, String value, String bssid) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) return false;
        if (name.equals("ssid")) {
            Boolean isGBK = null;
            String ssid = removeDoubleQuotes(value);
            isGBK = getIsGBKFromMapping(bssid, ssid);
            if (isGBK == null) {
                Log.d(mTAG, "this networkId is not exist in the mapping");
                ///M: ALPS02163016 special symbol may fail to connect if not encode ssid
                value = encodeSSID(value);
            } else {
                if (isGBK) {
                    value = encodeGbkSSID(value);
                } else {
                    value = encodeSSID(value);
                }
            }
            Log.d(mTAG, "netId: " + netId + ", isGBK: " + isGBK + ", value: " + value);
        }
        return doBooleanCommand("SET_NETWORK " + netId + " " + name + " " + value);
    }

    public String getNetworkVariable(int netId, String name) {
        if (TextUtils.isEmpty(name)) return null;
        ///M: for gbk ssid @{
        if (name.equals("ssid")) {
            String ssidString = null;
            byte[] ssidByteArray = doByteArrayCommand("GET_NETWORK " + netId + " " + name);
            ssidString = parseSsidInByteArray(ssidByteArray);
            return ssidString;
        }
        ///@}
        // GET_NETWORK will likely flood the logs ...
        return doStringCommandWithoutLogging("GET_NETWORK " + netId + " " + name);
    }

    public boolean removeNetwork(int netId) {
        return doBooleanCommand("REMOVE_NETWORK " + netId);
    }


    private void logDbg(String debug) {
        long now = SystemClock.elapsedRealtimeNanos();
        String ts = String.format("[%,d us] ", now/1000);
        Log.e("WifiNative: ", ts+debug+ " stack:"
                + Thread.currentThread().getStackTrace()[2].getMethodName() +" - "
                + Thread.currentThread().getStackTrace()[3].getMethodName() +" - "
                + Thread.currentThread().getStackTrace()[4].getMethodName() +" - "
                + Thread.currentThread().getStackTrace()[5].getMethodName()+" - "
                + Thread.currentThread().getStackTrace()[6].getMethodName());

    }
    public boolean enableNetwork(int netId, boolean disableOthers) {
        if (DBG) logDbg("enableNetwork nid=" + Integer.toString(netId)
                + " disableOthers=" + disableOthers);
        if (disableOthers) {
            return doBooleanCommand("SELECT_NETWORK " + netId);
        } else {
            return doBooleanCommand("ENABLE_NETWORK " + netId);
        }
    }

    public boolean disableNetwork(int netId) {
        if (DBG) logDbg("disableNetwork nid=" + Integer.toString(netId));
        return doBooleanCommand("DISABLE_NETWORK " + netId);
    }

    public boolean reconnect() {
        ///M: add
        mDisconnectCalled = false;
        if (DBG) logDbg("RECONNECT ");
        return doBooleanCommand("RECONNECT");
    }

    public boolean reassociate() {
        if (DBG) logDbg("REASSOCIATE ");
        return doBooleanCommand("REASSOCIATE");
    }

    public boolean disconnect() {
        ///M: add
        mDisconnectCalled = true;
        if (DBG) logDbg("DISCONNECT ");
        return doBooleanCommand("DISCONNECT");
    }

    public String status() {
        return status(false);
    }

    public String status(boolean noEvents) {
        ///M: for gbk ssid @{
        byte[] status;
        if (noEvents) {
        status = doByteArrayCommand("STATUS-NO_EVENTS");
        } else {
            status = doByteArrayCommand("STATUS");
        }
        return parseSsidInByteArray(status);
        ///@}
    }

    public String getMacAddress() {
        //Macaddr = XX.XX.XX.XX.XX.XX
        String ret = doStringCommand("DRIVER MACADDR");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" = ");
            if (tokens.length == 2) return tokens[1];
        }
        return null;
    }

    /**
     * Format of results:
     * =================
     * id=1
     * bssid=68:7f:74:d7:1b:6e
     * freq=2412
     * level=-43
     * tsf=1344621975160944
     * age=2623
     * flags=[WPA2-PSK-CCMP][WPS][ESS]
     * ssid=zubyb
     * ====
     *
     * RANGE=ALL gets all scan results
     * RANGE=ID- gets results from ID
     * MASK=<N> see wpa_supplicant/src/common/wpa_ctrl.h for details
     */
    public String scanResults(int sid) {
        ///M: for gbk ssid @{
        // return doStringCommandWithoutLogging("BSS RANGE=" + sid + "- MASK=0x21987");

        String scanResult = null;
        byte[] byteArray = doByteArrayCommand("BSS RANGE=" + sid + "- MASK=0x21987");
        ///M: ALPS02027115 scan result is null, add error handling
        if (byteArray != null) {
            scanResult = parseScanResultInByteArray(byteArray);
        }
        return scanResult;
        ///@}
    }

    /**
     * Format of result:
     * id=1016
     * bssid=00:03:7f:40:84:10
     * freq=2462
     * beacon_int=200
     * capabilities=0x0431
     * qual=0
     * noise=0
     * level=-46
     * tsf=0000002669008476
     * age=5
     * ie=00105143412d485332302d52322d54455354010882848b960c12182403010b0706555...
     * flags=[WPA2-EAP-CCMP][ESS][P2P][HS20]
     * ssid=QCA-HS20-R2-TEST
     * p2p_device_name=
     * p2p_config_methods=0x0SET_NE
     * anqp_venue_name=02083d656e6757692d466920416c6c69616e63650a3239383920436f...
     * anqp_network_auth_type=010000
     * anqp_roaming_consortium=03506f9a05001bc504bd
     * anqp_ip_addr_type_availability=0c
     * anqp_nai_realm=0200300000246d61696c2e6578616d706c652e636f6d3b636973636f2...
     * anqp_3gpp=000600040132f465
     * anqp_domain_name=0b65786d61706c652e636f6d
     * hs20_operator_friendly_name=11656e6757692d466920416c6c69616e63650e636869...
     * hs20_wan_metrics=01c40900008001000000000a00
     * hs20_connection_capability=0100000006140001061600000650000106bb010106bb0...
     * hs20_osu_providers_list=0b5143412d4f53552d425353010901310015656e6757692d...
     */
    public String scanResult(String bssid) {
        return doStringCommand("BSS " + bssid);
    }

    /**
     * Format of command
     * DRIVER WLS_BATCHING SET SCANFREQ=x MSCAN=r BESTN=y CHANNEL=<z, w, t> RTT=s
     * where x is an ascii representation of an integer number of seconds between scans
     *       r is an ascii representation of an integer number of scans per batch
     *       y is an ascii representation of an integer number of the max AP to remember per scan
     *       z, w, t represent a 1..n size list of channel numbers and/or 'A', 'B' values
     *           indicating entire ranges of channels
     *       s is an ascii representation of an integer number of highest-strength AP
     *           for which we'd like approximate distance reported
     *
     * The return value is an ascii integer representing a guess of the number of scans
     * the firmware can remember before it runs out of buffer space or -1 on error
     */
    public String setBatchedScanSettings(BatchedScanSettings settings) {
        if (settings == null) {
            return doStringCommand("DRIVER WLS_BATCHING STOP");
        }
        String cmd = "DRIVER WLS_BATCHING SET SCANFREQ=" + settings.scanIntervalSec;
        cmd += " MSCAN=" + settings.maxScansPerBatch;
        if (settings.maxApPerScan != BatchedScanSettings.UNSPECIFIED) {
            cmd += " BESTN=" + settings.maxApPerScan;
        }
        if (settings.channelSet != null && !settings.channelSet.isEmpty()) {
            cmd += " CHANNEL=<";
            int i = 0;
            for (String channel : settings.channelSet) {
                cmd += (i > 0 ? "," : "") + channel;
                ++i;
            }
            cmd += ">";
        }
        if (settings.maxApForDistance != BatchedScanSettings.UNSPECIFIED) {
            cmd += " RTT=" + settings.maxApForDistance;
        }
        return doStringCommand(cmd);
    }

    public String getBatchedScanResults() {
        ///M: for gbk ssid @{
        //return doStringCommand("DRIVER WLS_BATCHING GET");
        byte[] byteArray = doByteArrayCommand("DRIVER WLS_BATCHING GET");
        return parseSsidInByteArray(byteArray);
        ///@}
    }

    public boolean startDriver() {
        return doBooleanCommand("DRIVER START");
    }

    public boolean stopDriver() {
        return doBooleanCommand("DRIVER STOP");
    }


    /**
     * Start filtering out Multicast V4 packets
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     *
     * Multicast filtering rules work as follows:
     *
     * The driver can filter multicast (v4 and/or v6) and broadcast packets when in
     * a power optimized mode (typically when screen goes off).
     *
     * In order to prevent the driver from filtering the multicast/broadcast packets, we have to
     * add a DRIVER RXFILTER-ADD rule followed by DRIVER RXFILTER-START to make the rule effective
     *
     * DRIVER RXFILTER-ADD Num
     *   where Num = 0 - Unicast, 1 - Broadcast, 2 - Mutil4 or 3 - Multi6
     *
     * and DRIVER RXFILTER-START
     * In order to stop the usage of these rules, we do
     *
     * DRIVER RXFILTER-STOP
     * DRIVER RXFILTER-REMOVE Num
     *   where Num is as described for RXFILTER-ADD
     *
     * The  SETSUSPENDOPT driver command overrides the filtering rules
     */
    public boolean startFilteringMulticastV4Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP")
            && doBooleanCommand("DRIVER RXFILTER-REMOVE 2")
            && doBooleanCommand("DRIVER RXFILTER-START");
    }

    /**
     * Stop filtering out Multicast V4 packets.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public boolean stopFilteringMulticastV4Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP")
            && doBooleanCommand("DRIVER RXFILTER-ADD 2")
            && doBooleanCommand("DRIVER RXFILTER-START");
    }

    /**
     * Start filtering out Multicast V6 packets
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public boolean startFilteringMulticastV6Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP")
            && doBooleanCommand("DRIVER RXFILTER-REMOVE 3")
            && doBooleanCommand("DRIVER RXFILTER-START");
    }

    /**
     * Stop filtering out Multicast V6 packets.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public boolean stopFilteringMulticastV6Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP")
            && doBooleanCommand("DRIVER RXFILTER-ADD 3")
            && doBooleanCommand("DRIVER RXFILTER-START");
    }

    public int getBand() {
       String ret = doStringCommand("DRIVER GETBAND");
        if (!TextUtils.isEmpty(ret)) {
            //reply is "BAND X" where X is the band
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) return Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setBand(int band) {
        return doBooleanCommand("DRIVER SETBAND " + band);
    }

    /**
      * Sets the bluetooth coexistence mode.
      *
      * @param mode One of {@link #BLUETOOTH_COEXISTENCE_MODE_DISABLED},
      *            {@link #BLUETOOTH_COEXISTENCE_MODE_ENABLED}, or
      *            {@link #BLUETOOTH_COEXISTENCE_MODE_SENSE}.
      * @return Whether the mode was successfully set.
      */
    public boolean setBluetoothCoexistenceMode(int mode) {
        return doBooleanCommand("DRIVER BTCOEXMODE " + mode);
    }

    /**
     * Enable or disable Bluetooth coexistence scan mode. When this mode is on,
     * some of the low-level scan parameters used by the driver are changed to
     * reduce interference with A2DP streaming.
     *
     * @param isSet whether to enable or disable this mode
     * @return {@code true} if the command succeeded, {@code false} otherwise.
     */
    public boolean setBluetoothCoexistenceScanMode(boolean setCoexScanMode) {
        if (setCoexScanMode) {
            return doBooleanCommand("DRIVER BTCOEXSCAN-START");
        } else {
            return doBooleanCommand("DRIVER BTCOEXSCAN-STOP");
        }
    }

    public void enableSaveConfig() {
        doBooleanCommand("SET update_config 1");
    }

    public boolean saveConfig() {
        return doBooleanCommand("SAVE_CONFIG");
    }

    public boolean addToBlacklist(String bssid) {
        if (TextUtils.isEmpty(bssid)) return false;
        return doBooleanCommand("BLACKLIST " + bssid);
    }

    public boolean clearBlacklist() {
        return doBooleanCommand("BLACKLIST clear");
    }

    public boolean setSuspendOptimizations(boolean enabled) {
       // if (mSuspendOptEnabled == enabled) return true;
        mSuspendOptEnabled = enabled;

        Log.e("native", "do suspend " + enabled);
        if (enabled) {
            return doBooleanCommand("DRIVER SETSUSPENDMODE 1");
        } else {
            return doBooleanCommand("DRIVER SETSUSPENDMODE 0");
        }
    }

    public boolean setCountryCode(String countryCode) {
        return doBooleanCommand("DRIVER COUNTRY " + countryCode.toUpperCase(Locale.ROOT));
    }

    public void enableBackgroundScan(boolean enable) {
        if (enable) {
            doBooleanCommand("SET pno 1");
        } else {
            doBooleanCommand("SET pno 0");
        }
    }

    public void enableAutoConnect(boolean enable) {
        if (enable) {
            doBooleanCommand("STA_AUTOCONNECT 1");
        } else {
            doBooleanCommand("STA_AUTOCONNECT 0");
        }
    }

    public void setScanInterval(int scanInterval) {
        doBooleanCommand("SCAN_INTERVAL " + scanInterval);
    }

    public void startTdls(String macAddr, boolean enable) {
        if (enable) {
            doBooleanCommand("TDLS_DISCOVER " + macAddr);
            doBooleanCommand("TDLS_SETUP " + macAddr);
        } else {
            doBooleanCommand("TDLS_TEARDOWN " + macAddr);
        }
    }

    /** Example output:
     * RSSI=-65
     * LINKSPEED=48
     * NOISE=9999
     * FREQUENCY=0
     */
    public String signalPoll() {
        return doStringCommandWithoutLogging("SIGNAL_POLL");
    }

    /** Example outout:
     * TXGOOD=396
     * TXBAD=1
     */
    public String pktcntPoll() {
        return doStringCommand("PKTCNT_POLL");
    }

    public void bssFlush() {
        ///M: ALPS01771578: to flush p2p persistent group GO information last time  @{
        if (mInterfaceName.equals("p2p0")) {
            doBooleanCommand("IFNAME=" + mInterfaceName + " BSS_FLUSH 0");
            return;
        }
        ///@}
        doBooleanCommand("BSS_FLUSH 0");
    }

    public boolean startWpsPbc(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return doBooleanCommand("WPS_PBC");
        } else {
            return doBooleanCommand("WPS_PBC " + bssid);
        }
    }

    public boolean startWpsPbc(String iface, String bssid) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(bssid)) {
                return doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC");
            } else {
                return doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC " + bssid);
            }
        }
    }

    public boolean startWpsPinKeypad(String pin) {
        if (TextUtils.isEmpty(pin)) return false;
        return doBooleanCommand("WPS_PIN any " + pin);
    }

    public boolean startWpsPinKeypad(String iface, String pin) {
        if (TextUtils.isEmpty(pin)) return false;
        synchronized (mLock) {
            return doBooleanCommandNative("IFNAME=" + iface + " WPS_PIN any " + pin);
        }
    }


    public String startWpsPinDisplay(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return doStringCommand("WPS_PIN any");
        } else {
            return doStringCommand("WPS_PIN " + bssid);
        }
    }

    public String startWpsPinDisplay(String iface, String bssid) {
        synchronized (mLock) {
            if (TextUtils.isEmpty(bssid)) {
                return doStringCommandNative("IFNAME=" + iface + " WPS_PIN any");
            } else {
                return doStringCommandNative("IFNAME=" + iface + " WPS_PIN " + bssid);
            }
        }
    }

    public boolean setExternalSim(boolean external) {
        synchronized (mLock) {
            String value = external ? "1" : "0";
            Log.d(TAG, "Setting external_sim to " + value);
            return doBooleanCommand("SET external_sim " + value);
        }
    }

    public boolean simAuthResponse(int id, String response) {
        synchronized (mLock) {
            return doBooleanCommand("CTRL-RSP-SIM-" + id + ":GSM-AUTH" + response);
        }
    }

    /* Configures an access point connection */
    public boolean startWpsRegistrar(String bssid, String pin) {
        if (TextUtils.isEmpty(bssid) || TextUtils.isEmpty(pin)) return false;
        return doBooleanCommand("WPS_REG " + bssid + " " + pin);
    }

    public boolean cancelWps() {
        return doBooleanCommand("WPS_CANCEL");
    }

    public boolean setPersistentReconnect(boolean enabled) {
        int value = (enabled == true) ? 1 : 0;
        return doBooleanCommand("SET persistent_reconnect " + value);
    }

    public boolean setDeviceName(String name) {
        return doBooleanCommand("SET device_name " + name);
    }

    public boolean setDeviceType(String type) {
        return doBooleanCommand("SET device_type " + type);
    }

    public boolean setConfigMethods(String cfg) {
        return doBooleanCommand("SET config_methods " + cfg);
    }

    public boolean setManufacturer(String value) {
        return doBooleanCommand("SET manufacturer " + value);
    }

    public boolean setModelName(String value) {
        return doBooleanCommand("SET model_name " + value);
    }

    public boolean setModelNumber(String value) {
        return doBooleanCommand("SET model_number " + value);
    }

    public boolean setSerialNumber(String value) {
        return doBooleanCommand("SET serial_number " + value);
    }

    public boolean setP2pSsidPostfix(String postfix) {
        return doBooleanCommand("SET p2p_ssid_postfix " + postfix);
    }

    public boolean setP2pGroupIdle(String iface, int time) {
        synchronized (mLock) {
            return doBooleanCommandNative("IFNAME=" + iface + " SET p2p_group_idle " + time);
        }
    }

    public void setPowerSave(boolean enabled) {
        if (enabled) {
            doBooleanCommand("SET ps 1");
        } else {
            doBooleanCommand("SET ps 0");
        }
    }

    public boolean setP2pPowerSave(String iface, boolean enabled) {
        synchronized (mLock) {
            if (enabled) {
                return doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 1");
            } else {
                return doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 0");
            }
        }
    }

    public boolean setWfdEnable(boolean enable) {
        return doBooleanCommand("SET wifi_display " + (enable ? "1" : "0"));
    }

    public boolean setWfdDeviceInfo(String hex) {
        return doBooleanCommand("WFD_SUBELEM_SET 0 " + hex);
    }

    /**
     * "sta" prioritizes STA connection over P2P and "p2p" prioritizes
     * P2P connection over STA
     */
    public boolean setConcurrencyPriority(String s) {
        return doBooleanCommand("P2P_SET conc_pref " + s);
    }

    public boolean p2pFind() {
        return doBooleanCommand("P2P_FIND");
    }

    public boolean p2pFind(int timeout) {
        if (timeout <= 0) {
            return p2pFind();
        }
        return doBooleanCommand("P2P_FIND " + timeout);
    }

    public boolean p2pStopFind() {
       return doBooleanCommand("P2P_STOP_FIND");
    }

    public boolean p2pListen() {
        return doBooleanCommand("P2P_LISTEN");
    }

    public boolean p2pListen(int timeout) {
        if (timeout <= 0) {
            return p2pListen();
        }
        return doBooleanCommand("P2P_LISTEN " + timeout);
    }

    public boolean p2pExtListen(boolean enable, int period, int interval) {
        if (enable && interval < period) {
            return false;
        }
        return doBooleanCommand("P2P_EXT_LISTEN"
                    + (enable ? (" " + period + " " + interval) : ""));
    }

    public boolean p2pSetChannel(int lc, int oc) {
        if (lc >=1 && lc <= 11) {
            if (!doBooleanCommand("P2P_SET listen_channel " + lc)) {
                return false;
            }
        } else if (lc != 0) {
            return false;
        }

        if (oc >= 1 && oc <= 165 ) {
            int freq = (oc <= 14 ? 2407 : 5000) + oc * 5;
            return doBooleanCommand("P2P_SET disallow_freq 1000-"
                    + (freq - 5) + "," + (freq + 5) + "-6000");
        } else if (oc == 0) {
            /* oc==0 disables "P2P_SET disallow_freq" (enables all freqs) */
            return doBooleanCommand("P2P_SET disallow_freq \"\"");
        }

        return false;
    }

    public boolean p2pFlush() {
        return doBooleanCommand("P2P_FLUSH");
    }

    /* p2p_connect <peer device address> <pbc|pin|PIN#> [label|display|keypad]
        [persistent] [join|auth] [go_intent=<0..15>] [freq=<in MHz>] */
    public String p2pConnect(WifiP2pConfig config, boolean joinExistingGroup) {
        if (config == null) return null;
        List<String> args = new ArrayList<String>();
        WpsInfo wps = config.wps;
        args.add(config.deviceAddress);

        switch (wps.setup) {
            case WpsInfo.PBC:
                args.add("pbc");
                break;
            case WpsInfo.DISPLAY:
                if (TextUtils.isEmpty(wps.pin)) {
                    args.add("pin");
                } else {
                    args.add(wps.pin);
                }
                args.add("display");
                break;
            case WpsInfo.KEYPAD:
                args.add(wps.pin);
                args.add("keypad");
                break;
            case WpsInfo.LABEL:
                args.add(wps.pin);
                args.add("label");
            default:
                break;
        }

        if (config.netId == WifiP2pGroup.PERSISTENT_NET_ID) {
            args.add("persistent");
        }

        if (joinExistingGroup) {
            args.add("join");
        } else {
            //TODO: This can be adapted based on device plugged in state and
            //device battery state
            int groupOwnerIntent = config.groupOwnerIntent;
            if (groupOwnerIntent < 0 || groupOwnerIntent > 15) {
                groupOwnerIntent = DEFAULT_GROUP_OWNER_INTENT;
            }
            args.add("go_intent=" + groupOwnerIntent);
        }

        /** M: enhance frequency conflict @{ */
        int preferOperFreq = config.getPreferOperFreq();
        if (-1 != preferOperFreq) {
            args.add("freq=" + preferOperFreq);
        }
        /** @} */

        String command = "P2P_CONNECT ";
        for (String s : args) command += s + " ";

        return doStringCommand(command);
    }

    public boolean p2pCancelConnect() {
        return doBooleanCommand("P2P_CANCEL");
    }

    public boolean p2pProvisionDiscovery(WifiP2pConfig config) {
        if (config == null) return false;

        switch (config.wps.setup) {
            case WpsInfo.PBC:
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " pbc");
            case WpsInfo.DISPLAY:
                //We are doing display, so provision discovery is keypad
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " keypad");
            case WpsInfo.KEYPAD:
                //We are doing keypad, so provision discovery is display
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " display");
            default:
                break;
        }
        return false;
    }

    public boolean p2pGroupAdd(boolean persistent) {
        if (persistent) {
            return doBooleanCommand("P2P_GROUP_ADD persistent");
        }
        return doBooleanCommand("P2P_GROUP_ADD");
    }

    public boolean p2pGroupAdd(int netId) {
        return doBooleanCommand("P2P_GROUP_ADD persistent=" + netId);
    }

    public boolean p2pGroupRemove(String iface) {
        if (TextUtils.isEmpty(iface)) return false;
        synchronized (mLock) {
            return doBooleanCommandNative("IFNAME=" + iface + " P2P_GROUP_REMOVE " + iface);
        }
    }

    public boolean p2pReject(String deviceAddress) {
        return doBooleanCommand("P2P_REJECT " + deviceAddress);
    }

    /* Invite a peer to a group */
    public boolean p2pInvite(WifiP2pGroup group, String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress)) return false;

        if (group == null) {
            return doBooleanCommand("P2P_INVITE peer=" + deviceAddress);
        } else {
            return doBooleanCommand("P2P_INVITE group=" + group.getInterface()
                    + " peer=" + deviceAddress + " go_dev_addr=" + group.getOwner().deviceAddress);
        }
    }

    /* Reinvoke a persistent connection */
    public boolean p2pReinvoke(int netId, String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress) || netId < 0) return false;
        ///M: ALPS01771578: to flush p2p persistent group GO information last time  @{
        bssFlush();
        ///@}
        return doBooleanCommand("P2P_INVITE persistent=" + netId + " peer=" + deviceAddress);
    }

    public String p2pGetSsid(String deviceAddress) {
        return p2pGetParam(deviceAddress, "oper_ssid");
    }

    public String p2pGetDeviceAddress() {

        Log.d(TAG, "p2pGetDeviceAddress");

        String status = null;

        /* Explicitly calling the API without IFNAME= prefix to take care of the devices that
        don't have p2p0 interface. Supplicant seems to be returning the correct address anyway. */

        synchronized (mLock) {
            status = doStringCommandNative("STATUS");
        }

        String result = "";
        if (status != null) {
            String[] tokens = status.split("\n");
            for (String token : tokens) {
                if (token.startsWith("p2p_device_address=")) {
                    String[] nameValue = token.split("=");
                    if (nameValue.length != 2)
                        break;
                    result = nameValue[1];
                }
            }
        }

        Log.d(TAG, "p2pGetDeviceAddress returning " + result);
        return result;
    }

    public int getGroupCapability(String deviceAddress) {
        int gc = 0;
        if (TextUtils.isEmpty(deviceAddress)) return gc;
        String peerInfo = p2pPeer(deviceAddress);
        if (TextUtils.isEmpty(peerInfo)) return gc;

        String[] tokens = peerInfo.split("\n");
        for (String token : tokens) {
            if (token.startsWith("group_capab=")) {
                String[] nameValue = token.split("=");
                if (nameValue.length != 2) break;
                try {
                    return Integer.decode(nameValue[1]);
                } catch(NumberFormatException e) {
                    return gc;
                }
            }
        }
        return gc;
    }

    public String p2pPeer(String deviceAddress) {
        return doStringCommand("P2P_PEER " + deviceAddress);
    }

    private String p2pGetParam(String deviceAddress, String key) {
        if (deviceAddress == null) return null;

        String peerInfo = p2pPeer(deviceAddress);
        if (peerInfo == null) return null;
        String[] tokens= peerInfo.split("\n");

        key += "=";
        for (String token : tokens) {
            if (token.startsWith(key)) {
                String[] nameValue = token.split("=");
                if (nameValue.length != 2) break;
                return nameValue[1];
            }
        }
        return null;
    }

    public boolean p2pServiceAdd(WifiP2pServiceInfo servInfo) {
        /*
         * P2P_SERVICE_ADD bonjour <query hexdump> <RDATA hexdump>
         * P2P_SERVICE_ADD upnp <version hex> <service>
         *
         * e.g)
         * [Bonjour]
         * # IP Printing over TCP (PTR) (RDATA=MyPrinter._ipp._tcp.local.)
         * P2P_SERVICE_ADD bonjour 045f697070c00c000c01 094d795072696e746572c027
         * # IP Printing over TCP (TXT) (RDATA=txtvers=1,pdl=application/postscript)
         * P2P_SERVICE_ADD bonjour 096d797072696e746572045f697070c00c001001
         *  09747874766572733d311a70646c3d6170706c69636174696f6e2f706f7374736372797074
         *
         * [UPnP]
         * P2P_SERVICE_ADD upnp 10 uuid:6859dede-8574-59ab-9332-123456789012
         * P2P_SERVICE_ADD upnp 10 uuid:6859dede-8574-59ab-9332-123456789012::upnp:rootdevice
         * P2P_SERVICE_ADD upnp 10 uuid:6859dede-8574-59ab-9332-123456789012::urn:schemas-upnp
         * -org:device:InternetGatewayDevice:1
         * P2P_SERVICE_ADD upnp 10 uuid:6859dede-8574-59ab-9322-123456789012::urn:schemas-upnp
         * -org:service:ContentDirectory:2
         */
        for (String s : servInfo.getSupplicantQueryList()) {
            String command = "P2P_SERVICE_ADD";
            command += (" " + s);
            if (!doBooleanCommand(command)) {
                return false;
            }
        }
        return true;
    }

    public boolean p2pServiceDel(WifiP2pServiceInfo servInfo) {
        /*
         * P2P_SERVICE_DEL bonjour <query hexdump>
         * P2P_SERVICE_DEL upnp <version hex> <service>
         */
        for (String s : servInfo.getSupplicantQueryList()) {
            String command = "P2P_SERVICE_DEL ";

            String[] data = s.split(" ");
            if (data.length < 2) {
                return false;
            }
            if ("upnp".equals(data[0])) {
                command += s;
            } else if ("bonjour".equals(data[0])) {
                command += data[0];
                command += (" " + data[1]);
            } else {
                return false;
            }
            if (!doBooleanCommand(command)) {
                return false;
            }
        }
        return true;
    }

    public boolean p2pServiceFlush() {
        return doBooleanCommand("P2P_SERVICE_FLUSH");
    }

    public String p2pServDiscReq(String addr, String query) {
        String command = "P2P_SERV_DISC_REQ";
        command += (" " + addr);
        command += (" " + query);

        return doStringCommand(command);
    }

    public boolean p2pServDiscCancelReq(String id) {
        return doBooleanCommand("P2P_SERV_DISC_CANCEL_REQ " + id);
    }

    /* Set the current mode of miracast operation.
     *  0 = disabled
     *  1 = operating as source
     *  2 = operating as sink
     */
    public void setMiracastMode(int mode) {
        // Note: optional feature on the driver. It is ok for this to fail.
        doBooleanCommand("DRIVER MIRACAST " + mode);
    }

    ///M: wfd sink MCC mechanism  @{
    public void setMiracastMode(int mode, int freq) {
        // Note: optional feature on the driver. It is ok for this to fail.
        doBooleanCommand("DRIVER MIRACAST " + mode + " freq=" + freq);
    }
    ///@}

    public boolean fetchAnqp(String bssid, String subtypes) {
        return doBooleanCommand("ANQP_GET " + bssid + " " + subtypes);
    }

    /* WIFI HAL support */

    private static final String TAG = "WifiNative-HAL";
    private static long sWifiHalHandle = 0;  /* used by JNI to save wifi_handle */
    private static long[] sWifiIfaceHandles = null;  /* used by JNI to save interface handles */
    private static int sWlan0Index = -1;
    private static int sP2p0Index = -1;

    private static boolean sHalIsStarted = false;
    private static boolean sHalFailed = false;

    private static native boolean startHalNative();
    private static native void stopHalNative();
    private static native void waitForHalEventNative();

    private static class MonitorThread extends Thread {
        public void run() {
            Log.i(TAG, "Waiting for HAL events mWifiHalHandle=" + Long.toString(sWifiHalHandle));
            waitForHalEventNative();
        }
    }

    synchronized public static boolean startHal() {
        Log.i(TAG, "startHal");
        synchronized (mLock) {
            if (sHalIsStarted)
                return true;
            if (sHalFailed)
                return false;
            if (startHalNative() && (getInterfaces() != 0) && (sWlan0Index != -1)) {
                new MonitorThread().start();
                sHalIsStarted = true;
                return true;
            } else {
                Log.i(TAG, "Could not start hal");
                sHalIsStarted = false;
                sHalFailed = true;
                return false;
            }
        }
    }

    synchronized public static void stopHal() {
        stopHalNative();
    }

    private static native int getInterfacesNative();

    synchronized public static int getInterfaces() {
        synchronized (mLock) {
            if (sWifiIfaceHandles == null) {
                int num = getInterfacesNative();
                int wifi_num = 0;
                for (int i = 0; i < num; i++) {
                    String name = getInterfaceNameNative(i);
                    Log.i(TAG, "interface[" + i + "] = " + name);
                    if (name.equals("wlan0")) {
                        sWlan0Index = i;
                        wifi_num++;
                    } else if (name.equals("p2p0")) {
                        sP2p0Index = i;
                        wifi_num++;
                    }
                }
                return wifi_num;
            } else {
                return sWifiIfaceHandles.length;
            }
        }
    }

    private static native String getInterfaceNameNative(int index);
    synchronized public static String getInterfaceName(int index) {
        return getInterfaceNameNative(index);
    }

    public static class ScanCapabilities {
        public int  max_scan_cache_size;                 // in number of scan results??
        public int  max_scan_buckets;
        public int  max_ap_cache_per_scan;
        public int  max_rssi_sample_size;
        public int  max_scan_reporting_threshold;        // in number of scan results??
        public int  max_hotlist_aps;
        public int  max_significant_wifi_change_aps;
    }

    public static boolean getScanCapabilities(ScanCapabilities capabilities) {
        return getScanCapabilitiesNative(sWlan0Index, capabilities);
    }

    private static native boolean getScanCapabilitiesNative(
            int iface, ScanCapabilities capabilities);

    private static native boolean startScanNative(int iface, int id, ScanSettings settings);
    private static native boolean stopScanNative(int iface, int id);
    private static native ScanResult[] getScanResultsNative(int iface, boolean flush);
    private static native WifiLinkLayerStats getWifiLinkLayerStatsNative(int iface);

    public static class ChannelSettings {
        int frequency;
        int dwell_time_ms;
        boolean passive;
    }

    public static class BucketSettings {
        int bucket;
        int band;
        int period_ms;
        int report_events;
        int num_channels;
        ChannelSettings channels[];
    }

    public static class ScanSettings {
        int base_period_ms;
        int max_ap_per_scan;
        int report_threshold;
        int num_buckets;
        BucketSettings buckets[];
    }

    public static interface ScanEventHandler {
        void onScanResultsAvailable();
        void onFullScanResult(ScanResult fullScanResult);
        void onSingleScanComplete();
        void onScanPaused();
        void onScanRestarted();
    }

    synchronized static void onScanResultsAvailable(int id) {
        if (sScanEventHandler  != null) {
            sScanEventHandler.onScanResultsAvailable();
        }
    }

    /* scan status, keep these values in sync with gscan.h */
    private static int WIFI_SCAN_BUFFER_FULL = 0;
    private static int WIFI_SCAN_COMPLETE = 1;

    synchronized static void onScanStatus(int status) {
        Log.i(TAG, "Got a scan status changed event, status = " + status);

        if (status == WIFI_SCAN_BUFFER_FULL) {
            /* we have a separate event to take care of this */
        } else if (status == WIFI_SCAN_COMPLETE) {
            if (sScanEventHandler  != null) {
                sScanEventHandler.onSingleScanComplete();
            }
        }
    }

    synchronized static void onFullScanResult(int id, ScanResult result, byte bytes[]) {
        if (DBG) Log.i(TAG, "Got a full scan results event, ssid = " + result.SSID + ", " +
                "num = " + bytes.length);

        if (sScanEventHandler == null) {
            return;
        }

        int num = 0;
        for (int i = 0; i < bytes.length; ) {
            int type  = bytes[i] & 0xFF;
            int len = bytes[i + 1] & 0xFF;

            if (i + len + 2 > bytes.length) {
                Log.w(TAG, "bad length " + len + " of IE " + type + " from " + result.BSSID);
                Log.w(TAG, "ignoring the rest of the IEs");
                break;
            }
            num++;
            i += len + 2;
            if (DBG) Log.i(TAG, "bytes[" + i + "] = [" + type + ", " + len + "]" + ", " +
                    "next = " + i);
        }

        ScanResult.InformationElement elements[] = new ScanResult.InformationElement[num];
        for (int i = 0, index = 0; i < num; i++) {
            int type  = bytes[index] & 0xFF;
            int len = bytes[index + 1] & 0xFF;
            if (DBG) Log.i(TAG, "index = " + index + ", type = " + type + ", len = " + len);
            ScanResult.InformationElement elem = new ScanResult.InformationElement();
            elem.id = type;
            elem.bytes = new byte[len];
            for (int j = 0; j < len; j++) {
                elem.bytes[j] = bytes[index + j + 2];
            }
            elements[i] = elem;
            index += (len + 2);
        }

        result.informationElements = elements;
        sScanEventHandler.onFullScanResult(result);
    }

    private static int sScanCmdId = 0;
    private static ScanEventHandler sScanEventHandler;
    private static ScanSettings sScanSettings;

    synchronized public static boolean startScan(
            ScanSettings settings, ScanEventHandler eventHandler) {
        synchronized (mLock) {

            if (sScanCmdId != 0) {
                stopScan();
            } else if (sScanSettings != null || sScanEventHandler != null) {
                /* current scan is paused; no need to stop it */
            }

            sScanCmdId = getNewCmdIdLocked();

            sScanSettings = settings;
            sScanEventHandler = eventHandler;

            if (startScanNative(sWlan0Index, sScanCmdId, settings) == false) {
                sScanEventHandler = null;
                sScanSettings = null;
                return false;
            }

            return true;
        }
    }

    synchronized public static void stopScan() {

        synchronized (mLock) {
            //M: add error handling
            if (sWlan0Index >= 0 && sScanCmdId != 0) {
                stopScanNative(sWlan0Index, sScanCmdId);
            }
            sScanSettings = null;
            sScanEventHandler = null;
            sScanCmdId = 0;
        }
    }

    synchronized public static void pauseScan() {
        synchronized (mLock) {
            if (sScanCmdId != 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Pausing scan");
                stopScanNative(sWlan0Index, sScanCmdId);
                sScanCmdId = 0;
                sScanEventHandler.onScanPaused();
            }
        }
    }

    synchronized public static void restartScan() {
        synchronized (mLock) {
            if (sScanCmdId == 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Restarting scan");
                //M: error handling. if startScan fail ,sScanEventHandler could be null.
                if (startScan(sScanSettings, sScanEventHandler)) {
                    sScanEventHandler.onScanRestarted();
                }
            }
        }
    }

    synchronized public static ScanResult[] getScanResults() {
        synchronized (mLock) {
            return getScanResultsNative(sWlan0Index, /* flush = */ false);
        }
    }

    public static interface HotlistEventHandler {
        void onHotlistApFound (ScanResult[]result);
    }

    private static int sHotlistCmdId = 0;
    private static HotlistEventHandler sHotlistEventHandler;

    private native static boolean setHotlistNative(int iface, int id,
            WifiScanner.HotlistSettings settings);
    private native static boolean resetHotlistNative(int iface, int id);

    synchronized public static boolean setHotlist(WifiScanner.HotlistSettings settings,
                                    HotlistEventHandler eventHandler) {
        synchronized (mLock) {
            if (sHotlistCmdId != 0) {
                return false;
            } else {
                sHotlistCmdId = getNewCmdIdLocked();
            }

            sHotlistEventHandler = eventHandler;
            if (setHotlistNative(sWlan0Index, sScanCmdId, settings) == false) {
                sHotlistEventHandler = null;
                return false;
            }

            return true;
        }
    }

    synchronized public static void resetHotlist() {
        synchronized (mLock) {
            if (sHotlistCmdId != 0) {
                resetHotlistNative(sWlan0Index, sHotlistCmdId);
                sHotlistCmdId = 0;
                sHotlistEventHandler = null;
            }
        }
    }

    synchronized public static void onHotlistApFound(int id, ScanResult[] results) {
        synchronized (mLock) {
            if (sHotlistCmdId != 0) {
                sHotlistEventHandler.onHotlistApFound(results);
            } else {
                /* this can happen because of race conditions */
                Log.d(TAG, "Ignoring hotlist AP found change");
            }
        }
    }

    public static interface SignificantWifiChangeEventHandler {
        void onChangesFound(ScanResult[] result);
    }

    private static SignificantWifiChangeEventHandler sSignificantWifiChangeHandler;
    private static int sSignificantWifiChangeCmdId;

    private static native boolean trackSignificantWifiChangeNative(
            int iface, int id, WifiScanner.WifiChangeSettings settings);
    private static native boolean untrackSignificantWifiChangeNative(int iface, int id);

    synchronized public static boolean trackSignificantWifiChange(
            WifiScanner.WifiChangeSettings settings, SignificantWifiChangeEventHandler handler) {
        synchronized (mLock) {
            if (sSignificantWifiChangeCmdId != 0) {
                return false;
            } else {
                sSignificantWifiChangeCmdId = getNewCmdIdLocked();
            }

            sSignificantWifiChangeHandler = handler;
            if (trackSignificantWifiChangeNative(sWlan0Index, sScanCmdId, settings) == false) {
                sSignificantWifiChangeHandler = null;
                return false;
            }

            return true;
        }
    }

    synchronized static void untrackSignificantWifiChange() {
        synchronized (mLock) {
            if (sSignificantWifiChangeCmdId != 0) {
                untrackSignificantWifiChangeNative(sWlan0Index, sSignificantWifiChangeCmdId);
                sSignificantWifiChangeCmdId = 0;
                sSignificantWifiChangeHandler = null;
            }
        }
    }

    synchronized static void onSignificantWifiChange(int id, ScanResult[] results) {
        synchronized (mLock) {
            if (sSignificantWifiChangeCmdId != 0) {
                sSignificantWifiChangeHandler.onChangesFound(results);
            } else {
                /* this can happen because of race conditions */
                Log.d(TAG, "Ignoring significant wifi change");
            }
        }
    }

    synchronized public static WifiLinkLayerStats getWifiLinkLayerStats(String iface) {
        // TODO: use correct iface name to Index translation
        if (iface == null) return null;
        synchronized (mLock) {
            if (!sHalIsStarted)
                startHal();
            if (sHalIsStarted)
                return getWifiLinkLayerStatsNative(sWlan0Index);
        }
        return null;
    }

    /*
     * NFC-related calls
     */
    public String getNfcWpsConfigurationToken(int netId) {
        return doStringCommand("WPS_NFC_CONFIG_TOKEN WPS " + netId);
    }

    public String getNfcHandoverRequest() {
        return doStringCommand("NFC_GET_HANDOVER_REQ NDEF P2P-CR");
    }

    public String getNfcHandoverSelect() {
        return doStringCommand("NFC_GET_HANDOVER_SEL NDEF P2P-CR");
    }

    public boolean initiatorReportNfcHandover(String selectMessage) {
        return doBooleanCommand("NFC_REPORT_HANDOVER INIT P2P 00 " + selectMessage);
    }

    public boolean responderReportNfcHandover(String requestMessage) {
        return doBooleanCommand("NFC_REPORT_HANDOVER RESP P2P " + requestMessage + " 00");
    }

    public static native int getSupportedFeatureSetNative(int iface);
    synchronized public static int getSupportedFeatureSet() {
        return getSupportedFeatureSetNative(sWlan0Index);
    }

    /* Rtt related commands/events */
    public static interface RttEventHandler {
        void onRttResults(RttManager.RttResult[] result);
    }

    private static RttEventHandler sRttEventHandler;
    private static int sRttCmdId;

    synchronized private static void onRttResults(int id, RttManager.RttResult[] results) {
        if (id == sRttCmdId) {
            Log.d(TAG, "Received " + results.length + " rtt results");
            sRttEventHandler.onRttResults(results);
            sRttCmdId = 0;
        } else {
            Log.d(TAG, "Received event for unknown cmd = " + id + ", current id = " + sRttCmdId);
        }
    }

    private static native boolean requestRangeNative(
            int iface, int id, RttManager.RttParams[] params);
    private static native boolean cancelRangeRequestNative(
            int iface, int id, RttManager.RttParams[] params);

    synchronized public static boolean requestRtt(
            RttManager.RttParams[] params, RttEventHandler handler) {
        synchronized (mLock) {
            if (sRttCmdId != 0) {
                return false;
            } else {
                sRttCmdId = getNewCmdIdLocked();
            }
            sRttEventHandler = handler;
            return requestRangeNative(sWlan0Index, sRttCmdId, params);
        }
    }

    synchronized public static boolean cancelRtt(RttManager.RttParams[] params) {
        synchronized(mLock) {
            if (sRttCmdId == 0) {
                return false;
            }

            if (cancelRangeRequestNative(sWlan0Index, sRttCmdId, params)) {
                sRttEventHandler = null;
                return true;
            } else {
                return false;
            }
        }
    }

    private static native boolean setScanningMacOuiNative(int iface, byte[] oui);

    synchronized public static boolean setScanningMacOui(byte[] oui) {
        synchronized (mLock) {
            if (startHal()) {
                return setScanningMacOuiNative(sWlan0Index, oui);
            } else {
                return false;
            }
        }
    }

    private static native int[] getChannelsForBandNative(
            int iface, int band);

    synchronized public static int [] getChannelsForBand(int band) {
        synchronized (mLock) {
            if (startHal()) {
                return getChannelsForBandNative(sWlan0Index, band);
            } else {
                return null;
            }
        }
    }

    // M: Added functions
    /* Set the current mode of channel concurrent.
     *  0 = SCC
     *  1 = MCC
     */
    public String p2pSetCCMode(int ccMode) {
        return doStringCommand("DRIVER p2p_use_mcc=" + ccMode);
    }

    public String p2pGetDeviceCapa() {
        return doStringCommand("DRIVER p2p_get_cap p2p_dev_capa");
    }

    public String p2pSetDeviceCapa(String strDecimal) {
        return doStringCommand("DRIVER p2p_set_cap p2p_dev_capa " + strDecimal);
    }

    /*M: MTK power saving*/
    public boolean setP2pPowerSaveMtk(String iface, int mode) {
        return doBooleanCommand("DRIVER p2p_set_power_save " + mode);
    }

    public boolean setWfdExtCapability(String hex) {
        return doBooleanCommand("WFD_SUBELEM_SET 7 " + hex);
    }

    public void p2pBeamPlusGO(int reserve) {
        if (0 == reserve) {
            doStringCommand("DRIVER BEAMPLUS_GO_RESERVE_END");
        } else if (1 == reserve) {
            doStringCommand("DRIVER BEAMPLUS_GO_RESERVE_START");
        }
    }

    public void p2pBeamPlus(int state) {
        if (0 == state) {
            doStringCommand("DRIVER BEAMPLUS_STOP");
        } else if (1 == state) {
            doStringCommand("DRIVER BEAMPLUS_START");
        }
    }

    public boolean p2pSetBssid(int id, String bssid) {
        if (mInterfaceName.equals("p2p0")) {
            return doBooleanCommandNative("IFNAME=" + mInterfaceName + " SET_NETWORK " + id + " bssid " + bssid);
        }
        return doBooleanCommand("SET_NETWORK " + id + " bssid " + bssid);
    }

    public String p2pLinkStatics(String interfaceAddress) {
        return doStringCommand("DRIVER GET_STA_STATISTICS " + interfaceAddress);
    }

    public String p2pGoGetSta(String deviceAddress) {
        if (mInterfaceName.equals("p2p0")) {
            return doStringCommand("IFNAME=" + mInterfaceName + " " + "STA " + deviceAddress);
        }
        return doStringCommand("STA " + deviceAddress);
    }

    public int p2pAutoChannel(int enable) {
        return doIntCommand("enable_channel_selection " + enable);
    }

    public boolean doCtiaTestOn() {
        return doBooleanCommand("DRIVER smt-test-on");
    }

    public boolean doCtiaTestOff() {
        return doBooleanCommand("DRIVER smt-test-off");
    }

    public boolean doCtiaTestRate(int rate) {
        return doBooleanCommand("DRIVER smt-rate " + rate);
    }

    public boolean startApWpsPbcCommand() {
        return doBooleanCommand("WPS_PBC");
    }

    public boolean startApWpsWithPinFromDeviceCommand(String pin) {
        return doBooleanCommand("WPS_PIN any " + pin);
    }

    public String startApWpsCheckPinCommand(String pin) {
        return doStringCommand("WPS_CHECK_PIN " + pin);
    }

    public boolean blockClientCommand(String deviceAddress) {
        return doBooleanCommand("DRIVER STA-BLOCK " + deviceAddress);
    }

    public boolean unblockClientCommand(String deviceAddress) {
        return doBooleanCommand("DRIVER STA-UNBLOCK " + deviceAddress);
    }

    public boolean setApProbeRequestEnabledCommand(boolean enable) {
        return doBooleanCommand("cfg_ap wps_test " + (enable ? 1 : 0));
    }

    public boolean setMaxClientNumCommand(int num) {
        return doBooleanCommand("cfg_ap max_sta " + num);
    }

    public boolean setBssExpireAge(int value) {
        return doBooleanCommand("BSS_EXPIRE_AGE " + value);
    }

    public boolean setBssExpireCount(int value) {
        return doBooleanCommand("BSS_EXPIRE_COUNT " + value);
    }

    public boolean getDisconnectFlag() {
        return mDisconnectCalled;
    }

    public native static String getCredential();

    public native static boolean setTxPowerEnabled(boolean enable);

    public native static boolean setTxPower(int offset);

    private native boolean setNetworkVariableCommand(String iface, int netId, String name, String value);

    /** M: NFC Float II @{ */
    public boolean startWpsEr() {
        return doBooleanCommand("WPS_ER_START");
    }

    public boolean startWpsRegModify(String bssid, String pin, String ssid, String auth, String encr, String key) {
        if (TextUtils.isEmpty(bssid) || TextUtils.isEmpty(pin) || TextUtils.isEmpty(ssid)
            || TextUtils.isEmpty(auth) || TextUtils.isEmpty(encr)) {
            return false;
        }
        return doBooleanCommand("WPS_REG " + bssid + " " + pin + " " + ssid + " " + auth + " " + encr + " " + key);
    }

    public boolean startWpsErPin(String pin, String uuid, String bssid) {
        if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(uuid) || TextUtils.isEmpty(bssid)) {
            return false;
        }
        return doBooleanCommand("WPS_ER_PIN " + uuid + " " + pin + " " + bssid);
    }

    public boolean startWpsErPinAny(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        return doBooleanCommand("WPS_ER_PIN any " + pin);
    }

    public boolean startWpsErPbc(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return false;
        }
        return doBooleanCommand("WPS_ER_PBC " + uuid);
    }

    public String wpsNfcToken(boolean ndef) {
        if (ndef) {
            return doStringCommand("WPS_NFC_TOKEN NDEF");
        } else {
            return doStringCommand("WPS_NFC_TOKEN WPS");
        }
    }

    public boolean wpsNfc() {
        return doBooleanCommand("WPS_NFC");
    }

    public boolean wpsNfcTagRead(String token) {
        return doBooleanCommand("WPS_NFC_TAG_READ " + token);
    }

    public String wpsErNfcConfigToken(boolean ndef, String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        if (ndef) {
            return doStringCommand("WPS_ER_NFC_CONFIG_TOKEN NDEF " + uuid);
        } else {
            return doStringCommand("WPS_ER_NFC_CONFIG_TOKEN WPS " + uuid);
        }
    }

    public boolean wpsErLearn(String uuid, String pin) {
        return doBooleanCommand("WPS_ER_LEARN " + uuid + " " + pin);
    }

    public boolean wpsNfcCfgKeyType(int type) {
        return doBooleanCommand("WPS_NFC_CFG pubkey " + type);
    }

    public String getNfcConfigToken() {
        return doStringCommand("WPS_NFC_CONFIGURATION_TOKEN NDEF");
    }

    public boolean p2pNfcConnectWithOob(String device, boolean joinExistingGroup) {
        if (TextUtils.isEmpty(device)) {
            return false;
        }
        if (joinExistingGroup) {
            return doBooleanCommand("P2P_CONNECT " + device + " oob join");
        } else {
            return doBooleanCommand("P2P_CONNECT " + device + " oob");
        }
    }

    public boolean p2pNfcInvite(String device) {
        if (TextUtils.isEmpty(device)) {
            return false;
        }
        return doBooleanCommand("P2P_INVITE group=p2p0 peer=" + device);
    }

    public String getNfcHandoverToken(boolean request) {
        if (request) {
            return doStringCommand("NFC_GET_HANDOVER_REQ NDEF WPS");
        } else {
            return doStringCommand("NFC_GET_HANDOVER_SEL NDEF WPS");
        }
    }

    public boolean nfcRxHandoverToken(String token, boolean request) {
        if (TextUtils.isEmpty(token)) {
            return false;
        }
        if (request) {
            return doBooleanCommand("NFC_RX_HANDOVER_REQ " + token);
        } else {
            return doBooleanCommand("NFC_RX_HANDOVER_SEL " + token);
        }
    }

    public boolean p2pSetAllocateIp(String clientIp, String subMask, String goIp) {
        if (TextUtils.isEmpty(clientIp) || TextUtils.isEmpty(subMask) || TextUtils.isEmpty(goIp)) {
            return false;
        } else {
            return doBooleanCommand("p2p_set_allocate_ip client_ip=" + clientIp + " sub_mask=" + subMask + " go_ip=" + goIp);
        }
    }

    public String getStaticHandoverSelectToken() {
        return doStringCommand("P2P_NFC_SELECT_TOKEN");
    }
    /** @} */

    ///M: Add API For Set WOWLAN Mode @{
    public boolean setWoWlanNormalModeCommand() {
        return doBooleanCommand("DRIVER_WOWLAN_NORMAL");
    }

    public boolean setWoWlanMagicModeCommand() {
        return doBooleanCommand("DRIVER_WOWLAN_MAGIC");
    }
    //@}
    ///M: poor link threshold - RSSI
    public boolean setPoorlinkRssi(double value) {
        return doBooleanCommand("DRIVER POORLINK_RSSI " + value);

    }
    ///M: poor link threshold - Linkspeed
    public boolean setPoorlinkSpeed(double value) {
        return doBooleanCommand("DRIVER POORLINK_LINKSPEED " + value);
    }
    //@}

    ///M: for hotspot optimization
    //set_chip greenAp 1 --> enable
    //set_chip greenAp 0 --> disable
    public boolean setHotspotOptimization(boolean enable) {
        if (enable) {
            return  doBooleanCommand("DRIVER set_chip greenAp 1");
        } else {
            return  doBooleanCommand("DRIVER set_chip greenAp 0");
        }
    }

    /**
     * Get test environment.
     * @param channel Wi-Fi channel
     * @return test environment string
     */
    public String getTestEnv(int channel) {
        if (channel < -1) {
            return null;
        } else {
            return doStringCommand("DRIVER CH_ENV_GET" + (channel == -1 ? "" : " " + channel));
        }
    }


    /**
        * For Passpoint R1.
        * @param enabled enabled
        * @return {@code true}  if it's success,{@code false} otherwise
        */
    public boolean enableHS(boolean enabled) {
        Log.d(mTAG, ":enableHS, enabled = " + enabled);

        if (enabled) {
            doBooleanCommand("SET hs20 1");
            doBooleanCommand("SET interworking 1");
            doBooleanCommand("SET auto_interworking 1");

            doBooleanCommand("INTERWORKING_SELECT auto");

            doBooleanCommand("SAVE_CONFIG");
        } else {
            doBooleanCommand("SET hs20 0");
            doBooleanCommand("SET interworking 0");
            doBooleanCommand("SET auto_interworking 0");
            doBooleanCommand("REMOVE_NETWORK temp");
            doBooleanCommand("SAVE_CONFIG");
        }
        return true;
    }

    /**
        * For Passpoint R1.
       * @param type type
       * @param username username
       * @param passwd passwd
       * @param imsi imsi
       * @param root_ca root_ca
       * @param realm realm
       * @param fqdn fqdn
       * @param client_ca client_ca
       * @param milenage milenage
       * @param simslot simslot
       * @param priority priority
       * @param roamingconsortium roamingconsortium
       * @param mcc_mnc mcc_mnc
       * @return 1  if it's success,0 otherwise
        */
    public int addHsCredentialCommand(String type, String username,
    String passwd, String imsi, String root_ca, String realm, String fqdn,
    String client_ca, String milenage, String simslot, String priority,
    String roamingconsortium, String mcc_mnc) {
         int index = -1;
         int intPcsc = 0;
         String strPcsc = "";

         Log.d(mTAG, ":addHsCredentialCommand, type = " + type + " username = " +
            username + " passwd = " + passwd + " imsi = " + imsi + " root_ca = " +
            root_ca + " realm = " + realm + " fqdn = " + fqdn + " client_ca = " +
            client_ca + " milenage = " + milenage + " simslot = " + simslot +
            " priority = " + priority + " roamingconsortium = " +
            roamingconsortium + " mcc_mnc = " + mcc_mnc);

         index = doIntCommand("ADD_CRED");

         Log.d(mTAG, ":addHsCredentialCommand, return index = " + index);

         if (index == -1) {
             Log.d(mTAG, ":addHsCredentialCommand, index invalid");
             return index;
         }

         if (type != null) {
             if (type.equals("uname_pwd")) {
                 setHsCredentialCommand(index, "eap", "\"TTLS\"");
             }
             if (type.equals("sim")) {
                 if ((simslot != null) && (imsi != null)) {
                     Log.d(mTAG, ":addHsCredentialCommand, send disable_sw_sim command");
                     doBooleanCommand("disable_sw_sim");
                 } else if (milenage != null) {
                     Log.d(mTAG, ":addHsCredentialCommand, send enable_sw_sim command");
                     doBooleanCommand("enable_sw_sim");
                 }
             }
         }

         if (username != null) {
             setHsCredentialCommand(index, "username", "\"" + username + "\"");
         }

         if (passwd != null) {
             setHsCredentialCommand(index, "password", "\"" + passwd + "\"");
         }

         if (imsi != null) {
             if (mcc_mnc != null) {
                 //translate to format mccmnc-xxxxxxxxxx
                 String strSubImsi = imsi.substring(mcc_mnc.length());
                 String strimsi = mcc_mnc + "-" + strSubImsi;

                 Log.d(mTAG, ":addHsCredentialCommand, strSubImsi = " +
                    strSubImsi + ", new strimsi = " + strimsi);

                 setHsCredentialCommand(index, "imsi", "\"" + strimsi + "\"");
             } else {
                 setHsCredentialCommand(index, "imsi", "\"" + imsi + "\"");
             }
         }

         if (root_ca != null) {
             setHsCredentialCommand(index, "ca_cert", "\"" +
                "/data/misc/wpa_supplicant/" + root_ca + "\"");
         }

         if (realm != null) {
             setHsCredentialCommand(index, "realm", "\"" + realm + "\"");
         }

         if (fqdn != null) {
             setHsCredentialCommand(index, "domain", "\"" + fqdn + "\"");
         }

         if (client_ca != null) {
             setHsCredentialCommand(index, "client_cert", "\"" +
                "/data/misc/wpa_supplicant/" + client_ca + "\"");
         }

         if (milenage != null) {
             setHsCredentialCommand(index, "milenage", "\"" + milenage + "\"");
         }

         if (simslot != null) {
             intPcsc = Integer.parseInt(simslot);
             strPcsc = String.valueOf(intPcsc + 1);

             setHsCredentialCommand(index, "pcsc", strPcsc);
         }

         if (priority != null) {
             setHsCredentialCommand(index, "priority", priority);
         }

         if (roamingconsortium != null) {
             setHsCredentialCommand(index, "roaming_consortium", roamingconsortium);
         }

         return index;
     }

    /**
         * For Passpoint.
         * @param index index
         * @param name name
         * @param value value
         * @return {@code true}  if it's success,{@code false} otherwise
         */
     public boolean setHsCredentialCommand(int index, String name, String value) {
         boolean isSetOK;
         boolean isSaveOK;

         Log.d(mTAG, ":setHsCredentialCommand, index = " + index +
            " name = " + name + " value = " + value);

         isSetOK = doBooleanCommand("SET_CRED " + index + " " + name + " " + value);
         isSaveOK = doBooleanCommand("SAVE_CONFIG");

         Log.d(mTAG, ":setHsCredentialCommand, isSetOK = " + isSetOK + " isSaveOK = " + isSaveOK);

         if (isSetOK && isSaveOK) {
             return true;
         }
         return false;
     }

     /**
         * For Passpoint.
         * @return HS credential
         * @hide
         */
     public String getHsCredentialCommand() {
         Log.d(mTAG, ":getHsCredentialCommand");

         String results = doStringCommand("LIST_CREDS");

         if (results == null) {
             Log.d(mTAG, ":getHsCredentialCommand, results == null");
             return "";
         } else {
             Log.d(mTAG, ":getHsCredentialCommand, results == " + results);
             return results;
         }
     }

     /**
         * For Passpoint.
         * @param index index
         * @return {@code true}  if it's success,{@code false} otherwise
         * @hide
         */
     public boolean delHsCredentialCommand(int index) {
         Log.d(mTAG, ":delHsCredentialCommand, index = " + index);
         boolean isSetOK;
         boolean isSaveOK;

         isSetOK = doBooleanCommand("REMOVE_CRED " + index);
         isSaveOK = doBooleanCommand("SAVE_CONFIG");

         Log.d(mTAG, ":delHsCredentialCommand, isSetOK = " + isSetOK + " isSaveOK = " + isSaveOK);

         if (isSetOK && isSaveOK) {
             return true;
         }
         return false;
     }

     /**
         * For Passpoint.
         * @return HS status
         * @hide
         */
     public String getHsStatusCommand() {
         Log.d(mTAG, ":getHsStatusCommand");
         return doStringCommand("STATUS");
     }

     /**
         * For Passpoint.
         * @return HS network
         * @hide
         */
    public String getHsNetworkCommand() {
        Log.d(mTAG, ":getHsNetworkCommand");
        String results = doStringCommand("LIST_NETWORKS");

        if (results == null) {
            Log.d(mTAG, ":getHsNetworkCommand, results = null");
            return "";
        } else {
            Log.d(mTAG, ":getHsNetworkCommand, results = " + results);
            return results;
        }
    }

    /**
        * For Passpoint.
        * @param index index
        * @param name name
        * @param value value
        * @return {@code true}  if it's success,{@code false} otherwise
        * @hide
        */
    public boolean setHsNetworkCommand(int index, String name, String value) {
        Log.d(mTAG, ":setHsNetworkCommand, index = " +
            index + " name = " + name + " value = " + value);
        boolean isSetOK;
        boolean isSaveOK;

        isSetOK = doBooleanCommand("SET_NETWORK " + index + " " + name + " " + value);
        isSaveOK = doBooleanCommand("SAVE_CONFIG");

        Log.d(mTAG, ":setHsNetworkCommand, isSetOK = " + isSetOK + " isSaveOK = " + isSaveOK);
        if (isSetOK && isSaveOK) {
            return true;
        }
        return false;
    }

    /**
        * For Passpoint.
        * @param index index
        * @return {@code true}  if it's success,{@code false} otherwise
        * @hide
        */
    public boolean delHsNetworkCommand(int index) {
        Log.d(mTAG, ":delHsNetworkCommand, index = " + index);
        boolean isSetOK;
        boolean isSaveOK;

        isSetOK = doBooleanCommand("REMOVE_NETWORK " + index);
        isSaveOK = doBooleanCommand("SAVE_CONFIG");

        Log.d(mTAG, ":delHsNetworkCommand, isSetOK = " + isSetOK + " isSaveOK = " + isSaveOK);
        if (isSetOK && isSaveOK) {
            return true;
        }
        return false;
    }

    /**
        * For Passpoint.
        * @param index index
        * @param value value
        * @return {@code true}  if it's success,{@code false} otherwise
        * @hide
        */
    public boolean setHsPreferredNetworkCommand(int index, int value) {
        Log.d(mTAG, ":setHsPreferredNetworkCommand, index = " + index + "value=" + value);
        boolean isSetOK;

        isSetOK = doBooleanCommand("set_cred " + index + " priority %d2");

        Log.d(mTAG, ":setHsPreferredNetworkCommand, isSetOK = " + isSetOK);
        if (isSetOK) {
            return true;
        }
        return false;
    }

    ///M: for gbk ssid @{
    private String parseSsidInByteArray(byte[] chBytes) {
        if (chBytes == null){
            return null;
        }
        byte[] trim = trimByteArray(chBytes);
        String ret = ssidTransToUtf8(trim, 0, trim.length);
        return ret;
    }

    private Boolean getIsGBKFromMapping(String bssid, String ssid) {
        Boolean isGBK = null;
        for (int i = 0; i < mIsGBKMapping.size(); i++) {
            if (mIsGBKMapping.get(i).getEncodedSsid().equals(ssid)) {
                if (DBG) Log.e(mTAG, "getIsGBKFromMapping bssid: " + bssid + ", ssid: " + ssid
                        + ", getEncodedSsid()" + mIsGBKMapping.get(i).getEncodedSsid());
                isGBK = mIsGBKMapping.get(i).isGBK();
                break;
            }
        }
        return isGBK;
    }

    private void addGBKMapping(String bssid, boolean isGBK, String ssid) {
        if(bssid == null || ssid == null){
            Log.e(mTAG, "addGBKMapping null detected, bssid: " + bssid + ", ssid: " + ssid);
            return;
        }
        if (getIsGBKFromMapping(bssid, ssid) == null) {
            AccessPoint ap = new AccessPoint(bssid, ssid, isGBK);
            mIsGBKMapping.add(ap);
            Log.d(mTAG, "put bssid: " + bssid + ", ssid: " + ssid + ", isGBK: " + isGBK);
        } else {
            Log.d(mTAG, "mapping already exist");
        }
    }

    public void clearGBKMapping() {
        Log.d(mTAG, "clearGBKMapping");
        mIsGBKMapping.clear();
    }


    private byte[] trimByteArray(byte[] bytes) {
        int i = 0;
        for (; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                break;
            }
        }
        if(DBG) Log.d(mTAG, "trimByteArray, i: "+i);
        return Arrays.copyOf(bytes, i);
    }

    private String parseScanResultInByteArray(byte[] byteArray) {
        String newLineSSID_STR = "\n" + SSID_STR;
        String newLineDELIMITER_STR = "\n" + DELIMITER_STR;
        String newLineEND_STR = "\n" + END_STR;
        byte[] ssidStrBytes = newLineSSID_STR.getBytes(); // 0x0A0x730x730x690x640x3d
        byte[] delimiterStrBytes = newLineDELIMITER_STR.getBytes(); // 0x3d0x3d0x3d0x3d
        byte[] endStrBytes = newLineEND_STR.getBytes();

        String ret = "";
        Log.d(mTAG, "scan result len: " + byteArray.length);
        int ssidStartPos = 0;
        int ssidEndPos = 0;
        while (ssidStartPos <= -1 || ssidStartPos <= byteArray.length) {
            int start = ssidStartPos;
            // find ssid position
            ssidStartPos = findByteArray(byteArray, ssidStrBytes,
                    ssidStartPos);

            if (ssidStartPos == -1) {
                break;
            }
            ssidStartPos += ssidStrBytes.length;
            int end = ssidStartPos;
            // construct string from this scan result start to "ssid="
            ret += constructByteToUTF8String(byteArray, start, end);

            // get bssid from scan result
            String bssid = getBssidFromScanResult(byteArray, start, end);

            //Log.d(mTAG, "start: " + ssidStartPos);
            ssidEndPos = findByteArray(byteArray, delimiterStrBytes,
                    ssidStartPos);
            if (ssidEndPos == -1) {
                // maybe end of scan result
                ssidEndPos = findByteArray(byteArray, endStrBytes,
                        ssidStartPos);
                ///M: ALPS02136707 Due to max size of scan result, scan result may be
                ///    ended without expectation
                if (ssidEndPos == -1) {
                    break;
                }
            }
            //Log.d(mTAG, "end: " + ssidEndPos);
            boolean isSsidGBK = isGBK(byteArray, ssidStartPos,
               ssidEndPos);
            String utf8 = ssidTransToUtf8(byteArray, ssidStartPos,
                    ssidEndPos, isSsidGBK);
            if(DBG) Log.d(mTAG, "utf8: " + utf8);
            // append ssid to final scan result string
            ret += utf8;

            ssidStartPos = ssidEndPos + delimiterStrBytes.length;
            start = ssidEndPos;
            end = ssidStartPos;
            // construct string from ssid end to scan result end
            ret += constructByteToUTF8String(byteArray, start, end);

            // record bssid and isGBK
            if (isSsidGBK) {
                addGBKMapping(bssid, isSsidGBK, utf8);
            }
        }
        return ret;
    }

    private int findByteArray(byte[] source, byte[] match, int start_pos) {
        if (start_pos >= source.length) {
            return -1;
        }
        if (source == null || match == null)
            return -1;
        if (source.length == 0 || match.length == 0)
            return -1;
        int ret = -1;
        int spos = 0;
        int mpos = 0;
        byte m = match[mpos];
        for (spos = start_pos; spos < source.length; spos++) {
            if (m == source[spos]) {
                // starting match
                if (mpos == 0)
                    ret = spos;
                // finishing match
                else if (mpos == match.length - 1)
                    return ret;
                mpos++;
                m = match[mpos];
            } else {
                ret = -1;
                mpos = 0;
                m = match[mpos];
            }
        }
        return ret;
    }

    private String constructByteToUTF8String(byte[] byteArray,
            int start, int end) {
        String ret = null;
        byte[] slice = Arrays.copyOfRange(byteArray, start, end);
        try {
            ret = new String(slice, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }


    private String getBssidFromScanResult(byte[] byteArray, int start,
            int end) {
        final String BSSID_STR = "\nbssid=";
        final int bssidLength = 17;
        String bssid = "";
        int pos = findByteArray(byteArray, BSSID_STR.getBytes(),
                start);
        int bssidStart = pos + BSSID_STR.getBytes().length;
        byte[] slice = Arrays.copyOfRange(byteArray, bssidStart, bssidStart + bssidLength);
        bssid = new String(slice);
        //Log.d(mTAG, "bssid: "+bssid);
        return bssid;
    }

    private boolean isGBK(byte[] byteArray, int ssidStartPos,
            int ssidEndPos) {
        byte [] subArray = Arrays.copyOfRange(byteArray, ssidStartPos, ssidEndPos);
        return isGBK(subArray);
    }

    private boolean isGBK(byte[] chBytes) {
        if (isNotUtf8(chBytes)) {
            if (DBG) Log.d(mTAG, "is not utf8");
            return true;
        } else {
            if (DBG) Log.d(mTAG, "is utf8 format");
            ///M: ALPS02164902 if is true, then compare UTF8-like GBK encoding
            if (mUtf8LikeGbkEncoding) {
                // is utf8 format, still may be gbk
                if (isUtf8LikeGbk(chBytes)) {
                    if (DBG) Log.d(mTAG, "is utf8-like GBK");
                    return true;
                }
                if (DBG) Log.d(mTAG, "is truly utf8");
            }
            return false;
        }
    }

    private boolean isNotUtf8(byte[] input) {
        int nBytes = 0;
        byte chr;
        boolean isAllAscii = true;
        for (int i = 0; i < input.length; i++) {
            chr = input[i];
            if ((chr & 0x80) != 0) {
                isAllAscii = false;
            }
            if (0 == nBytes) {
                if ((chr & 0xFF) >= (0x80 & 0xFF)) {
                    if (chr >= (byte) 0xFC && chr <= (byte) 0xFD) {
                        nBytes = 6;
                    } else if (chr >= (byte) 0xF8) {
                        nBytes = 5;
                    } else if (chr >= (byte) 0xF0) {
                        nBytes = 4;
                    } else if (chr >= (byte) 0xE0) {
                        nBytes = 3;
                    } else if (chr >= (byte) 0xC0) {
                        nBytes = 2;
                    } else {
                        return true;
                    }
                    nBytes--;
                }
            } else {
                if ((chr & 0xC0) != 0x80) {
                    return true;
                }
                nBytes--;
            }
        }
        //Log.d(mTAG, "nBytes > 0: " + (nBytes > 0) + ", isAllAscii: " + isAllAscii);
        if (nBytes > 0) {
            if (isAllAscii) {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isUtf8LikeGbk(byte[] chBytes) {
        boolean isExistGBK = false;
        boolean isExistASCII = false;
        for (int i = 0; i < chBytes.length; ) {
            if (i < chBytes.length && i + 1 < chBytes.length
                && isWordUtf8LikeGBK(chBytes[i], chBytes[i + 1])) {
                isExistGBK = true;
                i += 2;
            } else if (isASCII(chBytes[i])) {
                isExistASCII = true;
                i++;
            } else {
                return false;
            }
        }
        if (!isExistGBK && isExistASCII) {
            return false;
        }
        return true;
    }

    private boolean isASCII(byte b) {
        if ((b & 0x80) == 0) {
            return true;
        }
        return false;
    }

    private boolean isWordUtf8LikeGBK(byte head, byte tail) {
        int iHead = head & 0xff;
        int iTail = tail & 0xff;
        return ((iHead >= 0xC0 && iHead <= 0xD0 && (iTail >= 0x80
                && iTail <= 0x7e || iTail >= 0x80 && iTail <= 0xBF)) ? true
                : false);
    }

    private String ssidTransToUtf8(byte[] scanResultBytes,
            int ssidStartPos, int ssidEndPos, boolean isGBK) {
        String ret = null;
        byte [] subArray = Arrays.copyOfRange(scanResultBytes, ssidStartPos, ssidEndPos);
        if(DBG) Log.d(mTAG, "isGBK: "+ isGBK + ", start: " + ssidStartPos + ", end: " + ssidEndPos);
        try {
            if (!isGBK) {
                ret = new String(subArray, "UTF-8");
            } else {
                ret = changeGbkBytesToUtfString(subArray);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String ssidTransToUtf8(byte[] scanResultBytes,
            int ssidStartPos, int ssidEndPos) {
        String ret = null;
        boolean isGbk = isGBK(scanResultBytes, ssidStartPos, ssidEndPos);
        return ssidTransToUtf8(scanResultBytes, ssidStartPos, ssidEndPos, isGbk);
    }
    private String changeGbkBytesToUtfString(byte[] gbkBytes) {
        String utf8 = null;
        try {
            //printBytes(gbkBytes);   // gbk bytes
            utf8 = new String(gbkBytes, "GBK");
            //printBytes(utf8.getBytes("UTF-8")); // utf8 bytes
            utf8 = new String(utf8.getBytes("UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return utf8;
    }

    private String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private String encodeSSID(String str) {
        String tmp = removeDoubleQuotes(str);
        return String.format("%x", new BigInteger(1, tmp.getBytes(Charset.forName("UTF-8"))));
    }

    private String encodeGbkSSID(String str) {
        String tmp = removeDoubleQuotes(str);
        return String.format("%x", new BigInteger(1, tmp.getBytes(Charset.forName("GBK"))));
    }


    public class AccessPoint {
        private String bssid;
        private String encodedSsid;
        private boolean isGBK;

        public AccessPoint(String bssid, String encodedSsid, boolean isGBK) {
            this.bssid = bssid;
            this.encodedSsid = encodedSsid;
            this.isGBK = isGBK;
        }
        public String getBssid() {
            return bssid;
        }
        public String getEncodedSsid() {
            return encodedSsid;
        }
        public boolean isGBK() {
            return isGBK;
        }
    }
    ///@}
}
