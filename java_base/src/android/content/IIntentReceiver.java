/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * System private API for dispatching intent broadcasts.  This is given to the
 * activity manager as part of registering for an intent broadcasts, and is
 * called when it receives intents.
 *
 * {@hide}
 */
public interface IIntentReceiver extends IInterface{
    void performReceive(Intent intent, int resultCode, String data,
            Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException;

    public abstract class Stub extends Binder implements IIntentReceiver{

        public Stub(){
            this.attachInterface(this, "IIntentReceiver");
        }

        @Override
        public String getInterfaceDescriptor() {
            return "IIntentReceiver";
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 10009){
                Intent inte = Intent.CREATOR.createFromParcel(data);

                performReceive(inte, data.readInt(), data.readString(), data.readBundle(), data.readBoolean(), data.readBoolean(), data.readInt());
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        public static IIntentReceiver asInterface(IBinder binder){
            IInterface iface = binder.queryLocalInterface("IIntentReceiver");
            if (iface != null && iface instanceof IIntentReceiver){
                return (IIntentReceiver)binder;
            } else{
                return new IIntentReceiver.Stub() {
                    @Override
                    public void performReceive(Intent intent, int resultCode, String data, Bundle extras,
                            boolean ordered, boolean sticky, int sendingUser) throws RemoteException{
                        Parcel send = Parcel.obtain();
                        Parcel reply = Parcel.obtain();

                        intent.writeToParcel(send, 0);
                        send.writeInt(resultCode);
                        send.writeString(data);
                        send.writeBundle(extras);
                        send.writeBoolean(ordered);
                        send.writeBoolean(sticky);
                        send.writeInt(sendingUser);
                        
                        transact(10009, send, reply, 0);
                        send.recycle();
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

