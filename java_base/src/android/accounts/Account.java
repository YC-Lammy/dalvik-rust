package android.accounts;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Account implements Parcelable{

    public Account(){

    }
    public static final Creator<Account> CREATOR = new Creator<>(){
        public Account createFromParcel(android.os.Parcel source) {
            return new Account();
        };

        public Account[] newArray(int size) {
            return new Account[size];
        };
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
