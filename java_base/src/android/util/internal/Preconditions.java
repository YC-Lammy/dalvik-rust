package android.util.internal;

import org.w3c.dom.ranges.RangeException;

public class Preconditions {
    public static <T> T checkNotNull(T v){
        if (v==null){
            throw new NullPointerException();
        }
        return v;
    }

    public static <T> T checkNotNull(T v, String reason){
        if (v==null){
            throw new NullPointerException(reason);
        }
        return v;
    }

    public static void checkArgument(boolean b, String reason){
        if (!b){
            throw new RuntimeException(reason);
        }
    }

    public static int checkArgumentInRange(int value, int from, int to, String reason){
        if (value>= from && value <= to){
            return value;
        } else{
            throw new RangeException((short)value, reason);
        }
    }

    public static float checkArgumentInRange(float value, float from, float to, String reason){
        if (value>= from && value <= to){
            return value;
        } else{
            throw new RangeException((short)value, reason);
        }
    }

    public static int checkArgumentNonnegative(int value,String reason){
        if (value <= 0){
            throw new RangeException((short)value, reason);
        }
        return value;
    }

    public static long checkArgumentPositive(long value, String reason){
        if (value<=0){
            throw new RangeException((short)value, reason);
        }
        return value;
    }
}
