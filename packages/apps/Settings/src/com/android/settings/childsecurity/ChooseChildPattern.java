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

package com.android.settings.childsecurity;

import com.google.android.collect.Lists;

import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.settings.childsecurity.ChooseChildGeneric.ChooseChildGenericFragment;

import static com.android.internal.widget.LockPatternView.DisplayMode;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.settings.R;

/**
 * If the user has a lock pattern set already, makes them confirm the existing one.
 *
 * Then, prompts the user to choose a lock pattern:
 * - prompts for initial pattern
 * - asks for confirmation / restart
 * - saves chosen password when confirmed
 */
public class ChooseChildPattern extends PreferenceActivity {
    /**
     * Used by the choose lock pattern wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;
    public static final String PATTERN_METHOD_TYPE = "key_lock_method";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseChildPatternFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChooseChildPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.child_mode_choose_your_pattern_header);
        showBreadCrumbs(msg, msg);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // *** TODO ***
        // ChooseChildPatternFragment.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    public static class ChooseChildPatternFragment extends Fragment
            implements View.OnClickListener {

        public static final int CONFIRM_EXISTING_REQUEST = 55;

        // how long after a confirmation message is shown before moving on
        static final int INFORMATION_MSG_TIMEOUT_MS = 3000;

        // how long we wait to clear a wrong pattern
        private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

        private static final int ID_EMPTY_MESSAGE = -1;

        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        protected TextView mFooterText;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected List<LockPatternView.Cell> mChosenPattern = null;
        /* add lihaizhou for password tip  begin */
        private TextView mTipHeader;
        private EditText mPasswordTip;
        /* add lihaizhou for password tip end */

