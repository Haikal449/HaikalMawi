/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
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

package com.android.internal.telephony;

/**
 * Contains a list of string constants used to get or set telephone properties
 * in the system. You can use {@link android.os.SystemProperties os.SystemProperties}
 * to get and set these values.
 * @hide
 */
public interface TelephonyProperties
{
    //****** Baseband and Radio Interface version

    //TODO T: property strings do not have to be gsm specific
    //        change gsm.*operator.*" properties to "operator.*" properties

    /**
     * Baseband version
     * Availability: property is available any time radio is on
     */
    static final String PROPERTY_BASEBAND_VERSION = "gsm.version.baseband";

    /**
    * Indicate Modem version for stack 2.
    */
    static final String PROPERTY_BASEBAND_VERSION_2 = "gsm.version.baseband.2";

    /** Radio Interface Layer (RIL) library implementation. */
    static final String PROPERTY_RIL_IMPL = "gsm.version.ril-impl";

    //****** Current Network

    /** Alpha name of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ALPHA = "gsm.operator.alpha";
    //[ALPS01804936]-start:fix JE when change system language to "Burmese"
    static final String PROPERTY_OPERATOR_ALPHA_2 = "gsm.operator.alpha.2";
    static final String PROPERTY_OPERATOR_ALPHA_3 = "gsm.operator.alpha.3";
    static final String PROPERTY_OPERATOR_ALPHA_4 = "gsm.operator.alpha.4";
    //[ALPS01804936]-end

    //TODO: most of these properties are generic, substitute gsm. with phone. bug 1856959

    /** Numeric name (MCC+MNC) of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_NUMERIC = "gsm.operator.numeric";

    /** 'true' if the device is on a manually selected network
     *
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISMANUAL = "operator.ismanual";

    /** 'true' if the device is considered roaming on this network for GSM
     *  purposes.
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISROAMING = "gsm.operator.isroaming";

    /** The ISO country code equivalent of the current registered operator's
     *  MCC (Mobile Country Code)<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ISO_COUNTRY = "gsm.operator.iso-country";

    /**
     * The contents of this property is the value of the kernel command line
     * product_type variable that corresponds to a product that supports LTE on CDMA.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE = "telephony.lteOnCdmaProductType";

    /**
     * The contents of this property is the one of {@link Phone#LTE_ON_CDMA_TRUE} or
     * {@link Phone#LTE_ON_CDMA_FALSE}. If absent the value will assumed to be false
     * and the {@see #PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE} will be used to determine its
     * final value which could also be {@link Phone#LTE_ON_CDMA_FALSE}.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_DEVICE = "telephony.lteOnCdmaDevice";

    static final String CURRENT_ACTIVE_PHONE = "gsm.current.phone-type";

    //****** SIM Card
    /**
     * One of <code>"UNKNOWN"</code> <code>"ABSENT"</code> <code>"PIN_REQUIRED"</code>
     * <code>"PUK_REQUIRED"</code> <code>"NETWORK_LOCKED"</code> or <code>"READY"</code>
     */
    static String PROPERTY_SIM_STATE = "gsm.sim.state";

    /** The MCC+MNC (mobile country code+mobile network code) of the
     *  provider of the SIM. 5 or 6 decimal digits.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_NUMERIC = "gsm.sim.operator.numeric";
    static String PROPERTY_ICC_OPERATOR_NUMERIC_2 = "gsm.sim.operator.numeric.2";//add by lipeng
    /** PROPERTY_ICC_OPERATOR_ALPHA is also known as the SPN, or Service Provider Name.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_ALPHA = "gsm.sim.operator.alpha";

    /** ISO country code equivalent for the SIM provider's country code*/
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY = "gsm.sim.operator.iso-country";

    /**
     * Indicates the available radio technology.  Values include: <code>"unknown"</code>,
     * <code>"GPRS"</code>, <code>"EDGE"</code> and <code>"UMTS"</code>.
     */
    static String PROPERTY_DATA_NETWORK_TYPE = "gsm.network.type";

    /** Indicate if phone is in emergency callback mode */
    static final String PROPERTY_INECM_MODE = "ril.cdma.inecmmode";

    /** Indicate the timer value for exiting emergency callback mode */
    static final String PROPERTY_ECM_EXIT_TIMER = "ro.cdma.ecmexittimer";

    /** the international dialing prefix of current operator network */
    static final String PROPERTY_OPERATOR_IDP_STRING = "gsm.operator.idpstring";

    /**
     * Defines the schema for the carrier specified OTASP number
     */
    static final String PROPERTY_OTASP_NUM_SCHEMA = "ro.cdma.otaspnumschema";

    /**
     * Disable all calls including Emergency call when it set to true.
     */
    static final String PROPERTY_DISABLE_CALL = "ro.telephony.disable-call";

    /**
     * Set to true for vendor RIL's that send multiple UNSOL_CALL_RING notifications.
     */
    static final String PROPERTY_RIL_SENDS_MULTIPLE_CALL_RING =
        "ro.telephony.call_ring.multiple";

    /**
     * The number of milliseconds between CALL_RING notifications.
     */
    static final String PROPERTY_CALL_RING_DELAY = "ro.telephony.call_ring.delay";

