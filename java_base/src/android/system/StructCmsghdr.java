package android.system;

import android.annotation.NonNull;
import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/**
 * Corresponds to C's {@code struct cmsghdr}.
 *
 */
public final class StructCmsghdr {
    /** Originating protocol */
    public final int cmsg_level;
    /** Protocol-specific type */
    public final int cmsg_type;
    /** message data sent/received */
    @NonNull 
    public final byte[] cmsg_data;

    public StructCmsghdr(int cmsg_level, int cmsg_type, short value) {
        // Short.Size unit is bits, ByteBuffer data unit is bytes
        ByteBuffer buf = ByteBuffer.allocate(Short.SIZE / 8);
        buf.order(ByteOrder.nativeOrder());
        buf.putShort(value);
        this.cmsg_level = cmsg_level;
        this.cmsg_type = cmsg_type;
        this.cmsg_data = buf.array();
    }
    public StructCmsghdr(int cmsg_level, int cmsg_type, @NonNull byte[] value) {
        this.cmsg_level = cmsg_level;
        this.cmsg_type = cmsg_type;
        this.cmsg_data = value;
    }

    @Override
    public String toString() {
        return TextUtils.toString(this);
    }
}