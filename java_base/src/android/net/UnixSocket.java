package android.net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class UnixSocket implements Closeable, ReadableByteChannel, WritableByteChannel{

    private static native void open(UnixSocket sock, String addr);

    public static UnixSocket open(String addr){
        UnixSocket soc = new UnixSocket();
        open(soc, addr);
        return soc;
    }

    private long mNativePtr;

    private UnixSocket(){

    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return write(src.array());
    }

    public native int write(byte[] src) throws IOException;

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return read(dst.array());
    }

    public native int read(byte[] src);
}
