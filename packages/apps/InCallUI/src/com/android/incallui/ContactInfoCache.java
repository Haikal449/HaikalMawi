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
 * limitations under the License
 */

package com.android.incallui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import com.android.contacts.common.util.PhoneNumberHelper;
import com.android.incallui.service.PhoneNumberService;
import com.android.incalluibind.ServiceFactory;
import com.android.services.telephony.common.MoreStrings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.mediatek.incallui.CallDetailChangeHandler;
import com.mediatek.incallui.CallDetailChangeHandler.CallDetailChangeListener;
import com.mediatek.incallui.volte.InCallUIVolteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class responsible for querying Contact Information for Call objects. Can perform asynchronous
 * requests to the Contact Provider for information as well as respond synchronously for any data
 * that it currently has cached from previous queries. This class always gets called from the UI
 * thread so it does not need thread protection.
 */
public class ContactInfoCache implements ContactsAsyncHelper.OnImageLoadCompleteListener {

    private static final String TAG = ContactInfoCache.class.getSimpleName();
    private static final int TOKEN_UPDATE_PHOTO_FOR_CALL_STATE = 0;

    private final Context mContext;
    private final PhoneNumberService mPhoneNumberService;
    private final HashMap<String, ContactCacheEntry> mInfoMap = Maps.newHashMap();
    private final HashMap<String, Set<ContactInfoCacheCallback>> mCallBacks = Maps.newHashMap();

    private static ContactInfoCache sCache = null;

    private Drawable mDefaultContactPhotoDrawable;
    private Drawable mConferencePhotoDrawable;

    public static synchronized ContactInfoCache getInstance(Context mContext) {
        if (sCache == null) {
            sCache = new ContactInfoCache(mContext.getApplicationContext());
        }
        return sCache;
    }

    private ContactInfoCache(Context context) {
        mContext = context;
        mPhoneNumberService = ServiceFactory.newPhoneNumberService(context);
        /// M: For volte @{
        CallDetailChangeHandler.getInstance().addCallDetailChangeListener(mCallDetailChangeListener);
        /// @}
    }

    public ContactCacheEntry getInfo(String callId) {
        return mInfoMap.get(callId);
    }

    public static ContactCacheEntry buildCacheEntryFromCall(Context context, Call call,
            boolean isIncoming) {
        final ContactCacheEntry entry = new ContactCacheEntry();

        // TODO: get rid of caller info.
        final CallerInfo info = CallerInfoUtils.buildCallerInfo(context, call);
        ContactInfoCache.populateCacheEntry(context, info, entry, call.getNumberPresentation(),
                isIncoming);

        /// M: For VoLTE @{
        // If the call is VoLTE call with sip-uri number, we do not think it as sip call.
        if (entry.isSipCall && call != null) {
            entry.isSipCall = !call.isTelephonyCall();
        }
        /// @}

        return entry;
    }

    private class FindInfoCallback implements CallerInfoAsyncQuery.OnQueryCompleteListener {
        private final boolean mIsIncoming;

        public FindInfoCallback(boolean isIncoming) {
            mIsIncoming = isIncoming;
        }

        @Override
        public void onQueryComplete(int token, Object cookie, CallerInfo callerInfo) {
            /// M: ALPS01771032. If first query is cancelled, it should not call function. @{
            String callId = ((Call) cookie).getId();
            if (!mCallBackCancel.containsKey(callId) || !mCallBackCancel.get(callId)) {
                findInfoQueryComplete((Call) cookie, callerInfo, mIsIncoming, true);
            } else {
                mCallBackCancel.remove(((Call) cookie).getId());
            }
            /// @}
        }
    }

    /**
     * Requests contact data for the Call object passed in.
     * Returns the data through callback.  If callback is null, no response is made, however the
     * query is still performed and cached.
     *
     * @param callback The function to call back when the call is found. Can be null.
     */
    public void findInfo(final Call call, final boolean isIncoming,
            ContactInfoCacheCallback callback) {
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread());
        Preconditions.checkNotNull(callback);

