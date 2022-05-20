package android.net;

import java.net.InetAddress;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteInfo implements Parcelable {

    public static final int RTN_THROW = 9;
    public static final int RTN_UNICAST = 1;
    public static final int RTN_UNREACHABLE = 7;
    
    public static final Creator<RouteInfo> CREATOR = new Creator<RouteInfo>() {

        public RouteInfo createFromParcel(Parcel source) {

            try{
                return new RouteInfo(
                InetAddress.getByAddress(source.readBlob()), 
                source.readString(), 
                source.readInt(), 
                source.readParcelable(IpPrefix.class.getClassLoader(), IpPrefix.class));
            }  catch(Exception e){
                throw  new RuntimeException(e);
            }
        };

        public RouteInfo[] newArray(int size) {
            return new RouteInfo[size];
        };
    };


    private InetAddress mAddress;
    private String mIface;
    private int mType;
    private IpPrefix mPrefix;

    public RouteInfo(InetAddress address, String iface, int type, IpPrefix prefix){
        mAddress = address;
        mIface = iface;
        mType = type;
        mPrefix = prefix;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBlob(mAddress.getAddress());
        dest.writeString(mIface);
        dest.writeInt(mType);
        dest.writeParcelable(mPrefix, 0);
    }

    public IpPrefix getDestination() {
        return mPrefix;
    }

    public InetAddress getGateway(){
        return mAddress;
    } 

    public String getInterface (){
        return mIface;
    }

    public int getType (){
        if (mAddress == null){
            mType = RTN_THROW;
            return RTN_THROW;
        }

        try{
            if (mType != RTN_UNICAST){
                if (mAddress.isReachable(500)){
                    mType = RTN_UNICAST;
                }
            }
        } catch (Exception e){
            mType = RTN_THROW;
        }
        
        return mType;
    }

    public boolean hasGateway (){
        return mAddress != null;
    }

    public boolean isDefaultRoute (){
        if (mPrefix == null){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return mPrefix.toString() + mAddress.toString();
    }
}
