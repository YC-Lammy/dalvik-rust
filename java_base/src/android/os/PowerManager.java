package android.os;

import java.time.Duration;
import java.util.concurrent.Executor;

public final class PowerManager{
    

    public static final String ACTION_DEVICE_IDLE_MODE_CHANGED = "android.os.action.DEVICE_IDLE_MODE_CHANGED";
    public static final String ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED = "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED";
    public static final String ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED = "android.os.action.LOW_POWER_STANDBY_ENABLED_CHANGED";
    public static final String ACTION_POWER_SAVE_MODE_CHANGED =  "android.os.action.POWER_SAVE_MODE_CHANGED";

    public static final int LOCATION_MODE_NO_CHANGE = 0;
    public static final int LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF = 1;
    public static final int LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF = 2;
    public static final int LOCATION_MODE_FOREGROUND_ONLY = 3;
    public static final int LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF = 4;

    // wake lock level
    public static final int FULL_WAKE_LOCK = 0x0000001a;
    public static final int PARTIAL_WAKE_LOCK = 1;
    public static final int SCREEN_DIM_WAKE_LOCK = 6;
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 10;
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;

    // wake lock release flags
    public static final int RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY = 1;
    
    // wake lock flags
    public static final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;
    public static final int ON_AFTER_RELEASE = 0x20000000;

    // thermal status code
    public static final int THERMAL_STATUS_NONE = 0;
    public static final int THERMAL_STATUS_LIGHT = 1;
    public static final int THERMAL_STATUS_MODERATE = 2;
    public static final int THERMAL_STATUS_SEVERE = 3;
    public static final int THERMAL_STATUS_CRITICAL = 4;
    public static final int THERMAL_STATUS_EMERGENCY = 5;
    public static final int THERMAL_STATUS_SHUTDOWN = 6;

    public static interface OnThermalStatusChangedListener {
        public abstract void onThermalStatusChanged (int status);
    }

    // for now wake lock is just a dummy, since the actual api is DE dependent.
    public final class WakeLock{

        private boolean mIsHeld;
        private boolean mIsReferenceCounted;

        private Executor mStateListenerExecutor;
        private WakeLockStateListener mStateListener;
        private long mRC; // the reference count

        WakeLock(){
            mIsHeld = false;
            mIsReferenceCounted = true;
            mStateListener = null;
            mStateListenerExecutor = null;
            mRC = 0;
        }

        public void aquire(){

        }

        public void aquire(long timeout){

        }

        public void release(){
            release(0);
        }

        public void release(int flags){
            // we ignore the flag, 
            // we have no proximity sensor anyway

            if (mIsReferenceCounted){
                mRC -= 1;
                if (mRC == 0){
                    // release
                }
            } else{
                mRC = 0;
                // release
            }
        }

        public boolean isHeld(){
            return mIsHeld;
        }

        public void setReferenceCounted (boolean value){
            mIsReferenceCounted = value;
        }

        public void setStateListener (Executor executor, WakeLockStateListener listener){

        }

        public void setWorkSource (WorkSource ws){
            // ignore
        }

        @Override
        public String toString() {
            return getClass().getName() + '@' + Integer.toHexString(hashCode());
        }

        @Override
        protected void finalize() throws Throwable {
            
        }
    }

    public static interface WakeLockStateListener{
        public abstract void onStateChanged (boolean enabled);
    } 

    public WakeLock newWakeLock (int levelAndFlags, String tag){
        return new WakeLock();
    }

    public void addThermalStatusListener (Executor executor, OnThermalStatusChangedListener listener){
        // dummy
    }

    public void addThermalStatusListener(OnThermalStatusChangedListener listener){
        // dummy
    }

    public Duration getBatteryDischargePrediction(){
        return null;
    }

    public int getCurrentThermalStatus (){
        return THERMAL_STATUS_NONE;
    }

    public int getLocationPowerSaveMode (){
        return LOCATION_MODE_NO_CHANGE;
    }

    public float getThermalHeadroom (int forecastSeconds){
        return 0;
    }

    public boolean isBatteryDischargePredictionPersonalized (){
        return false;
    }

    public boolean isDeviceIdleMode (){
        return false;
    }

    public boolean isDeviceLightIdleMode (){
        return false;
    }

    public boolean isIgnoringBatteryOptimizations (String packageName){
        return false;
    }

    public boolean isInteractive (){
        return true;
    }

    public boolean isLowPowerStandbyEnabled (){
        return false;
    }

    public boolean isPowerSaveMode (){
        return false;
    }

    public boolean isRebootingUserspaceSupported (){
        return false;
    }

    public boolean isScreenOn (){
        return true;
    }

    public boolean isSustainedPerformanceModeSupported (){
        return false;
    }

    public boolean isWakeLockLevelSupported (int level){
        return true;
    }

    public void reboot (String reason){
        throw new UnsupportedOperationException("userspace reboot not supported.");
    }

    public void removeThermalStatusListener (OnThermalStatusChangedListener listener){
        // dummy
    }
}