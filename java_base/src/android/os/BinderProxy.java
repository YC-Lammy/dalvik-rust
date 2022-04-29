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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.util.Log;
import android.util.SparseIntArray;

import android.annotation.GuardedBy;

import libcore.util.NativeAllocationRegistry;

import java.io.FileDescriptor;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java proxy for a native IBinder object.
 * Allocated and constructed by the native javaObjectforIBinder function. Never allocated
 * directly from Java code.
 *
 * @hide
 */
public final class BinderProxy implements IBinder {

    // Assume the process-wide default value when created
    volatile boolean mWarnOnBlocking = Binder.sWarnOnBlocking;

    private static volatile Binder.ProxyTransactListener sTransactListener = null;
    
    public static void setTransactListener(@Nullable Binder.ProxyTransactListener listener) {
        sTransactListener = listener;
    }

    protected native void finalize() throws Throwable;
    public native String getInterfaceDescriptor() throws RemoteException;
    public native boolean pingBinder();
    public native boolean transactNative(int code, Parcel data, Parcel reply, int flags) throws RemoteException;
    public native void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException;
    public native boolean unlinkToDeath(DeathRecipient recipient, int flags);


    static long mNativePtr;

    private BinderProxy(long nativeData) {
        mNativePtr = nativeData;
    }

    /**
     * @return false if the hosting process is gone
     */
    public boolean isBinderAlive(){
        return pingBinder();
    }

    /**
     * Retrieve a local interface - always null in case of a proxy
     */
    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }


    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Binder.checkParcel(this, code, data, "Unreasonably large binder buffer");

        boolean warnOnBlocking = mWarnOnBlocking; // Cache it to reduce volatile access.

        if (warnOnBlocking && ((flags & FLAG_ONEWAY) == 0)
                && Binder.sWarnOnBlockingOnCurrentThread.get()) {

            // For now, avoid spamming the log by disabling after we've logged
            // about this interface at least once
            mWarnOnBlocking = false;
            warnOnBlocking = false;

            if (Build.IS_USERDEBUG) {
                // Log this as a WTF on userdebug builds.
                Log.wtf(Binder.TAG,
                        "Outgoing transactions from this process must be FLAG_ONEWAY",
                        new Throwable());
            } else {
                Log.w(Binder.TAG,
                        "Outgoing transactions from this process must be FLAG_ONEWAY",
                        new Throwable());
            }
        }

        final boolean tracingEnabled = Binder.isTracingEnabled();
        if (tracingEnabled) {
            final Throwable tr = new Throwable();
            Binder.getTransactionTracker().addTrace(tr);
            StackTraceElement stackTraceElement = tr.getStackTrace()[1];
            Trace.traceBegin(Trace.TRACE_TAG_ALWAYS,
                    stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName());
        }

        // Make sure the listener won't change while processing a transaction.
        final Binder.ProxyTransactListener transactListener = sTransactListener;
        Object session = null;

        if (transactListener != null) {
            final int origWorkSourceUid = Binder.getCallingWorkSourceUid();
            session = transactListener.onTransactStarted(this, code, flags);

            // Allow the listener to update the work source uid. We need to update the request
            // header if the uid is updated.
            final int updatedWorkSourceUid = Binder.getCallingWorkSourceUid();
            if (origWorkSourceUid != updatedWorkSourceUid) {
                data.replaceCallingWorkSourceUid(updatedWorkSourceUid);
            }
        }

        final AppOpsManager.PausedNotedAppOpsCollection prevCollection =
                AppOpsManager.pauseNotedAppOpsCollection();

        if ((flags & FLAG_ONEWAY) == 0 && AppOpsManager.isListeningForOpNoted()) {
            flags |= FLAG_COLLECT_NOTED_APP_OPS;
        }

        try {
            final boolean result = transactNative(code, data, reply, flags);

            if (reply != null && !warnOnBlocking) {
                reply.addFlags(Parcel.FLAG_IS_REPLY_FROM_BLOCKING_ALLOWED_OBJECT);
            }

            return result;
        } finally {
            AppOpsManager.resumeNotedAppOpsCollection(prevCollection);

            if (transactListener != null) {
                transactListener.onTransactEnded(session);
            }

            if (tracingEnabled) {
                Trace.traceEnd(Trace.TRACE_TAG_ALWAYS);
            }
        }
    }

    /**
     * Perform a dump on the remote object
     *
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param args additional arguments to the dump request.
     * @throws RemoteException
     */
    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(DUMP_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /**
     * Perform an asynchronous dump on the remote object
     *
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param args additional arguments to the dump request.
     * @throws RemoteException
     */
    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(DUMP_TRANSACTION, data, reply, FLAG_ONEWAY);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /**
     * See {@link IBinder#shellCommand(FileDescriptor, FileDescriptor, FileDescriptor,
     * String[], ShellCallback, ResultReceiver)}
     *
     * @param in The raw file descriptor that an input data stream can be read from.
     * @param out The raw file descriptor that normal command messages should be written to.
     * @param err The raw file descriptor that command error messages should be written to.
     * @param args Command-line arguments.
     * @param callback Optional callback to the caller's shell to perform operations in it.
     * @param resultReceiver Called when the command has finished executing, with the result code.
     * @throws RemoteException
     */
    public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args, ShellCallback callback,
            ResultReceiver resultReceiver) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(in);
        data.writeFileDescriptor(out);
        data.writeFileDescriptor(err);
        data.writeStringArray(args);
        ShellCallback.writeToParcel(callback, data);
        resultReceiver.writeToParcel(data, 0);
        try {
            transact(SHELL_COMMAND_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static void sendDeathNotice(DeathRecipient recipient, IBinder binderProxy) {
        if (false) {
            Log.v("JavaBinder", "sendDeathNotice to " + recipient + " for " + binderProxy);
        }
        try {
            recipient.binderDied(binderProxy);
        } catch (RuntimeException exc) {
            Log.w("BinderNative", "Uncaught exception from death notification",
                    exc);
        }
    }
}
