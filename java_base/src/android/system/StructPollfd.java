package android.system;
import java.io.FileDescriptor;

import android.text.TextUtils;


/**
 * Used as an in/out parameter to {@link Os#poll}.
 * Corresponds to C's {@code struct pollfd} from {@code <poll.h>}.
 */
public final class StructPollfd {
    /** The file descriptor to poll. */
    public FileDescriptor fd;
    /**
     * The events we're interested in. POLLIN corresponds to being in select(2)'s read fd set,
     * POLLOUT to the write fd set.
     */
    public short events;
    /** The events that actually happened. */
    public short revents;
    /**
     * A non-standard extension that lets callers conveniently map back to the object
     * their fd belongs to. This is used by Selector, for example, to associate each
     * FileDescriptor with the corresponding SelectionKey.
     */
    public Object userData;

    @Override
    public String toString() {
        return TextUtils.toString(this);
    }
}