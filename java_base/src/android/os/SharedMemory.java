/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.system.ErrnoException;
import android.system.OsConstants;

import dalvik.system.VMRuntime;

import java.io.Closeable;
import java.nio.ByteBuffer;


/**
 * SharedMemory enables the creation, mapping, and protection control over anonymous shared memory.
 */
public final class SharedMemory implements Parcelable, Closeable {

    private static native int nCreate(String name, int size) throws ErrnoException;
    private static native ByteBuffer nMap(int fd, int prot, int offset, int length);
    private static native void nUnmap(ByteBuffer buf);
    private static native void nClose(int fd);
    private static native int nGetSize(int fd);

    private int mSize;
    private int mFd;

    private SharedMemory(int fd, int size){
        mFd = fd;
        mSize = size;
    }
    
    public static SharedMemory create(String name, int size) throws ErrnoException{
        return new SharedMemory(nCreate(name, size), size);
    }

    public static SharedMemory fromFileDescriptor(ParcelFileDescriptor fd){
        int fdd = fd.detachFd();
        return new SharedMemory(fdd, nGetSize(fdd));
    }

    public void close(){
        nClose(mFd);
    }

    public int getSize(){
        return mSize;
    }

    public ByteBuffer map(int prot, int offset, int length) throws ErrnoException{
        return nMap(mFd, prot, offset, length);
    }

    public ByteBuffer mapReadOnly () throws ErrnoException{
        return map(OsConstants.PROT_READ, 0, getSize());
    }

    public ByteBuffer mapReadWrite () throws ErrnoException{
        return map(OsConstants.PROT_READ | OsConstants.PROT_WRITE, 0, getSize());
    }

    public static void unmap(ByteBuffer buffer){
        nUnmap(buffer);
    }

    public boolean setProtect(int prot){
        return false;
    }

    public static final Creator<SharedMemory> CREATOR = new Creator<SharedMemory>() {
        public SharedMemory createFromParcel(Parcel source) {
            return new SharedMemory(source.readInt(), source.readInt());
        };

        public SharedMemory[] newArray(int size) {
            return new SharedMemory[size];
        };
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mFd);
        dest.writeInt(mSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
