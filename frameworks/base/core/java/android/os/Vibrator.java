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

package android.os;

import android.app.ActivityThread;
import android.content.Context;
import android.media.AudioAttributes;

/**
 * Class that operates the vibrator on the device.
 * <p>
 * If your process exits, any vibration you started will stop.
 * </p>
 *
 * To obtain an instance of the system vibrator, call
 * {@link Context#getSystemService} with {@link Context#VIBRATOR_SERVICE} as the argument.
 */
public abstract class Vibrator {

    private final String mPackageName;

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    public Vibrator() {
        mPackageName = ActivityThread.currentPackageName();
    }

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    protected Vibrator(Context context) {
        mPackageName = context.getOpPackageName();
    }

    /**
     * Check whether the hardware has a vibrator.
     *
     * @return True if the hardware has a vibrator, else false.
     */
    public abstract boolean hasVibrator();

    /**
     * Vibrate constantly for the specified period of time.
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#VIBRATE}.
     *
     * @param milliseconds The number of milliseconds to vibrate.
     */
    public void vibrate(long milliseconds) {
        vibrate(milliseconds, null);
    }

    /**
     * Vibrate constantly for the specified period of time.
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#VIBRATE}.
     *
     * @param milliseconds The number of milliseconds to vibrate.
     * @param attributes {@link AudioAttributes} corresponding to the vibration. For example,
     *        specify {@link AudioAttributes#USAGE_ALARM} for alarm vibrations or
     *        {@link AudioAttributes#USAGE_NOTIFICATION_RINGTONE} for
     *        vibrations associated with incoming calls.
     */
    public void vibrate(long milliseconds, AudioAttributes attributes) {
        vibrate(Process.myUid(), mPackageName, milliseconds, attributes);
    }

    /**
     * Vibrate with a given pattern.
     *
     * <p>
     * Pass in an array of ints that are the durations for which to turn on or off
     * the vibrator in milliseconds.  The first value indicates the number of milliseconds
     * to wait before turning the vibrator on.  The next value indicates the number of milliseconds
     * for which to keep the vibrator on before turning it off.  Subsequent values alternate
     * between durations in milliseconds to turn the vibrator off or to turn the vibrator on.
     * </p><p>
     * To cause the pattern to repeat, pass the index into the pattern array at which
     * to start the repeat, or -1 to disable repeating.
     * </p>
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#VIBRATE}.
     *
     * @param pattern an array of longs of times for which to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if
     *        you don't want to repeat.
     */
    public void vibrate(long[] pattern, int repeat) {
        vibrate(pattern, repeat, null);
    }

    /**
     * Vibrate with a given pattern.
     *
     * <p>
     * Pass in an array of ints that are the durations for which to turn on or off
     * the vibrator in milliseconds.  The first value indicates the number of milliseconds
     * to wait before turning the vibrator on.  The next value indicates the number of milliseconds
     * for which to keep the vibrator on before turning it off.  Subsequent values alternate
     * between durations in milliseconds to turn the vibrator off or to turn the vibrator on.
     * </p><p>
     * To cause the pattern to repeat, pass the index into the pattern array at which
     * to start the repeat, or -1 to disable repeating.
     * </p>
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#VIBRATE}.
     *
     * @param pattern an array of longs of times for which to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if
     *        you don't want to repeat.
     * @param attributes {@link AudioAttributes} corresponding to the vibration. For example,
     *        specify {@link AudioAttributes#USAGE_ALARM} for alarm vibrations or
     *        {@link AudioAttributes#USAGE_NOTIFICATION_RINGTONE} for
     *        vibrations associated with incoming calls.
     */
    public void vibrate(long[] pattern, int repeat, AudioAttributes attributes) {
        vibrate(Process.myUid(), mPackageName, pattern, repeat, attributes);
    }

    /**
     * @hide
     * Like {@link #vibrate(long, AudioAttributes)}, but allowing the caller to specify that
     * the vibration is owned by someone else.
     */
    public abstract void vibrate(int uid, String opPkg, long milliseconds,
            AudioAttributes attributes);

    /**
     * @hide
     * Like {@link #vibrate(long[], int, AudioAttributes)}, but allowing the caller to specify that
     * the vibration is owned by someone else.
     */
    public abstract void vibrate(int uid, String opPkg, long[] pattern, int repeat,
            AudioAttributes attributes);

    /**
     * Turn the vibrator off.
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#VIBRATE}.
     */
    public abstract void cancel();

    //add by wuhuihui for HQ_VIBRATOR_MODE  -----start@{
    /**
     * @hide
     * Like {@link #setVibrateMode(int)}, but allowing the caller to specify that
     * the vibration is owned by someone else.
     */
     public abstract void setVibrateMode(int mode);
    //add by wuhuihui for HQ_VIBRATOR_MODE  -----end @}
}
