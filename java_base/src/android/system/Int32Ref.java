package android.system;

import android.text.TextUtils;

/**
 * @hide
 * A signed 32bit integer reference suitable for passing to lower-level system calls.
 */
public class Int32Ref {

    public int value;
    public Int32Ref(int value) {
        this.value = value;
    }
    @Override 
    public String toString() {
        return TextUtils.toString(this);
    }
}