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

package android.content;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;


import dalvik.system.CloseGuard;

import libcore.io.IoUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;




public class ContentProviderClient implements ContentInterface, AutoCloseable {
    private static final String TAG = "ContentProviderClient";

    private static Handler sAnrHandler;

    private final ContentResolver mContentResolver;

    private final String mPackageName;
    private final AttributionSource mAttributionSource;

    private final String mAuthority;
    private final boolean mStable;

    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();

    private long mAnrTimeout;
    private NotRespondingRunnable mAnrRunnable;

    static native void nApplyBatch(Parcel data, Parcel reply);
    static native int nBulkInsert(Parcel data);
    static native void nCall(Parcel data, Parcel reply);
    static native void nCanonicalize(Parcel data, Parcel reply);
    static native void nUncanonicalize(Parcel data, Parcel reply);
    static native int nDelete(Parcel data);
    static native ContentProvider nGetLocalContentProvider(String authority);
    static native void nGetStreamTypes(String authority, Parcel data, Parcel reply);
    static native String nGetType(String authority, String uri);
    static native void nInsert(String authority, Parcel data, Parcel reply);
    static native void nOpenFile(String authority, Parcel data, Parcel reply);
    static native void nOpenAssetFile(String authority, Parcel data, Parcel reply);
    static native void nOpenTypedAssetFile(String authority, Parcel data, Parcel reply);
    static native void nQuery(String authority, Parcel data, Parcel reply);
    static native boolean nRefresh(String authority, Parcel data);
    static native int nUpdate(String authority, Parcel data);
    static native int nCheckUriPermission(String authority, Parcel data);

    /** {@hide} */
    public ContentProviderClient(ContentResolver contentResolver,
            boolean stable) {
        // Only used for testing, so use a fake authority
        this(contentResolver, "unknown", stable);
    }

    /** {@hide} */
    public ContentProviderClient(ContentResolver contentResolver, 
            String authority, boolean stable) {
        mContentResolver = contentResolver;
        mPackageName = contentResolver.mPackageName;
        mAttributionSource = contentResolver.getAttributionSource();

        mAuthority = authority;
        mStable = stable;

        mCloseGuard.open("close");
    }


    public @NonNull ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws RemoteException, OperationApplicationException {
        return applyBatch(mAuthority, operations);
    }

    @Override
    public ContentProviderResult[] applyBatch(@NonNull String authority,
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws RemoteException, OperationApplicationException {
        Objects.requireNonNull(operations, "operations");

        beforeRemote();
        Parcel src = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        mAttributionSource.writeToParcel(src, 0);
        src.writeString(authority);
        src.writeInt(operations.size());
        for (ContentProviderOperation op:operations){
            op.writeToParcel(src, 0);
        }

        nApplyBatch(src, reply);
        afterRemote();

        boolean success = reply.readBoolean();
        if (!success){
            src.recycle();
            reply.recycle();
            throw new OperationApplicationException(reply.readString());
        }
        int length = reply.readInt();
        ContentProviderResult[] res = new ContentProviderResult[length];
        for (int i=0;i<length;i++){
            res[i] = ContentProviderResult.CREATOR.createFromParcel(reply);
        }

        src.recycle();
        reply.recycle();

        return res;
    }


    @Override
    public int bulkInsert(@NonNull Uri url, @NonNull ContentValues[] initialValues)
            throws RemoteException {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(initialValues, "initialValues");

        beforeRemote();
        Parcel data = Parcel.obtain();

        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        data.writeInt(initialValues.length);
        for (ContentValues v:initialValues){
            v.writeToParcel(data, 0);
        }

        int re = nBulkInsert(data);
        data.recycle();
        afterRemote();
        return re;
    }


    public @Nullable Bundle call(@NonNull String method, @Nullable String arg,
        @Nullable Bundle extras) throws RemoteException {
        return call(mAuthority, method, arg, extras);
    }

    @Override
    public @Nullable Bundle call(@NonNull String authority, @NonNull String method,
            @Nullable String arg, @Nullable Bundle extras) throws RemoteException {
        Objects.requireNonNull(authority, "authority");
        Objects.requireNonNull(method, "method");

        beforeRemote();
        Parcel args = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(args, 0);
        args.writeString(authority);
        args.writeString(method);
        args.writeString(arg);
        args.writeBundle(extras);

        nCall(args, reply);
        
        afterRemote();
        Bundle b = reply.readBundle();
        args.recycle();
        reply.recycle();
        return b;
    }

    @Override
    public final @Nullable Uri canonicalize(@NonNull Uri url) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        nCanonicalize(data, reply);
        afterRemote();
        Uri uri = Uri.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return uri;
    }

