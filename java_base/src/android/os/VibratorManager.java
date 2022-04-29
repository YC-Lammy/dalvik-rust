/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;
import android.util.Log;

/**
 * Class that provides access to all vibrators from the device, as well as the ability to run them
 * in a synchronized fashion.
 * <p>
 * If your process exits, any vibration you started will stop.
 * </p>
 */
public abstract class VibratorManager {
    private static final String TAG = "VibratorManager";

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    protected VibratorManager() {

    }

    /**
     * List all available vibrator ids, returning a possible empty list.
     *
     * @return An array containing the ids of the vibrators available on the device.
     */
    @NonNull
    public abstract int[] getVibratorIds();

    /**
     * Retrieve a single vibrator by id.
     *
     * @param vibratorId The id of the vibrator to be retrieved.
     * @return The vibrator with given {@code vibratorId}, never null.
     */
    @NonNull
    public abstract Vibrator getVibrator(int vibratorId);

    /**
     * Returns the default Vibrator for the device.
     */
    @NonNull
    public abstract Vibrator getDefaultVibrator();

    /**
     * Configure an always-on haptics effect.
     *
     * @hide
     */
    public boolean setAlwaysOnEffect(int uid, String opPkg, int alwaysOnId,
            @Nullable CombinedVibration effect, @Nullable VibrationAttributes attributes) {
        Log.w(TAG, "Always-on effects aren't supported");
        return false;
    }

    /**
     * Vibrate with a given combination of effects.
     *
     * <p>
     * Pass in a {@link CombinedVibration} representing a combination of {@link
     * VibrationEffect VibrationEffects} to be played on one or more vibrators.
     * </p>
     *
     * <p>The app should be in foreground for the vibration to happen.</p>
     *
     * @param effect a combination of effects to be performed by one or more vibrators.
     */
    public final void vibrate(@NonNull CombinedVibration effect) {
        vibrate(effect, null);
    }

    /**
     * Vibrate with a given combination of effects.
     *
     * <p>
     * Pass in a {@link CombinedVibration} representing a combination of {@link
     * VibrationEffect} to be played on one or more vibrators.
     * </p>
     *
     * <p>The app should be in foreground for the vibration to happen. Background apps should
     * specify a ringtone, notification or alarm usage in order to vibrate.</p>
     *
     * @param effect a combination of effects to be performed by one or more vibrators.
     * @param attributes {@link VibrationAttributes} corresponding to the vibration. For example,
     *                   specify {@link VibrationAttributes#USAGE_ALARM} for alarm vibrations or
     *                   {@link VibrationAttributes#USAGE_RINGTONE} for vibrations associated with
     *                   incoming calls.
     */
    public final void vibrate(@NonNull CombinedVibration effect,
            @Nullable VibrationAttributes attributes) {

    }

    /**
     * Turn all the vibrators off.
     */
    public abstract void cancel();
}
