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

package android.content;

import android.content.SyncResult;
import android.content.IntentSender.OnFinished;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface used by the SyncAdapter to indicate its progress.
 * @hide
 */
public interface ISyncContext extends IInterface{
    /**
     * Call to indicate that the SyncAdapter is making progress. E.g., if this SyncAdapter
     * downloads or sends records to/from the server, this may be called after each record
     * is downloaded or uploaded.
     */
    public void sendHeartbeat() throws RemoteException;

    /**
     * Signal that the corresponding sync session is completed.
     * @param result information about this sync session
     */
    public void onFinished(SyncResult result) throws RemoteException; 

    public abstract class Stub extends Binder implements ISyncContext{
        public static String Descriptor = "ISyncContext";
        public static int ON_FINISH = Binder.FIRST_CALL_TRANSACTION +1;
        public static int SEND_HEART_BEAT = Binder.FIRST_CALL_TRANSACTION +2;

        public Stub(){
            this.attachInterface(this, Descriptor);
        }

        @Override
        public String getInterfaceDescriptor() {
            return Descriptor;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == ON_FINISH){
                onFinished(SyncResult.CREATOR.createFromParcel(data));
                return true;
            } else if (code == SEND_HEART_BEAT){
                sendHeartbeat();
                return true;
            }
            return false;
        }

        ISyncContext asInterface(IBinder binder){
            IInterface iface = binder.queryLocalInterface(Descriptor);
            if (iface!=null && iface instanceof ISyncContext){
                return (ISyncContext)binder;
            } else{
                return new ISyncContext(){
                    @Override
                    public void onFinished(SyncResult result) throws RemoteException {
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        result.writeToParcel(data, 0);
                        binder.transact(ON_FINISH, data, reply, 0);
                        data.recycle();
                        reply.recycle();
                    }

                    @Override
                    public void sendHeartbeat() throws RemoteException {
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        binder.transact(SEND_HEART_BEAT, data, reply, 0);
                        data.recycle();
                        reply.recycle();
                    }

                    @Override
                    public IBinder asBinder() {
                        return binder;
                    }
                };
            }
        }
    }
}
