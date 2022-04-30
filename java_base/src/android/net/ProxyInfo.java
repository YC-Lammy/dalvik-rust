package android.net;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class ProxyInfo implements Parcelable {
    public static final Creator<ProxyInfo> CREATOR = new Creator<ProxyInfo>() {

        public ProxyInfo createFromParcel(android.os.Parcel source) {
            return new ProxyInfo(
                source.readString(), 
                source.readInt(), 
                source.readStringArray(), 
                source.readParcelable(Uri.class.getClassLoader(), Uri.class));
        };
        public ProxyInfo[] newArray(int size) {
            return new ProxyInfo[size];
        };
    };

    private String mHost;
    private int mPort;
    private String[] mExclList;
    private Uri mPacUri;

    private ProxyInfo(String host, int port, String[] exclList, Uri pacUri){
        mHost = host;
        mPort = port;
        mExclList = exclList;
        mPacUri = pacUri;

    }

    public ProxyInfo(ProxyInfo source){
        mHost = source.mHost;
        mExclList = source.mExclList;
        mPort = source.mPort;
        mPacUri = source.mPacUri;
    }

    public static ProxyInfo buildDirectProxy (String host, 
            int port, 
            List<String> exclList
    ){
        return new ProxyInfo(host, port, (String[])exclList.toArray(), null);
    }

    public static ProxyInfo buildDirectProxy (String host, int port){
        return new ProxyInfo(host, port, new String[0], null);
    }

    public static ProxyInfo buildPacProxy (Uri pacUri, int port){
        return new ProxyInfo(null, port, null, pacUri);
    }

    public static ProxyInfo buildPacProxy (Uri pacUri){
        return new ProxyInfo(null, 80, null, pacUri);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProxyInfo){
            ProxyInfo info = (ProxyInfo)obj;
            return info.mExclList.equals(mExclList) &&
                    info.mHost == mHost &&
                    info.mPacUri.equals(mPacUri) &&
                    info.mPort == mPort;
        } else{
            return false;
        }
    }

    public String[] getExclusionList (){
        return mExclList;
    }

    public String getHost (){
        if (mHost == null){
            return mPacUri.getHost();
        }
        return mHost;
    }

    public Uri getPacFileUrl (){
        return mPacUri;
    }

    public int getPort (){
        return mPort;
    }

    public boolean isValid (){
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mHost);
        dest.writeInt(mPort);
        dest.writeStringArray(mExclList);
        dest.writeParcelable(mPacUri, 0);
    }

    @Override
    public int hashCode() {
        return mPort + mHost.hashCode() + mPacUri.hashCode() + mExclList.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode());
    }
}
