/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemService;
import android.content.Context;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The HardwarePropertiesManager class provides a mechanism of accessing hardware state of a
 * device: CPU, GPU and battery temperatures, CPU usage per core, fan speed, etc.
 */
@SystemService(Context.HARDWARE_PROPERTIES_SERVICE)
public class HardwarePropertiesManager {

    private static final String TAG = HardwarePropertiesManager.class.getSimpleName();

    /**
     * Device temperature types.
     */
    // These constants are also defined in android/os/enums.proto.
    // Any change to the types here or in the thermal hal should be made in the proto as well.
    /** Temperature of CPUs in Celsius. */
    public static final int DEVICE_TEMPERATURE_CPU = 0;

    /** Temperature of GPUs in Celsius. */
    public static final int DEVICE_TEMPERATURE_GPU = 1;

    /** Temperature of battery in Celsius. */
    public static final int DEVICE_TEMPERATURE_BATTERY = 2;

    /** Temperature of device skin in Celsius. */
    public static final int DEVICE_TEMPERATURE_SKIN = 3;

    /** Get current temperature. */
    public static final int TEMPERATURE_CURRENT = 0;

    /** Get throttling temperature threshold. */
    public static final int TEMPERATURE_THROTTLING = 1;

    /** Get shutdown temperature threshold. */
    public static final int TEMPERATURE_SHUTDOWN = 2;

    /**
     * Get throttling temperature threshold above which minimum clockrates for VR mode will not be
     * met.
     */
    public static final int TEMPERATURE_THROTTLING_BELOW_VR_MIN = 3;

    /** Undefined temperature constant. */
    public static final float UNDEFINED_TEMPERATURE = -Float.MAX_VALUE;

    /** Calling app context. */
    private final Context mContext;

    /** @hide */
    public HardwarePropertiesManager(Context context) {
        mContext = context;
    }

    public @NonNull float[] getDeviceTemperatures(int type, int source) {
        switch (type) {
            case DEVICE_TEMPERATURE_CPU:
            case DEVICE_TEMPERATURE_GPU:
            case DEVICE_TEMPERATURE_BATTERY:
            case DEVICE_TEMPERATURE_SKIN:
                switch (source) {
                    case TEMPERATURE_CURRENT:
                    case TEMPERATURE_THROTTLING:
                    case TEMPERATURE_SHUTDOWN:
                    case TEMPERATURE_THROTTLING_BELOW_VR_MIN:
                        try {
                            return new float[0];
                        } catch (Exception e) {
                            //throw e.rethrowFromSystemServer();
                        }
                    default:
                        Log.w(TAG, "Unknown device temperature source.");
                        return new float[0];
                }
            default:
                Log.w(TAG, "Unknown device temperature type.");
                return new float[0];
        }
    }

    public @NonNull CpuUsageInfo[] getCpuUsages() {
        try {
            return new CpuUsageInfo[0];
        } catch (Exception e) {
            //throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return an array of fan speeds in RPM.
     *
     * @return an array of float fan speeds in RPM. Empty if there are no fans or fan speed is not
     * supported on this system.
     *
     * @throws SecurityException if something other than the device owner or the current VR service
     *         tries to retrieve information provided by this service.
     */
    public @NonNull float[] getFanSpeeds() {
        try {
            return new float[0];
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
