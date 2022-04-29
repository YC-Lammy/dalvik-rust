package Fuse;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.TimerTask;

import obbstorage.fat32.FsDirectory;
import obbstorage.fat32.FsFile;

public class Fuse {
    public static final String MountPoint = "/mnt/android_fuse_"+Long.toString(System.currentTimeMillis())+"/";

    public static Fuse get_instance(){
        return new Fuse();
    }

    static HashMap<String, FsFile> mFiles = new HashMap<String, FsFile>();
    static HashMap<String, FsDirectory> mDirectories = new HashMap<String, FsDirectory>();

    public String mountPoint(){
        return MountPoint;
    }

    public Path mountFile(String name, FsFile file){
        return Path.of(MountPoint, name);
    }

    public Path mountDirectory(String name, FsDirectory dir){
        return Path.of(MountPoint, name);
    }

    public void unmountDirectory(String name){
        mDirectories.remove(name);
    }

    public void unmountFile(String name){
        mFiles.remove(name);
    }
}