    @Override
    public final @Nullable Uri uncanonicalize(@NonNull Uri url) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        nUncanonicalize(data, reply);
        afterRemote();
        Uri uri = Uri.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return uri;
    }

    @Deprecated
    public boolean release() {
        return closeInternal();
    }

    @Override
    public void close() {
        closeInternal();
    }

    private boolean closeInternal() {
        mCloseGuard.close();
        if (mClosed.compareAndSet(false, true)) {
            // We can't do ANR checks after we cease to exist! Reset any
            // blocking behavior changes we might have made.
            setDetectNotResponding(0);

            if (mStable) {
                //return mContentResolver.releaseProvider(mContentProvider);
                
            } else {
                //return mContentResolver.releaseUnstableProvider(mContentProvider);
            }
            return true;
        } else {
            return false;
        }
    }


    public int delete(@NonNull Uri url, @Nullable String selection,
            @Nullable String[] selectionArgs) throws RemoteException {
        return delete(url, ContentResolver.createSqlQueryBundle(selection, selectionArgs));
    }

    @Override
    public int delete(@NonNull Uri url, @Nullable Bundle extras) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        data.writeBundle(extras);
        int re = nDelete(data);
        afterRemote();
        data.recycle();
        return re;
    }

    public @Nullable ContentProvider getLocalContentProvider() {
        return nGetLocalContentProvider(mAuthority);
    }


    @Override
    public @Nullable String[] getStreamTypes(@NonNull Uri url, @NonNull String mimeTypeFilter)
            throws RemoteException {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(mimeTypeFilter, "mimeTypeFilter");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        url.writeToParcel(data, 0);
        data.writeString(mimeTypeFilter);
        nGetStreamTypes(mAuthority, data, reply);
        int length = reply.readInt();
        String[] re = new String[length];
        for (int i=0;i<length;i++){
            re[i] = reply.readString();
        }
        afterRemote();
        data.recycle();
        reply.recycle();
        return re;
    }

    @Override
    public @Nullable String getType(@NonNull Uri url) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        String re = nGetType(mAuthority, url.toString());
        afterRemote();
        return re;
    }

    public @Nullable Uri insert(@NonNull Uri url, @Nullable ContentValues initialValues)
            throws RemoteException {
        return insert(url, initialValues, null);
    }

    @Override
    public @Nullable Uri insert(@NonNull Uri url, @Nullable ContentValues initialValues,
            @Nullable Bundle extras) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        initialValues.writeToParcel(data, 0);
        data.writeBundle(extras);
        nInsert(mAuthority, data, reply);
        afterRemote();
        Uri r = Uri.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return r;
    }

    public @Nullable ParcelFileDescriptor openFile(@NonNull Uri url, @NonNull String mode)
            throws RemoteException, FileNotFoundException {
        return openFile(url, mode, null);
    }

    @Override
    public @Nullable ParcelFileDescriptor openFile(@NonNull Uri url, @NonNull String mode,
            @Nullable CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(mode, "mode");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        data.writeString(mode);
        nOpenFile(mAuthority, data, reply);
        afterRemote();
        boolean success = reply.readBoolean();
        if (!success){
            data.recycle();
            reply.recycle();
            throw new FileNotFoundException(reply.readString());
        }
        ParcelFileDescriptor p = ParcelFileDescriptor.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return p;
    }

    public @Nullable AssetFileDescriptor openAssetFile(@NonNull Uri url, @NonNull String mode)
            throws RemoteException, FileNotFoundException {
        return openAssetFile(url, mode, null);
    }

    @Override
    public @Nullable AssetFileDescriptor openAssetFile(@NonNull Uri url, @NonNull String mode,
            @Nullable CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(mode, "mode");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data, 0);
        data.writeString(mode);
        nOpenAssetFile(mAuthority, data, reply);
        afterRemote();
        boolean success = reply.readBoolean();
        if (!success){
            data.recycle();
            reply.recycle();
            throw new FileNotFoundException(reply.readString());
        }
        AssetFileDescriptor p = AssetFileDescriptor.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return p;
    }

    /** See {@link ContentProvider#openTypedAssetFile ContentProvider.openTypedAssetFile} */
    public final @Nullable AssetFileDescriptor openTypedAssetFileDescriptor(@NonNull Uri uri,
            @NonNull String mimeType, @Nullable Bundle opts)
                    throws RemoteException, FileNotFoundException {
        return openTypedAssetFileDescriptor(uri, mimeType, opts, null);
    }

    /** See {@link ContentProvider#openTypedAssetFile ContentProvider.openTypedAssetFile} */
    public final @Nullable AssetFileDescriptor openTypedAssetFileDescriptor(@NonNull Uri uri,
            @NonNull String mimeType, @Nullable Bundle opts, @Nullable CancellationSignal signal)
                    throws RemoteException, FileNotFoundException {
        return openTypedAssetFile(uri, mimeType, opts, signal);
    }

    @Override
    public final @Nullable AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
            @NonNull String mimeTypeFilter, @Nullable Bundle opts,
            @Nullable CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(mimeTypeFilter, "mimeTypeFilter");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        uri.writeToParcel(data,0);
        data.writeBundle(opts);

        afterRemote();
        boolean success = reply.readBoolean();
        if (!success){
            data.recycle();
            reply.recycle();
            throw new FileNotFoundException(reply.readString());
        }
        AssetFileDescriptor a = AssetFileDescriptor.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return a;
    }



    public @Nullable Cursor query(@NonNull Uri url, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) throws RemoteException {
        return query(url, projection, selection,  selectionArgs, sortOrder, null);
    }

    public @Nullable Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal)
                    throws RemoteException {
        Bundle queryArgs =
                ContentResolver.createSqlQueryBundle(selection, selectionArgs, sortOrder);
        return query(uri, projection, queryArgs, cancellationSignal);
    }

    @Override
    public @Nullable Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            Bundle queryArgs, @Nullable CancellationSignal cancellationSignal)
                    throws RemoteException {
        Objects.requireNonNull(uri, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        uri.writeToParcel(data,0);
        data.writeInt(projection.length);
        for (String v:projection){
            data.writeString(v);
        }
        data.writeBundle(queryArgs);
        nQuery(mAuthority, data, reply);
        afterRemote();
        Cursor a = Cursor.CREATOR.createFromParcel(reply);
        data.recycle();
        reply.recycle();
        return a;
    }


    @Override
    public boolean refresh(Uri url, @Nullable Bundle extras,
            @Nullable CancellationSignal cancellationSignal) throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data,0);
        data.writeBundle(extras);
        boolean re = nRefresh(mAuthority, data);
        afterRemote();
        data.recycle();
        reply.recycle();
        return re;
    }

    public int update(@NonNull Uri url, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) throws RemoteException {
        return update(url, values, ContentResolver.createSqlQueryBundle(selection, selectionArgs));
    }

    @Override
    public int update(@NonNull Uri url, @Nullable ContentValues values, @Nullable Bundle extras)
            throws RemoteException {
        Objects.requireNonNull(url, "url");

        beforeRemote();
        Parcel data = Parcel.obtain();
        mAttributionSource.writeToParcel(data, 0);
        url.writeToParcel(data,0);
        values.writeToParcel(data, 0);
        data.writeBundle(extras);
        int re = nUpdate(mAuthority, data);
        afterRemote();
        data.recycle();
        return re;
    }

    @Override
    public int checkUriPermission(Uri uri, int uid, int modeFlags) throws RemoteException {
        beforeRemote();
        Parcel data = Parcel.obtain();
        uri.writeToParcel(data, 0);
        data.writeInt(uid);
        data.writeInt(modeFlags);
        int re = nCheckUriPermission(mAuthority, data);
        afterRemote();
        data.recycle();
        return re;
    }


    /**
     * Configure this client to automatically detect and kill the remote
     * provider when an "application not responding" event is detected.
     *
     * @param timeoutMillis the duration for which a pending call is allowed
     *            block before the remote provider is considered to be
     *            unresponsive. Set to {@code 0} to allow pending calls to block
     *            indefinitely with no action taken.
     * @hide
     */
    @SystemApi
    public void setDetectNotResponding(long timeoutMillis) {
        synchronized (ContentProviderClient.class) {
            mAnrTimeout = timeoutMillis;

            if (timeoutMillis > 0) {
                if (mAnrRunnable == null) {
                    mAnrRunnable = new NotRespondingRunnable();
                }
                if (sAnrHandler == null) {
                    sAnrHandler = new Handler(Looper.getMainLooper(), null, true /* async */);
                }

                // If the remote process hangs, we're going to kill it, so we're
                // technically okay doing blocking calls.
                //Binder.allowBlocking(mContentProvider.asBinder());
            } else {
                mAnrRunnable = null;

                // If we're no longer watching for hangs, revert back to default
                // blocking behavior.
                //Binder.defaultBlocking(mContentProvider.asBinder());
            }
        }
    }

    private void beforeRemote() {
        if (mAnrRunnable != null) {
            sAnrHandler.postDelayed(mAnrRunnable, mAnrTimeout);
        }
    }

    private void afterRemote() {
        if (mAnrRunnable != null) {
            sAnrHandler.removeCallbacks(mAnrRunnable);
        }
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }

            close();
        } finally {
            super.finalize();
        }
    }
    

    /** {@hide} */
    @Deprecated
    public static void closeQuietly(ContentProviderClient client) {
        IoUtils.closeQuietly(client);
    }

    /** {@hide} */
    @Deprecated
    public static void releaseQuietly(ContentProviderClient client) {
        IoUtils.closeQuietly(client);
    }

    private class NotRespondingRunnable implements Runnable {
        @Override
        public void run() {
            Log.w(TAG, "Detected provider not responding: ");
            //mContentResolver.appNotRespondingViaProvider(mContentProvider);
        }
    }
}
