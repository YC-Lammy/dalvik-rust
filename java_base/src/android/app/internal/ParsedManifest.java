package android.app.internal;

import java.util.ArrayList;

public class ParsedManifest {

    public class Activity{
        public IntentFilter[] intentFilter;
        public MetaData[] metaData;
        public Layout layout;

        public boolean allowEmbedded;
        public boolean allowTaskReparenting;
        public boolean alwaysRetainTaskState;
        public boolean autoRemoveFromRecents;
        public String banner;
        public boolean clearTaskOnLaunch;
        public String colorMode;
        public String[] configChanges;
        public boolean directBootAware;
        public String documentLaunchMode;
        public boolean enabled;
        public boolean excludeFromRecents;
        public boolean exported;
        public boolean finishOnTaskLaunch;
        public boolean hardwareAccelerated;
        public String icon;
        public boolean immersive;
        public String label;
        public String launchMode;
        public String lockTaskMode;
        public int maxRecents;
        public float maxAspectRatio;
        public boolean multiprocess;
        public String name;
        public boolean noHistory;
        public String parentActivityName;
        public String persistableMode;
        public String permission;
        public String process;
        public boolean relinquishTaskIdentity;
        public boolean resizeableActivity;
        public String screenOrientation;
        public boolean showForAllUsers;
        public boolean stateNotNeeded;
        public boolean supportsPictureInPicture;
        public String taskAffinity;
        public String theme;
        public String[] windowSoftInputMode;
    }

    public class Service{
        public IntentFilter[] intentFilter;
        public MetaData[] metaData;

        public String description;
        public boolean directBootAware;
        public boolean enabled;
        public boolean exported;
        public String foregroundServiceType;
        public String icon;
        public boolean isolatedProcess;
        public String label;
        public String name;
        public String permission;
        public String process;
    }

    public class Reciever{
        public IntentFilter[] intentFilter;
        public MetaData[] metaData;

        public boolean directBootAware;
        public boolean enabled;
        public boolean exported;
        public String icon;
        public String label;
        public String name;
        public String permission;
        public String process;
    }

    public class Provider{
        public IntentFilter[] intentFilter;
        public MetaData[] metaData;
        public GrantedUriPermission[] grantedUriPermission;
        public PathPermission[] pathPermission;

        public String[] authorities;
        public boolean directBootAware;
        public boolean enabled;
        public boolean exported;
        public boolean grantUriPermission;
        public String icon;
        public int initOrder;
        public String label;
        public boolean multiprocess;
        public String name;
        public String permission;
        public String process;
        public String readPermission;
        public boolean syncable;
        public String writePermission;
    }


    public class IntentFilter{
        public String icon;
        public String label;
        public int priority;
    }

    public class MetaData{
        public String name;
        public String resources;
        public String value;
    }

    public class Layout{
        public String defaultHeight;
        public String defaultWidth;
        public String gravity;
        public String minHeight;
        public String minWidth;
    }

    public class GrantedUriPermission{

    }

    public class PathPermission{

    }

    public static String Package_Name = "";

    public static ArrayList<Activity> Activities = new ArrayList<Activity>();
    public static ArrayList<Service> Services = new ArrayList<Service>();
    public static ArrayList<Reciever> Recievers = new ArrayList<Reciever>();
    public static ArrayList<Provider> Providers = new ArrayList<Provider>();
}