        final String callId = call.getId();
        final ContactCacheEntry cacheEntry = mInfoMap.get(callId);
        Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);

        // If we have a previously obtained intermediate result return that now
        if (cacheEntry != null) {
            Log.d(TAG, "Contact lookup. In memory cache hit; lookup "
                    + (callBacks == null ? "complete" : "still running"));
            callback.onContactInfoComplete(callId, cacheEntry);
            // If no other callbacks are in flight, we're done.
            if (callBacks == null) {
                return;
            }
        }

        // If the entry already exists, add callback
        if (callBacks != null) {
            callBacks.add(callback);
            return;
        }
        Log.d(TAG, "Contact lookup. In memory cache miss; searching provider.");
        // New lookup
        /// M: for ALPS01769758 @{
        // Multi-Thread may cause ConcurrentModificationException for HashSet.
        // Original Code:
        // callBacks = Sets.newHashSet();
        callBacks = new CopyOnWriteArraySet<ContactInfoCacheCallback>();
        /// @}
        callBacks.add(callback);
        mCallBacks.put(callId, callBacks);
        mCallBackCancel.put(callId, false);

        /**
         * Performs a query for caller information.
         * Save any immediate data we get from the query. An asynchronous query may also be made
         * for any data that we do not already have. Some queries, such as those for voicemail and
         * emergency call information, will not perform an additional asynchronous query.
         */
        final CallerInfo callerInfo = CallerInfoUtils.getCallerInfoForCall(
                mContext, call, new FindInfoCallback(isIncoming));

        findInfoQueryComplete(call, callerInfo, isIncoming, false);
    }

    private void findInfoQueryComplete(Call call, CallerInfo callerInfo, boolean isIncoming,
            boolean didLocalLookup) {
        final String callId = call.getId();
        int presentationMode = call.getNumberPresentation();
        if (callerInfo.contactExists || callerInfo.isEmergencyNumber() ||
                callerInfo.isVoiceMailNumber()) {
            presentationMode = TelecomManager.PRESENTATION_ALLOWED;
        }

        ContactCacheEntry cacheEntry = mInfoMap.get(callId);
        // Ensure we always have a cacheEntry. Replace the existing entry if
        // it has no name or if we found a local contact.

		/// M: For VoLTE & SS @{
		// when phone number changed, we should force rebuild the ContactCacheEntry.
        // Original Code:        
		/*if (cacheEntry == null || TextUtils.isEmpty(cacheEntry.name) ||
                callerInfo.contactExists) {
		*/
		if (cacheEntry == null || TextUtils.isEmpty(cacheEntry.name) ||
                callerInfo.contactExists || isDifferentQueryResult(cacheEntry, callerInfo)) {
		/// @}
            cacheEntry = buildEntry(mContext, callId, callerInfo, presentationMode, isIncoming);
            mInfoMap.put(callId, cacheEntry);
        }

        sendInfoNotifications(callId, cacheEntry);

        if (didLocalLookup) {
            // Before issuing a request for more data from other services, we only check that the
            // contact wasn't found in the local DB.  We don't check the if the cache entry already
            // has a name because we allow overriding cnap data with data from other services.
            if (!callerInfo.contactExists && mPhoneNumberService != null) {
                Log.d(TAG, "Contact lookup. Local contacts miss, checking remote");
                final PhoneNumberServiceListener listener = new PhoneNumberServiceListener(callId);
                mPhoneNumberService.getPhoneNumberInfo(cacheEntry.number, listener, listener,
                        isIncoming);
            } else if (cacheEntry.displayPhotoUri != null) {
                Log.d(TAG, "Contact lookup. Local contact found, starting image load");
                // Load the image with a callback to update the image state.
                // When the load is finished, onImageLoadComplete() will be called.
                ContactsAsyncHelper.startObtainPhotoAsync(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE,
                        mContext, cacheEntry.displayPhotoUri, ContactInfoCache.this, callId);
            } else {
                if (callerInfo.contactExists) {
                    Log.d(TAG, "Contact lookup done. Local contact found, no image.");
                } else {
                    Log.d(TAG, "Contact lookup done. Local contact not found and"
                            + " no remote lookup service available.");
                }
                clearCallbacks(callId);
            }
        }
    }

    class PhoneNumberServiceListener implements PhoneNumberService.NumberLookupListener,
                                     PhoneNumberService.ImageLookupListener {
        private final String mCallId;

        PhoneNumberServiceListener(String callId) {
            mCallId = callId;
        }

        @Override
        public void onPhoneNumberInfoComplete(
                final PhoneNumberService.PhoneNumberInfo info) {
            // If we got a miss, this is the end of the lookup pipeline,
            // so clear the callbacks and return.
            if (info == null) {
                Log.d(TAG, "Contact lookup done. Remote contact not found.");
                clearCallbacks(mCallId);
                return;
            }

            ContactCacheEntry entry = new ContactCacheEntry();
            entry.name = info.getDisplayName();
            entry.number = info.getNumber();
            final int type = info.getPhoneType();
            final String label = info.getPhoneLabel();
            if (type == Phone.TYPE_CUSTOM) {
                entry.label = label;
            } else {
                final CharSequence typeStr = Phone.getTypeLabel(
                        mContext.getResources(), type, label);
                entry.label = typeStr == null ? null : typeStr.toString();
            }
            final ContactCacheEntry oldEntry = mInfoMap.get(mCallId);
            if (oldEntry != null) {
                // Location is only obtained from local lookup so persist
                // the value for remote lookups. Once we have a name this
                // field is no longer used; it is persisted here in case
                // the UI is ever changed to use it.
                entry.location = oldEntry.location;
            }

            // If no image and it's a business, switch to using the default business avatar.
            if (info.getImageUrl() == null && info.isBusiness()) {
                Log.d(TAG, "Business has no image. Using default.");
                entry.photo = mContext.getResources().getDrawable(R.drawable.img_business);
            }

            // Add the contact info to the cache.
            mInfoMap.put(mCallId, entry);
            sendInfoNotifications(mCallId, entry);

            // If there is no image then we should not expect another callback.
            if (info.getImageUrl() == null) {
                // We're done, so clear callbacks
                clearCallbacks(mCallId);
            }
        }

        @Override
        public void onImageFetchComplete(Bitmap bitmap) {
            onImageLoadComplete(TOKEN_UPDATE_PHOTO_FOR_CALL_STATE, null, bitmap, mCallId);
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    @Override
    public void onImageLoadComplete(int token, Drawable photo, Bitmap photoIcon, Object cookie) {
        Log.d(this, "Image load complete with context: ", mContext);
        // TODO: may be nice to update the image view again once the newer one
        // is available on contacts database.

        final String callId = (String) cookie;
        final ContactCacheEntry entry = mInfoMap.get(callId);

        if (entry == null) {
            Log.e(this, "Image Load received for empty search entry.");
            clearCallbacks(callId);
            return;
        }
        Log.d(this, "setting photo for entry: ", entry);

        // Conference call icons are being handled in CallCardPresenter.
        if (photo != null) {
            Log.v(this, "direct drawable: ", photo);
            entry.photo = photo;
        } else if (photoIcon != null) {
            Log.v(this, "photo icon: ", photoIcon);
            entry.photo = new BitmapDrawable(mContext.getResources(), photoIcon);
        } else {
            Log.v(this, "unknown photo");
            entry.photo = null;
        }

        sendImageNotifications(callId, entry);
        clearCallbacks(callId);
    }

    /**
     * Blows away the stored cache values.
     */
    public void clearCache() {
        mInfoMap.clear();
        mCallBacks.clear();
    }

    private ContactCacheEntry buildEntry(Context context, String callId,
            CallerInfo info, int presentation, boolean isIncoming) {
        // The actual strings we're going to display onscreen:
        Drawable photo = null;

        final ContactCacheEntry cce = new ContactCacheEntry();
        populateCacheEntry(context, info, cce, presentation, isIncoming);

        /// M: For VoLTE @{
        // If the call is VoLTE call with sip-uri number, we do not think it as sip call.
        // we can also do the judge in populateCacheEntry(),
        // but then we must scan the whole callList to get the call.
        // TODO: re-write populateCacheEntry(), and add an extra parameter (callId).
        if (cce.isSipCall) {
            Call call = CallList.getInstance().getCallById(callId);
            if (call != null) {
                cce.isSipCall = !call.isTelephonyCall();
            }
        }
        /// @}

        // This will only be true for emergency numbers
        if (info.photoResource != 0) {
            photo = context.getResources().getDrawable(info.photoResource);
        } else if (info.isCachedPhotoCurrent) {
            if (info.cachedPhoto != null) {
                photo = info.cachedPhoto;
            } else {
                photo = getDefaultContactPhotoDrawable();
            }
        } else if (info.contactDisplayPhotoUri == null) {
            photo = getDefaultContactPhotoDrawable();
        } else {
            cce.displayPhotoUri = info.contactDisplayPhotoUri;
        }

        if (info.lookupKeyOrNull == null || info.contactIdOrZero == 0) {
            Log.v(TAG, "lookup key is null or contact ID is 0. Don't create a lookup uri.");
            cce.lookupUri = null;
        } else {
            cce.lookupUri = Contacts.getLookupUri(info.contactIdOrZero, info.lookupKeyOrNull);
        }

        cce.photo = photo;
        cce.lookupKey = info.lookupKeyOrNull;

        return cce;
    }

    /**
     * Populate a cache entry from a call (which got converted into a caller info).
     */
    public static void populateCacheEntry(Context context, CallerInfo info, ContactCacheEntry cce,
            int presentation, boolean isIncoming) {
        Preconditions.checkNotNull(info);
        String displayName = null;
        String displayNumber = null;
        String displayLocation = null;
        String label = null;
        boolean isSipCall = false;

            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            // Currently, infi.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix. That
            // prefix isn't really useful to the user, though, so strip it off
            // if present. (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;

            if (!TextUtils.isEmpty(number)) {
                isSipCall = PhoneNumberHelper.isUriNumber(number);
                if (number.startsWith("sip:")) {
                    number = number.substring(4);
                }
            }

            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name" slot
                // onscreen, and possibly display a descriptive string in the
                // "number" slot.)
                if (TextUtils.isEmpty(number)) {
                    // No name *or* number! Display a generic "unknown" string
                    // (or potentially some other default based on the presentation.)
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> no name *or* number! displayName = " + displayName);
                } else if (presentation != TelecomManager.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> presentation not allowed! displayName = " + displayName);
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    // No name, but we do have a valid CNAP name, so use that.
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    displayNumber = number;
                    Log.d(TAG, "  ==> cnapName available: displayName '" + displayName +
                            "', displayNumber '" + displayNumber + "'");
                } else {
                    // No name; all we have is a number. This is the typical
                    // case when an incoming call doesn't match any contact,
                    // or if you manually dial an outgoing number using the
                    // dialpad.
                    displayNumber = number;

                    /// M: for ALPS01953843 @{
                    // For the number not stored in contact, we should still show
                    // the geo info even when make outgoing call.
                    // Original Code:
                    // Display a geographical description string if available
                    // (but only for incoming calls.)
                    // if (isIncoming) {
                    if (!TextUtils.isEmpty(info.geoDescription)) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the CallerInfo
                        // query to only do the geoDescription lookup in the first
                        // place for incoming calls.
                        displayLocation = info.geoDescription; // may be null
                        Log.d(TAG, "Geodescrption: " + info.geoDescription);
                    }
                    /// @}

                    Log.d(TAG, "  ==>  no name; falling back to number:"
                            + " displayNumber '" + Log.pii(displayNumber)
                            + "', displayLocation '" + displayLocation + "'");
                }
            } else {
                // We do have a valid "name" in the CallerInfo. Display that
                // in the "name" slot, and the phone number in the "number" slot.
                if (presentation != TelecomManager.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(context, presentation);
                    Log.d(TAG, "  ==> valid name, but presentation not allowed!" +
                            " displayName = " + displayName);
                } else {
                    displayName = info.name;
                    displayNumber = number;
                    label = info.phoneLabel;
                    Log.d(TAG, "  ==>  name is present in CallerInfo: displayName '" + displayName
                            + "', displayNumber '" + displayNumber + "'");
                }
            }
			//addby zhangjinqiang for al812 phone location start
			 if (!TextUtils.isEmpty(info.geoDescription)) {
                        // TODO (CallerInfoAsyncQuery cleanup): Fix the CallerInfo
                        // query to only do the geoDescription lookup in the first
                        // place for incoming calls.
                        displayLocation = info.geoDescription; // may be null
                        Log.d(TAG, "Geodescrption: " + info.geoDescription);
                    }
			//end

        cce.name = displayName;
        cce.number = displayNumber;
        cce.location = displayLocation;
        cce.label = label;
        cce.isSipCall = isSipCall;
    }

    /**
     * Sends the updated information to call the callbacks for the entry.
     */
    private void sendInfoNotifications(String callId, ContactCacheEntry entry) {
        final Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);
        if (callBacks != null) {
            for (ContactInfoCacheCallback callBack : callBacks) {
                callBack.onContactInfoComplete(callId, entry);
            }
        }
    }

    private void sendImageNotifications(String callId, ContactCacheEntry entry) {
        final Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);
        if (callBacks != null && entry.photo != null) {
            for (ContactInfoCacheCallback callBack : callBacks) {
                callBack.onImageLoadComplete(callId, entry);
            }
        }
    }

    private void clearCallbacks(String callId) {
        mCallBacks.remove(callId);
        mCallBackCancel.remove(callId);
    }

    /**
     * Gets name strings based on some special presentation modes.
     */
    private static String getPresentationString(Context context, int presentation) {
        String name = context.getString(R.string.unknown);
        if (presentation == TelecomManager.PRESENTATION_RESTRICTED) {
            name = context.getString(R.string.private_num);
        } else if (presentation == TelecomManager.PRESENTATION_PAYPHONE) {
            name = context.getString(R.string.payphone);
        }
        return name;
    }

    public Drawable getDefaultContactPhotoDrawable() {
        if (mDefaultContactPhotoDrawable == null) {
            mDefaultContactPhotoDrawable =
                    mContext.getResources().getDrawable(R.drawable.img_no_image_automirrored);
        }
        return mDefaultContactPhotoDrawable;
    }

    public Drawable getConferenceDrawable() {
        if (mConferencePhotoDrawable == null) {
            mConferencePhotoDrawable =
                    mContext.getResources().getDrawable(R.drawable.img_conference_automirrored);
        }
        return mConferencePhotoDrawable;
    }

    /**
     * Callback interface for the contact query.
     */
    public interface ContactInfoCacheCallback {
        public void onContactInfoComplete(String callId, ContactCacheEntry entry);
        public void onImageLoadComplete(String callId, ContactCacheEntry entry);
    }

    public static class ContactCacheEntry {
        public String name;
        public String number;
        public String location;
        public String label;
        public Drawable photo;
        public boolean isSipCall;
        /** This will be used for the "view" notification. */
        public Uri contactUri;
        /** Either a display photo or a thumbnail URI. */
        public Uri displayPhotoUri;
        public Uri lookupUri; // Sent to NotificationMananger
        public String lookupKey;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", MoreStrings.toSafeString(name))
                    .add("number", MoreStrings.toSafeString(number))
                    .add("location", MoreStrings.toSafeString(location))
                    .add("label", label)
                    .add("photo", photo)
                    .add("isSipCall", isSipCall)
                    .add("contactUri", contactUri)
                    .add("displayPhotoUri", displayPhotoUri)
                    .toString();
        }
    }

    /// M: For volte @{
    /**
     * listener, which will get notified onContactInfoUpdated() when re-query of certain call complete.
     *
     */
    public static abstract class ContactInfoUpdatedListener {
        public void onContactInfoUpdated(String callId) {}
    }

    /**
     * restore all ContactInfoUpdatedListener, who will get notified later.
     */
    private final List<ContactInfoUpdatedListener> mContactInfoUpdatedListener = new ArrayList<ContactInfoUpdatedListener>();

    /**
     * add ContactInfoUpdatedListener.
     * @param listener
     */
    public void addContactInfoUpdatedListener(ContactInfoUpdatedListener listener) {
        if (!mContactInfoUpdatedListener.contains(listener)) {
            mContactInfoUpdatedListener.add(listener);
        }
    }

    /**
     * remove ContactInfoUpdatedListener.
     * @param listener
     */
    public void removeContactInfoUpdatedListener(ContactInfoUpdatedListener listener) {
        if (mContactInfoUpdatedListener.contains(listener)) {
            mContactInfoUpdatedListener.remove(listener);
        }
    }

    /**
     * listen onVolteMarkedEccChanged() and onPhoneNumberChanged() from CallDetailsChangeHandler.
     */
    private final CallDetailChangeListener mCallDetailChangeListener = new CallDetailChangeListener() {

        public void onVolteMarkedEccChanged(Call call) {
            handleIsEmergencyChanged(call);
        }

        public void onPhoneNumberChanged(Call call) {
            handlePhoneNumberChanged(call);
        }
    };

    /**
     * When certain call is marked as Ecc by NW, notify listners.
     * @param call
     */
    private void handleIsEmergencyChanged(Call call) {
        Log.d(TAG, "handleIsEmergencyChanged()... call = " + call);
        // check whether it is a ecc call again
        if (call != null && call.isVolteMarkedEcc()) {
            final String callId = call.getId();
            final ContactCacheEntry cacheEntry = mInfoMap.get(callId);
            Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);

            if (cacheEntry != null && callBacks != null) {
                // query is still running, remove callbacks
                clearCallbacks(callId);
            }
            CallerInfo callerInfo = new CallerInfo().markAsEmergency(mContext);
            findInfoQueryComplete(call, callerInfo, call.getState() == Call.State.INCOMING, false);
            // make EntryCache complete into mInCfoMap, notify CallCardPresenter to get it again.
            for (ContactInfoUpdatedListener listener : mContactInfoUpdatedListener) {
                listener.onContactInfoUpdated(callId);
            }
        }
    }

    /**
     * when number of certain call changed, re-query for it.
     * when re-query complete, will notify all listeners to trigger them to get new ContactInfo from here.
     * @param call
     */
    private void handlePhoneNumberChanged(Call call) {
        Log.d(TAG, "handlePhoneNumberChanged()... call = " + call);
        if (call != null && !call.isVolteMarkedEcc()) {
            // if is ecc, will show ecc info, so no need re-query.(consider network mark it as ecc while pau changed).
            updateInfo(call, call.getState() == Call.State.INCOMING, new ContactInfoCacheCallback() {
                @Override
                public void onContactInfoComplete(String callId, ContactCacheEntry entry) {
                    // re-query complete, notify users to re-get new ContactCacheEntry.
                    for (ContactInfoUpdatedListener listener : mContactInfoUpdatedListener) {
                        listener.onContactInfoUpdated(callId);
                    }
                }
                @Override
                public void onImageLoadComplete(String callId, ContactCacheEntry entry) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    /**
     * This function trigger re-query. If satisfied, will always force re-query,
     * even if there has a running query. So need avoiding call this function continue.
     * @param call
     * @param isIncoming
     * @param callback
     */
    private void updateInfo(final Call call, final boolean isIncoming,
            ContactInfoCacheCallback callback) {
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread());
        Preconditions.checkNotNull(callback);

        final String callId = call.getId();
        final ContactCacheEntry cacheEntry = mInfoMap.get(callId);
        Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);

        if (cacheEntry == null) {
            // never do the query before, no need to re-query
            callback.onContactInfoComplete(callId, null);
            return;
        }

        if (cacheEntry != null) {
            // the query has done or still running.
            if (callBacks != null) {
                // still running, clear callbacks
                clearCallbacks(callId);
                mCallBackCancel.put(callId, true);
            }
            // force re-query
            callBacks = new CopyOnWriteArraySet<ContactInfoCache.ContactInfoCacheCallback>();
            callBacks.add(callback);
            mCallBacks.put(callId, callBacks);
            final CallerInfo callerInfo = CallerInfoUtils.getCallerInfoForCall(
                    mContext, call, new FindInfoCallback(isIncoming));
        }
    }

    /**
     * The main logic is same as findInfo().
     * You maybe consider that there may exist conflict like ALPS01771032.
     * because findInfo() equals to findInfoEx(). so may exist below conflicts:
     * 1/ findInfo() + updateInfo()     => resolved by ALPS01771032.
     * 2/ updateInfo() + findInfo()     => updateInfo() will skip this query request.
     * 3/ findInfo() + findInfo()       => findInfo() will handle this case.
     * @param call
     * @param isIncoming
     * @param callback
     * @param returnTempleResult
     */
    public void findInfoEx(final Call call, final boolean isIncoming,
            ContactInfoCacheCallback callback, boolean returnTempleResult) {
        Log.d(TAG, "findInfoEx()...");
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread());
        Preconditions.checkNotNull(callback);

        final String callId = call.getId();
        final ContactCacheEntry cacheEntry = mInfoMap.get(callId);
        Set<ContactInfoCacheCallback> callBacks = mCallBacks.get(callId);

        // If we have a previously obtained intermediate result return that now
        if (cacheEntry != null) {
            Log.d(TAG, "findInfoEx()... Contact lookup. In memory cache hit; lookup "
                    + (callBacks == null ? "complete" : "still running"));
            callback.onContactInfoComplete(callId, cacheEntry);
            // If no other callbacks are in flight, we're done.
            if (callBacks == null) {
                return;
            }
        }

        // If the entry already exists, add callback
        if (callBacks != null) {
            callBacks.add(callback);
            return;
        }
        Log.d(TAG, "findInfoEx()... Contact lookup. In memory cache miss; searching provider.");
        // New lookup
        callBacks = new CopyOnWriteArraySet<ContactInfoCache.ContactInfoCacheCallback>();
        callBacks.add(callback);
        mCallBacks.put(callId, callBacks);

        /**
         * Performs a query for caller information.
         * Save any immediate data we get from the query. An asynchronous query may also be made
         * for any data that we do not already have. Some queries, such as those for voicemail and
         * emergency call information, will not perform an additional asynchronous query.
         */
        final CallerInfo callerInfo = CallerInfoUtils.getCallerInfoForCall(
                mContext, call, new FindInfoCallback(isIncoming));

        if (returnTempleResult) {
            findInfoQueryComplete(call, callerInfo, isIncoming, false);
        }
    }

    /**
     * Judge whether the phoneNumber is updated, if so, we should force update ContactCacheEntry.
     * That means phoneNumber is different in ContactCacheEntry and CallerInfo
     * @param entry
     * @param callerInfo
     * @return
     */
    private boolean isDifferentQueryResult(ContactCacheEntry entry, CallerInfo callerInfo) {
        boolean isDifferent = false;
        if (entry != null && callerInfo != null) {
            String numberInEntry = entry.number;
            String numberInCallerInfo = callerInfo.phoneNumber;
            if (!TextUtils.equals(numberInEntry, numberInCallerInfo)) {
                isDifferent = true;
            }
        }
        return isDifferent;
    }
    /// @}

    /*
     * M: ALPS01771032. If two queries are so close,
     * the second ContactInfoCacheCallback would be removed,
     * so that the second query result could not update to UI.
     * Add this variable to record the first query FindInfoCallback
     * can be cancelled when second query is coming.
     */
    private HashMap<String, Boolean> mCallBackCancel = new HashMap<String, Boolean>();
}
