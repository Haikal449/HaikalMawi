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

package com.android.settings.brightness;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.ImageView;
import android.util.Log;

import java.util.ArrayList;

public class BrightnessController implements ToggleSlider.Listener {
    private static final String TAG = "StatusBar.BrightnessController";
    private static final boolean SHOW_AUTOMATIC_ICON = false;

    /**
     * {@link android.provider.Settings.System#SCREEN_AUTO_BRIGHTNESS_ADJ} uses the range [-1, 1].
     * Using this factor, it is converted to [0, BRIGHTNESS_ADJ_RESOLUTION] for the SeekBar.
     */
    private static final float BRIGHTNESS_ADJ_RESOLUTION = 10000;

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;

    private final Context mContext;
    private final ImageView mIcon;
    private final ToggleSlider mControl;
    private final boolean mAutomaticAvailable;
    private final IPowerManager mPower;
    private final CurrentUserTracker mUserTracker;
    private final Handler mHandler;
    private final BrightnessObserver mBrightnessObserver;

    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks =
            new ArrayList<BrightnessStateChangeCallback>();

    private boolean mAutomatic;
    private boolean mListening;
    private boolean mExternalChange;
    
    private int updateSliderValue = 0;

    public interface BrightnessStateChangeCallback {
        public void onBrightnessLevelChanged();
    }

