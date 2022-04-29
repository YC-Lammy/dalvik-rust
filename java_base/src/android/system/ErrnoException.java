package android.system;

import java.io.IOException;
import java.net.SocketException;

public class ErrnoException extends Exception{
    private final String mFunctionName;

    public final int errno;

    public ErrnoException(String functionName, int errno_){
        mFunctionName = functionName;
        errno = errno_;
    }

    public ErrnoException(String functionName, int errno_, Throwable cause){
        super(cause);
        mFunctionName = functionName;
        errno = errno_;
    }

    @Override
    public String getMessage() {
        String errnoName = OsConstants.errnoName(errno);
        if (errnoName == null) {
            errnoName = "errno " + errno;
        }
        String description = "";
        return mFunctionName + " failed: " + errnoName + " (" + description + ")";
    }

    public IOException rethrowAsIOException () throws IOException{
        throw new IOException(getMessage(), this);
    }

    public SocketException rethrowAsSocketException () throws SocketException{
        SocketException s = new SocketException(getMessage());
        s.initCause(this);
        throw s;
    }
}
