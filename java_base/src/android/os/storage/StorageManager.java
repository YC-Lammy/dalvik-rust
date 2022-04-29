/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.os.storage;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.TestApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ObbInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.ProxyFileDescriptorCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.os.UserHandle;
import android.os.ParcelFileDescriptor.OnCloseListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DataUnit;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;

import android.util.internal.Preconditions;
import obbstorage.fat32.FsFile;
import obbstorage.fat32.ReadOnlyException;
import obbstorage.obbstorage.ObbFile;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.security.MessageDigest;

/**
 * StorageManager is the interface to the systems storage service. The storage
 * manager handles storage-related items such as Opaque Binary Blobs (OBBs).
 * <p>
 * OBBs contain a filesystem that maybe be encrypted on disk and mounted
 * on-demand from an application. OBBs are a good way of providing large amounts
 * of binary assets without packaging them into APKs as they may be multiple
 * gigabytes in size. However, due to their size, they're most likely stored in
 * a shared storage pool accessible from all programs. The system does not
 * guarantee the security of the OBB file itself: if any program modifies the
 * OBB, there is no guarantee that a read from that OBB will produce the
 * expected output.
 */

public class StorageManager {
    private static final String TAG = "StorageManager";
    private static final boolean LOCAL_LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    /** {@hide} */
    public static final String PROP_PRIMARY_PHYSICAL = "ro.vold.primary_physical";
    /** {@hide} */
    public static final String PROP_HAS_ADOPTABLE = "vold.has_adoptable";
    /** {@hide} */
    public static final String PROP_HAS_RESERVED = "vold.has_reserved";
    /** {@hide} */
    public static final String PROP_ADOPTABLE = "persist.sys.adoptable";
    /** {@hide} */
    public static final String PROP_EMULATE_FBE = "persist.sys.emulate_fbe";
    /** {@hide} */
    public static final String PROP_SDCARDFS = "persist.sys.sdcardfs";
    /** {@hide} */
    public static final String PROP_VIRTUAL_DISK = "persist.sys.virtual_disk";
    /** {@hide} */
    public static final String PROP_FORCED_SCOPED_STORAGE_WHITELIST =
            "forced_scoped_storage_whitelist";

    /** {@hide} */
    public static final String UUID_PRIVATE_INTERNAL = null;
    /** {@hide} */
    public static final String UUID_PRIMARY_PHYSICAL = "primary_physical";
    /** {@hide} */
    public static final String UUID_SYSTEM = "system";

    // NOTE: UUID constants below are namespaced
    // uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad default
    // uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad primary_physical
    // uuid -v5 ad99aa3d-308e-4191-a200-ebcab371c0ad system

    /**
     * UUID representing the default internal storage of this device which
     * provides {@link Environment#getDataDirectory()}.
     * <p>
     * This value is constant across all devices and it will never change, and
     * thus it cannot be used to uniquely identify a particular physical device.
     *
     * @see #getUuidForPath(File)
     * @see ApplicationInfo#storageUuid
     */
    public static final UUID UUID_DEFAULT = staticGetUuidForPath(android.os.Environment.getDataDirectory());

    /**
     * Activity Action: Allows the user to manage their storage. This activity
     * provides the ability to free up space on the device by deleting data such
     * as apps.
     * <p>
     * If the sending application has a specific storage device or allocation
     * size in mind, they can optionally define {@link #EXTRA_UUID} or
     * {@link #EXTRA_REQUESTED_BYTES}, respectively.
     * <p>
     * This intent should be launched using
     * {@link Activity#startActivityForResult(Intent, int)} so that the user
     * knows which app is requesting the storage space. The returned result will
     * be {@link Activity#RESULT_OK} if the requested space was made available,
     * or {@link Activity#RESULT_CANCELED} otherwise.
     */
    
    public static final String ACTION_MANAGE_STORAGE = "android.os.storage.action.MANAGE_STORAGE";

