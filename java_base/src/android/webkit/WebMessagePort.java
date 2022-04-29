package android.webkit;

import android.os.Handler;

public abstract class WebMessagePort {
    public static abstract class WebMessageCallback extends Object{
        public WebMessageCallback(){

        }
        
        public void onMessage(WebMessagePort port, WebMessage message){

        }
    }

    public abstract void close();

    public abstract void postMessage(WebMessage message);

    public abstract void setWebMessageCallback(WebMessagePort.WebMessageCallback callback, Handler handler); 

    abstract void setWebMessageCallback(WebMessagePort.WebMessageCallback callback);
}
