/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.util.Log;

/**
 * Special-purpose API for use with {@link IBinder#shellCommand IBinder.shellCommand} for
 * performing operations back on the invoking shell.
 * @hide
 */
public class ShellCallback implements Parcelable {
    final static String TAG = "ShellCallback";

    final static boolean DEBUG = false;

    final static int OpenFile = 0x8345;

    final boolean mLocal;

    IShellCallback mShellCallback;

    interface IShellCallback extends IInterface{
        public ParcelFileDescriptor openFile(String path, String seLinuxContext, String mode) throws RemoteException;
    }

    class MyShellCallback extends Binder implements IShellCallback{
        public ParcelFileDescriptor openFile(String path, String seLinuxContext,
                String mode) throws RemoteException{
            return onOpenFile(path, seLinuxContext, mode);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == OpenFile){
                ParcelFileDescriptor f = openFile(data.readString(), reply.readString(), reply.readString());
                f.writeToParcel(reply, 0);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public String getInterfaceDescriptor() {
            return "ShellCallback";
        }
    }

    /**
     * Create a new ShellCallback to receive requests.
     */
    public ShellCallback() {
        mLocal = true;
    }

    /**
     * Ask the shell to open a file.  If opening for writing, will truncate the file if it
     * already exists and will create the file if it doesn't exist.
     * @param path Path of the file to be opened/created.
     * @param seLinuxContext Optional SELinux context that must be allowed to have
     * access to the file; if null, nothing is required.
     * @param mode Mode to open file in: "r" for input/reading an existing file,
     * "r+" for reading/writing an existing file, "w" for output/writing a new file (either
     * creating or truncating an existing one), "w+" for reading/writing a new file (either
     * creating or truncating an existing one).
     */
    public ParcelFileDescriptor openFile(String path, String seLinuxContext, String mode) {
        if (DEBUG) Log.d(TAG, "openFile " + this + " mode=" + mode + ": mLocal=" + mLocal
                + " mShellCallback=" + mShellCallback);

        if (mLocal) {
            return onOpenFile(path, seLinuxContext, mode);
        }

        if (mShellCallback != null) {
            try {
                return mShellCallback.openFile(path, seLinuxContext, mode);
            } catch (Exception e) {
                Log.w(TAG, "Failure opening " + path, e);
            }
        }
        return null;
    }

    public ParcelFileDescriptor onOpenFile(String path, String seLinuxContext, String mode) {
        return null;
    }

    public static void writeToParcel(ShellCallback callback, Parcel out) {
        if (callback == null) {
            out.writeStrongBinder(null);
        } else {
            callback.writeToParcel(out, 0);
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        synchronized (this) {
            if (mShellCallback == null) {
                mShellCallback = new MyShellCallback();
            }
            out.writeStrongBinder(mShellCallback.asBinder());
        }
    }

    public IBinder getShellCallbackBinder() {
        return mShellCallback.asBinder();
    }

    ShellCallback(Parcel in) {
        mLocal = false;
        IBinder binder = in.readStrongBinder();
        IInterface iface = binder.queryLocalInterface("ShellCallback");
        if (iface != null){
            mShellCallback = (IShellCallback)iface;
        } else{
            mShellCallback = new IShellCallback() {
                @Override
                public ParcelFileDescriptor openFile(String path, String seLinuxContext, String mode) throws RemoteException{
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    binder.transact(OpenFile, data, reply, 0);
                    data.recycle();
                    reply.recycle();
                    return reply.readFileDescriptor();
                }

                @Override
                public IBinder asBinder() {
                    return binder;
                }
            };
        }
        if (mShellCallback != null) {
            Binder.allowBlocking(mShellCallback.asBinder());
        }
    }

    public static final @android.annotation.NonNull Parcelable.Creator<ShellCallback> CREATOR
            = new Parcelable.Creator<ShellCallback>() {
        public ShellCallback createFromParcel(Parcel in) {
            return new ShellCallback(in);
        }
        public ShellCallback[] newArray(int size) {
            return new ShellCallback[size];
        }
    };
}
