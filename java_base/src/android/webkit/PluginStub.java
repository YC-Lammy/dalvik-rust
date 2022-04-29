package android.webkit;

import android.view.View;
import android.context.Context;

public interface PluginStub {
    abstract View getEmbeddedView(int NPP, Context context);
    abstract View getFullScreenView(int NPP, Context context);
}
