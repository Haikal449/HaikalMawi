package com.android.dialer.settings;

import com.google.common.collect.Lists;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity.Header;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;

import java.util.List;

public class DialerSettingsActivity extends PreferenceActivity {

    protected SharedPreferences mPreferences;
    private HeaderAdapter mHeaderAdapter;

    private static final int OWNER_HANDLE_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {

	    // /annotated by guofeiyao
	    /*
        final Header generalSettingsHeader = new Header();
        generalSettingsHeader.titleRes = R.string.general_settings_label;
        generalSettingsHeader.fragment = GeneralSettingsFragment.class.getName();
        target.add(generalSettingsHeader);
        */
        // /end

		// /added by guofeiyao
		loadHeadersFromResource(R.xml.speed_response, target);
		// /end

        // Only show call setting menus if the current user is the primary/owner user.
        if (isPrimaryUser()) {
            final TelephonyManager telephonyManager =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            // Show "Call Settings" if there is one SIM and "Phone Accounts" if there are more.
            if (telephonyManager.getPhoneCount() <= 1) {
                final Header callSettingsHeader = new Header();
                Intent callSettingsIntent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
                callSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                callSettingsHeader.titleRes = R.string.call_settings_label;
                callSettingsHeader.intent = callSettingsIntent;
                target.add(callSettingsHeader);
            } else {
                final Header phoneAccountSettingsHeader = new Header();
                Intent phoneAccountSettingsIntent =
                        new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                phoneAccountSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                phoneAccountSettingsHeader.titleRes = R.string.phone_account_settings_label;
                phoneAccountSettingsHeader.intent = phoneAccountSettingsIntent;
                target.add(phoneAccountSettingsHeader);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            // We don't have access to the hidden getHeaders() method, so grab the headers from
            // the intended adapter and then replace it with our own.
            int headerCount = adapter.getCount();
            List<Header> headers = Lists.newArrayList();
            for (int i = 0; i < headerCount; i++) {
                headers.add((Header) adapter.getItem(i));
            }
            mHeaderAdapter = new HeaderAdapter(this, headers);
            super.setListAdapter(mHeaderAdapter);
        }
    }

    /**
     * Whether a user handle associated with the current user is that of the primary owner. That is,
     * whether there is a user handle which has an id which matches the owner's handle.
     * @return Whether the current user is the primary user.
     */
    private boolean isPrimaryUser() {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        List<UserHandle> userHandles = userManager.getUserProfiles();
        for (int i = 0; i < userHandles.size(); i++){
            if (userHandles.get(i).myUserId() == OWNER_HANDLE_ID) {
                return true;
            }
        }

        return false;
    }

    /**
     * This custom {@code ArrayAdapter} is mostly identical to the equivalent one in
     * {@code PreferenceActivity}, except with a local layout resource.
     */
    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static class HeaderViewHolder {
            TextView title;
            TextView summary;
        }

        private LayoutInflater mInflater;

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.dialer_preferences, parent, false);
                holder = new HeaderViewHolder();
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.summary = (TextView) view.findViewById(R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            Header header = getItem(position);
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
            return view;
        }
    }

    //---------------------------------mtk--------------------------------
    /// Fix CR: ALPS01792584, Invalidate headers when leave this page @{
    @Override
    protected void onStop() {
        super.onStop();
        invalidateHeaders();
    }
    /// @}

    //---------------------------------MTK--------------------------------
    /// Fix CR: ALPS01810923, Invalidate headers when resume @{
    @Override
    protected void onResume() {
        super.onResume();
        invalidateHeaders();
    }
    /// @}
    
}
