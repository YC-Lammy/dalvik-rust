/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.internal;

import java.util.List;

import android.bluetooth.BluetoothActivityEnergyInfo;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.ParcelFileDescriptor;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.os.connectivity.WifiBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.health.HealthStatsParceler;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;

public interface IBatteryStats {
    // These first methods are also called by native code, so must
    // be kept sync with frameworks/native/libs/binder/include_batterystats/batterystats/IBatteryStats.h
    public void noteStartSensor(int uid, int sensor);
    public void noteStopSensor(int uid, int sensor);
    public void noteStartVideo(int uid);
    public void noteStopVideo(int uid);
    public void noteStartAudio(int uid);
    public void noteStopAudio(int uid);
    public void noteResetVideo();
    public void noteResetAudio();
    public void noteFlashlightOn(int uid);
    public void noteFlashlightOff(int uid);
    public void noteStartCamera(int uid);
    public void noteStopCamera(int uid);
    public void noteResetCamera();
    public void noteResetFlashlight();

    // Remaining methods are only used Java.

    public List<BatteryUsageStats> getBatteryUsageStats(List<BatteryUsageStatsQuery> queries);


    public byte[] getStatistics();

    public ParcelFileDescriptor getStatisticsStream(boolean updateAll);

    // Return true if we see the battery as currently charging.
    public boolean isCharging();

    // Return the computed amount of time remaining on battery, milliseconds.
    // Returns -1 if nothing could be computed.
    public long computeBatteryTimeRemaining();

    // Return the computed amount of time remaining to fully charge, milliseconds.
    // Returns -1 if nothing could be computed.
    public long computeChargeTimeRemaining();

    public void noteEvent(int code, String name, int uid);

    public void noteSyncStart(String name, int uid);
    public void noteSyncFinish(String name, int uid);
    public void noteJobStart(String name, int uid);
    public void noteJobFinish(String name, int uid, int stopReason);

    public void noteStartWakelock(int uid, int pid, String name, String historyName,
            int type, boolean unimportantForLogging);
    public void noteStopWakelock(int uid, int pid, String name, String historyName, int type);

    public void noteStartWakelockFromSource(WorkSource ws, int pid, String name, String historyName,
            int type, boolean unimportantForLogging);
    public void noteChangeWakelockFromSource(WorkSource ws, int pid, String name, String histyoryName,
            int type, WorkSource newWs, int newPid, String newName,
            String newHistoryName, int newType, boolean newUnimportantForLogging);
    public void noteStopWakelockFromSource(WorkSource ws, int pid, String name, String historyName,
            int type);
    public void noteLongPartialWakelockStart(String name, String historyName, int uid);
    public void noteLongPartialWakelockStartFromSource(String name, String historyName,
            WorkSource workSource);
    public void noteLongPartialWakelockFinish(String name, String historyName, int uid);
    public void noteLongPartialWakelockFinishFromSource(String name, String historyName,
            WorkSource workSource);

    public void noteVibratorOn(int uid, long durationMillis);
    public void noteVibratorOff(int uid);
    public void noteGpsChanged(WorkSource oldSource, WorkSource newSource);
    public void noteGpsSignalQuality(int signalLevel);
    public void noteScreenState(int state);
    public void noteScreenBrightness(int brightness);
    public void noteUserActivity(int uid, int event);
    public void noteWakeUp(String reason, int reasonUid);
    public void noteInteractive(boolean interactive);
    public void noteConnectivityChanged(int type, String extra);
    public void noteMobileRadioPowerState(int powerState, long timestampNs, int uid);
    public void notePhoneOn();
    public void notePhoneOff();
    public void notePhoneSignalStrength(SignalStrength signalStrength);
    public void notePhoneDataConnectionState(int dataType, boolean hasData, int serviceType);
    public void notePhoneState(int phoneState);
    public void noteWifiOn();
    public void noteWifiOff();
    public void noteWifiRunning(WorkSource ws);
    public void noteWifiRunningChanged(WorkSource oldWs, WorkSource newWs);
    public void noteWifiStopped(WorkSource ws);
    public void noteWifiState(int wifiState, String accessPoint);
    public void noteWifiSupplicantStateChanged(int supplState, boolean failedAuth);
    public void noteWifiRssiChanged(int newRssi);
    public void noteFullWifiLockAcquired(int uid);
    public void noteFullWifiLockReleased(int uid);
    public void noteWifiScanStarted(int uid);
    public void noteWifiScanStopped(int uid);
    public void noteWifiMulticastEnabled(int uid);
    public void noteWifiMulticastDisabled(int uid);
    public void noteFullWifiLockAcquiredFromSource(WorkSource ws);
    public void noteFullWifiLockReleasedFromSource(WorkSource ws);
    public void noteWifiScanStartedFromSource(WorkSource ws);
    public void noteWifiScanStoppedFromSource(WorkSource ws);
    public void noteWifiBatchedScanStartedFromSource(WorkSource ws, int csph);
    public void noteWifiBatchedScanStoppedFromSource(WorkSource ws);
    public void noteWifiRadioPowerState(int powerState, long timestampNs, int uid);
    public void noteNetworkInterfaceForTransports(String iface,int[] transportTypes);
    public void noteNetworkStatsEnabled();
    public void noteDeviceIdleMode(int mode, String activeReason, int activeUid);
    public void setBatteryState(int status, int health, int plugType, int level, int temp, int volt,
            int chargeUAh, int chargeFullUAh, long chargeTimeToFullSeconds);

    public long getAwakeTimeBattery();
    public long getAwakeTimePlugged();

    public void noteBleScanStarted(WorkSource ws, boolean isUnoptimized);
    public void noteBleScanStopped(WorkSource ws, boolean isUnoptimized);
    public void noteBleScanReset();
    public void noteBleScanResults(WorkSource ws, int numNewResults);

    /** {@hide} */
    public CellularBatteryStats getCellularBatteryStats();

    /** {@hide} */
    public WifiBatteryStats getWifiBatteryStats();

    /** {@hide} */
    public GpsBatteryStats getGpsBatteryStats();

    public HealthStatsParceler takeUidSnapshot(int uid);
    public HealthStatsParceler[] takeUidSnapshots(int[] uid);

    public void noteBluetoothControllerActivity(BluetoothActivityEnergyInfo info);
    public void noteModemControllerActivity(ModemActivityInfo info);
    public void noteWifiControllerActivity(WifiActivityEnergyInfo info);

    /** {@hide} */
    boolean setChargingStateUpdateDelayMillis(int delay);

    /** Exposed as a test API. */
    public void setChargerAcOnline(boolean online, boolean forceUpdate);
    /** Exposed as a test API. */
    public void setBatteryLevel(int level, boolean forceUpdate);
    /** Exposed as a test API. */
    public void unplugBattery(boolean forceUpdate);
    /** Exposed as a test API. */
    public void resetBattery(boolean forceUpdate);
    /** Exposed as a test API. */
    public void suspendBatteryInput();
}
