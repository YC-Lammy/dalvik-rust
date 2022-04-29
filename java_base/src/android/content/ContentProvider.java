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

import static android.os.Trace.TRACE_TAG_DATABASE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.content.Intent.AccessUriMode;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * Content providers are one of the primary building blocks of Android applications, providing
 * content to applications. They encapsulate data and provide it to applications through the single
 * {@link ContentResolver} interface. A content provider is only required if you need to share
 * data between multiple applications. For example, the contacts data is used by multiple
 * applications and must be stored in a content provider. If you don't need to share data amongst
 * multiple applications you can use a database directly via
 * {@link android.database.sqlite.SQLiteDatabase}.
 *
 * <p>When a request is made via
 * a {@link ContentResolver} the system inspects the authority of the given URI and passes the
 * request to the content provider registered with the authority. The content provider can interpret
 * the rest of the URI however it wants. The {@link UriMatcher} class is helpful for parsing
 * URIs.</p>
 *
 * <p>The primary methods that need to be implemented are:
 * <ul>
 *   <li>{@link #onCreate} which is called to initialize the provider</li>
 *   <li>{@link #query} which returns data to the caller</li>
 *   <li>{@link #insert} which inserts new data into the content provider</li>
 *   <li>{@link #update} which updates existing data in the content provider</li>
 *   <li>{@link #delete} which deletes data from the content provider</li>
 *   <li>{@link #getType} which returns the MIME type of data in the content provider</li>
 * </ul></p>
 *
 * <p class="caution">Data access methods (such as {@link #insert} and
 * {@link #update}) may be called from many threads at once, and must be thread-safe.
 * Other methods (such as {@link #onCreate}) are only called from the application
 * main thread, and must avoid performing lengthy operations.  See the method
 * descriptions for their expected thread behavior.</p>
 *
 * <p>Requests to {@link ContentResolver} are automatically forwarded to the appropriate
 * ContentProvider instance, so subclasses don't have to worry about the details of
 * cross-process calls.</p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about using content providers, read the
 * <a href="{@docRoot}guide/topics/providers/content-providers.html">Content Providers</a>
 * developer guide.</p>
 * </div>
 */
public abstract class ContentProvider implements ContentInterface, ComponentCallbacks2 {

    private static final String TAG = "ContentProvider";

    private static HashMap<String, ContentProvider> ActiveProviders = new HashMap<String, ContentProvider>();


    private Context mContext = null;
    private int mMyUid;

    // Since most Providers have only one authority, we keep both a String and a String[] to improve
    // performance.
    private String mAuthority;

    private String[] mAuthorities;

    private String mReadPermission;

    private String mWritePermission;

    private PathPermission[] mPathPermissions;
    private boolean mExported;
    private boolean mNoPerms;
    private boolean mSingleUser;

    private ThreadLocal<AttributionSource> mCallingAttributionSource;

    /**
     * Construct a ContentProvider instance.  Content providers must be
     * <a href="{@docRoot}guide/topics/manifest/provider-element.html">declared
     * in the manifest</a>, accessed with {@link ContentResolver}, and created
     * automatically by the system, so applications usually do not create
     * ContentProvider instances directly.
     *
     * <p>At construction time, the object is uninitialized, and most fields and
     * methods are unavailable.  Subclasses should initialize themselves in
     * {@link #onCreate}, not the constructor.
     *
     * <p>Content providers are created on the application main thread at
     * application launch time.  The constructor must not perform lengthy
     * operations, or application startup will be delayed.
     */
    public ContentProvider() {
    }

    /**
     * Constructor just for mocking.
     *
     * @param context A Context object which should be some mock instance (like the
     * instance of {@link android.test.mock.MockContext}).
     * @param readPermission The read permision you want this instance should have in the
     * test, which is available via {@link #getReadPermission()}.
     * @param writePermission The write permission you want this instance should have
     * in the test, which is available via {@link #getWritePermission()}.
     * @param pathPermissions The PathPermissions you want this instance should have
     * in the test, which is available via {@link #getPathPermissions()}.
     * @hide
     */
    public ContentProvider(
            Context context,
            String readPermission,
            String writePermission,
            PathPermission[] pathPermissions) {
        mContext = context;
        mReadPermission = readPermission;
        mWritePermission = writePermission;
        mPathPermissions = pathPermissions;
    }


    @Override
    public @NonNull ContentProviderResult[] applyBatch(@NonNull String authority,
            @NonNull ArrayList<ContentProviderOperation> operations)
                    throws OperationApplicationException {
        return applyBatch(operations);
    }

    public @NonNull ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
                    throws OperationApplicationException {
        final int numOperations = operations.size();
        final ContentProviderResult[] results = new ContentProviderResult[numOperations];
        for (int i = 0; i < numOperations; i++) {
            results[i] = operations.get(i).apply(this, results, i);
        }
        return results;
    }


    public void attachInfoForTesting(Context context, ProviderInfo info) {
        attachInfo(context, info, true);
    }

    public void attachInfo(Context context, ProviderInfo info) {
        attachInfo(context, info, false);
    }

    private void attachInfo(Context context, ProviderInfo info, boolean testing) {
        mNoPerms = testing;
        mCallingAttributionSource = new ThreadLocal<>();

        /*
         * Only allow it to be set once, so after the content service gives
         * this to us clients can't change it.
         */
        if (mContext == null) {
            mContext = context;

            mMyUid = Process.myUid();
            if (info != null) {
                setReadPermission(info.readPermission);
                setWritePermission(info.writePermission);
                setPathPermissions(info.pathPermissions);
                mExported = info.exported;
                mSingleUser = (info.flags & ProviderInfo.FLAG_SINGLE_USER) != 0;
                setAuthorities(info.authority);
            }
            if (Build.IS_DEBUGGABLE) {

            }
            ContentProvider.this.onCreate();
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int numValues = values.length;
        for (int i = 0; i < numValues; i++) {
            insert(uri, values[i]);
        }
        return numValues;
    }

    @Override
    public Bundle call(@NonNull String authority, @NonNull String method, String arg, Bundle extras) {
        return call(method, arg, extras);
    }

    public Bundle call(@NonNull String method, String arg, Bundle extras) {
        return null;
    }

    @Override
    public Uri canonicalize(@NonNull Uri url) {
        return null;
    }

    @Override
    public Uri uncanonicalize(@NonNull Uri url) {
        return url;
    }


    @SuppressWarnings("AndroidFrameworkBinderIdentity")
    public final @NonNull CallingIdentity clearCallingIdentity() {
        return new CallingIdentity(Binder.clearCallingIdentity(),
                setCallingAttributionSource(null));
    }

    public final void restoreCallingIdentity(@NonNull CallingIdentity identity) {
        Binder.restoreCallingIdentity(identity.binderToken);
        mCallingAttributionSource.set(identity.callingAttributionSource);
    }
    
    public abstract int delete(@NonNull Uri uri, String selection, String[] selectionArgs);

    @Override
    public int delete(@NonNull Uri uri, Bundle extras) {
        extras = (extras != null) ? extras : Bundle.EMPTY;
        return delete(uri,
                extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
                extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
    }


    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("nothing to dump");
    }


    public final AttributionSource getCallingAttributionSource() {
        final AttributionSource attributionSource = mCallingAttributionSource.get();
        if (attributionSource != null) {
            //mTransport.mAppOpsManager.checkPackage(Binder.getCallingUid(),
            //        attributionSource.getPackageName());
        }
        return attributionSource;
    }

    public final String getCallingAttributionTag() {
        final AttributionSource attributionSource = mCallingAttributionSource.get();
        if (attributionSource != null) {
            return attributionSource.getAttributionTag();
        }
        return null;
    }

    public final String getCallingPackage() {
        final AttributionSource callingAttributionSource = getCallingAttributionSource();
        return (callingAttributionSource != null)
                ? callingAttributionSource.getPackageName() : null;
    }

    public final String getCallingPackageUnchecked() {
        final AttributionSource attributionSource = mCallingAttributionSource.get();
        if (attributionSource != null) {
            return attributionSource.getPackageName();
        }
        return null;
    }

    public final Context getContext() {
        return mContext;
    }

    public final PathPermission[] getPathPermissions() {
        return mPathPermissions;
    }
    protected final void setPathPermissions(PathPermission[] permissions) {
        mPathPermissions = permissions;
    }

    
    protected final void setReadPermission(String permission) {
        mReadPermission = permission;
    }
    public final String getReadPermission() {
        return mReadPermission;
    }


    public final String getWritePermission() {
        return mWritePermission;
    }
    protected final void setWritePermission(String permission) {
        mWritePermission = permission;
    }

    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter) {
        return null;
    }

    @Override
    public abstract String getType(@NonNull Uri uri);
    
    public abstract Uri insert(@NonNull Uri uri, ContentValues values);

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values,
            Bundle extras) {
        return insert(uri, values);
    }

    public void onCallingPackageChanged() {}

    public abstract boolean onCreate();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {}

    @Override
    public void onLowMemory() {}

    @Override
    public void onTrimMemory(int level) {}


    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        ParcelFileDescriptor fd = openFile(uri, mode);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode,
            CancellationSignal signal) throws FileNotFoundException {
        return openAssetFile(uri, mode);
    }

    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        throw new FileNotFoundException("No files supported by provider at "
                + uri);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode,
            CancellationSignal signal) throws FileNotFoundException {
        return openFile(uri, mode);
    }

    public @NonNull <T> ParcelFileDescriptor openPipeHelper(final @NonNull Uri uri,
            final @NonNull String mimeType, final Bundle opts, final T args,
            final @NonNull PipeDataWriter<T> func) throws FileNotFoundException {
        try {
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    func.writeDataToPipe(fds[1], uri, mimeType, opts, args);
                    try {
                        fds[1].close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failure closing pipe", e);
                    }
                }
            });

            return fds[0];
        } catch (IOException e) {
            throw new FileNotFoundException("failure making pipe");
        }
    }

    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
            @NonNull String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        if ("*/*".equals(mimeTypeFilter)) {
            // If they can take anything, the untyped open call is good enough.
            return openAssetFile(uri, "r");
        }
        String baseType = getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
            // Use old untyped open call if this provider has a type for this
            // URI and it matches the request.
            return openAssetFile(uri, "r");
        }
        throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
            @NonNull String mimeTypeFilter, Bundle opts,
            CancellationSignal signal) throws FileNotFoundException {
        return openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    public abstract Cursor query(@NonNull Uri uri, String[] projection,
            String selection, String[] selectionArgs,
            String sortOrder);

    public Cursor query(@NonNull Uri uri, String[] projection,
            String selection, String[] selectionArgs,
            String sortOrder, CancellationSignal cancellationSignal) {
        return query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
            Bundle queryArgs, CancellationSignal cancellationSignal) {
        queryArgs = queryArgs != null ? queryArgs : Bundle.EMPTY;

        // if client doesn't supply an SQL sort order argument, attempt to build one from
        // QUERY_ARG_SORT* arguments.
        String sortClause = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
        if (sortClause == null && queryArgs.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
            sortClause = ContentResolver.createSqlSortClause(queryArgs);
        }

        return query(
                uri,
                projection,
                queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
                queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS),
                sortClause,
                cancellationSignal);
    }


    @Override
    public boolean refresh(Uri uri, Bundle extras, CancellationSignal cancellationSignal) {
        return false;
    }


    @NonNull
    public final Context requireContext() {
        final Context ctx = getContext();
        if (ctx == null) {
            throw new IllegalStateException("Cannot find context from the provider.");
        }
        return ctx;
    }

    public void shutdown() {
        Log.w(TAG, "implement ContentProvider shutdown() to make sure all database " +
                "connections are gracefully shutdown");
    }

    /**
     * Set the calling package/feature, returning the current value (or {@code null})
     * which can be used later to restore the previous state.
     */
    private AttributionSource setCallingAttributionSource(
            AttributionSource attributionSource) {
        final AttributionSource original = mCallingAttributionSource.get();
        mCallingAttributionSource.set(attributionSource);
        onCallingPackageChanged();
        return original;
    }

    

    /**
     * @removed
     */
    @Deprecated
    public final String getCallingFeatureId() {
        return getCallingAttributionTag();
    }
    

    /**
     * Opaque token representing the identity of an incoming IPC.
     */
    public final class CallingIdentity {
        /** {@hide} */
        public final long binderToken;
        /** {@hide} */
        public final AttributionSource callingAttributionSource;

        /** {@hide} */
        public CallingIdentity(long binderToken, AttributionSource attributionSource) {
            this.binderToken = binderToken;
            this.callingAttributionSource = attributionSource;
        }
    }


    /**
     * Change the authorities of the ContentProvider.
     * This is normally set for you from its manifest information when the provider is first
     * created.
     * @hide
     * @param authorities the semi-colon separated authorities of the ContentProvider.
     */
    protected final void setAuthorities(String authorities) {
        if (authorities != null) {
            if (authorities.indexOf(';') == -1) {
                mAuthority = authorities;
                mAuthorities = null;
            } else {
                mAuthority = null;
                mAuthorities = authorities.split(";");
            }
        }
    }

    /** @hide */
    protected final boolean matchesOurAuthorities(String authority) {
        if (mAuthority != null) {
            return mAuthority.equals(authority);
        }
        if (mAuthorities != null) {
            int length = mAuthorities.length;
            for (int i = 0; i < length; i++) {
                if (mAuthorities[i].equals(authority)) return true;
            }
        }
        return false;
    }
    

    /**
     * Perform a detailed internal check on a {@link Uri} to determine if a UID
     * is able to access it with specific mode flags.
     * <p>
     * This method is typically used when the provider implements more dynamic
     * access controls that cannot be expressed with {@code <path-permission>}
     * style static rules.
     * <p>
     * Because validation of these dynamic access controls has significant
     * system health impact, this feature is only available to providers that
     * are built into the system.
     *
     * @param uri the {@link Uri} to perform an access check on.
     * @param uid the UID to check the permission for.
     * @param modeFlags the access flags to use for the access check, such as
     *            {@link Intent#FLAG_GRANT_READ_URI_PERMISSION}.
     * @return {@link PackageManager#PERMISSION_GRANTED} if access is allowed,
     *         otherwise {@link PackageManager#PERMISSION_DENIED}.
     * @hide
     */
    @Override
    @SystemApi
    public int checkUriPermission(@NonNull Uri uri, int uid, @Intent.AccessUriMode int modeFlags) {
        return PackageManager.PERMISSION_DENIED;
    }

    /**
     * @hide
     * Implementation when a caller has performed an insert on the content
     * provider, but that call has been rejected for the operation given
     * to {@link #setAppOps(int, int)}.  The default implementation simply
     * returns a URI that is the base URI with a 0 path element appended.
     */
    public Uri rejectInsert(Uri uri, ContentValues values) {
        // If not allowed, we need to return some reasonable URI.  Maybe the
        // content provider should be responsible for this, but for now we
        // will just return the base URI with a '0' tagged on to it.
        // You shouldn't be able to read if you can't write, anyway, so it
        // shouldn't matter much what is returned.
        return uri.buildUpon().appendPath("0").build();
    }

    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
    
    @Override
    public int update(@NonNull Uri uri, ContentValues values,
            Bundle extras) {
        extras = (extras != null) ? extras : Bundle.EMPTY;
        return update(uri, values,
                extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
                extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
    }

    /**
     * Convenience for subclasses that wish to implement {@link #openFile}
     * by looking up a column named "_data" at the given URI.
     *
     * @param uri The URI to be opened.
     * @param mode The string representation of the file mode. Can be "r", "w", "wt", "wa", "rw"
     *             or "rwt". See{@link ParcelFileDescriptor#parseMode} for more details.
     *
     * @return Returns a new ParcelFileDescriptor that can be used by the
     * client to access the file.
     */
    protected final @NonNull ParcelFileDescriptor openFileHelper(@NonNull Uri uri,
            @NonNull String mode) throws FileNotFoundException {
        Cursor c = query(uri, new String[]{"_data"}, null, null, null);
        int count = (c != null) ? c.getCount() : 0;
        if (count != 1) {
            // If there is not exactly one result, throw an appropriate
            // exception.
            if (c != null) {
                c.close();
            }
            if (count == 0) {
                throw new FileNotFoundException("No entry for " + uri);
            }
            throw new FileNotFoundException("Multiple items at " + uri);
        }

        c.moveToFirst();
        int i = c.getColumnIndex("_data");
        String path = (i >= 0 ? c.getString(i) : null);
        c.close();
        if (path == null) {
            throw new FileNotFoundException("Column _data not found.");
        }

        int modeBits = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(new File(path), modeBits);
    }
    


    public interface PipeDataWriter<T> {
        public void writeDataToPipe(@NonNull ParcelFileDescriptor output, @NonNull Uri uri,
                @NonNull String mimeType, Bundle opts, T args);
    }


    protected boolean isTemporary() {
        return false;
    }
    

    private void validateIncomingAuthority(String authority) throws SecurityException {
        if (!matchesOurAuthorities(getAuthorityWithoutUserId(authority))) {
            String message = "The authority " + authority + " does not match the one of the "
                    + "contentProvider: ";
            if (mAuthority != null) {
                message += mAuthority;
            } else {
                message += Arrays.toString(mAuthorities);
            }
            throw new SecurityException(message);
        }
    }

    /** @hide */
    public Uri validateIncomingUri(Uri uri) throws SecurityException {
        String auth = uri.getAuthority();
        if (!mSingleUser) {
            int userId = getUserIdFromAuthority(auth, UserHandle.USER_CURRENT);
            if (userId != UserHandle.USER_CURRENT && userId != mContext.getUserId()) {
                throw new SecurityException("trying to query a ContentProvider in user "
                        + mContext.getUserId() + " with a uri belonging to user " + userId);
            }
        }
        validateIncomingAuthority(auth);

        // Normalize the path by removing any empty path segments, which can be
        // a source of security issues.
        final String encodedPath = uri.getEncodedPath();
        if (encodedPath != null && encodedPath.indexOf("//") != -1) {
            final Uri normalized = uri.buildUpon()
                    .encodedPath(encodedPath.replaceAll("//+", "/")).build();
            Log.w(TAG, "Normalized " + uri + " to " + normalized
                    + " to avoid possible security issues");
            return normalized;
        } else {
            return uri;
        }
    }

    /** @hide */
    private Uri maybeGetUriWithoutUserId(Uri uri) {
        if (mSingleUser) {
            return uri;
        }
        return getUriWithoutUserId(uri);
    }

    /** @hide */
    public static int getUserIdFromAuthority(String auth, int defaultUserId) {
        if (auth == null) return defaultUserId;
        int end = auth.lastIndexOf('@');
        if (end == -1) return defaultUserId;
        String userIdString = auth.substring(0, end);
        try {
            return Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Error parsing userId.", e);
            return UserHandle.USER_NULL;
        }
    }

    /** @hide */
    public static int getUserIdFromAuthority(String auth) {
        return getUserIdFromAuthority(auth, UserHandle.USER_CURRENT);
    }

    /** @hide */
    public static int getUserIdFromUri(Uri uri, int defaultUserId) {
        if (uri == null) return defaultUserId;
        return getUserIdFromAuthority(uri.getAuthority(), defaultUserId);
    }

    /** @hide */
    public static int getUserIdFromUri(Uri uri) {
        return getUserIdFromUri(uri, UserHandle.USER_CURRENT);
    }

    /**
     * Returns the user associated with the given URI.
     *
     * @hide
     */
    @TestApi
    public @NonNull static UserHandle getUserHandleFromUri(@NonNull Uri uri) {
        return UserHandle.of(getUserIdFromUri(uri, Process.myUserHandle().getIdentifier()));
    }

    /**
     * Removes userId part from authority string. Expects format:
     * userId@some.authority
     * If there is no userId in the authority, it symply returns the argument
     * @hide
     */
    public static String getAuthorityWithoutUserId(String auth) {
        if (auth == null) return null;
        int end = auth.lastIndexOf('@');
        return auth.substring(end+1);
    }

    /** @hide */
    public static Uri getUriWithoutUserId(Uri uri) {
        if (uri == null) return null;
        Uri.Builder builder = uri.buildUpon();
        builder.authority(getAuthorityWithoutUserId(uri.getAuthority()));
        return builder.build();
    }

    /** @hide */
    public static boolean uriHasUserId(Uri uri) {
        if (uri == null) return false;
        return !TextUtils.isEmpty(uri.getUserInfo());
    }

    /**
     * Returns the given content URI explicitly associated with the given {@link UserHandle}.
     *
     * @param contentUri The content URI to be associated with a user handle.
     * @param userHandle The user handle with which to associate the URI.
     *
     * @throws IllegalArgumentException if
     * <ul>
     *  <li>the given URI is not content URI (a content URI has {@link Uri#getScheme} equal to
     *  {@link ContentResolver.SCHEME_CONTENT}) or</li>
     *  <li>the given URI is already explicitly associated with a {@link UserHandle}, which is
     *  different than the given one.</li>
     *  </ul>
     *
     * @hide
     */
    @NonNull
    public static Uri createContentUriForUser(
            @NonNull Uri contentUri, @NonNull UserHandle userHandle) {
        if (!ContentResolver.SCHEME_CONTENT.equals(contentUri.getScheme())) {
            throw new IllegalArgumentException(String.format(
                "Given URI [%s] is not a content URI: ", contentUri));
        }

        int userId = userHandle.getIdentifier();
        if (uriHasUserId(contentUri)) {
            if (String.valueOf(userId).equals(contentUri.getUserInfo())) {
                return contentUri;
            }
            throw new IllegalArgumentException(String.format(
                "Given URI [%s] already has a user ID, different from given user handle [%s]",
                contentUri,
                userId));
        }

        Uri.Builder builder = contentUri.buildUpon();
        builder.encodedAuthority(
                "" + userHandle.getIdentifier() + "@" + contentUri.getEncodedAuthority());
        return builder.build();
    }

    /** @hide */
    public static Uri maybeAddUserId(Uri uri, int userId) {
        if (uri == null) return null;
        if (userId != UserHandle.USER_CURRENT
                && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            if (!uriHasUserId(uri)) {
                //We don't add the user Id if there's already one
                Uri.Builder builder = uri.buildUpon();
                builder.encodedAuthority("" + userId + "@" + uri.getEncodedAuthority());
                return builder.build();
            }
        }
        return uri;
    }

    private static void traceBegin(long traceTag, String methodName, String subInfo) {
        if (Trace.isTagEnabled(traceTag)) {
            Trace.traceBegin(traceTag, methodName + subInfo);
        }
    }


    // native entries
    void applyBatch(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        String auth = data.readString();
        int length = data.readInt();
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (int i=0;i<length;i++){
            ops.add(ContentProviderOperation.CREATOR.createFromParcel(data));
        }
        try{
            ContentProviderResult[] re = applyBatch(auth, ops);
            reply.writeBoolean(true);
            reply.writeInt(re.length);
            for (ContentProviderResult r:re){
                r.writeToParcel(reply, 0);
            }
        } catch(OperationApplicationException e){
            reply.writeBoolean(false);
            reply.writeString(e.toString());
        }
        
    }

    int bulkInsert(Parcel data){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }

        Uri uri = Uri.CREATOR.createFromParcel(data);
        int length = data.readInt();
        ContentValues[] vals = new ContentValues[length];
        for (int i=0;i<length;i++){
            vals[i] = ContentValues.CREATOR.createFromParcel(data);
        }
        return bulkInsert(uri, vals);
    }

    void call(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Bundle b = call(data.readString(), data.readString(), data.readString(), data.readBundle());
        reply.writeBundle(b);
    }

    void canonicalize(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = canonicalize(Uri.CREATOR.createFromParcel(data));
        uri.writeToParcel(reply, 0);
    }

    void uncanonicalize(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = uncanonicalize(Uri.CREATOR.createFromParcel(data));
        uri.writeToParcel(reply, 0);
    }

    int delete(Parcel data){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        Bundle b = data.readBundle();
        return delete(uri, b);
    }

    void getStreamTypes(Parcel data, Parcel reply){
        Uri uri = Uri.CREATOR.createFromParcel(data);
        String filter = data.readString();
        String[] re = getStreamTypes(uri, filter);
        reply.writeInt(re.length);
        for (String v:re){
            reply.writeString(v);
        }
    }

    String getType(String uri){
        return getType(Uri.parse(uri));
    }

    void insert(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        ContentValues values = ContentValues.CREATOR.createFromParcel(data);

        insert(uri, values, data.readBundle()).writeToParcel(reply, 0);
    }

    void openFile(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        String mode = data.readString();
        try{
            ParcelFileDescriptor p= openFile(uri, mode);
            reply.writeBoolean(true);
            p.writeToParcel(reply, 0);
        } catch (FileNotFoundException e){
            reply.writeBoolean(false);
            reply.writeString(e.toString());
        }
    }

    void openAssetFile(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        String mode = data.readString();
        try{
            AssetFileDescriptor p= openAssetFile(uri, mode);
            reply.writeBoolean(true);
            p.writeToParcel(reply, 0);
        } catch (FileNotFoundException e){
            reply.writeBoolean(false);
            reply.writeString(e.toString());
        }
    }

    void openTypedAssetFile(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        String filter = data.readString();
        Bundle opts = data.readBundle();
        try{
            AssetFileDescriptor p= openTypedAssetFile(uri, filter, opts);
            reply.writeBoolean(true);
            p.writeToParcel(reply, 0);
        } catch (FileNotFoundException e){
            reply.writeBoolean(false);
            reply.writeString(e.toString());
        }
    }

    void query(Parcel data, Parcel reply){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        int length = data.readInt();
        String[] proj = new String[length];
        for(int i=0;i<length;i++){
            proj[i] = data.readString();
        }
        Bundle b = data.readBundle();
        query(uri, proj, b, null).writeToParcel(reply, 0);
    }

    boolean refresh(Parcel data){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        Bundle b = data.readBundle();
        return refresh(uri, b, null);
    }

    int update(Parcel data){
        AttributionSource src = AttributionSource.CREATOR.createFromParcel(data);
        if (!(mCallingAttributionSource.get() == src)){
            setCallingAttributionSource(src);
        }
        Uri uri = Uri.CREATOR.createFromParcel(data);
        ContentValues values = ContentValues.CREATOR.createFromParcel(data);
        Bundle extras = data.readBundle();
        return update(uri, values, extras);
    }

    int checkUriPermission(Parcel data){
        Uri uri = Uri.CREATOR.createFromParcel(data);
        int uid = data.readInt();
        int modeFlags = data.readInt();
        return checkUriPermission(uri, uid, modeFlags);
    }
}
