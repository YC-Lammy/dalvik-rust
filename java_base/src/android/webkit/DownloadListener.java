package android.webkit;

public interface DownloadListener {
    abstract void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength);
}
