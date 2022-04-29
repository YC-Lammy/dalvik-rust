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

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/** @hide */
public interface IIntentSender extends IInterface{
    void send(int code, Intent intent, String resolvedType, IBinder whitelistToken,
            IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) throws RemoteException;

    static final String Descriptor = "IIntentSender";

    public abstract class Stub extends Binder implements IIntentSender{
        public Stub(){
            this.attachInterface(this, Descriptor);
        }
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 10008){
                int cod = data.readInt();
                Intent intent = Intent.CREATOR.createFromParcel(data);
                String resolvedType = data.readString();
                IBinder whitelistToken = data.readStrongBinder();
                IIntentReceiver finishedReceiver = IIntentReceiver.Stub.asInterface(data.readStrongBinder());
                String requiredPermission = data.readString();
                Bundle options = data.readBundle();

                send(cod, intent, resolvedType, whitelistToken, finishedReceiver, requiredPermission, options);
                
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        public static IIntentSender asInterface(IBinder binder){
            IInterface iface = binder.queryLocalInterface(Descriptor);
            if (iface != null && iface instanceof IIntentSender){
                return new IIntentSender.Stub() {
                    @Override
                    public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken,
                            IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) throws RemoteException{
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();

                        data.writeInt(code);
                        intent.writeToParcel(data, 0);
                        data.writeString(resolvedType);
                        data.writeStrongBinder(whitelistToken);
                        data.writeStrongInterface(finishedReceiver);
                        data.writeString(requiredPermission);
                        data.writeBundle(options);

                        transact(10008, data, reply, 0);
                    }

                    @Override
                    public IBinder asBinder() {
                        return binder;
                    }
                };
            } else{
                return (IIntentSender)binder;
            }
        }
    }
}
