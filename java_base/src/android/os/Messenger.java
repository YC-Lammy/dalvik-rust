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

package android.os;

import android.annotation.Nullable;

/**
 * Reference to a Handler, which others can use to send messages to it.
 * This allows for the implementation of message-based communication across
 * processes, by creating a Messenger pointing to a Handler in one process,
 * and handing that Messenger to another process.
 *
 * <p>Note: the implementation underneath is just a simple wrapper around
 * a {@link Binder} that is used to perform the communication.  This means
 * semantically you should treat it as such: this class does not impact process
 * lifecycle management (you must be using some higher-level component to tell
 * the system that your process needs to continue running), the connection will
 * break if your process goes away for any reason, etc.</p>
 */
public final class Messenger implements Parcelable {

    public static int TRANSECTION_CODE = 0x6782648;

    private IBinder mTargetIbinder;

    public class MessengerBinder extends Binder implements IInterface{
        private Handler mTargetHandler;

        MessengerBinder(Handler handler){
            mTargetHandler = handler;
        }

        @Override
        public String getInterfaceDescriptor() {
            return "Messanger";
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            if (descriptor == "Messanger"){
                return this;
            }
            return null;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            mTargetHandler.handleMessage(Message.CREATOR.createFromParcel(data));
            return true;
        }
    }

    /**
     * Create a new Messenger pointing to the given Handler.  Any Message
     * objects sent through this Messenger will appear in the Handler as if
     * {@link Handler#sendMessage(Message) Handler.sendMessage(Message)} had
     * been called directly.
     * 
     * @param target The Handler that will receive sent messages.
     */
    public Messenger(Handler target) {
        mTargetIbinder = new MessengerBinder(target);
    }

    /**
     * Create a Messenger from a raw IBinder, which had previously been
     * retrieved with {@link #getBinder}.
     * 
     * @param target The IBinder this Messenger should communicate with.
     */
    public Messenger(IBinder target) {
        mTargetIbinder = target;
    }
    
    /**
     * Send a Message to this Messenger's Handler.
     * 
     * @param message The Message to send.  Usually retrieved through
     * {@link Message#obtain() Message.obtain()}.
     * 
     * @throws RemoteException Throws DeadObjectException if the target
     * Handler no longer exists.
     */
    public void send(Message message) throws RemoteException {
        Parcel p = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        message.writeToParcel(p, 0);
        mTargetIbinder.transact(TRANSECTION_CODE, p, reply, 0);
        p.recycle();
        reply.recycle();
    }
    
    /**
     * Retrieve the IBinder that this Messenger is using to communicate with
     * its associated Handler.
     * 
     * @return Returns the IBinder backing this Messenger.
     */
    public IBinder getBinder() {
        return mTargetIbinder;
    }
    
    /**
     * Comparison operator on two Messenger objects, such that true
     * is returned then they both point to the same Handler.
     */
    public boolean equals(@Nullable Object otherObj) {
        if (otherObj == null) {
            return false;
        }
        try {
            return mTargetIbinder.equals(((Messenger)otherObj)
                    .mTargetIbinder);
        } catch (ClassCastException e) {
        }
        return false;
    }

    public int hashCode() {
        return mTargetIbinder.hashCode();
    }
    
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(mTargetIbinder);
    }

    public static final @android.annotation.NonNull Parcelable.Creator<Messenger> CREATOR
            = new Parcelable.Creator<Messenger>() {
        public Messenger createFromParcel(Parcel in) {
            IBinder target = in.readStrongBinder();
            return target != null ? new Messenger(target) : null;
        }

        public Messenger[] newArray(int size) {
            return new Messenger[size];
        }
    };

    /**
     * Convenience function for writing either a Messenger or null pointer to
     * a Parcel.  You must use this with {@link #readMessengerOrNullFromParcel}
     * for later reading it.
     * 
     * @param messenger The Messenger to write, or null.
     * @param out Where to write the Messenger.
     */
    public static void writeMessengerOrNullToParcel(Messenger messenger,
            Parcel out) {
        out.writeStrongBinder(messenger != null ? messenger.mTargetIbinder
                : null);
    }
    
    /**
     * Convenience function for reading either a Messenger or null pointer from
     * a Parcel.  You must have previously written the Messenger with
     * {@link #writeMessengerOrNullToParcel}.
     * 
     * @param in The Parcel containing the written Messenger.
     * 
     * @return Returns the Messenger read from the Parcel, or null if null had
     * been written.
     */
    public static Messenger readMessengerOrNullFromParcel(Parcel in) {
        IBinder b = in.readStrongBinder();
        return b != null ? new Messenger(b) : null;
    }
}