    /**
     * Activity Action: Allows the user to free up space by clearing app external cache directories.
     * The intent doesn't automatically clear cache, but shows a dialog and lets the user decide.
     * <p>
     * This intent should be launched using
     * {@link Activity#startActivityForResult(Intent, int)} so that the user
     * knows which app is requesting to clear cache. The returned result will be:
     * {@link Activity#RESULT_OK} if the activity was launched and all cache was cleared,
     * {@link OsConstants#EIO} if an error occurred while clearing the cache or
     * {@link Activity#RESULT_CANCELED} otherwise.
     */
    
    
    public static final String ACTION_CLEAR_APP_CACHE = "android.os.storage.action.CLEAR_APP_CACHE";

    /**
     * Extra {@link UUID} used to indicate the storage volume where an
     * application is interested in allocating or managing disk space.
     *
     * @see #ACTION_MANAGE_STORAGE
     * @see #UUID_DEFAULT
     * @see #getUuidForPath(File)
     * @see Intent#putExtra(String, java.io.Serializable)
     */
    public static final String EXTRA_UUID = "android.os.storage.extra.UUID";

    /**
     * Extra used to indicate the total size (in bytes) that an application is
     * interested in allocating.
     * <p>
     * When defined, the management UI will help guide the user to free up
     * enough disk space to reach this requested value.
     *
     * @see #ACTION_MANAGE_STORAGE
     */
    public static final String EXTRA_REQUESTED_BYTES = "android.os.storage.extra.REQUESTED_BYTES";

    /** {@hide} */
    public static final int DEBUG_ADOPTABLE_FORCE_ON = 1 << 0;
    /** {@hide} */
    public static final int DEBUG_ADOPTABLE_FORCE_OFF = 1 << 1;
    /** {@hide} */
    public static final int DEBUG_EMULATE_FBE = 1 << 2;
    /** {@hide} */
    public static final int DEBUG_SDCARDFS_FORCE_ON = 1 << 3;
    /** {@hide} */
    public static final int DEBUG_SDCARDFS_FORCE_OFF = 1 << 4;
    /** {@hide} */
    public static final int DEBUG_VIRTUAL_DISK = 1 << 5;

    /** {@hide} */
    public static final int FLAG_FOR_WRITE = 1 << 8;
    /** {@hide} */
    public static final int FLAG_REAL_STATE = 1 << 9;
    /** {@hide} */
    public static final int FLAG_INCLUDE_INVISIBLE = 1 << 10;
    /** {@hide} */
    public static final int FLAG_INCLUDE_RECENT = 1 << 11;

    /** @hide The volume is not encrypted. */
    
    public static final int ENCRYPTION_STATE_NONE = 1;

    // Project IDs below must match android_projectid_config.h
    /**
     * Default project ID for files on external storage
     *
     * {@hide}
     */
    public static final int PROJECT_ID_EXT_DEFAULT = 1000;

    /**
     * project ID for audio files on external storage
     *
     * {@hide}
     */
    public static final int PROJECT_ID_EXT_MEDIA_AUDIO = 1001;

    /**
     * project ID for video files on external storage
     *
     * {@hide}
     */
    public static final int PROJECT_ID_EXT_MEDIA_VIDEO = 1002;

    /**
     * project ID for image files on external storage
     *
     * {@hide}
     */
    public static final int PROJECT_ID_EXT_MEDIA_IMAGE = 1003;

    /**
     * Constant for use with
     * {@link #updateExternalStorageFileQuotaType(String, int)} (String, int)}, to indicate the file
     * is not a media file.
     *
     * @hide
     */
    
    public static final int QUOTA_TYPE_MEDIA_NONE = 0;

    /**
     * Constant for use with
     * {@link #updateExternalStorageFileQuotaType(String, int)} (String, int)}, to indicate the file
     * is an image file.
     *
     * @hide
     */
    
    public static final int QUOTA_TYPE_MEDIA_IMAGE = 1;

    /**
     * Constant for use with
     * {@link #updateExternalStorageFileQuotaType(String, int)} (String, int)}, to indicate the file
     * is an audio file.
     *
     * @hide
     */
    
    public static final int QUOTA_TYPE_MEDIA_AUDIO = 2;

    /**
     * Constant for use with
     * {@link #updateExternalStorageFileQuotaType(String, int)} (String, int)}, to indicate the file
     * is a video file.
     *
     * @hide
     */
    
    public static final int QUOTA_TYPE_MEDIA_VIDEO = 3;

