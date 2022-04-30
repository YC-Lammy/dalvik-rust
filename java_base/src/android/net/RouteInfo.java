package android.net;

import android.os.Parcelable;

public class RouteInfo implements Parcelable {
    
    public static final Creator<RouteInfo> CREATOR = new Creator<RouteInfo>() {
        
        public RouteInfo[] newArray(int size) {
            return new RouteInfo[size];
        };
    };
}
