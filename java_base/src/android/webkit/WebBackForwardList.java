package android.webkit;

import java.io.Serializable;

public abstract class WebBackForwardList extends Object implements Cloneable, Serializable{
    public WebBackForwardList(){

    }

    public abstract int getCurrentIndex();

    public abstract WebHistoryItem getCurrentItem();

    public abstract WebHistoryItem getItemAtIndex(int index);

    public abstract int getSize();

    protected abstract WebBackForwardList clone();
}
