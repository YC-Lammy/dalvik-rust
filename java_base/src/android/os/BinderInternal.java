package android.os;

import android.util.SparseIntArray;

public class BinderInternal {

    public static native SparseIntArray getBinderProxyPerUidCounts();
    /**
     * A session used by {@link Observer} in order to keep track of some data.
     */
    public static class CallSession {
        // Binder interface descriptor.
        public Class<? extends Binder> binderClass;
        // Binder transaction code.
        public int transactionCode;
        // CPU time at the beginning of the call.
        long cpuTimeStarted;
        // System time at the beginning of the call.
        long timeStarted;
        // Should be set to one when an exception is thrown.
        boolean exceptionThrown;
        // Detailed information should be recorded for this call when it ends.
        public boolean recordedCall;
    }

    /**
     * Responsible for resolving a work source.
     */
    @FunctionalInterface
    public interface WorkSourceProvider {
        /**
         * <p>This method is called in a critical path of the binder transaction.
         * <p>The implementation should never execute a binder call since it is called during a
         * binder transaction.
         *
         * @param untrustedWorkSourceUid The work source set by the caller.
         * @return the uid of the process to attribute the binder transaction to.
         */
        int resolveWorkSourceUid(int untrustedWorkSourceUid);
    }

    /**
     * Allows to track various steps of an API call.
     */
    public interface Observer {
        /**
         * Called when a binder call starts.
         *
         * @return a CallSession to pass to the callEnded method.
         */
        CallSession callStarted(Binder binder, int code, int workSourceUid);

        /**
         * Called when a binder call stops.
         *
         * <li>This method will be called even when an exception is thrown by the binder stub
         * implementation.
         */
        void callEnded(CallSession s, int parcelRequestSize, int parcelReplySize,
                int workSourceUid);

        /**
         * Called if an exception is thrown while executing the binder transaction.
         *
         * <li>BinderCallsStats#callEnded will be called afterwards.
         * <li>Do not throw an exception in this method, it will swallow the original exception
         * thrown by the binder transaction.
         */
        public void callThrewException(CallSession s, Exception exception);
    }
}