    //fengyaling 20150814 add for default brightbess start
    public static float DEFAULT_CONVERT_FACTOR=3.2f;
    private float covertFactor;
    //fengyaling 20150814 add for default brightbess end

    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            try {
                mExternalChange = true;
                if (BRIGHTNESS_MODE_URI.equals(uri)) {
                	Log.d(TAG,"in onChange BRIGHTNESS_MODE_URI.equals(uri)");
                    updateMode();
                    updateSlider();//HQ_wuhuihui_20151026 modified for mode changed
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                	Log.d(TAG,"in onChange BRIGHTNESS_URI.equals(uri) && !mAutomatic");
                    updateSlider();
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                	Log.d(TAG,"in onChange BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic");
                    updateSlider();
                } /*else {
                	Log.d(TAG,"in onChange else");
                    updateMode();
                    updateSlider();
                } */
                for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
                    cb.onBrightnessLevelChanged();
                }
            } finally {
                mExternalChange = false;
            }
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    BRIGHTNESS_MODE_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_ADJ_URI,
                    false, this, UserHandle.USER_ALL);
        }

        public void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }

    }

    public BrightnessController(Context context, ImageView icon, ToggleSlider control) {
        mContext = context;
        mIcon = icon;
        mControl = control;
        mHandler = new Handler();
        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                updateMode();
                updateSlider();
            }
        };
        mBrightnessObserver = new BrightnessObserver(mHandler);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        
        //fengyaling 20150814 add for default brightbess start
        covertFactor = Settings.Global.getFloat(mContext.getContentResolver(), 
            "convert_brightnesss_factor",DEFAULT_CONVERT_FACTOR);
        //fengyaling 20150814 add for default brightbess end
    }

    public void addStateChangedCallback(BrightnessStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public boolean removeStateChangedCallback(BrightnessStateChangeCallback cb) {
        return mChangeCallbacks.remove(cb);
    }

    @Override
    public void onInit(ToggleSlider control) {
        // Do nothing
    }

    public void registerCallbacks() {
        if (mListening) {
            return;
        }

        mBrightnessObserver.startObserving();
        mUserTracker.startTracking();

        // Update the slider and mode before attaching the listener so we don't
        // receive the onChanged notifications for the initial values.
        updateMode();
        updateSlider();

        mControl.setOnChangedListener(this);
        mListening = true;
    }

    /** Unregister all call backs, both to and from the controller */
    public void unregisterCallbacks() {
        if (!mListening) {
            return;
        }

        mBrightnessObserver.stopObserving();
        mUserTracker.stopTracking();
        mControl.setOnChangedListener(null);
        mListening = false;
    }
    
    @Override
    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value) {
        updateIcon(mAutomatic);
        if (mExternalChange) return;
        //add by HQ_caoxuhao at 20150908 HQ01359294 begin
//      if (true) {
        //if (!mAutomatic) 
			{
        //add by HQ_caoxuhao at 20150908 HQ01359294 end
            Log.d(TAG,"onChanged get value:" + value);
            int range = mMaximumBacklight - mMinimumBacklight;
            int seekBarRange = 10000;
            //float coverFactor = 3.2f;
            float bright=(value*range)/seekBarRange;
            float britness= (float)Math.pow((bright/range),covertFactor)*range + (float)mMinimumBacklight;
            final int brightnessValue = Math.round(britness);
            Log.d(TAG,"get brightness value:" + brightnessValue);
            setBrightness(brightnessValue);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, brightnessValue,
                                    UserHandle.USER_CURRENT);
                        }
                    });
            }
        }
		//else 
		{
            final float adj = value / (BRIGHTNESS_ADJ_RESOLUTION / 2f) - 1;
            Log.d(TAG,"get brightness value adj = " + adj);
            setBrightnessAdj(adj);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        Settings.System.putFloatForUser(mContext.getContentResolver(),
                                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj,
                                UserHandle.USER_CURRENT);
                    }
                });
            }
        }

        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessLevelChanged();
        }
    }

    private void setMode(int mode) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode,
                mUserTracker.getCurrentUserId());
    }

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
        }
    }

    private void setBrightnessAdj(float adj) {
        try {
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
        } catch (RemoteException ex) {
        }
    }

    private void updateIcon(boolean automatic) {
        if (mIcon != null) {
            mIcon.setImageResource(automatic && SHOW_AUTOMATIC_ICON ?
                    com.android.settings.R.drawable.ic_qs_brightness_auto_on :
                    com.android.settings.R.drawable.ic_qs_brightness_auto_off);
        }
    }

    /** Fetch the brightness mode from the system settings and update the icon */
    private void updateMode() {
        if (mAutomaticAvailable) {
            int automatic;
            automatic = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    UserHandle.USER_CURRENT);
            mAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
            updateIcon(mAutomatic);
        } else {
            mControl.setChecked(false);
            updateIcon(false /*automatic*/);
            //M: [ALPS01748251] hide the Toggle slider 
            mControl.hideToggle();
        }
    }

    /** Fetch the brightness from the system settings and update the slider */
    private void updateSlider() {
    	//add by HQ_caoxuhao at 20150908 HQ01359294 begin
//    	if (false) {
        if (mAutomatic) {
        //add by HQ_caoxuhao at 20150908 HQ01359294 end
    		
            float value = Settings.System.getFloatForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0,
                    UserHandle.USER_CURRENT);
            mControl.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            Log.d(TAG,"updateSlider get value from SCREEN_AUTO_BRIGHTNESS_ADJ before value=" + value);
            //HQ_wuhuihui add for HQ01460081 start
            int brightVal = (int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f);
            mControl.setValue(brightVal);
            updateSliderValue = brightVal;
            Log.d(TAG,"updateSlider get value from SCREEN_AUTO_BRIGHTNESS_ADJ after brightVal=" + brightVal);
            //HQ_wuhuihui add for HQ01460081 end

        } else {
            int value;
            int range = mMaximumBacklight - mMinimumBacklight;
            int seekBarRange = 10000;
           // float coverFactor = 3.2f;
            int brightness = Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                    UserHandle.USER_CURRENT);
            mControl.setMax(10000);
            Log.d(TAG,"get brightness from SCREEN_BRIGHTNESS before brightness=" + brightness);
            double baseBright = (double)(((float)brightness - (float)mMinimumBacklight)/(float)range);
            float index = (float)(1/covertFactor);
            float bright = (float)(Math.pow(baseBright, index) * range);
            float value_progress = (float)(bright * seekBarRange)/range;
            value = Math.round(value_progress);
            //Log.d(TAG,"get slider baseBright:" + baseBright + ", index" + index);
            //Log.d(TAG,"get slider value:" + value + ", bright:" + bright + ", value_progress" + value_progress);
            mControl.setValue(value);
            Log.d(TAG,"get brightness from SCREEN_BRIGHTNESS after value=" + value);
            updateSliderValue = value;
        }
    }
    
    public int getUpdateSliderValue(){
    	Log.d(TAG,"in getUpdateSliderValue = " + updateSliderValue);
    	return updateSliderValue;
    }
    
    public void setSliderValue(int value){
    	Log.d(TAG,"in setSliderValue");
        mControl.setValue(value);
        updateSliderValue = value;
    }

}
