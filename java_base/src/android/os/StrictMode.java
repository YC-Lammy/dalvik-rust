package android.os;

import java.util.concurrent.Executor;

import android.os.strictmode.Violation;

/**
 * the strict mode implementation here is just a dummy.
 * this project does not intend to provide debugging interface.
 */
public final class StrictMode {

    public static interface OnThreadViolationListener{
        public abstract void onThreadViolation(Violation v); 
    }

    public static interface OnVmViolationListener{
        public abstract void onVmViolation(Violation v);
    }

    public static final class ThreadPolicy {
        public static final ThreadPolicy LAX = new ThreadPolicy();

        private ThreadPolicy(){

        }

        public static final class Builder{
            public Builder(){

            }

            public Builder(ThreadPolicy policy){

            }

            public Builder detectAll(){
                return this;
            }

            public Builder detectCustomSlowCalls() {
                return this;
            }

            public Builder detectDiskReads(){
                return this;
            }

            public Builder detectDiskWrites() {
                return this;
            }

            public Builder detectNetwork() {
                return this;
            }

            public Builder detectResourceMismatches(){
                return this;
            }

            public Builder detectUnbufferedIo() {
                return this;
            }

            public Builder penaltyDeath() {
                return this;
            }

            public Builder penaltyDeathOnNetwork() {
                return this;
            }

            public Builder penaltyDialog() {
                return this;
            }

            public Builder penaltyDropBox() {
                return this;
            }

            public Builder penaltyFlashScreen() {
                return this;
            }

            public Builder penaltyListener(Executor executor, StrictMode.OnThreadViolationListener listener){
                return this;
            }

            public Builder penaltyLog(){
                return this;
            }

            public Builder permitAll() {
                return this;
            }

            public Builder permitCustomSlowCalls() {
                return this;
            }

            public Builder permitDiskReads() {
                return this;
            }

            public Builder permitDiskWrites() {
                return this;
            }

            public Builder permitNetwork() {
                return this;
            }

            public Builder permitResourceMismatches() {
                return this;
            }

            public Builder permitUnbufferedIo() {
                return this;
            }

            public ThreadPolicy build(){
                return new ThreadPolicy();
            }
        }

        @Override
        public String toString() {
            return getClass().getName() + '@' + Integer.toHexString(hashCode());
        }
    }

    public static final class VmPolicy{
        public static final VmPolicy LAX = new VmPolicy();

        private VmPolicy(){

        }

        public static final class Builder{
            public Builder(){

            }

            public Builder(VmPolicy policy){

            }

            public VmPolicy build(){
                return new VmPolicy();
            }

            public StrictMode.VmPolicy.Builder detectActivityLeaks(){
                return this;
            }
            public StrictMode.VmPolicy.Builder detectAll(){
                return this;
            }
            public StrictMode.VmPolicy.Builder detectCleartextNetwork(){
                return this;
            }
            public StrictMode.VmPolicy.Builder detectContentUriWithoutPermission(){
                return this;
            }
            public StrictMode.VmPolicy.Builder detectCredentialProtectedWhileLocked(){
                return this;
            }

            public StrictMode.VmPolicy.Builder detectFileUriExposure(){
                return this;
            }

            
            public StrictMode.VmPolicy.Builder detectImplicitDirectBoot(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectIncorrectContextUse(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectLeakedClosableObjects(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectLeakedRegistrationObjects(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectLeakedSqlLiteObjects(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectNonSdkApiUsage(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectUnsafeIntentLaunch(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder detectUntaggedSockets(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyDeath(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyDeathOnCleartextNetwork(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyDeathOnFileUriExposure(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyDropBox(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyListener(Executor executor, StrictMode.OnVmViolationListener listener){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder penaltyLog(){
                return this;
            }
            
            public StrictMode.VmPolicy.Builder permitNonSdkApiUsage(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder permitUnsafeIntentLaunch(){
                return this;
            }
            
            
            public StrictMode.VmPolicy.Builder setClassInstanceLimit(Class klass, int instanceLimit){
                return this;
            }
            
            
        }

        @Override
        public String toString() {
            return getClass().getName() + '@' + Integer.toHexString(hashCode());
        }
    }

    public static ThreadPolicy allowThreadDiskReads(){
        return ThreadPolicy.LAX;
    }

    public static ThreadPolicy allowThreadDiskWrites() {
        return ThreadPolicy.LAX;
    }

    public static void enableDefaults() {

    }

    public static ThreadPolicy getThreadPolicy() {
        return ThreadPolicy.LAX;
    }

    public static VmPolicy getVmPolicy() {
        return VmPolicy.LAX;
    }

    public static void noteSlowCall(String name) {

    }

    public static void setThreadPolicy(ThreadPolicy policy){

    }

    public static void setVmPolicy(VmPolicy policy){
        
    }

    /**@hide */
    public static void clearGatheredViolations(){
        
    }
}