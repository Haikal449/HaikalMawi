/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.android.camera.v2.uimanager;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.android.camera.R;

import com.android.camera.v2.ui.RotateImageView;
import com.android.camera.v2.uimanager.preference.IconListPreference;
import com.android.camera.v2.uimanager.preference.ListPreference;
import com.android.camera.v2.uimanager.preference.PreferenceManager;
import com.android.camera.v2.util.CameraUtil;
import com.android.camera.v2.util.SettingKeys;

public class IndicatorManager extends AbstractUiManager{
    private static final String                  TAG = "IndicatorManager";
    private static final int                     INDICATOR_COUNT = 6;
    private RotateImageView[]                    mViews = new RotateImageView[INDICATOR_COUNT];
    private static final int[]                   VIEW_IDS = new int[] {
        R.id.onscreen_white_balance_indicator,
        R.id.onscreen_scene_indicator, 
        R.id.onscreen_exposure_indicator,
        R.id.onscreen_timelapse_indicator, 
        R.id.onscreen_selftimer_indicator,
        R.id.onscreen_voice_indicator,
    };
    
    private static final String[]                 SETTING_KEYS = new String[] { 
        SettingKeys.KEY_WHITE_BALANCE,
        SettingKeys.KEY_SCENE_MODE,
        SettingKeys.KEY_EXPOSURE,
        SettingKeys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL, 
        SettingKeys.KEY_SELF_TIMER,
        SettingKeys.KEY_VOICE,
    };
    
    private PreferenceManager                     mPreferenceManager;
    private View                                  mIndicatorGroup;
    private String                                mAsdDetectedScene;
    
    public IndicatorManager(Activity activity, ViewGroup parent, 
            PreferenceManager preferenceManager) {
        super(activity, parent);
        mPreferenceManager = preferenceManager;
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.onscreen_indicators_v2);
        for (int i = 0; i < INDICATOR_COUNT; i++) {
            mViews[i] = (RotateImageView) view.findViewById(VIEW_IDS[i]);
        }
        mIndicatorGroup = view.findViewById(R.id.on_screen_group);
        return view;
    }

    @Override
    protected void onRefresh() {
        int showcount = 0;
        for (int i = 0; i < INDICATOR_COUNT; i++) {
            String key = SETTING_KEYS[i];
            String value = null;
            String defaultValue = null;
            if (SettingKeys.KEY_SCENE_MODE.equals(key)) {
                value = mAsdDetectedScene;
            }
            // get indicator setting's value, if the override value is not null, its value is override value.
            ListPreference pref = mPreferenceManager.getListPreference(key);
            if (pref != null) {
                defaultValue = pref.getDefaultValue();
            }
            
            if (pref != null && value == null ) {
                String overrideValue = pref.getOverrideValue();
                if (overrideValue != null) {
                    value = overrideValue;
                } else {
                    value = pref.getValue();
                }
            }
 
            // if scene mode value is hdr, force convert to hdr-detection
            if (SettingKeys.KEY_SCENE_MODE.equals(key) && "hdr".equals(value)) {
                value = "hdr-detection";
            }
            // if value is null or the value is equals to default value, set the icon's visibility as gone. 
            if (value == null 
                    || (defaultValue != null && defaultValue.equals(value))) {
                mViews[i].setVisibility(View.GONE);
            } else {
                mViews[i].setVisibility(View.VISIBLE);
                IconListPreference iconPref = (IconListPreference) pref;
                if (iconPref.getOriginalIconIds() != null) {
                    int index = CameraUtil.index(iconPref.getOriginalEntryValues(), value);
                    mViews[i].setImageResource(iconPref.getOriginalIconIds()[index]);
                }
                showcount++;
            }
        }
        
        if (showcount > 0) {
            mIndicatorGroup.setBackgroundResource(R.drawable.bg_indicator_background);
        } else {
            mIndicatorGroup.setBackgroundDrawable(null);
        }
    }
    
    public void updateAsdDetectedScene(String scene) {
        Log.i(TAG, "[updateAsdDetectedScene], scene:" + scene);
        mAsdDetectedScene = scene;
        refresh();
    } 
}
