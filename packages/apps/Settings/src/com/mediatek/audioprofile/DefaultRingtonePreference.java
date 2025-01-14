/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.audioprofile;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;

import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.xlog.Xlog;
import android.media.Ringtone; //wuhuihui added
import android.provider.Settings;// wuhuihui added
import android.media.RingtoneManager;
import android.telephony.SubscriptionManager;
import android.content.SharedPreferences;// ddd SimIdValume



public class DefaultRingtonePreference extends RingtonePreference {
    private static final String TAG = "Settings/Rt_Pref";

    public static final String RING_TYPE = "RING";
    public static final String NOTIFICATION_TYPE = "NOTIFICATION";

    private final AudioProfileManager mProfileManager;
    private String mKey;
    private String mStreamType;
    private IAudioProfileExt mExt;
    private long mSimId = -1;
    private boolean mNoNeedSIMSelector = false;
    private static final int SINGLE_SIMCARD = 1;
	private static final String PREF_SIM_ID_VALUME = "SimIdValume"; // ddd SimIdValume
	private Context mContext;

    /**
     * set the select sim id
     * @param mSimId
     *           the selected sim id
     */
    public void setSimId(long simId) {
		//start SimIdValume
		//end SimIdValume
        this.mSimId = simId;
        Xlog.d(TAG, "setSimId   simId= " + simId  + " this.mSimId = " + this.mSimId);
    }

    /**
     * the DefaultRingtonePreference construct method
     *
     * @param context
     *            the context which is associated with, through which it can
     *            access the theme and the resources
     * @param attrs
     *            the attributes of XML tag that is inflating the preference
     */
    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
		mContext = context;
        mProfileManager = (AudioProfileManager) context
                .getSystemService(Context.AUDIO_PROFILE_SERVICE);
        mExt = UtilsExt.getAudioProfilePlgin(context);
    }

    /**
     * bind the defaultRingtonePreference with the profile key
     *
     * @param key
     *            the profile key
     */
    public void setProfile(String key) {
        mKey = key;
        /*HQ_wuhuihui_2014-11-27 modified to set notification sound summary start*/
        updateSummary(getRingtoneUri());
        /*HQ_wuhuihui_2014-11-27 modified to set notification sound summary end*/

    }

    /**
     * Set the defaultRingtonePreference with some stream type for
     * STREAM_RINGER, STREAM_NOTIFICATION etc
     *
     * @param streamType
     */
    public void setStreamType(String streamType) {
        mStreamType = streamType;
    }

   /* HQ_wuhuihui_2014-11-27 modified to set notification sound start*/
   @Override
    public void setRingtoneType(int type) {
        super.setRingtoneType(type);
        updateSummary(getRingtoneUri());
    }
    private Uri getRingtoneUri(){
        Uri ringtoneUri = null;
        if (mKey != null){
            ringtoneUri = mProfileManager.getRingtoneUri(mKey,getRingtoneType(),mSimId);
            if ((ringtoneUri != null) && (false ==  mProfileManager.isRingtoneExist(ringtoneUri))) {
                ringtoneUri = RingtoneManager.getDefaultRingtoneUri(mContext, getRingtoneType());
            }
        }
		Xlog.d(TAG, "get ringtoneUri= " + ringtoneUri);
        return ringtoneUri;
    }

    private void updateSummary(Uri ringtoneUri) {
        boolean showSilent = getShowSilent();
        if ((true == showSilent) && (null == ringtoneUri)){
            setSummary(com.android.internal.R.string.ringtone_silent);
            return;
        } else if (ringtoneUri != null){
            final Ringtone ringtone = RingtoneManager.getRingtone(getContext(), ringtoneUri);
            if (ringtone != null){
                Xlog.d(TAG, "Test ringtone name:= " + ringtone.getTitle(getContext()));
                setSummary(ringtone.getTitle(getContext()));
                return;
            }
        }
        setSummary(null);
    }
    /*HQ_wuhuihui_2014-11-27 modified to set notification sound end*/

    /**
     * Prepare the intent to launch the ringtone picker For Ring, hide the
     * "default Ringtone" item For CMCC, add the "More Ringtone" item.
     *
     * @param ringtonePickerIntent
     */
    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);

        if (mStreamType.equals(RING_TYPE)) {
            ringtonePickerIntent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        }

        mExt.setRingtonePickerParams(ringtonePickerIntent);
    }

    /**
     * Called when the ringtone is choosen , set the selected ringtone uri to
     * framework
     *
     * @param ringtoneUri
     *            the selected ringtone uri
     */
    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        Xlog.d(TAG, "onSaveRingtone mSimId= " + mSimId);
        mProfileManager.setRingtoneUri(mKey, getRingtoneType(), mSimId, ringtoneUri);
        /*HQ_wuhuihui_2014-11-27 modified to set notification sound start*/
        updateSummary(ringtoneUri);
        /* HQ_wuhuihui_2014-11-27 modified to set notification sound end*/
    }

    /**
     * Called when the chooser is about to shown, get the current selected
     * profile uri
     *
     * @return the ringtone uri that need to be choosen
     */
    @Override
    protected Uri onRestoreRingtone() {
        int type = getRingtoneType();
        Xlog.d(TAG, "onRestoreRingtone: type = " + type + " mKey = " + mKey  + "  mSimId= " + mSimId);

        Uri uri = mProfileManager.getRingtoneUri(mKey, type, mSimId);
        Xlog.d(TAG,
                "onRestoreRingtone: uri = "
                        + (uri == null ? "null" : uri.toString()));

        return uri;
    }

    @Override
    protected void onClick() {
        // M: Set different SIM ringtone
        // modified by mtk54031
        final TelephonyManager mTeleManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        //int simNum = mTeleManager.getSimCount();
        int simNum = SubscriptionManager.from(getContext()).getActiveSubscriptionInfoCount();
        Xlog.d(TAG, "onClick  : isNoNeedSIMSelector = " + isNoNeedSIMSelector() + "simNum <= SINGLE_SIMCARD: simNum = " + simNum);

        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT && simNum == SINGLE_SIMCARD) {
			int subId = SubscriptionManager.from(getContext()).getActiveSubscriptionIdList()[0];
			setSimId(subId);
           //setSimId(1);
        }
		
        /*if (isNoNeedSIMSelector() || simNum <= SINGLE_SIMCARD) {
            super.onClick();
        } */
         super.onClick();
    }

    void simSelectorOnClick() {
        Xlog.d(TAG, "onClick  : simSelectorOnClick  ");
        super.onClick();
    }

    public boolean isNoNeedSIMSelector() {
        return mNoNeedSIMSelector;
    }

    public void setNoNeedSIMSelector(boolean mNoNeedSIMSelector) {
        this.mNoNeedSIMSelector = mNoNeedSIMSelector;
    }

}
