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

package android.security;

import com.android.org.conscrypt.NativeCrypto;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.Locale;

/**
 * @hide This should not be made public in its present form because it
 * assumes that private and secret key bytes are available and would
 * preclude the use of hardware crypto.
 */
public class KeyStore {
    private static final String TAG = "KeyStore";

    // ResponseCodes
    public static final int NO_ERROR = 1;
    public static final int LOCKED = 2;
    public static final int UNINITIALIZED = 3;
    public static final int SYSTEM_ERROR = 4;
    public static final int PROTOCOL_ERROR = 5;
    public static final int PERMISSION_DENIED = 6;
    public static final int KEY_NOT_FOUND = 7;
    public static final int VALUE_CORRUPTED = 8;
    public static final int UNDEFINED_ACTION = 9;
    public static final int WRONG_PASSWORD = 10;

    /// M: Event for reset credentials
    public static final String ACTION_KEYSTORE_RESET =
            "com.mediatek.android.keystore.action.KEYSTORE_RESET";
    private Context mContext;

    // Used for UID field to indicate the calling UID.
    public static final int UID_SELF = -1;

    // Flags for "put" "import" and "generate"
    public static final int FLAG_NONE = 0;
    public static final int FLAG_ENCRYPTED = 1;

    // States
    public enum State { UNLOCKED, LOCKED, UNINITIALIZED };

    private int mError = NO_ERROR;

    private final IKeystoreService mBinder;

    private KeyStore(IKeystoreService binder) {
        mBinder = binder;
        /// M: @{ Retrive the caller's context in order to notify reset event.
         // However, this works only on UI thread from caller process.
        ActivityThread caller = ActivityThread.currentActivityThread();
        mContext = (caller != null) ? caller.getApplication() : null;
        /// @}
    }

    public static KeyStore getInstance() {
        IKeystoreService keystore = IKeystoreService.Stub.asInterface(ServiceManager
                .getService("android.security.keystore"));
        return new KeyStore(keystore);
    }

    static int getKeyTypeForAlgorithm(String keyType) throws IllegalArgumentException {
        if ("RSA".equalsIgnoreCase(keyType)) {
            return NativeCrypto.EVP_PKEY_RSA;
        } else if ("DSA".equalsIgnoreCase(keyType)) {
            return NativeCrypto.EVP_PKEY_DSA;
        } else if ("EC".equalsIgnoreCase(keyType)) {
            return NativeCrypto.EVP_PKEY_EC;
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + keyType);
        }
    }

    public State state() {
        final int ret;
        try {
            ret = mBinder.test();
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            throw new AssertionError(e);
        }

        switch (ret) {
            case NO_ERROR: return State.UNLOCKED;
            case LOCKED: return State.LOCKED;
            case UNINITIALIZED: return State.UNINITIALIZED;
            default: throw new AssertionError(mError);
        }
    }

    public boolean isUnlocked() {
        return state() == State.UNLOCKED;
    }

    public byte[] get(String key) {
	if(mBinder == null)
	    return null;

        try {
            return mBinder.get(key);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean put(String key, byte[] value, int uid, int flags) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.insert(key, value, uid, flags) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delete(String key, int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.del(key, uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delete(String key) {
        return delete(key, UID_SELF);
    }

    public boolean contains(String key, int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.exist(key, uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean contains(String key) {
        return contains(key, UID_SELF);
    }

    public String[] saw(String prefix, int uid) {
	if(mBinder == null)
	    return null;

        try {
            return mBinder.saw(prefix, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public String[] saw(String prefix) {
        return saw(prefix, UID_SELF);
    }

    public boolean reset() {
	if(mBinder == null)
	    return false;

        boolean result = false;
        try {
            result = (mBinder.reset() == NO_ERROR);
            /// M: Event for reset credentials
            if (result) {
                if (mContext != null) {
                    Intent intent = new Intent(ACTION_KEYSTORE_RESET);
                    mContext.sendBroadcast(intent);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
        return result;
    }

    public boolean password(String password) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.password(password) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean lock() {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.lock() == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean unlock(String password) {
	if(mBinder == null)
	    return false;

        try {
            mError = mBinder.unlock(password);
            return mError == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean isEmpty() {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.zero() == KEY_NOT_FOUND;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean generate(String key, int uid, int keyType, int keySize, int flags,
            byte[][] args) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.generate(key, uid, keyType, keySize, flags, args) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean importKey(String keyName, byte[] key, int uid, int flags) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.import_key(keyName, key, uid, flags) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public byte[] getPubkey(String key) {
	if(mBinder == null)
	    return null;

        try {
            return mBinder.get_pubkey(key);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean delKey(String key, int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.del_key(key, uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delKey(String key) {
        return delKey(key, UID_SELF);
    }

    public byte[] sign(String key, byte[] data) {
	if(mBinder == null)
	    return null;

        try {
            return mBinder.sign(key, data);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean verify(String key, byte[] data, byte[] signature) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.verify(key, data, signature) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean grant(String key, int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.grant(key, uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean ungrant(String key, int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.ungrant(key, uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    /**
     * Returns the last modification time of the key in milliseconds since the
     * epoch. Will return -1L if the key could not be found or other error.
     */
    public long getmtime(String key) {
	if(mBinder == null)
	    return -1L;

        try {
            final long millis = mBinder.getmtime(key);
            if (millis == -1L) {
                return -1L;
            }

            return millis * 1000L;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return -1L;
        }
    }

    public boolean duplicate(String srcKey, int srcUid, String destKey, int destUid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.duplicate(srcKey, srcUid, destKey, destUid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    // TODO remove this when it's removed from Settings
    public boolean isHardwareBacked() {
        return isHardwareBacked("RSA");
    }

    public boolean isHardwareBacked(String keyType) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.is_hardware_backed(keyType.toUpperCase(Locale.US)) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean clearUid(int uid) {
	if(mBinder == null)
	    return false;

        try {
            return mBinder.clear_uid(uid) == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean resetUid(int uid) {
	if(mBinder == null)
	    return false;

        try {
            mError = mBinder.reset_uid(uid);
            return mError == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean syncUid(int sourceUid, int targetUid) {
	if(mBinder == null)
	    return false;

        try {
            mError = mBinder.sync_uid(sourceUid, targetUid);
            return mError == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean passwordUid(String password, int uid) {
	if(mBinder == null)
	    return false;

        try {
            mError = mBinder.password_uid(password, uid);
            return mError == NO_ERROR;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public int getLastError() {
        return mError;
    }
}
