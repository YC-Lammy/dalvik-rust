package android.webkit;

import android.graphics.Bitmap;

public abstract class WebHistoryItem extends Object implements Cloneable{
    public WebHistoryItem(){

    }

    public abstract Bitmap getFavicon();

    public abstract String getOriginalUrl();

    public abstract String getTitle();

    public abstract String getUrl();

    protected abstract WebHistoryItem clone();
}
