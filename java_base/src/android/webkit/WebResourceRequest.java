package android.webkit;

import java.util.Map;

import android.net.Uri;

public interface WebResourceRequest {
    abstract String getMethod();
    abstract Map<String, String> getRequestHeaders();
    abstract Uri getUrl();
    abstract boolean hasGesture();
    abstract boolean isForMainFrame();
    abstract boolean isRedirect();
}
