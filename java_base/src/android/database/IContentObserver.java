/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License")
** you may not use this file except in compliance with the License
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.database;

import java.net.URI;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.EventLogTags.Description;

/**
 * @hide
 */
public interface IContentObserver extends IInterface{
    /**
     * This method is called when an update occurs to the cursor that is being
     * observed. selfUpdate is true if the update was caused by a call to
     * commit on the cursor that is being observed.
     */
    public void onChange(boolean selfUpdate, Uri uri, int userId) throws RemoteException;

    /**
     * This method is called when an update occurs to the cursor that is being
     * observed. selfUpdate is true if the update was caused by a call to
     * commit on the cursor that is being observed.
     */
    public void onChangeEtc(boolean selfUpdate, Uri[] uri, int flags, int userId) throws RemoteException;

    public abstract class Stub extends Binder implements IContentObserver{
        public static final String DESCRIPTOR = "IContentObserver";
        public static int ON_CHANGE = Binder.FIRST_CALL_TRANSACTION +1;
        public static int ON_CHANGE_ETC = Binder.FIRST_CALL_TRANSACTION +2;

        public Stub(){
            this.attachInterface(this, DESCRIPTOR);
        }

        @Override
        public String getInterfaceDescriptor() {
            return DESCRIPTOR;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == ON_CHANGE){
                onChange(data.readBoolean() ,Uri.CREATOR.createFromParcel(data), data.readInt());
                return true;
            } else if (code == ON_CHANGE_ETC){
                boolean b = data.readBoolean();
                int length = data.readInt();
                Uri[] uri = new Uri[length];
                for (int i=0;i<length;i++){
                    uri[i] = Uri.CREATOR.createFromParcel(data);
                }

                onChangeEtc(b, uri, data.readInt(), data.readInt());
                return true;
            }

            return false;
        }

        public static IContentObserver asInterface(IBinder binder){
            IInterface iface = binder.queryLocalInterface(DESCRIPTOR);
            if (iface != null && iface instanceof IContentObserver){
                return (IContentObserver)iface;
            } else{
                return new IContentObserver(){
                    @Override
                    public void onChange(boolean selfUpdate, Uri uri, int userId) throws RemoteException{
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        data.writeBoolean(selfUpdate);
                        uri.writeToParcel(data, 0);
                        data.writeInt(userId);
                        binder.transact(ON_CHANGE, data, reply, 0);
                        data.recycle();
                        reply.recycle();
                    }

                    @Override
                    public void onChangeEtc(boolean selfUpdate, Uri[] uri, int flags, int userId) throws RemoteException {
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        data.writeBoolean(selfUpdate);
                        data.writeInt(uri.length);
                        for (Uri i:uri){
                            i.writeToParcel(data, 0);
                        }
                        data.writeInt(flags);
                        data.writeInt(userId);
                        binder.transact(ON_CHANGE_ETC, data, reply, 0);
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
