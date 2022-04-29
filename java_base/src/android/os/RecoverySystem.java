package android.os;

import java.io.File;

import android.content.Context;

public class RecoverySystem{
    public static interface ProgressListener{
        abstract void onProgress(int progress);
    }

    public static void installPackage(Context context, File packageFile) {
        // dummy
    }

    public static void rebootWipeCache(Context context){
        // dummy
    } 

    public static void rebootWipeUserData(Context context){
        // dummy
    }

    public static void verifyPackage(File packageFile, RecoverySystem.ProgressListener listener, File deviceCertsZipFile){
        // dummy
    }
}