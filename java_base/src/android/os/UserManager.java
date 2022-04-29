/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.Manifest;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.util.AndroidException;
import android.util.ArraySet;

import android.R;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages users and user details on a multi-user system. There are two major categories of
 * users: fully customizable users with their own login, and profiles that share a workspace
 * with a related user.
 * <p>
 * Users are different from accounts, which are managed by
 * {@link AccountManager}. Each user can have their own set of accounts.
 * <p>
 * See {@link DevicePolicyManager#ACTION_PROVISION_MANAGED_PROFILE} for more on managed profiles.
 */
public class UserManager {

    private static final String TAG = "UserManager";
    
    /** The userId of the constructor param context. To be used instead of mContext.getUserId(). */
    private final int mUserId;

    private Boolean mIsManagedProfileCached;
    private Boolean mIsProfileCached;


    public static final String USER_TYPE_FULL_SYSTEM = "android.os.usertype.full.SYSTEM";

    public static final String USER_TYPE_FULL_SECONDARY = "android.os.usertype.full.SECONDARY";

    public static final String USER_TYPE_FULL_GUEST = "android.os.usertype.full.GUEST";

    public static final String USER_TYPE_FULL_DEMO = "android.os.usertype.full.DEMO";

    public static final String USER_TYPE_FULL_RESTRICTED = "android.os.usertype.full.RESTRICTED";

    public static final String USER_TYPE_PROFILE_MANAGED = "android.os.usertype.profile.MANAGED";

    public static final String USER_TYPE_PROFILE_CLONE = "android.os.usertype.profile.CLONE";

    public static final String USER_TYPE_PROFILE_TEST = "android.os.usertype.profile.TEST";

    public static final String USER_TYPE_SYSTEM_HEADLESS = "android.os.usertype.system.HEADLESS";

    /**
     * Flag passed to {@link #requestQuietModeEnabled} to request disabling quiet mode only if
     * there is no need to confirm the user credentials. If credentials are required to disable
     * quiet mode, {@link #requestQuietModeEnabled} will do nothing and return {@code false}.
     */
    public static final int QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED = 0x1;

    /**
     * Flag passed to {@link #requestQuietModeEnabled} to request disabling quiet mode without
     * asking for credentials. This is used when managed profile password is forgotten. It starts
     * the user in locked state so that a direct boot aware DPC could reset the password.
     * Should not be used together with
     * {@link #QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED} or an exception will be thrown.
     * @hide
     */
    public static final int QUIET_MODE_DISABLE_DONT_ASK_CREDENTIAL = 0x2;


    /**
     * @hide
     * No user restriction.
     */
    @SystemApi
    public static final int RESTRICTION_NOT_SET = 0x0;

    /**
     * @hide
     * User restriction set by system/user.
     */
    @SystemApi
    public static final int RESTRICTION_SOURCE_SYSTEM = 0x1;

    /**
     * @hide
     * User restriction set by a device owner.
     */
    @SystemApi
    public static final int RESTRICTION_SOURCE_DEVICE_OWNER = 0x2;

    /**
     * @hide
     * User restriction set by a profile owner.
     */
    @SystemApi
    public static final int RESTRICTION_SOURCE_PROFILE_OWNER = 0x4;

    /**
     * Specifies if a user is disallowed from adding and removing accounts, unless they are
     * {@link android.accounts.AccountManager#addAccountExplicitly programmatically} added by
     * Authenticator.
     * The default value is <code>false</code>.
     *
     * <p>From {@link android.os.Build.VERSION_CODES#N} a profile or device owner app can still
     * use {@link android.accounts.AccountManager} APIs to add or remove accounts when account
     * management is disallowed.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_MODIFY_ACCOUNTS = "no_modify_accounts";

    /**
     * Specifies if a user is disallowed from changing Wi-Fi access points via Settings.
     *
     * <p>A device owner and a profile owner can set this restriction, although the restriction has
     * no effect in a managed profile. When it is set by a device owner, a profile owner on the
     * primary user or by a profile owner of an organization-owned managed profile on the parent
     * profile, it disallows the primary user from changing Wi-Fi access points.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_WIFI = "no_config_wifi";

    /**
     * Specifies if a user is disallowed from changing the device
     * language. The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_LOCALE = "no_config_locale";

    /**
     * Specifies if a user is disallowed from installing applications. This user restriction also
     * prevents device owners and profile owners installing apps. The default value is
     * {@code false}.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_INSTALL_APPS = "no_install_apps";

    /**
     * Specifies if a user is disallowed from uninstalling applications.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_UNINSTALL_APPS = "no_uninstall_apps";

    /**
     * Specifies if a user is disallowed from turning on location sharing.
     *
     * <p>In a managed profile, location sharing by default reflects the primary user's setting, but
     * can be overridden and forced off by setting this restriction to true in the managed profile.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, a profile owner on the primary user or by a profile owner of an organization-owned
     * managed profile on the parent profile, it prevents the primary user from turning on
     * location sharing.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SHARE_LOCATION = "no_share_location";

    /**
     * Specifies if airplane mode is disallowed on the device.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by any of these owners, it applies globally - i.e., it disables airplane mode
     * on the entire device.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_AIRPLANE_MODE = "no_airplane_mode";

    /**
     * Specifies if a user is disallowed from configuring brightness. When device owner sets it,
     * it'll only be applied on the target(system) user.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This user restriction has no effect on managed profiles.
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_BRIGHTNESS = "no_config_brightness";

    /**
     * Specifies if ambient display is disallowed for the user.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This user restriction has no effect on managed profiles.
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_AMBIENT_DISPLAY = "no_ambient_display";

    /**
     * Specifies if a user is disallowed from changing screen off timeout.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This user restriction has no effect on managed profiles.
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_SCREEN_TIMEOUT = "no_config_screen_timeout";

    /**
     * Specifies if a user is disallowed from enabling the
     * "Unknown Sources" setting, that allows installation of apps from unknown sources.
     * Unknown sources exclude adb and special apps such as trusted app stores.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_INSTALL_UNKNOWN_SOURCES = "no_install_unknown_sources";

    /**
     * This restriction is a device-wide version of {@link #DISALLOW_INSTALL_UNKNOWN_SOURCES}.
     *
     * Specifies if all users on the device are disallowed from enabling the
     * "Unknown Sources" setting, that allows installation of apps from unknown sources.
     *
     * This restriction can be enabled by the profile owner, in which case all accounts and
     * profiles will be affected.
     *
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY =
            "no_install_unknown_sources_globally";

    /**
     * Specifies if a user is disallowed from configuring bluetooth via Settings. This does
     * <em>not</em> restrict the user from turning bluetooth on or off.
     *
     * <p>This restriction doesn't prevent the user from using bluetooth. For disallowing usage of
     * bluetooth completely on the device, use {@link #DISALLOW_BLUETOOTH}.
     *
     * <p>A device owner and a profile owner can set this restriction, although the restriction has
     * no effect in a managed profile. When it is set by a device owner, a profile owner on the
     * primary user or by a profile owner of an organization-owned managed profile on the parent
     * profile, it disallows the primary user from configuring bluetooth.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_BLUETOOTH = "no_config_bluetooth";

    /**
     * Specifies if bluetooth is disallowed on the device. If bluetooth is disallowed on the device,
     * bluetooth cannot be turned on or configured via Settings.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally - i.e., it disables bluetooth on
     * the entire device and all users will be affected. When it is set by a profile owner on the
     * primary user or by a profile owner of an organization-owned managed profile on the parent
     * profile, it disables the primary user from using bluetooth and configuring bluetooth
     * in Settings.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_BLUETOOTH = "no_bluetooth";

    /**
     * Specifies if outgoing bluetooth sharing is disallowed.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, it applies globally. When it is set by a profile owner on the primary user or by a
     * profile owner of an organization-owned managed profile on the parent profile, it disables
     * the primary user from any outgoing bluetooth sharing.
     *
     * <p>Default is <code>true</code> for managed profiles and false otherwise.
     *
     * <p>When a device upgrades to {@link android.os.Build.VERSION_CODES#O}, the system sets it
     * for all existing managed profiles.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_BLUETOOTH_SHARING = "no_bluetooth_sharing";

    /**
     * Specifies if a user is disallowed from transferring files over USB.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from transferring files over USB. No other
     * user on the device is able to use file transfer over USB because the UI for file transfer
     * is always associated with the primary user.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_USB_FILE_TRANSFER = "no_usb_file_transfer";

    /**
     * Specifies if a user is disallowed from configuring user
     * credentials. The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_CREDENTIALS = "no_config_credentials";

    /**
     * When set on the admin user this specifies if the user can remove users.
     * When set on a non-admin secondary user, this specifies if the user can remove itself.
     * This restriction has no effect on managed profiles.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_REMOVE_USER = "no_remove_user";

    /**
     * Specifies if managed profiles of this user can be removed, other than by its profile owner.
     * The default value is <code>false</code>.
     * <p>
     * This restriction has no effect on managed profiles.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @deprecated As the ability to have a managed profile on a fully-managed device has been
     * removed from the platform, this restriction will be silently ignored when applied by the
     * device owner.
     * When the device is provisioned with a managed profile on an organization-owned device,
     * the managed profile could not be removed anyway.
     */
    @Deprecated
    public static final String DISALLOW_REMOVE_MANAGED_PROFILE = "no_remove_managed_profile";

    /**
     * Specifies if a user is disallowed from enabling or accessing debugging features.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, a profile owner on the primary user or by a profile owner of an organization-owned
     * managed profile on the parent profile, it disables debugging features altogether, including
     * USB debugging. When set on a managed profile or a secondary user, it blocks debugging for
     * that user only, including starting activities, making service calls, accessing content
     * providers, sending broadcasts, installing/uninstalling packages, clearing user data, etc.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_DEBUGGING_FEATURES = "no_debugging_features";

    /**
     * Specifies if a user is disallowed from configuring a VPN. The default value is
     * <code>false</code>. This restriction has an effect when set by device owners and, in Android
     * 6.0 ({@linkplain android.os.Build.VERSION_CODES#M API level 23}) or higher, profile owners.
     * <p>This restriction also prevents VPNs from starting. However, in Android 7.0
     * ({@linkplain android.os.Build.VERSION_CODES#N API level 24}) or higher, the system does
     * start always-on VPNs created by the device or profile owner.
     * <p>From Android 12 ({@linkplain android.os.Build.VERSION_CODES#S API level 31}) enforcing
     * this restriction clears currently active VPN if it was configured by the user.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_VPN = "no_config_vpn";

    /**
     * Specifies if a user is disallowed from enabling or disabling location providers. As a
     * result, user is disallowed from turning on or off location via Settings.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, a profile owner on the primary user or by a profile owner of an organization-owned
     * managed profile on the parent profile, it disallows the primary user from turning location
     * on or off.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This user restriction is different from {@link #DISALLOW_SHARE_LOCATION},
     * as a device owner or a profile owner can still enable or disable location mode via
     * {@link DevicePolicyManager#setLocationEnabled} when this restriction is on.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see LocationManager#isLocationEnabled()
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_LOCATION = "no_config_location";

    /**
     * Specifies configuring date, time and timezone is disallowed via Settings.
     *
     * <p>A device owner and a profile owner can set this restriction, although the restriction has
     * no effect in a managed profile. When it is set by a device owner or by a profile owner of an
     * organization-owned managed profile on the parent profile, it applies globally - i.e.,
     * it disables date, time and timezone setting on the entire device and all users are affected.
     * When it is set by a profile owner on the primary user, it disables the primary user
     * from configuring date, time and timezone and disables all configuring of date, time and
     * timezone in Settings.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_DATE_TIME = "no_config_date_time";

    /**
     * Specifies if a user is disallowed from configuring Tethering and portable hotspots
     * via Settings.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from using Tethering and hotspots and
     * disables all configuring of Tethering and hotspots in Settings.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>In Android 9.0 or higher, if tethering is enabled when this restriction is set,
     * tethering will be automatically turned off.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_TETHERING = "no_config_tethering";

    /**
     * Specifies if a user is disallowed from resetting network settings
     * from Settings. This can only be set by device owners and profile owners on the primary user.
     * The default value is <code>false</code>.
     * <p>This restriction has no effect on secondary users and managed profiles since only the
     * primary user can reset the network settings of the device.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_NETWORK_RESET = "no_network_reset";

    /**
     * Specifies if a user is disallowed from factory resetting from Settings.
     * This can only be set by device owners and profile owners on an admin user.
     * The default value is <code>false</code>.
     * <p>This restriction has no effect on non-admin users since they cannot factory reset the
     * device.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_FACTORY_RESET = "no_factory_reset";

    /**
     * Specifies if a user is disallowed from adding new users. This can only be set by device
     * owners or profile owners on the primary user. The default value is <code>false</code>.
     * <p>This restriction has no effect on secondary users and managed profiles since only the
     * primary user can add other users.
     * <p> When the device is an organization-owned device provisioned with a managed profile,
     * this restriction will be set as a base restriction which cannot be removed by any admin.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_ADD_USER = "no_add_user";

    /**
     * Specifies if a user is disallowed from adding managed profiles.
     * <p>The default value for an unmanaged user is <code>false</code>.
     * For users with a device owner set, the default is <code>true</code>.
     * <p>This restriction has no effect on managed profiles.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @deprecated As the ability to have a managed profile on a fully-managed device has been
     * removed from the platform, this restriction will be silently ignored when applied by the
     * device owner.
     */
    @Deprecated
    public static final String DISALLOW_ADD_MANAGED_PROFILE = "no_add_managed_profile";

    /**
     * Specifies if a user is disallowed from disabling application verification. The default
     * value is <code>false</code>.
     *
     * <p>In Android 8.0 ({@linkplain android.os.Build.VERSION_CODES#O API level 26}) and higher,
     * this is a global user restriction. If a device owner or profile owner sets this restriction,
     * the system enforces app verification across all users on the device. Running in earlier
     * Android versions, this restriction affects only the profile that sets it.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String ENSURE_VERIFY_APPS = "ensure_verify_apps";

    /**
     * Specifies if a user is disallowed from configuring cell broadcasts.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from configuring cell broadcasts.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This restriction has no effect on secondary users and managed profiles since only the
     * primary user can configure cell broadcasts.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_CELL_BROADCASTS = "no_config_cell_broadcasts";

    /**
     * Specifies if a user is disallowed from configuring mobile networks.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from configuring mobile networks.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>This restriction has no effect on secondary users and managed profiles since only the
     * primary user can configure mobile networks.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_MOBILE_NETWORKS = "no_config_mobile_networks";

    /**
     * Specifies if a user is disallowed from modifying
     * applications in Settings or launchers. The following actions will not be allowed when this
     * restriction is enabled:
     * <li>uninstalling apps</li>
     * <li>disabling apps</li>
     * <li>clearing app caches</li>
     * <li>clearing app data</li>
     * <li>force stopping apps</li>
     * <li>clearing app defaults</li>
     * <p>
     * The default value is <code>false</code>.
     *
     * <p><strong>Note:</strong> The user will still be able to perform those actions via other
     * means (such as adb). Third party apps will also be able to uninstall apps via the
     * {@link android.content.pm.PackageInstaller}. {@link #DISALLOW_UNINSTALL_APPS} or
     * {@link DevicePolicyManager#setUninstallBlocked(ComponentName, String, boolean)} should be
     * used to prevent the user from uninstalling apps completely, and
     * {@link DevicePolicyManager#addPersistentPreferredActivity(ComponentName, IntentFilter, ComponentName)}
     * to add a default intent handler for a given intent filter.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_APPS_CONTROL = "no_control_apps";

    /**
     * Specifies if a user is disallowed from mounting physical external media.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from mounting physical external media.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_MOUNT_PHYSICAL_MEDIA = "no_physical_media";

    /**
     * Specifies if a user is disallowed from adjusting microphone volume. If set, the microphone
     * will be muted.
     *
     * <p>A device owner and a profile owner can set this restriction, although the restriction has
     * no effect in a managed profile. When it is set by a device owner, it applies globally. When
     * it is set by a profile owner on the primary user or by a profile owner of an
     * organization-owned managed profile on the parent profile, it will disallow the primary user
     * from adjusting the microphone volume.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_UNMUTE_MICROPHONE = "no_unmute_microphone";

    /**
     * Specifies if a user is disallowed from adjusting the global volume. If set, the global volume
     * will be muted. This can be set by device owners from API 21 and profile owners from API 24.
     * The default value is <code>false</code>.
     *
     * <p>When the restriction is set by profile owners, then it only applies to relevant
     * profiles.
     *
     * <p>This restriction has no effect on managed profiles.
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_ADJUST_VOLUME = "no_adjust_volume";

    /**
     * Specifies that the user is not allowed to make outgoing phone calls. Emergency calls are
     * still permitted.
     *
     * <p>A device owner and a profile owner can set this restriction, although the restriction has
     * no effect in a managed profile. When it is set by a device owner, a profile owner on the
     * primary user or by a profile owner of an organization-owned managed profile on the parent
     * profile, it disallows the primary user from making outgoing phone calls.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_OUTGOING_CALLS = "no_outgoing_calls";

    /**
     * Specifies that the user is not allowed to send or receive SMS messages.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from sending or receiving SMS messages.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SMS = "no_sms";

    /**
     * Specifies if the user is not allowed to have fun. In some cases, the
     * device owner may wish to prevent the user from experiencing amusement or
     * joy while using the device. The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_FUN = "no_fun";

    /**
     * Specifies that windows besides app windows should not be
     * created. This will block the creation of the following types of windows.
     * <li>{@link LayoutParams#TYPE_TOAST}</li>
     * <li>{@link LayoutParams#TYPE_PHONE}</li>
     * <li>{@link LayoutParams#TYPE_PRIORITY_PHONE}</li>
     * <li>{@link LayoutParams#TYPE_SYSTEM_ALERT}</li>
     * <li>{@link LayoutParams#TYPE_SYSTEM_ERROR}</li>
     * <li>{@link LayoutParams#TYPE_SYSTEM_OVERLAY}</li>
     * <li>{@link LayoutParams#TYPE_APPLICATION_OVERLAY}</li>
     *
     * <p>This can only be set by device owners and profile owners on the primary user.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CREATE_WINDOWS = "no_create_windows";

    /**
     * Specifies that system error dialogs for crashed or unresponsive apps should not be shown.
     * In this case, the system will force-stop the app as if the user chooses the "close app"
     * option on the UI. A feedback report isn't collected as there is no way for the user to
     * provide explicit consent. The default value is <code>false</code>.
     *
     * <p>When this user restriction is set by device owners, it's applied to all users. When set by
     * the profile owner of the primary user or a secondary user, the restriction affects only the
     * calling user. This user restriction has no effect on managed profiles.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SYSTEM_ERROR_DIALOGS = "no_system_error_dialogs";

    /**
     * Specifies if the clipboard contents can be exported by pasting the data into other users or
     * profiles. This restriction doesn't prevent import, such as someone pasting clipboard data
     * from other profiles or users. The default value is {@code false}.
     *
     * <p><strong>Note</strong>: Because it's possible to extract data from screenshots using
     * optical character recognition (OCR), we strongly recommend combining this user restriction
     * with {@link DevicePolicyManager#setScreenCaptureDisabled(ComponentName, boolean)}.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CROSS_PROFILE_COPY_PASTE = "no_cross_profile_copy_paste";

    /**
     * Specifies if the user is not allowed to use NFC to beam out data from apps.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_OUTGOING_BEAM = "no_outgoing_beam";

    /**
     * Hidden user restriction to disallow access to wallpaper manager APIs. This restriction
     * generally means that wallpapers are not supported for the particular user. This user
     * restriction is always set for managed profiles, because such profiles don't have wallpapers.
     * @hide
     * @see #DISALLOW_SET_WALLPAPER
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_WALLPAPER = "no_wallpaper";

    /**
     * User restriction to disallow setting a wallpaper. Profile owner and device owner
     * are able to set wallpaper regardless of this restriction.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SET_WALLPAPER = "no_set_wallpaper";

    /**
     * Specifies if the user is not allowed to reboot the device into safe boot mode.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from rebooting the device into safe
     * boot mode.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SAFE_BOOT = "no_safe_boot";

    /**
     * Specifies if a user is not allowed to record audio. This restriction is always enabled for
     * background users. The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @hide
     */
    public static final String DISALLOW_RECORD_AUDIO = "no_record_audio";

    /**
     * Specifies if a user is not allowed to run in the background and should be stopped during
     * user switch. The default value is <code>false</code>.
     *
     * <p>This restriction can be set by device owners and profile owners.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @hide
     */
    @SystemApi
    public static final String DISALLOW_RUN_IN_BACKGROUND = "no_run_in_background";

    /**
     * Specifies if a user is not allowed to use the camera.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a
     * device owner, it applies globally - i.e., it disables the use of camera on the entire device
     * and all users are affected. When it is set by a profile owner on the primary user or by a
     * profile owner of an organization-owned managed profile on the parent profile, it disables
     * the primary user from using camera.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @hide
     */
    public static final String DISALLOW_CAMERA = "no_camera";

    /**
     * Specifies if a user is not allowed to unmute the device's global volume.
     *
     * @see DevicePolicyManager#setMasterVolumeMuted(ComponentName, boolean)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @hide
     */
    public static final String DISALLOW_UNMUTE_DEVICE = "disallow_unmute_device";

    /**
     * Specifies if a user is not allowed to use cellular data when roaming.
     *
     * <p>This restriction can only be set by a device owner, a profile owner on the primary
     * user or a profile owner of an organization-owned managed profile on the parent profile.
     * When it is set by a device owner, it applies globally. When it is set by a profile owner
     * on the primary user or by a profile owner of an organization-owned managed profile on
     * the parent profile, it disables the primary user from using cellular data when roaming.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_DATA_ROAMING = "no_data_roaming";

    /**
     * Specifies if a user is not allowed to change their icon. Device owner and profile owner
     * can set this restriction. When it is set by device owner, only the target user will be
     * affected. The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SET_USER_ICON = "no_set_user_icon";

    /**
     * Specifies if a user is not allowed to enable the oem unlock setting. The default value is
     * <code>false</code>. Setting this restriction has no effect if the bootloader is already
     * unlocked.
     *
     * <p>Not for use by third-party applications.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     * @deprecated use {@link OemLockManager#setOemUnlockAllowedByCarrier(boolean, byte[])} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String DISALLOW_OEM_UNLOCK = "no_oem_unlock";

    /**
     * Specifies that the managed profile is not allowed to have unified lock screen challenge with
     * the primary user.
     *
     * <p><strong>Note:</strong> Setting this restriction alone doesn't automatically set a
     * separate challenge. Profile owner can ask the user to set a new password using
     * {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD} and verify it using
     * {@link DevicePolicyManager#isUsingUnifiedPassword(ComponentName)}.
     *
     * <p>Can be set by profile owners. It only has effect on managed profiles when set by managed
     * profile owner. Has no effect on non-managed profiles or users.
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_UNIFIED_PASSWORD = "no_unified_password";

    /**
     * Allows apps in the parent profile to handle web links from the managed profile.
     *
     * This user restriction has an effect only in a managed profile.
     * If set:
     * Intent filters of activities in the parent profile with action
     * {@link android.content.Intent#ACTION_VIEW},
     * category {@link android.content.Intent#CATEGORY_BROWSABLE}, scheme http or https, and which
     * define a host can handle intents from the managed profile.
     * The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String ALLOW_PARENT_PROFILE_APP_LINKING
            = "allow_parent_profile_app_linking";

    /**
     * Specifies if a user is not allowed to use Autofill Services.
     *
     * <p>Device owner and profile owner can set this restriction. When it is set by device owner,
     * only the target user will be affected.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_AUTOFILL = "no_autofill";

    /**
     * Specifies if the contents of a user's screen is not allowed to be captured for artificial
     * intelligence purposes.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, a profile owner on the primary user or by a profile owner of an organization-owned
     * managed profile on the parent profile, it disables the primary user's screen from being
     * captured for artificial intelligence purposes.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONTENT_CAPTURE = "no_content_capture";

    /**
     * Specifies if the current user is able to receive content suggestions for selections based on
     * the contents of their screen.
     *
     * <p>A device owner and a profile owner can set this restriction. When it is set by a device
     * owner, a profile owner on the primary user or by a profile owner of an organization-owned
     * managed profile on the parent profile, it disables the primary user from receiving content
     * suggestions for selections based on the contents of their screen.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONTENT_SUGGESTIONS = "no_content_suggestions";

    /**
     * Specifies if user switching is blocked on the current user.
     *
     * <p> This restriction can only be set by the device owner, it will be applied to all users.
     * Device owner can still switch user via
     * {@link DevicePolicyManager#switchUser(ComponentName, UserHandle)} when this restriction is
     * set.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_USER_SWITCH = "no_user_switch";

    /**
     * Specifies whether the user can share file / picture / data from the primary user into the
     * managed profile, either by sending them from the primary side, or by picking up data within
     * an app in the managed profile.
     * <p>
     * When a managed profile is created, the system allows the user to send data from the primary
     * side to the profile by setting up certain default cross profile intent filters. If
     * this is undesired, this restriction can be set to disallow it. Note that this restriction
     * will not block any sharing allowed by explicit
     * {@link DevicePolicyManager#addCrossProfileIntentFilter} calls by the profile owner.
     * <p>
     * This restriction is only meaningful when set by profile owner. When it is set by device
     * owner, it does not have any effect.
     * <p>
     * The default value is <code>false</code>.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_SHARE_INTO_MANAGED_PROFILE = "no_sharing_into_profile";

    /**
     * Specifies whether the user is allowed to print.
     *
     * This restriction can be set by device or profile owner.
     *
     * The default value is {@code false}.
     *
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_PRINTING = "no_printing";

    /**
     * Specifies whether the user is allowed to modify private DNS settings.
     *
     * <p>This restriction can only be set by a device owner or a profile owner of an
     * organization-owned managed profile on the parent profile. When it is set by either of these
     * owners, it applies globally.
     *
     * <p>The default value is <code>false</code>.
     *
     * <p>Key for user restrictions.
     * <p>Type: Boolean
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CONFIG_PRIVATE_DNS =
            "disallow_config_private_dns";

    /**
     * Specifies whether the microphone toggle is available to the user. If this restriction is set,
     * the user will not be able to block microphone access via the system toggle. If microphone
     * access is blocked when the restriction is added, it will be automatically re-enabled.
     *
     * This restriction can only be set by a device owner.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see android.hardware.SensorPrivacyManager
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_MICROPHONE_TOGGLE =
            "disallow_microphone_toggle";

    /**
     * Specifies whether the camera toggle is available to the user. If this restriction is set,
     * the user will not be able to block camera access via the system toggle. If camera
     * access is blocked when the restriction is added, it will be automatically re-enabled.
     *
     * This restriction can only be set by a device owner.
     *
     * <p>The default value is <code>false</code>.
     *
     * @see android.hardware.SensorPrivacyManager
     * @see DevicePolicyManager#addUserRestriction(ComponentName, String)
     * @see DevicePolicyManager#clearUserRestriction(ComponentName, String)
     * @see #getUserRestrictions()
     */
    public static final String DISALLOW_CAMERA_TOGGLE =
            "disallow_camera_toggle";

    /**
     * This is really not a user restriction in the normal sense. This can't be set to a user,
     * via UserManager nor via DevicePolicyManager. This is not even set in UserSettingsUtils.
     * This is defined here purely for convenience within the settings app.
     *
     * TODO(b/191306258): Refactor the Settings app to remove the need for this field, and delete it
     *
     * Specifies whether biometrics are available to the user. This is used internally only,
     * as a means of communications between biometric settings and
     * {@link com.android.settingslib.enterprise.ActionDisabledByAdminControllerFactory}.
     *
     * @see {@link android.hardware.biometrics.ParentalControlsUtilsInternal}
     * @see {@link com.android.settings.biometrics.ParentalControlsUtils}
     *
     * @hide
     */
    public static final String DISALLOW_BIOMETRIC = "disallow_biometric";

    /**
     * Application restriction key that is used to indicate the pending arrival
     * of real restrictions for the app.
     *
     * <p>
     * Applications that support restrictions should check for the presence of this key.
     * A <code>true</code> value indicates that restrictions may be applied in the near
     * future but are not available yet. It is the responsibility of any
     * management application that sets this flag to update it when the final
     * restrictions are enforced.
     *
     * <p>Key for application restrictions.
     * <p>Type: Boolean
     * @see android.app.admin.DevicePolicyManager#setApplicationRestrictions(
     *      android.content.ComponentName, String, Bundle)
     * @see android.app.admin.DevicePolicyManager#getApplicationRestrictions(
     *      android.content.ComponentName, String)
     */
    public static final String KEY_RESTRICTIONS_PENDING = "restrictions_pending";



    private static final String ACTION_CREATE_USER = "android.os.action.CREATE_USER";

    /**
     * Extra containing a name for the user being created. Optional parameter passed to
     * ACTION_CREATE_USER activity.
     * @hide
     */
    public static final String EXTRA_USER_NAME = "android.os.extra.USER_NAME";

    /**
     * Extra containing account name for the user being created. Optional parameter passed to
     * ACTION_CREATE_USER activity.
     * @hide
     */
    public static final String EXTRA_USER_ACCOUNT_NAME = "android.os.extra.USER_ACCOUNT_NAME";

    /**
     * Extra containing account type for the user being created. Optional parameter passed to
     * ACTION_CREATE_USER activity.
     * @hide
     */
    public static final String EXTRA_USER_ACCOUNT_TYPE = "android.os.extra.USER_ACCOUNT_TYPE";

    /**
     * Extra containing account-specific data for the user being created. Optional parameter passed
     * to ACTION_CREATE_USER activity.
     * @hide
     */
    public static final String EXTRA_USER_ACCOUNT_OPTIONS
            = "android.os.extra.USER_ACCOUNT_OPTIONS";

    /** @hide */
    public static final int PIN_VERIFICATION_FAILED_INCORRECT = -3;
    /** @hide */
    public static final int PIN_VERIFICATION_FAILED_NOT_SET = -2;
    /** @hide */
    public static final int PIN_VERIFICATION_SUCCESS = -1;

    /**
     * Sent when user restrictions have changed.
     *
     * @hide
     */
    @SystemApi // To allow seeing it from CTS.
    public static final String ACTION_USER_RESTRICTIONS_CHANGED =
            "android.os.action.USER_RESTRICTIONS_CHANGED";

    /**
     * Error result indicating that this user is not allowed to add other users on this device.
     * This is a result code returned from the activity created by the intent
     * {@link #createUserCreationIntent(String, String, String, PersistableBundle)}.
     */
    public static final int USER_CREATION_FAILED_NOT_PERMITTED = 1;

    /**
     * Error result indicating that no more users can be created on this device.
     * This is a result code returned from the activity created by the intent
     * {@link #createUserCreationIntent(String, String, String, PersistableBundle)}.
     */
    public static final int USER_CREATION_FAILED_NO_MORE_USERS = 2;

    /**
     * Indicates that users are switchable.
     * @hide
     */
    @SystemApi
    public static final int SWITCHABILITY_STATUS_OK = 0;

    /**
     * Indicated that the user is in a phone call.
     * @hide
     */
    @SystemApi
    public static final int SWITCHABILITY_STATUS_USER_IN_CALL = 1 << 0;

    /**
     * Indicates that user switching is disallowed ({@link #DISALLOW_USER_SWITCH} is set).
     * @hide
     */
    @SystemApi
    public static final int SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED = 1 << 1;

    /**
     * Indicates that the system user is locked and user switching is not allowed.
     * @hide
     */
    @SystemApi
    public static final int SWITCHABILITY_STATUS_SYSTEM_USER_LOCKED = 1 << 2;


    /**
     * A response code from {@link #removeUserOrSetEphemeral(int)} indicating that the specified
     * user has been successfully removed.
     * @hide
     */
    public static final int REMOVE_RESULT_REMOVED = 0;

    /**
     * A response code from {@link #removeUserOrSetEphemeral(int)} indicating that the specified
     * user has had its {@link UserInfo#FLAG_EPHEMERAL} flag set to {@code true}, so that it will be
     * removed when the user is stopped or on boot.
     * @hide
     */
    public static final int REMOVE_RESULT_SET_EPHEMERAL = 1;

    /**
     * A response code from {@link #removeUserOrSetEphemeral(int)} indicating that the specified
     * user is already in the process of being removed.
     * @hide
     */
    public static final int REMOVE_RESULT_ALREADY_BEING_REMOVED = 2;

    /**
     * A response code from {@link #removeUserOrSetEphemeral(int)} indicating that an error occurred
     * that prevented the user from being removed or set as ephemeral.
     * @hide
     */
    public static final int REMOVE_RESULT_ERROR = 3;


    /**
     * Indicates user operation is successful.
     */
    public static final int USER_OPERATION_SUCCESS = 0;

    /**
     * Indicates user operation failed for unknown reason.
     */
    public static final int USER_OPERATION_ERROR_UNKNOWN = 1;

    /**
     * Indicates user operation failed because target user is a managed profile.
     */
    public static final int USER_OPERATION_ERROR_MANAGED_PROFILE = 2;

    /**
     * Indicates user operation failed because maximum running user limit has been reached.
     */
    public static final int USER_OPERATION_ERROR_MAX_RUNNING_USERS = 3;

    /**
     * Indicates user operation failed because the target user is in the foreground.
     */
    public static final int USER_OPERATION_ERROR_CURRENT_USER = 4;

    /**
     * Indicates user operation failed because device has low data storage.
     */
    public static final int USER_OPERATION_ERROR_LOW_STORAGE = 5;

    /**
     * Indicates user operation failed because maximum user limit has been reached.
     */
    public static final int USER_OPERATION_ERROR_MAX_USERS = 6;


    /**
     * Thrown to indicate user operation failed.
     */
    public static class UserOperationException extends RuntimeException {
        private final int mUserOperationResult;

        /**
         * Constructs a UserOperationException with specific result code.
         *
         * @param message the detail message
         * @param userOperationResult the result code
         * @hide
         */
        public UserOperationException(String message,
                int userOperationResult) {
            super(message);
            mUserOperationResult = userOperationResult;
        }

        /**
         * Returns the operation result code.
         */
        public int getUserOperationResult() {
            return mUserOperationResult;
        }

        /**
         * Returns a UserOperationException containing the same message and error code.
         * @hide
         */
        public static UserOperationException from(ServiceSpecificException exception) {
            return new UserOperationException(exception.getMessage(), exception.errorCode);
        }
    }

    /**
     * Converts the ServiceSpecificException into a UserOperationException or throws null;
     *
     * @param exception exception to convert.
     * @param throwInsteadOfNull if an exception should be thrown or null returned.
     * @return null if chosen not to throw exception.
     * @throws UserOperationException
     */
    private <T> T returnNullOrThrowUserOperationException(ServiceSpecificException exception,
            boolean throwInsteadOfNull) throws UserOperationException {
        if (throwInsteadOfNull) {
            throw UserOperationException.from(exception);
        } else {
            return null;
        }
    }

    /**
     * Thrown to indicate user operation failed. (Checked exception)
     * @hide
     */
    public static class CheckedUserOperationException extends AndroidException {
        private final int mUserOperationResult;

        /**
         * Constructs a CheckedUserOperationException with specific result code.
         *
         * @param message the detail message
         * @param userOperationResult the result code
         * @hide
         */
        public CheckedUserOperationException(String message,
                int userOperationResult) {
            super(message);
            mUserOperationResult = userOperationResult;
        }

        /** Returns the operation result code. */
        public int getUserOperationResult() {
            return mUserOperationResult;
        }

        /** Return a ServiceSpecificException containing the same message and error code. */
        public ServiceSpecificException toServiceSpecificException() {
            return new ServiceSpecificException(mUserOperationResult, getMessage());
        }
    }

    /** @hide */
    public UserManager() {
        mUserId = UserHandle.myUserId();
    }

    /**
     * Returns whether this device supports multiple users with their own login and customizable
     * space.
     * @return whether the device supports multiple users.
     */
    public static boolean supportsMultipleUsers() {
        return true;
    }

    /**
     * @hide
     * @return Whether the device is running with split system user. It means the system user and
     * primary user are two separate users. Previously system user and primary user are combined as
     * a single owner user.  see @link {android.os.UserHandle#USER_OWNER}
     */
    @TestApi
    public static boolean isSplitSystemUser() {
        return true;
    }

    /**
     * @return Whether guest user is always ephemeral
     * @hide
     */
    @TestApi
    public static boolean isGuestUserEphemeral() {
        return false;
    }

    /**
     * Checks whether the device is running in a headless system user mode.
     *
     * <p>Headless system user mode means the {@link #isSystemUser() system user} runs system
     * services and some system UI, but it is not associated with any real person and additional
     * users must be created to be associated with real persons.
     *
     * @return whether the device is running in a headless system user mode.
     */
    public static boolean isHeadlessSystemUserMode() {
        return false;
    }

    /**
     * @deprecated use {@link #getUserSwitchability()} instead.
     *
     * @removed
     * @hide
     */
    public boolean canSwitchUsers() {
        return true;
    }

    /**
     * Returns whether switching users is currently allowed for the user this process is running
     * under.
     * <p>
     * Switching users is not allowed in the following cases:
     * <li>the user is in a phone call</li>
     * <li>{@link #DISALLOW_USER_SWITCH} is set</li>
     * <li>system user hasn't been unlocked yet</li>
     *
     * @return A {@link UserSwitchabilityResult} flag indicating if the user is switchable.
     * @hide
     */
    @SystemApi
    public int getUserSwitchability() {
        return SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED | SWITCHABILITY_STATUS_SYSTEM_USER_LOCKED;
    }


    /**
     * Returns the user handle for the user that this process is running under.
     *
     * @return the user handle of this process.
     * @hide
     */
    public int getUserHandle() {
        return UserHandle.myUserId();
    }


    /**
     * Returns the user name of the context user. This call is only available to applications on
     * the system image; it requires the {@code android.permission.MANAGE_USERS} or {@code
     * android.permission.GET_ACCOUNTS_PRIVILEGED} permissions.
     *
     * @return the user name
     */
    public @NonNull String getUserName() {
        return System.getProperty("user.name");
    }

    /**
     * Returns whether user name has been set.
     * <p>This method can be used to check that the value returned by {@link #getUserName()} was
     * set by the user and is not a placeholder string provided by the system.
     * @hide
     */
    public boolean isUserNameSet() {
        return true;
    }

    /**
     * Used to determine whether the user making this call is subject to
     * teleportations.
     *
     * <p>As of {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this method can
     * now automatically identify goats using advanced goat recognition technology.</p>
     *
     * <p>As of {@link android.os.Build.VERSION_CODES#R}, this method always returns
     * {@code false} in order to protect goat privacy.</p>
     *
     * @return Returns whether the user making this call is a goat.
     */
    public boolean isUserAGoat() {
        return false;
    }

    @SystemApi
    public boolean isPrimaryUser() {
        return true;
    }

    /**
     * Used to check if this process is running under the system user. The system user
     * is the initial user that is implicitly created on first boot and hosts most of the
     * system services.
     *
     * @return whether this process is running under the system user.
     */
    public boolean isSystemUser() {
        return UserHandle.myUserId() == UserHandle.USER_SYSTEM;
    }

    /**
     * Used to check if this process is running as an admin user. An admin user is allowed to
     * modify or configure certain settings that aren't available to non-admin users,
     * create and delete additional users, etc. There can be more than one admin users.
     *
     * @return whether this process is running under an admin user.
     * @hide
     */
    @SystemApi
    public boolean isAdminUser() {
        return false;
    }

    public boolean isUserAdmin(int userId) {
        return false;
    }

    /**
     * @hide
     * @deprecated Use {@link #isRestrictedProfile()}
     */
    @Deprecated
    public boolean isLinkedUser() {
        return isRestrictedProfile();
    }

    /**
     * Used to check if this process is running under a restricted profile. Restricted profiles
     * may have a reduced number of available apps, app restrictions, and account restrictions.
     *
     * @return whether this process is running under a restricted profile.
     * @hide
     */
    @SystemApi
    public boolean isRestrictedProfile() {
        return false;
    }

    /**
     * Check if a user is a restricted profile. Restricted profiles may have a reduced number of
     * available apps, app restrictions, and account restrictions.
     *
     * @param user the user to check
     * @return whether the user is a restricted profile.
     * @hide
     */
    @SystemApi
    public boolean isRestrictedProfile(@NonNull UserHandle user) {
        return false;
    }

    /**
     * Checks if the calling context user can have a restricted profile.
     * @return whether the context user can have a restricted profile.
     * @hide
     */
    @SystemApi
    public boolean canHaveRestrictedProfile() {
        return false;
    }

    /**
     * Returns whether the calling user has at least one restricted profile associated with it.
     * @return
     * @hide
     */
    @SystemApi
    public boolean hasRestrictedProfiles() {
        return false;
    }

    /**
     * Return whether the given user is actively running.  This means that
     * the user is in the "started" state, not "stopped" -- it is currently
     * allowed to run code through scheduled alarms, receiving broadcasts,
     * etc.  A started user may be either the current foreground user or a
     * background user; the result here does not distinguish between the two.
     *
     * <p>Note prior to Android Nougat MR1 (SDK version <= 24;
     * {@link android.os.Build.VERSION_CODES#N}, this API required a system permission
     * in order to check other profile's status.
     * Since Android Nougat MR1 (SDK version >= 25;
     * {@link android.os.Build.VERSION_CODES#N_MR1}), the restriction has been relaxed, and now
     * it'll accept any {@link android.os.UserHandle} within the same profile group as the caller.
     *
     * @param user The user to retrieve the running state for.
     */
    public boolean isUserRunning(UserHandle user) {
        return false;
    }

    /**
     * Return whether the given user is actively running <em>or</em> stopping.
     * This is like {@link #isUserRunning(UserHandle)}, but will also return
     * true if the user had been running but is in the process of being stopped
     * (but is not yet fully stopped, and still running some code).
     *
     * <p>Note prior to Android Nougat MR1 (SDK version <= 24;
     * {@link android.os.Build.VERSION_CODES#N}, this API required a system permission
     * in order to check other profile's status.
     * Since Android Nougat MR1 (SDK version >= 25;
     * {@link android.os.Build.VERSION_CODES#N_MR1}), the restriction has been relaxed, and now
     * it'll accept any {@link android.os.UserHandle} within the same profile group as the caller.
     *
     * @param user The user to retrieve the running state for.
     */
    public boolean isUserRunningOrStopping(UserHandle user) {
        return true;
    }

    /**
     * Checks if the context user is running in the foreground.
     *
     * @return whether the context user is running in the foreground.
     */
    public boolean isUserForeground() {
        return true;
    }

    /**
     * Return whether the calling user is running in an "unlocked" state.
     * <p>
     * On devices with direct boot, a user is unlocked only after they've
     * entered their credentials (such as a lock pattern or PIN). On devices
     * without direct boot, a user is unlocked as soon as it starts.
     * <p>
     * When a user is locked, only device-protected data storage is available.
     * When a user is unlocked, both device-protected and credential-protected
     * private app data storage is available.
     *
     * @see Intent#ACTION_USER_UNLOCKED
     * @see Context#createDeviceProtectedStorageContext()
     */
    public boolean isUserUnlocked() {
        return true;
    }

    public boolean isUserUnlocked(UserHandle user) {
        return true;
    }

    private static final String CACHE_KEY_IS_USER_UNLOCKED_PROPERTY = "cache_key.is_user_unlocked";

    /** {@hide} */
    public boolean isUserUnlocked(int userId) {
        return true;
    }

    /**
     * Returns the user-wide restrictions imposed on this user.
     * @return a Bundle containing all the restrictions.
     */
    public Bundle getUserRestrictions() {
        return getUserRestrictions(Process.myUserHandle());
    }

    /**
     * Returns the user-wide restrictions imposed on the user specified by <code>userHandle</code>.
     * @param userHandle the UserHandle of the user for whom to retrieve the restrictions.
     * @return a Bundle containing all the restrictions.
     *
     * <p>Requires {@code android.permission.MANAGE_USERS} or
     * {@code android.permission.INTERACT_ACROSS_USERS}, otherwise specified {@link UserHandle user}
     * must be the calling user or a profile associated with it.
     */
    public Bundle getUserRestrictions(UserHandle userHandle) {
        return new Bundle();
    }

     /**
     * @hide
     * Returns whether the given user has been disallowed from performing certain actions
     * or setting certain settings through UserManager (e.g. this type of restriction would prevent
     * the guest user from doing certain things, such as making calls). This method disregards
     * restrictions set by device policy.
     * @param restrictionKey the string key representing the restriction
     * @param userHandle the UserHandle of the user for whom to retrieve the restrictions.
     */
    public boolean hasBaseUserRestriction(String restrictionKey, UserHandle userHandle) {
        return true;
    }

    /**
     * This will no longer work.  Device owners and profile owners should use
     * {@link DevicePolicyManager#addUserRestriction(ComponentName, String)} instead.
     */
    // System apps should use UserManager.setUserRestriction() instead.
    @Deprecated
    public void setUserRestrictions(Bundle restrictions) {
        throw new UnsupportedOperationException("This method is no longer supported");
    }

    /**
     * This will no longer work.  Device owners and profile owners should use
     * {@link DevicePolicyManager#addUserRestriction(ComponentName, String)} instead.
     */
    // System apps should use UserManager.setUserRestriction() instead.
    @Deprecated
    public void setUserRestrictions(Bundle restrictions, UserHandle userHandle) {
        throw new UnsupportedOperationException("This method is no longer supported");
    }


    @Deprecated
    public void setUserRestriction(String key, boolean value) {
        setUserRestriction(key, value, Process.myUserHandle());
    }

    @Deprecated
    public void setUserRestriction(String key, boolean value, UserHandle userHandle) {
        // dummy
    }

    /**
     * Returns whether the current user has been disallowed from performing certain actions
     * or setting certain settings.
     *
     * @param restrictionKey The string key representing the restriction.
     * @return {@code true} if the current user has the given restriction, {@code false} otherwise.
     */
    public boolean hasUserRestriction(String restrictionKey) {
        return false;
    }

    /**
     * @hide
     * Returns whether the given user has been disallowed from performing certain actions
     * or setting certain settings.
     * @param restrictionKey the string key representing the restriction
     * @param userHandle the UserHandle of the user for whom to retrieve the restrictions.
     */
    public boolean hasUserRestriction(String restrictionKey,
            UserHandle userHandle) {
        return hasUserRestrictionForUser(restrictionKey, userHandle);
    }

    /**
     * Returns whether the given user has been disallowed from performing certain actions
     * or setting certain settings.
     * @param restrictionKey the string key representing the restriction
     * @param userHandle the UserHandle of the user for whom to retrieve the restrictions.
     *
     * <p>Requires {@code android.permission.MANAGE_USERS} or
     * {@code android.permission.INTERACT_ACROSS_USERS}, otherwise specified {@link UserHandle user}
     * must be the calling user or a profile associated with it.
     *
     * @hide
     */
    @SystemApi
    public boolean hasUserRestrictionForUser(String restrictionKey,
            @NonNull UserHandle userHandle) {
        return false;
    }

    /**
     * @hide
     * Returns whether any user on the device has the given user restriction set.
     */
    public boolean hasUserRestrictionOnAnyUser(String restrictionKey) {
        return false;
    }

    /**
     * Return the serial number for a user.  This is a device-unique
     * number assigned to that user; if the user is deleted and then a new
     * user created, the new users will not be given the same serial number.
     * @param user The user whose serial number is to be retrieved.
     * @return The serial number of the given user; returns -1 if the
     * given UserHandle does not exist.
     * @see #getUserForSerialNumber(long)
     */
    public long getSerialNumberForUser(UserHandle user) {
        return 0;
    }

    /**
     * Return the user associated with a serial number previously
     * returned by {@link #getSerialNumberForUser(UserHandle)}.
     * @param serialNumber The serial number of the user that is being
     * retrieved.
     * @return Return the user associated with the serial number, or null
     * if there is not one.
     * @see #getSerialNumberForUser(UserHandle)
     */
    public UserHandle getUserForSerialNumber(long serialNumber) {
        return UserHandle.of(UserHandle.myUserId());
    }


    /**
     * Returns an intent to create a user for the provided name and account name. The name
     * and account name will be used when the setup process for the new user is started.
     * <p>
     * The intent should be launched using startActivityForResult and the return result will
     * indicate if the user consented to adding a new user and if the operation succeeded. Any
     * errors in creating the user will be returned in the result code. If the user cancels the
     * request, the return result will be {@link Activity#RESULT_CANCELED}. On success, the
     * result code will be {@link Activity#RESULT_OK}.
     * <p>
     * Use {@link #supportsMultipleUsers()} to first check if the device supports this operation
     * at all.
     * <p>
     * The new user is created but not initialized. After switching into the user for the first
     * time, the preferred user name and account information are used by the setup process for that
     * user.
     *
     * @param userName Optional name to assign to the user.
     * @param accountName Optional account name that will be used by the setup wizard to initialize
     *                    the user.
     * @param accountType Optional account type for the account to be created. This is required
     *                    if the account name is specified.
     * @param accountOptions Optional bundle of data to be passed in during account creation in the
     *                       new user via {@link AccountManager#addAccount(String, String, String[],
     *                       Bundle, android.app.Activity, android.accounts.AccountManagerCallback,
     *                       Handler)}.
     * @return An Intent that can be launched from an Activity.
     * @see #USER_CREATION_FAILED_NOT_PERMITTED
     * @see #USER_CREATION_FAILED_NO_MORE_USERS
     * @see #supportsMultipleUsers
     */
    public static Intent createUserCreationIntent(@Nullable String userName,
            @Nullable String accountName,
            @Nullable String accountType, @Nullable PersistableBundle accountOptions) {
        Intent intent = new Intent(ACTION_CREATE_USER);
        if (userName != null) {
            intent.putExtra(EXTRA_USER_NAME, userName);
        }
        if (accountName != null && accountType == null) {
            throw new IllegalArgumentException("accountType must be specified if accountName is "
                    + "specified");
        }
        if (accountName != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_NAME, accountName);
        }
        if (accountType != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_TYPE, accountType);
        }
        if (accountOptions != null) {
            intent.putExtra(EXTRA_USER_ACCOUNT_OPTIONS, accountOptions);
        }
        return intent;
    }

    /**
     * Return the number of users currently created on the device.
     */
    public int getUserCount() {
        return 1;
    }

    /**
     * Returns a list of UserHandles for profiles associated with the user that the calling process
     * is running on, including the user itself.
     * <p>Note that this includes all profile types (not including Restricted profiles).
     *
     * @return A non-empty list of UserHandles associated with the calling user.
     */
    public List<UserHandle> getUserProfiles() {
        int[] userIds = getProfileIds(UserHandle.myUserId(), true /* enabledOnly */);
        List<UserHandle> result = new ArrayList<>(userIds.length);
        for (int userId : userIds) {
            result.add(UserHandle.of(userId));
        }
        return result;
    }


    /**
     * Returns a list of ids for profiles associated with the context user including the user
     * itself.
     * <p>Note that this includes all profile types (not including Restricted profiles).
     *
     * @param enabledOnly whether to return only {@link UserInfo#isEnabled() enabled} profiles
     * @return A non-empty list of UserHandles associated with the calling user.
     */
    private @NonNull List<UserHandle> getProfiles(boolean enabledOnly) {
        final int[] userIds = getProfileIds(mUserId, enabledOnly);
        final List<UserHandle> result = new ArrayList<>(userIds.length);
        for (int userId : userIds) {
            result.add(UserHandle.of(userId));
        }
        return result;
    }

    /**
     * Returns a list of ids for profiles associated with the specified user including the user
     * itself.
     * <p>Note that this includes all profile types (not including Restricted profiles).
     *
     * @param userId      id of the user to return profiles for
     * @param enabledOnly whether return only {@link UserInfo#isEnabled() enabled} profiles
     * @return A non-empty list of ids of profiles associated with the specified user.
     *
     * @hide
     */
    public @NonNull int[] getProfileIds(int userId, boolean enabledOnly) {
        return new int[]{userId};
    }

    public boolean requestQuietModeEnabled(boolean enableQuietMode, @NonNull UserHandle userHandle) {
        return false;
    }

    public boolean requestQuietModeEnabled(boolean enableQuietMode, @NonNull UserHandle userHandle,int flags) {
        return false;
    }

    public boolean isQuietModeEnabled(UserHandle userHandle) {
        // there is no such quiet mode
        return false;
    }


    /**
     * Returns a {@link Bundle} containing any saved application restrictions for this user, for the
     * given package name. Only an application with this package name can call this method.
     *
     * <p>The returned {@link Bundle} consists of key-value pairs, as defined by the application,
     * where the types of values may be:
     * <ul>
     * <li>{@code boolean}
     * <li>{@code int}
     * <li>{@code String} or {@code String[]}
     * <li>From {@link android.os.Build.VERSION_CODES#M}, {@code Bundle} or {@code Bundle[]}
     * </ul>
     *
     * <p>NOTE: The method performs disk I/O and shouldn't be called on the main thread
     *
     * @param packageName the package name of the calling application
     * @return a {@link Bundle} with the restrictions for that package, or an empty {@link Bundle}
     * if there are no saved restrictions.
     *
     * @see #KEY_RESTRICTIONS_PENDING
     */
    public Bundle getApplicationRestrictions(String packageName) {
        return new Bundle();
    }

    /**
     * Sets a new challenge PIN for restrictions. This is only for use by pre-installed
     * apps and requires the MANAGE_USERS permission.
     * @param newPin the PIN to use for challenge dialogs.
     * @return Returns true if the challenge PIN was set successfully.
     * @deprecated The restrictions PIN functionality is no longer provided by the system.
     * This method is preserved for backwards compatibility reasons and always returns false.
     */
    @Deprecated
    public boolean setRestrictionsChallenge(String newPin) {
        return false;
    }

    /**
     * Returns creation time of the given user. The given user must be the calling user or
     * a profile associated with it.
     * @param userHandle user handle of the calling user or a profile associated with the
     *                   calling user.
     * @return creation time in milliseconds since Epoch time.
     */
    public long getUserCreationTime(UserHandle userHandle) {
        return 0;
    }
}