    /**
     * Reason to provide if app IO is blocked/resumed for unknown reasons
     *
     * @hide
     */
    
    public static final int APP_IO_BLOCKED_REASON_UNKNOWN = 0;

    /**
     * Reason to provide if app IO is blocked/resumed because of transcoding
     *
     * @hide
     */
    
    public static final int APP_IO_BLOCKED_REASON_TRANSCODING = 1;


    /**
     * Flag indicating that a disk space allocation request should operate in an
     * aggressive mode. This flag should only be rarely used in situations that
     * are critical to system health or security.
     * <p>
     * When set, the system is more aggressive about the data that it considers
     * for possible deletion when allocating disk space.
     * <p class="note">
     * Note: your app must hold the
     * {@link android.Manifest.permission#ALLOCATE_AGGRESSIVE} permission for
     * this flag to take effect.
     * </p>
     *
     * @see #getAllocatableBytes(UUID, int)
     * @see #allocateBytes(UUID, long, int)
     * @see #allocateBytes(FileDescriptor, long, int)
     * @hide
     */
    
    
    public static final int FLAG_ALLOCATE_AGGRESSIVE = 1 << 0;

    /**
     * Flag indicating that a disk space allocation request should be allowed to
     * clear up to all reserved disk space.
     *
     * @hide
     */
    public static final int FLAG_ALLOCATE_DEFY_ALL_RESERVED = 1 << 1;

    /**
     * Flag indicating that a disk space allocation request should be allowed to
     * clear up to half of all reserved disk space.
     *
     * @hide
     */
    public static final int FLAG_ALLOCATE_DEFY_HALF_RESERVED = 1 << 2;

    /**
     * Flag indicating that a disk space check should not take into account
     * freeable cached space when determining allocatable space.
     *
     * Intended for use with {@link #getAllocatableBytes()}.
     * @hide
     */
    public static final int FLAG_ALLOCATE_NON_CACHE_ONLY = 1 << 3;

    /**
     * Flag indicating that a disk space check should only return freeable
     * cached space when determining allocatable space.
     *
     * Intended for use with {@link #getAllocatableBytes()}.
     * @hide
     */
    public static final int FLAG_ALLOCATE_CACHE_ONLY = 1 << 4;

    public static final int CRYPT_TYPE_PASSWORD = 0;
    /** @hide */
    
    public static final int CRYPT_TYPE_DEFAULT = 1;

    private static HashMap<UUID, String> uuid_map = new HashMap<UUID, String>();
    
