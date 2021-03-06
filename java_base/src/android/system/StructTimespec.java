package android.system;

/**
 * Corresponds to C's {@code struct timespec} from {@code <time.h>}.
 */
public final class StructTimespec implements Comparable<StructTimespec> {
    /** Seconds part of time of last data modification. */
    public final long tv_sec; /*time_t*/
    /** Nanoseconds (values are [0, 999999999]). */
    public final long tv_nsec;
    public StructTimespec(long tv_sec, long tv_nsec) {
        this.tv_sec = tv_sec;
        this.tv_nsec = tv_nsec;
        if (tv_nsec < 0 || tv_nsec > 999_999_999) {
            throw new IllegalArgumentException(
                    "tv_nsec value " + tv_nsec + " is not in [0, 999999999]");
        }
    }
    @Override
    public int compareTo(StructTimespec other) {
        if (tv_sec > other.tv_sec) {
            return 1;
        }
        if (tv_sec < other.tv_sec) {
            return -1;
        }
        if (tv_nsec > other.tv_nsec) {
            return 1;
        }
        if (tv_nsec < other.tv_nsec) {
            return -1;
        }
        return 0;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructTimespec that = (StructTimespec) o;
        if (tv_sec != that.tv_sec) return false;
        return tv_nsec == that.tv_nsec;
    }
    @Override
    public int hashCode() {
        int result = (int) (tv_sec ^ (tv_sec >>> 32));
        result = 31 * result + (int) (tv_nsec ^ (tv_nsec >>> 32));
        return result;
    }
    @Override
    public String toString() {
        return Long.toString(tv_sec) + Long.toString(tv_nsec);
    }
}