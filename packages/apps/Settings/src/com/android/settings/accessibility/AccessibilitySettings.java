/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.util.Log;
/**
 * Activity with the accessibility settings.
 */
public class AccessibilitySettings extends SettingsPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "AccessibilitySettings";
    /// M: MTK fix fonts problem, CR ALPS00261477
    private static final float LARGE_FONT_SCALE_PHONE = 1.15f;
    private float LARGE_FONT_SCALE_TABLET = 1.30f;
    private boolean mIsScreenLarge = false;

    static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';

    // Preference categories
    private static final String SERVICES_CATEGORY = "services_category";
    private static final String SYSTEM_CATEGORY = "system_category";

    // Preferences
    /*HQ_jiazaizheng 20150820 modify for HQ01336076
     private static final String TOGGLE_LARGE_TEXT_PREFERENCE =
            "toggle_large_text_preference";*/
    private static final String TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE =
            "toggle_high_text_contrast_preference";
    private static final String TOGGLE_INVERSION_PREFERENCE =
            "toggle_inversion_preference";
    private static final String TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE =
            "toggle_power_button_ends_call_preference";
    /*HQ_jiazaizheng 20150820 modify for HQ01336076
     private static final String TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE =
            "toggle_lock_screen_rotation_preference";*/
    private static final String TOGGLE_SPEAK_PASSWORD_PREFERENCE =
            "toggle_speak_password_preference";
    private static final String SELECT_LONG_PRESS_TIMEOUT_PREFERENCE =
            "select_long_press_timeout_preference";
    private static final String ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN =
            "enable_global_gesture_preference_screen";
    private static final String CAPTIONING_PREFERENCE_SCREEN =
            "captioning_preference_screen";
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN =
            "screen_magnification_preference_screen";
    private static final String DISPLAY_DALTONIZER_PREFERENCE_SCREEN =
            "daltonizer_preference_screen";

    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "preference_key";
    static final String EXTRA_CHECKED = "checked";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_SUMMARY = "summary";
    static final String EXTRA_SETTINGS_TITLE = "settings_title";
    static final String EXTRA_COMPONENT_NAME = "component_name";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";

    // Timeout before we update the services if packages are added/removed
    // since the AccessibilityManagerService has to do that processing first
    // to generate the AccessibilityServiceInfo we need for proper
    // presentation.
    private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    // Auxiliary members.
    final static SimpleStringSplitter sStringColonSplitter =
            new SimpleStringSplitter(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);

    static final Set<ComponentName> sInstalledServices = new HashSet<ComponentName>();

    private final Map<String, String> mLongPressTimeoutValuetoTitleMap =
            new HashMap<String, String>();

    private final Configuration mCurConfig = new Configuration();

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            loadInstalledServices();
            updateServicesPreferences();
        }
    };

    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_SERVICES_MILLIS);
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    loadInstalledServices();
                    updateAllPreferences();
                }
            };

    /*HQ_jiazaizheng 20150820 modify for HQ01336076
     private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            updateLockScreenRotationCheckbox();
        }
    };*/

    // Preference controls.
    private PreferenceCategory mServicesCategory;
    private PreferenceCategory mSystemsCategory;

    //private SwitchPreference mToggleLargeTextPreference;//HQ_jiazaizheng 20150820 modify for HQ01336076
    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    //private SwitchPreference mToggleLockScreenRotationPreference;//HQ_jiazaizheng 20150820 modify for HQ01336076
    private SwitchPreference mToggleSpeakPasswordPreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private Preference mNoServicesMessagePreference;
    private PreferenceScreen mCaptioningPreferenceScreen;
    private PreferenceScreen mDisplayMagnificationPreferenceScreen;
    private PreferenceScreen mGlobalGesturePreferenceScreen;
    private PreferenceScreen mDisplayDaltonizerPreferenceScreen;
    private SwitchPreference mToggleInversionPreference;

    private int mLongPressTimeoutDefault;

    private DevicePolicyManager mDpm;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        /** M: MTK fix fonts problem, CR ALPS00261477 @{ */
        int screenSize = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        mIsScreenLarge = ((screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE)
                || (screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE));
         /** @} */

        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        mDpm = (DevicePolicyManager) (getActivity()
                .getSystemService(Context.DEVICE_POLICY_SERVICE));
				
        /** M: MTK fix fonts problem, CR ALPS01846045 @{ */		
        String[] array = getResources().getStringArray(R.array.entryvalues_font_size);
        if(array.length>0){
            LARGE_FONT_SCALE_TABLET = Float.parseFloat(array[array.length-1]);
            Log.d(TAG,"LARGE_FONT_SCALE_TABLET "+LARGE_FONT_SCALE_TABLET);
        }
		/** @} */
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInstalledServices();
        updateAllPreferences();

        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        /*HQ_jiazaizheng 20150820 modify for HQ01336076
         if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }*/
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        /*HQ_jiazaizheng 20150820 modify for HQ01336076
         if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }*/
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) newValue);
            return true;
        } else if (mToggleInversionPreference == preference) {
            handleToggleInversionPreferenceChange((Boolean) newValue);
            return true;
        } else if (mToggleSpeakPasswordPreference == preference) {             //hanchao add HQ01387002 2015.9.2
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                    (Boolean) newValue ? 1 : 0);
            return true;
        }        
        /*HQ_yuankangbo 2015-07-24 modify for AccessibilitySettings fontsize start*/
        /*HQ_jiazaizheng 20150820 modify for HQ01336076
         else if(mToggleLargeTextPreference == preference){
            float updateFontScale = Settings.System.getFloat(getContentResolver(),
                    Settings.System.FONT_SCALE_EXTRALARGE, -1);
            float fontScale = LARGE_FONT_SCALE_PHONE;
            if (updateFontScale == -1) {
                fontScale = mIsScreenLarge ? LARGE_FONT_SCALE_TABLET : LARGE_FONT_SCALE_PHONE;
            } else {
                fontScale = updateFontScale;
            }

            try {
                mCurConfig.fontScale = !mToggleLargeTextPreference.isChecked() ? fontScale : 1;
                ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
            } catch (RemoteException re) {
                 ignore 
            }
            return true;
        }*/
        /*HQ_yuankangbo 2015-07-24 modify for AccessibilitySettings fontsize end*/
        //HQ_jiazaizheng 20150804 modify for HQ01307988 start
        else if (mTogglePowerButtonEndsCallPreference == preference) {
                Settings.Secure.putInt(getContentResolver(),Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    (boolean)newValue ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                            : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF);
            return true;
        }//HQ_jiazaizheng 20150804 modify for HQ01307988 end
        //HQ_jiazaizheng 20150805 modify for HQ01307950 start
        else if (mToggleHighTextContrastPreference == preference) {
                Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                    ((boolean)newValue ? 1 : 0));
            return true;
        }//HQ_jiazaizheng 20150805 modify for HQ01307950 end
        //HQ_jiazaizheng 20150811 modify for HQ01314762 start
        /*else if (mToggleLockScreenRotationPreference == preference) {
                  RotationPolicy.setRotationLockForAccessibility(getActivity(),
                    (boolean)newValue ? false : true);
            return true;
        }*///HQ_jiazaizheng 20150811 modify for HQ01314762 end
        return false;
    }

    private void handleLongPressTimeoutPreferenceChange(String stringValue) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, Integer.parseInt(stringValue));
        mSelectLongPressTimeoutPreference.setSummary(
                mLongPressTimeoutValuetoTitleMap.get(stringValue));
    }

    private void handleToggleInversionPreferenceChange(boolean checked) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, (checked ? 1 : 0));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /*HQ_jiazaizheng 20150820 modify for HQ01336076
         if (mToggleLargeTextPreference == preference) {
            handleToggleLargeTextPreferenceClick();
            return true;
        } else */
        if (mToggleHighTextContrastPreference == preference) {
            handleToggleTextContrastPreferenceClick();
            return true;
        } else if (mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } /*HQ_jiazaizheng 20150820 modify for HQ01336076
          else if (mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        } */else if (mToggleSpeakPasswordPreference == preference) {
            handleToggleSpeakPasswordPreferenceClick();
            return true;
        } else if (mGlobalGesturePreferenceScreen == preference) {
            handleToggleEnableAccessibilityGesturePreferenceClick();
            return true;
        } else if (mDisplayMagnificationPreferenceScreen == preference) {
            handleDisplayMagnificationPreferenceScreenClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    //HQ_jiazaizheng 20150820 modify for HQ01336076 start
    /*private void handleToggleLargeTextPreferenceClick() {
        /// M: MTK fix fonts problem, CR ALPS00261477 @{
        float updateFontScale = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE_EXTRALARGE, -1);
        float fontScale = LARGE_FONT_SCALE_PHONE;
        if (updateFontScale == -1) {
            fontScale = mIsScreenLarge ? LARGE_FONT_SCALE_TABLET : LARGE_FONT_SCALE_PHONE;
        } else {
            fontScale = updateFontScale;
        }
        /// @}

        try {
            mCurConfig.fontScale = mToggleLargeTextPreference.isChecked() ? fontScale : 1;
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException re) {
             ignore 
        }
    }*/
    //HQ_jiazaizheng 20150820 modify for HQ01336076 end

    private void handleToggleTextContrastPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                (mToggleHighTextContrastPreference.isChecked() ? 1 : 0));
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                (mTogglePowerButtonEndsCallPreference.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    //HQ_jiazaizheng 20150820 modify for HQ01336076 start
    /*private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(),
                !mToggleLockScreenRotationPreference.isChecked());
    }*/
    //HQ_jiazaizheng 20150820 modify for HQ01336076 end

    private void handleToggleSpeakPasswordPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                mToggleSpeakPasswordPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleEnableAccessibilityGesturePreferenceClick() {
        Bundle extras = mGlobalGesturePreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_global_gesture_preference_title));
        extras.putString(EXTRA_SUMMARY, getString(
                R.string.accessibility_global_gesture_preference_description));
        extras.putBoolean(EXTRA_CHECKED, Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mGlobalGesturePreferenceScreen,
                mGlobalGesturePreferenceScreen);
    }

    private void handleDisplayMagnificationPreferenceScreenClick() {
        Bundle extras = mDisplayMagnificationPreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_screen_magnification_title));
        extras.putCharSequence(EXTRA_SUMMARY, getActivity().getResources().getText(
                R.string.accessibility_screen_magnification_summary));
        extras.putBoolean(EXTRA_CHECKED, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mDisplayMagnificationPreferenceScreen,
                mDisplayMagnificationPreferenceScreen);
    }

    private void initializeAllPreferences() {
        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
        mSystemsCategory = (PreferenceCategory) findPreference(SYSTEM_CATEGORY);

        // Large text.
        //HQ_jiazaizheng 20150820 modify for HQ01336076
        //mToggleLargeTextPreference = (SwitchPreference) findPreference(TOGGLE_LARGE_TEXT_PREFERENCE);

        /*HQ_yuankangbo 2015-07-24 modify for AccessibilitySettings fontsize */
       // mToggleLargeTextPreference.setOnPreferenceChangeListener(this);//HQ_jiazaizheng 20150820 modify for HQ01336076

        // Text contrast.
        mToggleHighTextContrastPreference =
                (SwitchPreference) findPreference(TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE);
        mToggleHighTextContrastPreference.setOnPreferenceChangeListener(this);//HQ_jiazaizheng 20150805 modify for HQ01307950

        // Display inversion.
        mToggleInversionPreference = (SwitchPreference) findPreference(TOGGLE_INVERSION_PREFERENCE);
        mToggleInversionPreference.setOnPreferenceChangeListener(this);

        // Power button ends calls.
        mTogglePowerButtonEndsCallPreference =
                (SwitchPreference) findPreference(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
        mTogglePowerButtonEndsCallPreference.setOnPreferenceChangeListener(this);//HQ_jiazaizheng 20150804 modify for HQ01307988
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !Utils.isVoiceCapable(getActivity())) {
            mSystemsCategory.removePreference(mTogglePowerButtonEndsCallPreference);
        }

        // Lock screen rotation.
        //HQ_jiazaizheng 20150820 modify for HQ01336076
        /*mToggleLockScreenRotationPreference =
                (SwitchPreference) findPreference(TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE);
        mToggleLockScreenRotationPreference.setOnPreferenceChangeListener(this);//HQ_jiazaizheng 20150811 modify for HQ01314762
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            mSystemsCategory.removePreference(mToggleLockScreenRotationPreference);
        }*/

        // Speak passwords.
        mToggleSpeakPasswordPreference =
                (SwitchPreference) findPreference(TOGGLE_SPEAK_PASSWORD_PREFERENCE);
        mToggleSpeakPasswordPreference.setOnPreferenceChangeListener(this);   //hanchao add HQ01387002 2015.9.2

        // Long press timeout.
        mSelectLongPressTimeoutPreference =
                (ListPreference) findPreference(SELECT_LONG_PRESS_TIMEOUT_PREFERENCE);
        mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (mLongPressTimeoutValuetoTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(
                    R.array.long_press_timeout_selector_values);
            mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(
                    R.array.long_press_timeout_selector_titles);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mLongPressTimeoutValuetoTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }

        // Captioning.
        mCaptioningPreferenceScreen = (PreferenceScreen) findPreference(
                CAPTIONING_PREFERENCE_SCREEN);

        // Display magnification.
        mDisplayMagnificationPreferenceScreen = (PreferenceScreen) findPreference(
                DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN);

        // Display color adjustments.
        mDisplayDaltonizerPreferenceScreen = (PreferenceScreen) findPreference(
                DISPLAY_DALTONIZER_PREFERENCE_SCREEN);

        // Global gesture.
        mGlobalGesturePreferenceScreen =
                (PreferenceScreen) findPreference(ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN);
        //mGlobalGesturePreferenceScreen.setEnabled(false);//HQ_jiazaizheng 20150828 modify for HQ01347493
        final int longPressOnPowerBehavior = getActivity().getResources().getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior);
        final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || longPressOnPowerBehavior != LONG_PRESS_POWER_GLOBAL_ACTIONS) {
            // Remove accessibility shortcut if power key is not present
            // nor long press power does not show global actions menu.
            mSystemsCategory.removePreference(mGlobalGesturePreferenceScreen);
        }

	// gepengfei modified to remmove IPO
        //if (!FeatureOption.MTK_IPO_SUPPORT || UserHandle.myUserId() != UserHandle.USER_OWNER){
    }

    private void updateAllPreferences() {
        updateServicesPreferences();
        updateSystemPreferences();
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        mServicesCategory.removeAll();

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                getActivity());
        List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            AccessibilityServiceInfo info = installedServices.get(i);

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);
            String serviceEnabledString;
            if (serviceEnabled) {
                serviceEnabledString = getString(R.string.accessibility_feature_state_on);
            } else {
                serviceEnabledString = getString(R.string.accessibility_feature_state_off);
            }

            // Disable all accessibility services that are not permitted.
            String packageName = serviceInfo.packageName;
            boolean serviceAllowed =
                    permittedServices == null || permittedServices.contains(packageName);
            preference.setEnabled(serviceAllowed || serviceEnabled);

            String summaryString;
            if (serviceAllowed) {
                summaryString = serviceEnabledString;
            } else  {
                summaryString = getString(R.string.accessibility_feature_or_input_method_not_allowed);
            }
            preference.setSummary(summaryString);

            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString(EXTRA_SUMMARY, description);

            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);

            mServicesCategory.addPreference(preference);
        }

        if (mServicesCategory.getPreferenceCount() == 0) {
            if (mNoServicesMessagePreference == null) {
                mNoServicesMessagePreference = new Preference(getActivity());
                mNoServicesMessagePreference.setPersistent(false);
                mNoServicesMessagePreference.setLayoutResource(
                        R.layout.text_description_preference);
                mNoServicesMessagePreference.setSelectable(false);
                mNoServicesMessagePreference.setSummary(
                        getString(R.string.accessibility_no_services_installed));
            }
            mServicesCategory.addPreference(mNoServicesMessagePreference);
        }
    }

    private void updateSystemPreferences() {
        // Large text.
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException re) {
            /* ignore */
        }

        mToggleHighTextContrastPreference.setChecked(
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1);

        /// M: MTK fix fonts problem, CR ALPS00261477 @{
        float updateFontScale = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE_EXTRALARGE, -1);
        boolean isChecked = false;
        if (updateFontScale == -1) {
            isChecked = mIsScreenLarge ? (mCurConfig.fontScale == LARGE_FONT_SCALE_TABLET)
                            : (mCurConfig.fontScale == LARGE_FONT_SCALE_PHONE);
        } else {
            isChecked = (mCurConfig.fontScale == updateFontScale);
        }
        //mToggleLargeTextPreference.setChecked(isChecked);//HQ_jiazaizheng 20150820 modify for HQ01336076
        /// @}

        // If the quick setting is enabled, the preference MUST be enabled.
        mToggleInversionPreference.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1);

        // Power button ends calls.
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
        }

        // Auto-rotate screen
        //updateLockScreenRotationCheckbox();//HQ_jiazaizheng 20150820 modify for HQ01336076

        // Speak passwords.
        final boolean speakPasswordEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0) != 0;
        mToggleSpeakPasswordPreference.setChecked(speakPasswordEnabled);

        // Long press timeout.
        final int longPressTimeout = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
        String value = String.valueOf(longPressTimeout);
        mSelectLongPressTimeoutPreference.setValue(value);
        mSelectLongPressTimeoutPreference.setSummary(mLongPressTimeoutValuetoTitleMap.get(value));

        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED,
                mCaptioningPreferenceScreen);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                mDisplayMagnificationPreferenceScreen);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                mDisplayDaltonizerPreferenceScreen);

        // Global gesture
        final boolean globalGestureEnabled = Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1;
        if (globalGestureEnabled) {
            mGlobalGesturePreferenceScreen.setSummary(
                    R.string.accessibility_global_gesture_preference_summary_on);
        } else {
            mGlobalGesturePreferenceScreen.setSummary(
                    R.string.accessibility_global_gesture_preference_summary_off);
        }
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }

    //HQ_jiazaizheng 20150820 modify for HQ01336076 start
    /*private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mToggleLockScreenRotationPreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }*/
    //HQ_jiazaizheng 20150820 modify for HQ01336076 end

    private void loadInstalledServices() {
        Set<ComponentName> installedServices = sInstalledServices;
        installedServices.clear();

        List<AccessibilityServiceInfo> installedServiceInfos =
                AccessibilityManager.getInstance(getActivity())
                        .getInstalledAccessibilityServiceList();
        if (installedServiceInfos == null) {
            return;
        }

        final int installedServiceInfoCount = installedServiceInfos.size();
        for (int i = 0; i < installedServiceInfoCount; i++) {
            ResolveInfo resolveInfo = installedServiceInfos.get(i).getResolveInfo();
            ComponentName installedService = new ComponentName(
                    resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            installedServices.add(installedService);
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            PackageManager packageManager = context.getPackageManager();
            AccessibilityManager accessibilityManager = (AccessibilityManager)
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE);

            String screenTitle = context.getResources().getString(
                    R.string.accessibility_services_title);

            // Indexing all services, regardless if enabled.
            List<AccessibilityServiceInfo> services = accessibilityManager
                    .getInstalledAccessibilityServiceList();
            final int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                AccessibilityServiceInfo service = services.get(i);
                if (service == null || service.getResolveInfo() == null) {
                    continue;
                }

                ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName,
                        serviceInfo.name);

                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = componentName.flattenToString();
                indexable.title = service.getResolveInfo().loadLabel(packageManager).toString();
                indexable.summaryOn = context.getString(R.string.accessibility_feature_state_on);
                indexable.summaryOff = context.getString(R.string.accessibility_feature_state_off);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
               boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList<SearchIndexableResource>();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.accessibility_settings;
            indexables.add(indexable);
            return indexables;
        }
    };
}
