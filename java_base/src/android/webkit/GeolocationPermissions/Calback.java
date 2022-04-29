package android.webkit.GeolocationPermissions;

public interface Calback {
    abstract void invoke(String origin, boolean allow, boolean retain);
}