    /**
     * Track CDMA SMS message id numbers to ensure they increment
     * monotonically, regardless of reboots.
     */
    static final String PROPERTY_CDMA_MSG_ID = "persist.radio.cdma.msgid";

    /**
     * Property to override DEFAULT_WAKE_LOCK_TIMEOUT
     */
    static final String PROPERTY_WAKE_LOCK_TIMEOUT = "ro.ril.wake_lock_timeout";

    /**
     * Set to true to indicate that the modem needs to be reset
     * when there is a radio technology change.
     */
    static final String PROPERTY_RESET_ON_RADIO_TECH_CHANGE = "persist.radio.reset_on_switch";

    /**
     * Set to false to disable SMS receiving, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_RECEIVE = "telephony.sms.receive";

    /**
     * Set to false to disable SMS sending, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_SEND = "telephony.sms.send";

    /**
     * Set to true to indicate a test CSIM card is used in the device.
     * This property is for testing purpose only. This should not be defined
     * in commercial configuration.
     */
    static final String PROPERTY_TEST_CSIM = "persist.radio.test-csim";

    /**
     * Ignore RIL_UNSOL_NITZ_TIME_RECEIVED completely, used for debugging/testing.
     */
    static final String PROPERTY_IGNORE_NITZ = "telephony.test.ignore.nitz";

     /**
     * Property to set multi sim feature.
     * Type:  String(dsds, dsda)
     */
    static final String PROPERTY_MULTI_SIM_CONFIG = "persist.radio.multisim.config";

    /**
     * Property to store default subscription.
     */
    static final String PROPERTY_DEFAULT_SUBSCRIPTION = "persist.radio.default.sub";

    /**
     * Property to enable MMS Mode.
     * Type: string ( default = silent, enable to = prompt )
     */
    static final String PROPERTY_MMS_TRANSACTION = "mms.transaction";

    /**
     * Set to the sim count.
     */
    static final String PROPERTY_SIM_COUNT = "ro.telephony.sim.count";
    // Added by M begin
    /** The IMSI of the SIM
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_IMSI   = "gsm.sim.operator.imsi";

    /**
    * Indicate if chaneing to SIM locale is processing
    */
    static final String PROPERTY_SIM_LOCALE_SETTINGS = "gsm.sim.locale.waiting";

    /** PROPERTY_ICC_OPERATOR_DEFAULT_NAME is the operator name for plmn which origins the SIM.
     *  Availablity: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME = "gsm.sim.operator.default-name";
    // Added by M end

    static final String PROPERTY_ACTIVE_MD = "ril.active.md";

 /**
    * Indicate the highest radio access capability(ex: UMTS,LTE,etc.) of modem
    */
    static String PROPERTY_BASEBAND_CAPABILITY = "gsm.baseband.capability";
    static String PROPERTY_BASEBAND_CAPABILITY_MD2 = "gsm.baseband.capability.md2";

 /**
    * NITZ operator long name,short name, numeric (if ever received from MM information)
    */
    static String PROPERTY_NITZ_OPER_CODE = "persist.radio.nitz_oper_code";
    static String PROPERTY_NITZ_OPER_LNAME = "persist.radio.nitz_oper_lname";
    static String PROPERTY_NITZ_OPER_SNAME = "persist.radio.nitz_oper_sname";

    /** PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE is the mode for the terminal-based call waiting
     *  possible values: "disabled_tbcw", "enabled_tbcw_on" and "enabled_tbcw_off".
     */
    static String PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE = "persist.radio.terminal-based.cw";
    static String TERMINAL_BASED_CALL_WAITING_DISABLED = "disabled_tbcw";
    static String TERMINAL_BASED_CALL_WAITING_ENABLED_ON = "enabled_tbcw_on";
    static String TERMINAL_BASED_CALL_WAITING_ENABLED_OFF = "enabled_tbcw_off";

     /**
     * Property to set/get svlte mode
     * Type:  String(svlte, csfb)
     */
    static final String PROPERTY_RADIO_SVLTE_MODE = "persist.radio.svlte.mode";

    /** PROPERTY_UT_CFU_NOTIFICATION_MODE is the mode for the UT/XCAP CFU notification
     * possible values: "disabled_ut_cfu_notification", "enabled_ut_cfu_notification_on"
     * and "enabled_ut_cfu_notification_off".
     */
    static String PROPERTY_UT_CFU_NOTIFICATION_MODE = "persist.radio.ut.cfu.mode";
    static String UT_CFU_NOTIFICATION_MODE_DISABLED = "disabled_ut_cfu_mode";
    static String UT_CFU_NOTIFICATION_MODE_ON = "enabled_ut_cfu_mode_on";
    static String UT_CFU_NOTIFICATION_MODE_OFF = "enabled_ut_cfu_mode_off";

	
	static final String PROPERTY_STKAPP_NAME = "gsm.stkapp.name";
    static final String PROPERTY_STKAPP_NAME_2 = "gsm.stkapp.name2";


    /**
     * External SIM enabled properties.
     */
    static final String PROPERTY_EXTERNAL_SIM_ENABLED = "gsm.external.sim.enabled";

    /**
     * External SIM inserted properties.
     * 1: local SIM inserted, 2: remote sim inserted
     */
    static final String PROPERTY_EXTERNAL_SIM_INSERTED = "gsm.external.sim.inserted";


}
