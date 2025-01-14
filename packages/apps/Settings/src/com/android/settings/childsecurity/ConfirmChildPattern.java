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

package com.android.settings.childsecurity;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternView.Cell;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;

import java.util.List;

import com.android.settings.R;

/**
 * Launch this when you want the user to confirm their lock pattern.
 *
 * Sets an activity result of {@link Activity#RESULT_OK} when the user
 * successfully confirmed their pattern.
 */
public class ConfirmChildPattern extends PreferenceActivity {

    /**
     * Names of {@link CharSequence} fields within the originating {@link Intent}
     * that are used to configure the keyguard confirmation view's labeling.
     * The view will use the system-defined resource strings for any labels that
     * the caller does not supply.
     */
    public static final String PACKAGE = "com.android.settings";
    public static final String HEADER_TEXT = PACKAGE + ".ConfirmChildPattern.header";
    public static final String FOOTER_TEXT = PACKAGE + ".ConfirmChildPattern.footer";
    public static final String HEADER_WRONG_TEXT = PACKAGE + ".ConfirmChildPattern.header_wrong";
    public static final String FOOTER_WRONG_TEXT = PACKAGE + ".ConfirmChildPattern.footer_wrong";

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_confirm_your_pattern_header);
        showBreadCrumbs(msg, msg);
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmChildPatternFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmChildPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /**
     * author: xingle
     * date: 20150321
     * purpose: return special rslt for BACK/HOME SW00122419
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            setResult(Activity.RESULT_FIRST_USER);
        }
        return super.onKeyDown(keyCode, event);
    }
    public static class ConfirmChildPatternFragment extends Fragment {

        // how long we wait to clear a wrong pattern
        private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

        private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

        private LockPatternView mLockPatternView;
        private ChooseChildLockHelper mChooseChildLockHelper;
        private int mNumWrongConfirmAttempts;
        private CountDownTimer mCountdownTimer;

        private TextView mHeaderTextView;
        private TextView mFooterTextView;
        private TextView mPasswordTip;// add xingle for child mode password tip (QL1701) 20150430

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mFooterText;
        private CharSequence mHeaderWrongText;
        private CharSequence mFooterWrongText;

        // required constructor for fragments
        public ConfirmChildPatternFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mChooseChildLockHelper = new ChooseChildLockHelper(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_child_pattern, null);// add xingle for child mode password tip (QL1701) 20150430
            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mFooterTextView = (TextView) view.findViewById(R.id.footerText);

            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            /* add xingle for child mode password tip (QL1701) 20150430 begin */
            mPasswordTip = (TextView) view.findViewById(R.id.tipText);
            mPasswordTip.setText(getText(R.string.child_mode_password_tip_header)
                    + ": " + mChooseChildLockHelper.getPasswordTip());
            mPasswordTip.setVisibility(View.INVISIBLE);
            /* add xingle for child mode password tip (QL1701) 20150430 end */

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                mHeaderText = intent.getCharSequenceExtra(HEADER_TEXT);
                mFooterText = intent.getCharSequenceExtra(FOOTER_TEXT);
                mHeaderWrongText = intent.getCharSequenceExtra(HEADER_WRONG_TEXT);
                mFooterWrongText = intent.getCharSequenceExtra(FOOTER_WRONG_TEXT);
            }

            mLockPatternView.setTactileFeedbackEnabled(true);
            mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState != null) {
                mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
            } else {
                // on first launch, if no lock pattern is set, then finish with
                // success (don't want user to get stuck confirming something that
                // doesn't exist).
                if (!mChooseChildLockHelper.savedPatternExists()) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // deliberately not calling super since we are managing this in full
            outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            // if the user is currently locked out, enforce it.
            long deadline = 30000L;//mChooseChildLockHelper.getLockoutAttemptDeadline();
            if (deadline != 0) {
                //handleAttemptLockout(deadline);// delete xingle for child mode password tip (QL1701) 20150430
            } else if (!mLockPatternView.isEnabled()) {
                // The deadline has passed, but the timer was cancelled...
                // Need to clean up.
                mNumWrongConfirmAttempts = 0;
                updateStage(Stage.NeedToUnlock);
            }
        }

        private void updateStage(Stage stage) {
            switch (stage) {
                case NeedToUnlock:
                    if (mHeaderText != null) {
                        mHeaderTextView.setText(mHeaderText);
                    } else {
                        mHeaderTextView.setText(R.string.lockpattern_need_to_unlock);
                    }
                    if (mFooterText != null) {
                        mFooterTextView.setText(mFooterText);
                    } else {
                        mFooterTextView.setText(R.string.lockpattern_need_to_unlock_footer);
                    }

                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    break;
                case NeedToUnlockWrong:
                    if (mHeaderWrongText != null) {
                        mHeaderTextView.setText(mHeaderWrongText);
                    } else {
                        mHeaderTextView.setText(R.string.lockpattern_need_to_unlock_wrong);
                    }
                    if (mFooterWrongText != null) {
                        mFooterTextView.setText(mFooterWrongText);
                    } else {
                        mFooterTextView.setText(R.string.lockpattern_need_to_unlock_wrong_footer);
                    }

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    break;
                case LockedOut:
                    mLockPatternView.clearPattern();
                    // enabled = false means: disable input, and have the
                    // appearance of being disabled.
                    mLockPatternView.setEnabled(false); // appearance of being disabled
                    break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
        }

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }

        /**
         * The pattern listener that responds according to a user confirming
         * an existing lock pattern.
         */
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
                = new LockPatternView.OnPatternListener()  {

            public void onPatternStart() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCleared() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<Cell> pattern) {

            }

            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mChooseChildLockHelper.checkPattern(pattern)) {

                    Intent intent = new Intent();
                    intent.putExtra(ChooseChildLockHelper.EXTRA_KEY_PASSWORD,
                                    LockPatternUtils.patternToString(pattern));

                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                } else {
                    /* add xingle for child mode password tip (QL1701) 20150430 begin */
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                        long deadline = 30000L;//mChooseChildLockHelper.setLockoutAttemptDeadline();
                        //handleAttemptLockout(deadline);
                        ++mNumWrongConfirmAttempts;
                    }
                    updateStage(Stage.NeedToUnlockWrong);
                    postClearPatternRunnable();
                    if (mNumWrongConfirmAttempts >= 5 && !TextUtils.isEmpty(mChooseChildLockHelper.getPasswordTip())) {
                        mPasswordTip.setVisibility(View.VISIBLE);
                    }
                    /* add xingle for child mode password tip (QL1701) 20150430 end */
                }
            }
        };


        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    mHeaderTextView.setText(R.string.lockpattern_too_many_failed_confirmation_attempts_header);
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    mFooterTextView.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    mNumWrongConfirmAttempts = 0;
                    updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }
    }
}
