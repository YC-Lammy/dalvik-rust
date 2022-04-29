package android.system;

import android.text.TextUtils;

/**
 * Information returned by {@link Os#uname}.
 * Corresponds to C's {@code struct utsname} from {@code <sys/utsname.h>}.
 */
public final class StructUtsname {
    /** The OS name, such as "Linux". */
    public final String sysname;
    /** The machine's unqualified name on some implementation-defined network. */
    public final String nodename;
    /** The OS release, such as "2.6.35-27-generic". */
    public final String release;
    /** The OS version, such as "#48-Ubuntu SMP Tue Feb 22 20:25:29 UTC 2011". */
    public final String version;
    /** The machine architecture, such as "armv7l" or "x86_64". */
    public final String machine;
    /**
     * Constructs an instance with the given field values.
     */
    public StructUtsname(String sysname, String nodename, String release, String version, String machine) {
        this.sysname = sysname;
        this.nodename = nodename;
        this.release = release;
        this.version = version;
        this.machine = machine;
    }
    @Override public String toString() {
        return TextUtils.toString(this);
    }
}