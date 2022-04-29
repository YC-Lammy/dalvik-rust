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

package android.database.sqlite;

import android.annotation.TestApi;
import android.content.res.Resources;
import android.os.StatFs;

/**
 * Provides access to SQLite functions that affect all database connection,
 * such as memory management.
 *
 * The native code associated with SQLiteGlobal is also sets global configuration options
 * using sqlite3_config() then calls sqlite3_initialize() to ensure that the SQLite
 * library is properly initialized exactly once before any other framework or application
 * code has a chance to run.
 *
 * Verbose SQLite logging is enabled if the "log.tag.SQLiteLog" property is set to "V".
 * (per {@link SQLiteDebug#DEBUG_SQL_LOG}).
 *
 * @hide
 */
@TestApi
public final class SQLiteGlobal {
    private static final String TAG = "SQLiteGlobal";

    /** @hide */
    public static final String SYNC_MODE_FULL = "FULL";

    /** @hide */
    static final String WIPE_CHECK_FILE_SUFFIX = "-wipecheck";

    private static final Object sLock = new Object();

    private static int sDefaultPageSize;

    private static native int nativeReleaseMemory();

    /** @hide */
    public static volatile String sDefaultSyncMode;

    private SQLiteGlobal() {
    }

    /**
     * Attempts to release memory by pruning the SQLite page cache and other
     * internal data structures.
     *
     * @return The number of bytes that were freed.
     */
    public static int releaseMemory() {
        return nativeReleaseMemory();
    }

    /**
     * Gets the default page size to use when creating a database.
     */
    public static int getDefaultPageSize() {
        synchronized (sLock) {
            if (sDefaultPageSize == 0) {
                // If there is an issue accessing /data, something is so seriously
                // wrong that we just let the IllegalArgumentException propagate.
                sDefaultPageSize = (int)new StatFs("/data").getBlockSizeLong();
            }
            return sDefaultPageSize;
        }
    }

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    public static String getDefaultJournalMode() {
        return "DELETE";
    }

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    public static String getDefaultSyncMode() {
        // Use the FULL synchronous mode for system processes by default.
        String defaultMode = sDefaultSyncMode;
        if (defaultMode != null) {
            return defaultMode;
        }
        return "FULL";
    }

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    public static String getWALSyncMode() {
        // Use the FULL synchronous mode for system processes by default.
        String defaultMode = sDefaultSyncMode;
        if (defaultMode != null) {
            return defaultMode;
        }
        return "FULL";
    }

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    public static int getWALAutoCheckpoint() {
        return 1;
    }

    /**
     * Gets the connection pool size when in WAL mode.
     */
    public static int getWALConnectionPoolSize() {
        return 2;
    }

    /**
     * The default number of milliseconds that SQLite connection is allowed to be idle before it
     * is closed and removed from the pool.
     */
    public static int getIdleConnectionTimeout() {
        return Integer.MAX_VALUE;
    }

    /**
     * When opening a database, if the WAL file is larger than this size, we'll truncate it.
     *
     * (If it's 0, we do not truncate.)
     *
     * @hide
     */
    public static long getWALTruncateSize() {
        final long setting = SQLiteCompatibilityWalFlags.getTruncateSize();
        if (setting >= 0) {
            return setting;
        }
        return 0;
    }

    /** @hide */
    public static boolean checkDbWipe() {
        return false;
    }
}
