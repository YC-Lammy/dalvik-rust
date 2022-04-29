package android.webkit;

public class WebMessage extends Object{
    private String mdata;
    private WebMessagePort[] mports;

    public WebMessage(String data){
        mdata = data;
    }

    public WebMessage(String data, WebMessagePort[] ports){
        mports = ports;
        mdata = data;
    }

    public String getData(){
        return mdata;
    }

    public WebMessagePort[] getPorts(){
        return mports;
    }
}
