/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;

/**
 * @hide
 */
@SystemApi
public final class RemoteCallback implements Parcelable {

    public interface OnResultListener {
        void onResult(@Nullable Bundle result);
    }

    private final OnResultListener mListener;

    private final Handler mHandler;
    private final IRemoteCallback mCallback;

    interface IRemoteCallback extends IInterface{
        public void sendResult(Bundle data) throws RemoteException;

        abstract class Stub extends Binder implements IRemoteCallback{
            public abstract void sendResult(Bundle data) throws RemoteException;

            @Override
            public IBinder asBinder() {
                return this;
            }

            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                sendResult(data.readBundle());
                return true;
            }

            @Override
            public String getInterfaceDescriptor() {
                return "IRemoteCallback";
            }

            public static IRemoteCallback asInterface(IBinder binder){
                IInterface iface = binder.queryLocalInterface("IRemoteCallback");
                if (iface == null){
                    return new IRemoteCallback(){
                        @Override
                        public void sendResult(Bundle dat) throws RemoteException{
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            data.writeBundle(dat);
                            binder.transact(0, data, reply, 0);
                            data.recycle();
                            reply.recycle();
                        }

                        @Override
                        public IBinder asBinder() {
                            return binder;
                        }
                    };
                } else{
                    return (IRemoteCallback.Stub)binder;
                }
            }
        }
    }

    public RemoteCallback(OnResultListener listener) {
        this(listener, null);
    }

    public RemoteCallback(@NonNull OnResultListener listener, @Nullable Handler handler) {
        if (listener == null) {
            throw new NullPointerException("listener cannot be null");
        }
        mListener = listener;
        mHandler = handler;
        mCallback = new IRemoteCallback.Stub() {
            @Override
            public void sendResult(Bundle data) {
                RemoteCallback.this.sendResult(data);
            }
        };
    }

    RemoteCallback(Parcel parcel) {
        mListener = null;
        mHandler = null;
        mCallback = IRemoteCallback.Stub.asInterface(
                parcel.readStrongBinder());
    }

    public void sendResult(@Nullable final Bundle result) {
        // Do local dispatch
        if (mListener != null) {
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onResult(result);
                    }
                });
            } else {
                mListener.onResult(result);
            }
        // Do remote dispatch
        } else {
            try {
                mCallback.sendResult(result);
            } catch (RemoteException e) {
                /* ignore */
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStrongBinder(mCallback.asBinder());
    }

    public static final @android.annotation.NonNull Parcelable.Creator<RemoteCallback> CREATOR
            = new Parcelable.Creator<RemoteCallback>() {
        public RemoteCallback createFromParcel(Parcel parcel) {
            return new RemoteCallback(parcel);
        }

        public RemoteCallback[] newArray(int size) {
            return new RemoteCallback[size];
        }
    };
}
