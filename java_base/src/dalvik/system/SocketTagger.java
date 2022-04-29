/*
 * Copyright (C) 2011 The Android Open Source Project
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

package dalvik.system;

import java.io.FileDescriptor;
import java.net.Socket;
import java.net.SocketException;
import java.lang.reflect.*;

/**
 * Callbacks for socket assignment and reassignment.
 *
 * @hide
 */
public abstract class SocketTagger {

    private static SocketTagger tagger = new SocketTagger() {
        @Override public void tag(FileDescriptor socketDescriptor) throws SocketException {}
        @Override public void untag(FileDescriptor socketDescriptor) throws SocketException {}
    };

    /**
     * Notified when {@code socketDescriptor} is either assigned to the current
     * thread. The socket is either newly connected or reused from a connection
     * pool. Implementations of this method should be thread-safe.
     */
    public abstract void tag(FileDescriptor socketDescriptor) throws SocketException;

    /**
     * Notified when {@code socketDescriptor} is released from the current
     * thread to a connection pool. Implementations of this method should be
     * thread-safe.
     *
     * <p><strong>Note:</strong> this method will not be invoked when the socket
     * is closed.
     */
    public abstract void untag(FileDescriptor socketDescriptor) throws SocketException;

    public final void tag(Socket socket) throws SocketException {
        try{
            Class cls = socket.getClass();
            Field f = cls.getDeclaredField("impl");
            f.setAccessible(true);
            Object o = f.get(socket);
            Class cls2 = o.getClass();
            Field f1 = cls2.getDeclaredField("fd");
            f1.setAccessible(true);
            Object fd = f1.get(o);

            if (!socket.isClosed()) {
                tag((FileDescriptor)fd);
            }

        } catch(Exception e){
            throw new SocketException();
        }
    }

    public final void untag(Socket socket) throws SocketException {
        try{
            Class cls = socket.getClass();
            Field f = cls.getDeclaredField("impl");
            f.setAccessible(true);
            Object o = f.get(socket);
            Class cls2 = o.getClass();
            Field f1 = cls2.getDeclaredField("fd");
            f1.setAccessible(true);
            Object fd = f1.get(o);

            if (!socket.isClosed()) {
                untag((FileDescriptor)fd);
            }

        } catch(Exception e){
            throw new SocketException();
        }
        
    }

    /**
     * Sets this process' socket tagger to {@code tagger}.
     */
    public static synchronized void set(SocketTagger tagger) {
        if (tagger == null) {
            throw new NullPointerException("tagger == null");
        }
        SocketTagger.tagger = tagger;
    }

    /**
     * Returns this process socket tagger.
     */
    public static synchronized SocketTagger get() {
        return tagger;
    }
}