    public static UUID staticGetUuidForPath(File path){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = md.digest(path.getAbsolutePath().getBytes());
            long msb = 0;
            long lsb = 0;
            for (int i=0; i<8; i++)
                msb = (msb << 8) | (data[i] & 0xff);
            for (int i=8; i<16; i++)
                lsb = (lsb << 8) | (data[i] & 0xff);

            UUID u = new UUID(msb, lsb);
            uuid_map.put(u, path.getAbsolutePath());
            return u;

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private Context mContext;

    public UUID getUuidForPath(File path){
        return staticGetUuidForPath(path);
    }

    public void allocateBytes(FileDescriptor fd, long bytes) throws IOException{
        //todo
    }

    public void allocateBytes(UUID storageUuid, long bytes) throws IOException{
        //todo
    }

    public long getAllocatableBytes(UUID storageUuid) throws IOException{
        return 2147483647;
    }

    public long getCacheQuotaBytes(UUID storageUuid) throws IOException{
        return 0;
    }

    private long getDirSize(File f){
        long length = 0;
        for (File fi:f.listFiles()){
            if (fi.isFile()){
                length += fi.length();
            } else{
                length += getDirSize(fi);
            }
        }
        return length;
    }

    public long getCacheSizeBytes(UUID storageUuid) throws IOException{
        return getDirSize(mContext.getCacheDir()) + getDirSize(mContext.getCodeCacheDir()) + getDirSize(mContext.getExternalCacheDir());
    }

    public PendingIntent getManageSpaceActivityIntent (String packageName, int requestCode) throws IllegalArgumentException{
        //todo
        return null;
    }

    public StorageVolume getPrimaryStorageVolume(){
        // todo
        File root = File.listRoots()[0];
        
        return new StorageVolume(
            "", 
            root, 
            root, 
            "", 
            true, 
            true, 
            false, 
            true,
            root.getTotalSpace(), 
            android.os.UserHandle.of(android.os.UserHandle.myUserId()), 
            staticGetUuidForPath(root), 
            "", 
            ""
        );
    }

    public List<StorageVolume> getRecentStorageVolumes(){
        //todo
        return List.of(
            getPrimaryStorageVolume()
        );
    }
    
    
    public static StorageVolume getStorageVolume(File file, int user){
        Path p = file.toPath().getRoot();
        
        return new StorageVolume(
            "", 
            p.toFile(), 
            p.toFile(), 
            "", 
            true, 
            true, 
            false, 
            true,
            p.toFile().getTotalSpace(), 
            android.os.UserHandle.of(android.os.UserHandle.myUserId()), 
            staticGetUuidForPath(p.toFile()), 
            "", 
            ""
        );
    }

    public StorageVolume getStorageVolume(File file){
        Path p = file.toPath().getRoot();
        
        return new StorageVolume(
            "", 
            p.toFile(), 
            p.toFile(), 
            "", 
            true, 
            true, 
            false, 
            true,
            p.toFile().getTotalSpace(), 
            android.os.UserHandle.of(android.os.UserHandle.myUserId()), 
            getUuidForPath(p.toFile()), 
            "", 
            ""
        );
    }

    

    public StorageVolume getStorageVolume(Uri uri){
        File p = new File(uri.getPath());
        return getStorageVolume(p);
    }

    public static StorageVolume[] getVolumeList(int user, int flag){
        ArrayList<StorageVolume> arr = new ArrayList<StorageVolume>();
        int i = 0;
        for (File f:File.listRoots()){
            arr.add(new StorageVolume(
                "", 
                f, 
                f, 
                "", 
                i==0, 
                f.canWrite(), 
                false, 
                true, 
                f.getTotalSpace(), 
                android.os.UserHandle.of(android.os.UserHandle.myUserId()), 
                staticGetUuidForPath(f),
                "", 
                ""
                ));
            i+=1;
        }
        return (StorageVolume[])arr.toArray();
    }

    public List<StorageVolume> getStorageVolumes(){
        //todo
        return Arrays.asList(getVolumeList(0,0));
    }

    public List<StorageVolume> getStorageVolumesIncludingSharedProfiles (){
        //todo
        return List.of(
            getPrimaryStorageVolume()
        );
    }

    public boolean isAllocationSupported (FileDescriptor fd){
        return true;
    }

    private HashSet<File> mCache_Groups = new HashSet<File>();

    public boolean isCacheBehaviorGroup (File path) throws IOException{
        return mCache_Groups.contains(path);
    }

    public void setCacheBehaviorGroup (File path, boolean group) throws IOException{
        if (group){
            mCache_Groups.add(path);
        } else{
            mCache_Groups.remove(path);
        }
        
    }


    private HashSet<File> mCache_Tombstones = new HashSet<File>();

    public boolean isCacheBehaviorTombstone (File path) throws IOException{
        return mCache_Tombstones.contains(path);
    }

    public void setCacheBehaviorTombstone (File path, boolean group) throws IOException{
        if (group){
            mCache_Tombstones.add(path);
        } else{
            mCache_Tombstones.remove(path);
        }
    }

    public boolean isCheckpointSupported (){
        return false;
    }

    public boolean isEncrypted (File file){
        return false;
    }

    /* 
        Since we may not be able to mount the FAT32 OBB file due to its footer format,
        As a workaround, we mount it on the file system using FUSE. 
    */

    private Map<Path, File> mMounted_OBBs = new HashMap<Path, File>();

    public String getMountedObbPath(String rawPath){
        return mMounted_OBBs.get(new File(rawPath).toPath().toAbsolutePath()).getAbsolutePath();
    }

    public boolean isObbMounted (String rawPath){
        try{
            return mMounted_OBBs.containsKey(new File(rawPath).toPath().toAbsolutePath());
        } catch (Exception e){
            return false;
        }
    }

    public boolean mountObb (String rawPath, String key, OnObbStateChangeListener listener){
        File file = new File(rawPath);
        Path p = file.toPath();
        if (mMounted_OBBs.containsKey(p)){
            listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_ALREADY_MOUNTED);
            return false;
        }
        try{
            // not a file, return imediately
            if (!file.isFile()){
                listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_COULD_NOT_MOUNT);
                return false;
            }

            File f = android.os.Environment.getDataDirectory();
            File mountDir = new File(f.getParentFile(), "mnt");
            File[] childs = mountDir.listFiles();
            File mountedOBB = null;
            for (File child:childs){
                if (child.getName() == file.getName()){
                    mountedOBB = child;
                    break;
                }
            }

            // the dumped directory exist
            if (mountedOBB != null){
                mMounted_OBBs.put(p.toAbsolutePath(), mountedOBB);
                listener.onObbStateChange(rawPath, OnObbStateChangeListener.MOUNTED);
                return true;
            } else{
                // mount the OBB to FUSE;
                Path mounted = obbstorage.ObbMountStorage.MountOBB(file, key);
                mMounted_OBBs.put(p.toAbsolutePath(), mounted.toFile());
                listener.onObbStateChange(rawPath, OnObbStateChangeListener.MOUNTED);
                return true;
            }
        } catch (Exception e){
            listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_COULD_NOT_MOUNT);
            return false;
        }
    }

    public boolean unmountObb(String rawPath, boolean force, OnObbStateChangeListener listener){
        try{
            File re = mMounted_OBBs.remove(new File(rawPath).toPath().toAbsolutePath());
            if (re == null){
                listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_NOT_MOUNTED);
                return true;
            } else{
                listener.onObbStateChange(rawPath, OnObbStateChangeListener.UNMOUNTED);
                return false;
            }
        } catch(Exception ignored){

        }
        listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_INTERNAL);
        return false;
    }


    /*
        the proxy File descriptor is achieved by using the FUSE service.
    */
    public  ParcelFileDescriptor openProxyFileDescriptor(int mode, ProxyFileDescriptorCallback callback, Handler handler) throws IOException{
        if (handler==null){
            handler = new Handler(Looper.getMainLooper());
        }

        String name = Long.toString(System.currentTimeMillis());

        Path p = Fuse.Fuse.get_instance().mountFile(name, new FsFile() {
            @Override
            public long getLength() {
                try{
                    return callback.onGetSize();
                }catch (Exception e){
                    return 0;
                }
            }

            @Override
            public void flush() throws IOException {
                try{
                    callback.onFsync();
                } catch(Exception e){
                    throw new IOException(e);
                }
            }

            @Override
            public void read(long offset, ByteBuffer dest) throws IOException {
                try{
                    callback.onRead(offset, dest.remaining(),dest.slice().array());
                } catch(Exception e){
                    throw new IOException(e);
                }   
            }

            @Override
            public void write(long offset, ByteBuffer src) throws ReadOnlyException, IOException {
                try{
                    callback.onWrite(offset, src.remaining(), src.slice().array());
                } catch(Exception e){
                    throw new IOException(e);
                }  
            }

            @Override
            public void setLength(long length) throws IOException {}

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public boolean isValid() {
                return true;
            }
        });
        
        return ParcelFileDescriptor.open(p.toFile(), mode, handler, new OnCloseListener() {
            @Override
            public void onClose(IOException e) {
                Fuse.Fuse.get_instance().unmountFile(name);
            }
        });
    }

    /////////////////////////////////////////////////////////////
    //       storage volume callback
    ////////////////////////////////////////////////////////////

    private ArrayList<StorageVolumeCallback> mStorageVolumeCallbacks = new ArrayList<StorageVolumeCallback>();

    public static class StorageVolumeCallback {
        protected Executor executor = null;
        public void onStateChanged(@NonNull StorageVolume volume) { }
    }

    public void registerStorageVolumeCallback(Executor executor, StorageVolumeCallback callback){
        callback.executor = executor;
        mStorageVolumeCallbacks.add(callback);
    }

    public void unregisterStorageVolumeCallback(StorageManager.StorageVolumeCallback callback){
        mStorageVolumeCallbacks.remove(callback);
    }
}