        /**
         * The patten used during the help screen to show how to draw a pattern.
         */
        private final List<LockPatternView.Cell> mAnimatePattern =
                Collections.unmodifiableList(Lists.newArrayList(
                        LockPatternView.Cell.of(0, 0),
                        LockPatternView.Cell.of(0, 1),
                        LockPatternView.Cell.of(1, 1),
                        LockPatternView.Cell.of(2, 1)
                ));

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case CONFIRM_EXISTING_REQUEST:
                    if (resultCode != Activity.RESULT_OK) {
                        getActivity().setResult(RESULT_FINISHED);
                        getActivity().finish();
                    }
                    updateStage(Stage.Introduction);
                    break;
            }
        }

        /**
         * The pattern listener that responds according to a user choosing a new
         * lock pattern.
         */
        protected LockPatternView.OnPatternListener mChooseNewLockPatternListener =
                new LockPatternView.OnPatternListener() {

                public void onPatternStart() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    patternInProgress();
                }

                public void onPatternCleared() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    mFooterLeftButton.setEnabled(true); // add lihaizhou to fix cancel disabled after pattern cancel 
                }

                public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                    if (mUiStage == Stage.NeedToConfirm || mUiStage == Stage.ConfirmWrong) {
                        if (mChosenPattern == null) throw new IllegalStateException(
                                "null chosen pattern in stage 'need to confirm");
                        if (mChosenPattern.equals(pattern)) {
                            updateStage(Stage.ChoiceConfirmed);
                        } else {
                            updateStage(Stage.ConfirmWrong);
                        }
                    } else if (mUiStage == Stage.Introduction || mUiStage == Stage.ChoiceTooShort){
                        if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                            updateStage(Stage.ChoiceTooShort);
                        } else {
                            mChosenPattern = new ArrayList<LockPatternView.Cell>(pattern);
                            updateStage(Stage.FirstChoiceValid);
                        }
                    } else {
                        throw new IllegalStateException("Unexpected stage " + mUiStage + " when "
                                + "entering the pattern.");
                    }
                }

                public void onPatternCellAdded(List<Cell> pattern) {

                }

                private void patternInProgress() {
                    mHeaderText.setText(R.string.lockpattern_recording_inprogress);
                    mFooterText.setText("");
                    mFooterLeftButton.setEnabled(false);
                    mFooterRightButton.setEnabled(false);
                }
         };


        /**
         * The states of the left footer button.
         */
        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(ID_EMPTY_MESSAGE, false);


            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            LeftButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * The states of the right button.
         */
        enum RightButtonMode {
            Continue(R.string.lockpattern_continue_button_text, true),
            ContinueDisabled(R.string.lockpattern_continue_button_text, false),
            Confirm(R.string.lockpattern_confirm_button_text, true),
            ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
            Ok(android.R.string.ok, true);

            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            RightButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockpattern_recording_intro_header,
                    LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            HelpScreen(
                    R.string.lockpattern_settings_help_how_to_record,
                    LeftButtonMode.Gone, RightButtonMode.Ok, ID_EMPTY_MESSAGE, false),
            ChoiceTooShort(
                    R.string.lockpattern_recording_incorrect_too_short,
                    LeftButtonMode.Retry, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            FirstChoiceValid(
                    R.string.lockpattern_pattern_entered_header,
                    LeftButtonMode.Retry, RightButtonMode.Continue, ID_EMPTY_MESSAGE, false),
            NeedToConfirm(
                    R.string.lockpattern_need_to_confirm,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ConfirmWrong(
                    R.string.lockpattern_need_to_unlock_wrong,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ChoiceConfirmed(
                    R.string.lockpattern_pattern_confirmed_header,
                    LeftButtonMode.Cancel, RightButtonMode.Confirm, ID_EMPTY_MESSAGE, false);


            /**
             * @param headerMessage The message displayed at the top.
             * @param leftMode The mode of the left button.
             * @param rightMode The mode of the right button.
             * @param footerMessage The footer message.
             * @param patternEnabled Whether the pattern widget is enabled.
             */
            Stage(int headerMessage,
                    LeftButtonMode leftMode,
                    RightButtonMode rightMode,
                    int footerMessage, boolean patternEnabled) {
                this.headerMessage = headerMessage;
                this.leftMode = leftMode;
                this.rightMode = rightMode;
                this.footerMessage = footerMessage;
                this.patternEnabled = patternEnabled;
            }

            final int headerMessage;
            final LeftButtonMode leftMode;
            final RightButtonMode rightMode;
            final int footerMessage;
            final boolean patternEnabled;
        }

        private Stage mUiStage = Stage.Introduction;

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        private ChooseChildLockHelper mChooseChildLockHelper;

        private static final String KEY_UI_STAGE = "uiStage";
        private static final String KEY_PATTERN_CHOICE = "chosenPattern";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mChooseChildLockHelper = new ChooseChildLockHelper(getActivity());
            if (!(getActivity() instanceof ChooseChildPattern)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            // setupViews()
            View view = inflater.inflate(R.layout.choose_child_pattern, null);// modified lihaizhou for password tip 
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);
            mLockPatternView.setTactileFeedbackEnabled(true);

            mFooterText = (TextView) view.findViewById(R.id.footerText);

            mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);

            mFooterLeftButton.setOnClickListener(this);
            mFooterRightButton.setOnClickListener(this);
            /* add lihaizhou for password tip  begin */
            mTipHeader = (TextView) view.findViewById(R.id.tipHeaderText);
            mPasswordTip = (EditText) view.findViewById(R.id.password_tip_entry);
            mTipHeader.setVisibility(View.INVISIBLE);
            mPasswordTip.setVisibility(View.INVISIBLE);
            /* add lihaizhou for password tip  end */
            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(
                    R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            final boolean confirmCredentials = getActivity().getIntent()
                    .getBooleanExtra("confirm_credentials", true);

            if (savedInstanceState == null) {
                if (confirmCredentials) {
                    // first launch. As a security measure, we're in NeedToConfirm mode until we
                    // know there isn't an existing password or the user confirms their password.
                    updateStage(Stage.NeedToConfirm);
                    boolean launchedConfirmationActivity =
                        mChooseChildLockHelper.launchConfirmationActivity(
                                CONFIRM_EXISTING_REQUEST, null, null);
                    if (!launchedConfirmationActivity) {
                        updateStage(Stage.Introduction);
                    }
                } else {
                    updateStage(Stage.Introduction);
                }
            } else {
                // restore from previous state
                final String patternString = savedInstanceState.getString(KEY_PATTERN_CHOICE);
                if (patternString != null) {
                    mChosenPattern = LockPatternUtils.stringToPattern(patternString);
                }
                updateStage(Stage.values()[savedInstanceState.getInt(KEY_UI_STAGE)]);
            }
            return view;
        }

        public void onClick(View v) {
            if (v == mFooterLeftButton) {
                if (mUiStage.leftMode == LeftButtonMode.Retry) {
                    mChosenPattern = null;
                    mLockPatternView.clearPattern();
                    updateStage(Stage.Introduction);
                } else if (mUiStage.leftMode == LeftButtonMode.Cancel) {
                    // They are canceling the entire wizard
                    getActivity().setResult(RESULT_CANCELED); // modified lihaizhouto fix 1st time password set cancled  20150714
                    getActivity().finish();
                } else {
                    throw new IllegalStateException("left footer button pressed, but stage of " +
                        mUiStage + " doesn't make sense");
                }
            } else if (v == mFooterRightButton) {

                if (mUiStage.rightMode == RightButtonMode.Continue) {
                    if (mUiStage != Stage.FirstChoiceValid) {
                        throw new IllegalStateException("expected ui stage " + Stage.FirstChoiceValid
                                + " when button is " + RightButtonMode.Continue);
                    }
                    updateStage(Stage.NeedToConfirm);
                    /* add lihaizhou for password tip  begin */
                    mTipHeader.setVisibility(View.VISIBLE);
                    mPasswordTip.setVisibility(View.VISIBLE);
                    mPasswordTip.clearFocus();
                    /* add lihaizhou for password tip end */
                } else if (mUiStage.rightMode == RightButtonMode.Confirm) {
                    if (mUiStage != Stage.ChoiceConfirmed) {
                        throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed
                                + " when button is " + RightButtonMode.Confirm);
                    }
                    saveChosenPatternAndFinish();
                } else if (mUiStage.rightMode == RightButtonMode.Ok) {
                    if (mUiStage != Stage.HelpScreen) {
                        throw new IllegalStateException("Help screen is only mode with ok button, but " +
                                "stage is " + mUiStage);
                    }
                    mLockPatternView.clearPattern();
                    mLockPatternView.setDisplayMode(DisplayMode.Correct);
                    updateStage(Stage.Introduction);
                }
            }
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if (mUiStage == Stage.HelpScreen) {
                    updateStage(Stage.Introduction);
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_MENU && mUiStage == Stage.Introduction) {
                updateStage(Stage.HelpScreen);
                return true;
            }
            return false;
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putInt(KEY_UI_STAGE, mUiStage.ordinal());
            if (mChosenPattern != null) {
                outState.putString(KEY_PATTERN_CHOICE,
                        LockPatternUtils.patternToString(mChosenPattern));
            }
        }

        /**
         * Updates the messages and buttons appropriate to what stage the user
         * is at in choosing a view.  This doesn't handle clearing out the pattern;
         * the pattern is expected to be in the right state.
         * @param stage
         */
        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;

            mUiStage = stage;

            // header text, footer text, visibility and
            // enabled state all known from the stage
            if (stage == Stage.ChoiceTooShort) {
                mHeaderText.setText(
                        getResources().getString(
                                stage.headerMessage,
                                LockPatternUtils.MIN_LOCK_PATTERN_SIZE));
            } else {
                mHeaderText.setText(stage.headerMessage);
            }
            if (stage.footerMessage == ID_EMPTY_MESSAGE) {
                mFooterText.setText("");
            } else {
                mFooterText.setText(stage.footerMessage);
            }

            if (stage.leftMode == LeftButtonMode.Gone) {
                mFooterLeftButton.setVisibility(View.GONE);
            } else {
                mFooterLeftButton.setVisibility(View.VISIBLE);
                mFooterLeftButton.setText(stage.leftMode.text);
                mFooterLeftButton.setEnabled(stage.leftMode.enabled);
            }

            mFooterRightButton.setText(stage.rightMode.text);
            mFooterRightButton.setEnabled(stage.rightMode.enabled);

            // same for whether the patten is enabled
            if (stage.patternEnabled) {
                mLockPatternView.enableInput();
            } else {
                mLockPatternView.disableInput();
            }

            // the rest of the stuff varies enough that it is easier just to handle
            // on a case by case basis.
            mLockPatternView.setDisplayMode(DisplayMode.Correct);

            switch (mUiStage) {
                case Introduction:
                    mLockPatternView.clearPattern();
                    break;
                case HelpScreen:
                    mLockPatternView.setPattern(DisplayMode.Animate, mAnimatePattern);
                    break;
                case ChoiceTooShort:
                    mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
                case FirstChoiceValid:
                    break;
                case NeedToConfirm:
                    mLockPatternView.clearPattern();
                    break;
                case ConfirmWrong:
                    mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
                case ChoiceConfirmed:
                    mPasswordTip.requestFocus();//add lihaizhou for password tip 
                    break;
            }

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }


        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }

        private void saveChosenPatternAndFinish() {
            mChooseChildLockHelper.saveLockPattern(mChosenPattern);
            /* add lihaizhou for password tip begin */
            mChooseChildLockHelper.savePasswordTip(mPasswordTip.getText() != null
                    ? mPasswordTip.getText().toString() : "");
            /* add lihaizhou for password tip end */

            getActivity().setResult(RESULT_FINISHED);
            getActivity().finish();
        }
    }
}
