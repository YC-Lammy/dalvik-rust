/**
 * Copyright (c) 2010, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Interface to the clipboard service, for placing and retrieving text in
 * the global clipboard.
 *
 * <p>
 * The ClipboardManager API itself is very simple: it consists of methods
 * to atomically get and set the current primary clipboard data.  That data
 * is expressed as a {@link ClipData} object, which defines the protocol
 * for data exchange between applications.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about using the clipboard framework, read the
 * <a href="{@docRoot}guide/topics/clipboard/copy-paste.html">Copy and Paste</a>
 * developer guide.</p>
 * </div>
 */
public class ClipboardManager extends android.text.ClipboardManager {

    public static final String DEVICE_CONFIG_SHOW_ACCESS_NOTIFICATIONS = "show_access_notifications";

    public static final boolean DEVICE_CONFIG_DEFAULT_SHOW_ACCESS_NOTIFICATIONS = true;

    private final Context mContext;

    private final ArrayList<OnPrimaryClipChangedListener> mPrimaryClipChangedListeners
             = new ArrayList<OnPrimaryClipChangedListener>();


    public interface OnPrimaryClipChangedListener {
        void onPrimaryClipChanged();
    }

    static native void nSetPrimaryClip(Parcel clip);
    static native void nGetPrimaryClip(Parcel clip);
    static native void nClearPrimaryClip();
    static native void addPrimaryClipChangedListener();
    //todo: static native String nGetPrimaryClipSource();


    /** {@hide} */
    public ClipboardManager(Context context) throws ServiceNotFoundException {
        mContext = context;
    }

    public void setPrimaryClip(@NonNull ClipData clip) {
        Objects.requireNonNull(clip);
        clip.prepareToLeaveProcess(true);
        Parcel data = Parcel.obtain();
        clip.writeToParcel(data, 0);
        nSetPrimaryClip(data);
    }

    public void clearPrimaryClip() {
        //mService.clearPrimaryClip(mContext.getOpPackageName(), mContext.getUserId());
    }

    
    public @Nullable ClipData getPrimaryClip() {
        Parcel reply = Parcel.obtain();
        nGetPrimaryClip(reply);
        return ClipData.CREATOR.createFromParcel(reply);
    }

    public @Nullable ClipDescription getPrimaryClipDescription() {
        try {
            return null;
            //return mService.getPrimaryClipDescription(mContext.getOpPackageName(), mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasPrimaryClip() {
        return false;
        //return mService.hasPrimaryClip(mContext.getOpPackageName(), mContext.getUserId());
    }

    public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        synchronized (mPrimaryClipChangedListeners) {
            mPrimaryClipChangedListeners.add(what);
            if (mPrimaryClipChangedListeners.isEmpty()){
                addPrimaryClipChangedListener();
            }
        }
    }

    public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        synchronized (mPrimaryClipChangedListeners) {
            mPrimaryClipChangedListeners.remove(what);
        }
    }

    /**
     * @deprecated Use {@link #getPrimaryClip()} instead.  This retrieves
     * the primary clip and tries to coerce it to a string.
     */
    @Deprecated
    public CharSequence getText() {
        ClipData clip = getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            return clip.getItemAt(0).coerceToText(mContext);
        }
        return null;
    }

    /**
     * @deprecated Use {@link #setPrimaryClip(ClipData)} instead.  This
     * creates a ClippedItem holding the given text and sets it as the
     * primary clip.  It has no label or icon.
     */
    @Deprecated
    public void setText(CharSequence text) {
        setPrimaryClip(ClipData.newPlainText(null, text));
    }

    /**
     * @deprecated Use {@link #hasPrimaryClip()} instead.
     */
    @Deprecated
    public boolean hasText() {
        return false;
    }

    /**
     * Returns the package name of the source of the current primary clip, or null if there is no
     * primary clip or if a source is not available.
     *
     * @hide
     */
    public String getPrimaryClipSource() {
        try {
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    // the native entry for reporting change
    void reportPrimaryClipChanged() {
        Object[] listeners;

        synchronized (mPrimaryClipChangedListeners) {
            final int N = mPrimaryClipChangedListeners.size();
            if (N <= 0) {
                return;
            }
            listeners = mPrimaryClipChangedListeners.toArray();
        }

        for (int i=0; i<listeners.length; i++) {
            ((OnPrimaryClipChangedListener)listeners[i]).onPrimaryClipChanged();
        }
    }
}
