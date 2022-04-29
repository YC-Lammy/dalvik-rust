/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except compliance with the License.
 * You may obtaa copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content;

import java.util.List;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.SyncInfo;
import android.content.ISyncStatusObserver;
import android.content.SyncAdapterType;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.content.PeriodicSync;
import android.net.Uri;
import android.os.Bundle;
import android.os.IInterface;
import android.database.IContentObserver;

/**
 * @hide
 */
interface IContentService extends IInterface{
    void unregisterContentObserver(IContentObserver observer);

    void registerContentObserver(Uri uri, boolean notifyForDescendants,
            IContentObserver observer, int userHandle, int targetSdkVersion);

    void notifyChange(Uri[] uris, IContentObserver observer,
            boolean observerWantsSelfNotifications, int flags,
            int userHandle, int targetSdkVersion, String callingPackage);

    void requestSync(Account account, String authority, Bundle extras, String callingPackage);
    /**
     * Start a sync given a request.
     */
    void sync(SyncRequest request, String callingPackage);
    void syncAsUser(SyncRequest request, int userId, String callingPackage);
    void cancelSync(Account account, String authority, ComponentName cname);
    void cancelSyncAsUser(Account account, String authority, ComponentName cname, int userId);

    /** Cancel a sync, providing information about the sync to be cancelled. */
     void cancelRequest(SyncRequest request);

    /**
     * Check if the provider should be synced when a network tickle is received
     * @param providerName the provider whose setting we are querying
     * @return true if the provider should be synced when a network tickle is received
     */
    boolean getSyncAutomatically(Account account, String providerName);
    boolean getSyncAutomaticallyAsUser(Account account, String providerName, int userId);

    /**
     * Set whether or not the provider is synced when it receives a network tickle.
     *
     * @param providerName the provider whose behavior is being controlled
     * @param sync true if the provider should be synced when tickles are received for it
     */
    void setSyncAutomatically(Account account, String providerName, boolean sync);
    void setSyncAutomaticallyAsUser(Account account, String providerName, boolean sync,
            int userId);

    /**
     * Get a list of periodic operations for a specified authority, or service.
     * @param account account for authority, must be null if cname is non-null.
     * @param providerName name of provider, must be null if cname is non-null.
     * @param cname component to identify sync service, must be null if account/providerName are
     * non-null.
     */
    List<PeriodicSync> getPeriodicSyncs(Account account, String providerName,
        ComponentName cname);

    /**
     * Set whether or not the provider is to be synced on a periodic basis.
     *
     * @param providerName the provider whose behavior is being controlled
     * @param pollFrequency the period that a sync should be performed, seconds. If this is
     * zero or less then no periodic syncs will be performed.
     */
    void addPeriodicSync(Account account, String providerName, Bundle extras,
      long pollFrequency);

    /**
     * Set whether or not the provider is to be synced on a periodic basis.
     *
     * @param providerName the provider whose behavior is being controlled
     * @param pollFrequency the period that a sync should be performed, seconds. If this is
     * zero or less then no periodic syncs will be performed.
     */
    void removePeriodicSync(Account account, String providerName, Bundle extras);

    /**
     * Check if this account/provider is syncable.
     * @return >0 if it is syncable, 0 if not, and <0 if the state isn't known yet.
     */
    int getIsSyncable(Account account, String providerName);
    int getIsSyncableAsUser(Account account, String providerName, int userId);

    /**
     * Set whether this account/provider is syncable.
     * @param syncable, >0 denotes syncable, 0 means not syncable, <0 means unknown
     */
    void setIsSyncable(Account account, String providerName, int syncable);
    void setIsSyncableAsUser(Account account, String providerName, int syncable, int userId);


    void setMasterSyncAutomatically(boolean flag);
    void setMasterSyncAutomaticallyAsUser(boolean flag, int userId);


    boolean getMasterSyncAutomatically();
    boolean getMasterSyncAutomaticallyAsUser(int userId);

    List<SyncInfo> getCurrentSyncs();
    List<SyncInfo> getCurrentSyncsAsUser(int userId);

    /**
     * Returns the types of the SyncAdapters that are registered with the system.
     * @return Returns the types of the SyncAdapters that are registered with the system.
     */
    SyncAdapterType[] getSyncAdapterTypes();
    SyncAdapterType[] getSyncAdapterTypesAsUser(int userId);

    String[] getSyncAdapterPackagesForAuthorityAsUser(String authority, int userId);

    /**
     * Returns true if there is currently a operation for the given account/authority or service
     * actively being processed.
     * @param account account for authority, must be null if cname is non-null.
     * @param providerName name of provider, must be null if cname is non-null.
     * @param cname component to identify sync service, must be null if account/providerName are
     * non-null.
     */
    boolean isSyncActive(Account account, String authority, ComponentName cname);

    /**
     * Returns the status that matches the authority. If there are multiples accounts for
     * the authority, the one with the latest "lastSuccessTime" status is returned.
     * @param account account for authority, must be null if cname is non-null.
     * @param providerName name of provider, must be null if cname is non-null.
     * @param cname component to identify sync service, must be null if account/providerName are
     * non-null.
     */
    SyncStatusInfo getSyncStatus(Account account, String authority, ComponentName cname);
    SyncStatusInfo getSyncStatusAsUser(Account account, String authority, ComponentName cname,
            int userId);

    /**
     * Return true if the pending status is true of any matching authorities.
     * @param account account for authority, must be null if cname is non-null.
     * @param providerName name of provider, must be null if cname is non-null.
     * @param cname component to identify sync service, must be null if account/providerName are
     * non-null.
     */
    boolean isSyncPending(Account account, String authority, ComponentName cname);
    boolean isSyncPendingAsUser(Account account, String authority, ComponentName cname,
            int userId);

    void addStatusChangeListener(int mask, ISyncStatusObserver callback);
    void removeStatusChangeListener(ISyncStatusObserver callback);

    void putCache(String packageName, Uri key, Bundle value, int userId);
    Bundle getCache(String packageName, Uri key, int userId);

    void resetTodayStats();

    void onDbCorruption(String tag, String message, String stacktrace);
}
