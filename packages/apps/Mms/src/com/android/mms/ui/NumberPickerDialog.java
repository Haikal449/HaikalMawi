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

package com.android.mms.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.android.mms.R;
/// M:
import android.util.Log;

/**
 * A dialog that prompts the user for the message deletion limits.
 */
public class NumberPickerDialog extends AlertDialog implements OnClickListener {

    private static final String NUMBER = "number";
    /// M: Code analyze 001, new feature, personal use on device by Hongduo Wang @{
    private int mInitialNumber;
    /// @}
    private final String TAG = "Mms/NumberPickerDialog";
    private Context mContext;

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnNumberSetListener {

        /**
         * @param number The number that was set.
         */
        void onNumberSet(int number);
    }

    private final NumberPicker mNumberPicker;
    private final OnNumberSetListener mCallback;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public NumberPickerDialog(Context context,
            OnNumberSetListener callBack,
            int number,
            int rangeMin,
            int rangeMax,
            int title) {
        this(context, AlertDialog.THEME_HOLO_LIGHT, callBack, number, rangeMin, rangeMax, title);
    }

    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public NumberPickerDialog(Context context,
            int theme,
            OnNumberSetListener callBack,
            int number,
            int rangeMin,
            int rangeMax,
            int title) {
        super(context, theme);
        mContext= context;
		/*HQ_zhangjing 2015-10-08 modified for CQ   HQ01431856 begin*/
		int alertThemeId = context.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null);
		if(alertThemeId > 0) { 
			context.setTheme(alertThemeId);
		}else{
        	context.setTheme(com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert);
		}
        //context.setTheme(com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert);
		/*HQ_zhangjing 2015-10-08 modified for CQ   HQ01431856 end*/
        mCallback = callBack;
        /// M: Code analyze 001, new feature, personal use on device by Hongduo Wang @{
        mInitialNumber = number;
        /// @}

        setTitle(title);

        setButton(DialogInterface.BUTTON_POSITIVE, context.getText(R.string.set), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getText(R.string.no),
                (OnClickListener) null);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_dialog, null);
        setView(view);
        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        Log.d(TAG, "NumberPickerDialog mInitialNumber " + mInitialNumber);
        mNumberPicker.setMaxValue(rangeMax);
        mNumberPicker.setMinValue(rangeMin);
        /// M: Code analyze 001, new feature, personal use on device by Hongduo Wang @{
        mNumberPicker.setValue(mInitialNumber);
        /// @}
        mNumberPicker.setOnLongPressUpdateInterval(100);
        mNumberPicker.setWrapSelectorWheel(false);
        /// M: for ALPS01844387, the activity theme had been set by NumberPickerDialog as
        // Theme_Holo_Light_Dialog_Alert, should re-set as MmsTheme when dismiss dialog. @{
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
			/*HQ_zhangjing 2015-10-08 modified for CQ   HQ01431856 begin*/
                //mContext.setTheme(R.style.MmsTheme);
				int themeId = mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null);
				if(themeId > 0) { 
					mContext.setTheme(themeId);
				}else{
					mContext.setTheme(R.style.MmsTheme);
				}
			/*HQ_zhangjing 2015-10-08 modified for CQ   HQ01431856 end*/
            }
        });
        /// @}
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            mNumberPicker.clearFocus();
            mCallback.onNumberSet(mNumberPicker.getValue());
            dialog.dismiss();
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
         state.putInt(NUMBER, mNumberPicker.getValue());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int number = savedInstanceState.getInt(NUMBER);
        mNumberPicker.setValue(number);
    }
}
