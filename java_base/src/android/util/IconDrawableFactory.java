/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

/**
 * Utility class to load app drawables with appropriate badging.
 *
 * @hide
 */
public class IconDrawableFactory {

    protected final Context mContext;
    protected final PackageManager mPm;
    protected final UserManager mUm;
    protected final LauncherIcons mLauncherIcons;
    protected final boolean mEmbedShadow;

    private IconDrawableFactory(Context context, boolean embedShadow) {
        mContext = context;
        mPm = context.getPackageManager();
        mUm = context.getSystemService(UserManager.class);
        mLauncherIcons = new LauncherIcons(context);
        mEmbedShadow = embedShadow;
    }

    protected boolean needsBadging(ApplicationInfo appInfo, int userId) {
        return appInfo.isInstantApp() || mUm.hasBadge(userId);
    }

    public Drawable getBadgedIcon(ApplicationInfo appInfo) {
        return getBadgedIcon(appInfo, UserHandle.getUserId(appInfo.uid));
    }

    public Drawable getBadgedIcon(ApplicationInfo appInfo, int userId) {
        return getBadgedIcon(appInfo, appInfo, userId);
    }

    
    public Drawable getBadgedIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo,
            int userId) {
        Drawable icon = mPm.loadUnbadgedItemIcon(itemInfo, appInfo);
        if (!mEmbedShadow && !needsBadging(appInfo, userId)) {
            return icon;
        }

        icon = getShadowedIcon(icon);
        if (appInfo.isInstantApp()) {
            int badgeColor = Resources.getSystem().getColor(
                    android.R.color.instant_app_badge, null);
            icon = mLauncherIcons.getBadgedDrawable(icon,
                    android.R.drawable.ic_instant_icon_badge_bolt,
                    badgeColor);
        }
        if (mUm.hasBadge(userId)) {
            icon = mLauncherIcons.getBadgedDrawable(icon,
                    mUm.getUserIconBadgeResId(userId),
                    mUm.getUserBadgeColor(userId));
        }
        return icon;
    }

    /**
     * Add shadow to the icon if {@link AdaptiveIconDrawable}
     */
    public Drawable getShadowedIcon(Drawable icon) {
        return mLauncherIcons.wrapIconDrawableWithShadow(icon);
    }

    
    public static IconDrawableFactory newInstance(Context context) {
        return new IconDrawableFactory(context, true);
    }

    public static IconDrawableFactory newInstance(Context context, boolean embedShadow) {
        return new IconDrawableFactory(context, embedShadow);
    }
}
