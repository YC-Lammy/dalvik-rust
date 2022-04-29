package android.webkit.WebIconDatabase;

import android.graphics.Bitmap;

public interface IconListener {
    abstract void onReceivedIcon(String url, Bitmap icon);
}
