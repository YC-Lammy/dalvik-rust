package android.system;
import android.annotation.NonNull;
import android.text.TextUtils;

/**
 * Corresponds to C's {@code struct timeval} from {@code sys/time.h}.
 */
public final class StructTimeval {
    /** Seconds. */
    public final long tv_sec;
    /** Microseconds. */
    public final long tv_usec;
    private StructTimeval(long tv_sec, long tv_usec) {
        this.tv_sec = tv_sec;
        this.tv_usec = tv_usec;
        if (tv_usec < 0 || tv_usec > 999_999) {
            throw new IllegalArgumentException(
                    "tv_usec value " + tv_usec + " is not in [0, 999999]");
        }
    }
    public static @NonNull StructTimeval fromMillis(long millis) {
        // tv_sec can be positive or negative. tv_usec can only be positive. Negative numbers are
        // represented by rounding down to the nearest whole second <= the one we need
        // (i.e. floor()) and adding the necessary micro seconds.
        long tv_sec = millis / 1000;
        if (tv_sec * 1000 > millis) {
            --tv_sec;
        }
        long tv_usec = (millis - (tv_sec * 1000)) * 1000;
        return new StructTimeval(tv_sec, tv_usec);
    }
    public long toMillis() {
        return (tv_sec * 1000) + (tv_usec / 1000);
    }
    @Override
    public String toString() {
        return TextUtils.toString(this);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StructTimeval that = (StructTimeval) o;
        return tv_sec == that.tv_sec &&
                tv_usec == that.tv_usec;
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tv_sec, tv_usec);
    }
}