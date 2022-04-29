package android.system;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.text.TextUtils;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
/**
 * Corresponds to C's {@code struct msghdr}
 *
 */
public final class StructMsghdr{
    /**
     * Optional address.
     * <p>Sendmsg: Caller must populate to specify the target address for a datagram, or pass
     * {@code null} to send to the destination of an already-connected socket.
     * Recvmsg: Populated by the system to specify the source address.
     */
    @Nullable 
    public SocketAddress msg_name;
    /** Scatter/gather array */
    @NonNull 
    public final ByteBuffer[] msg_iov;
    /** Ancillary data */
    @Nullable 
    public StructCmsghdr[] msg_control;
    /** Flags on received message. */
    public int msg_flags;
    /**
     * Constructs an instance with the given field values
     */
    public StructMsghdr(@Nullable SocketAddress msg_name, @NonNull ByteBuffer[] msg_iov,
                        @Nullable StructCmsghdr[] msg_control, int msg_flags) {
        this.msg_name = msg_name;
        this.msg_iov = msg_iov;
        this.msg_control = msg_control;
        this.msg_flags = msg_flags;
    }

    @Override
    public String toString() {
        return TextUtils.toString(this);
    }
}