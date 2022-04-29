package android.util;

public class SneakyThrow {

    public static void sneakyThrow(Throwable t) {
        SneakyThrow.<RuntimeException>sneakyThrow_(t);
    }
    private static <T extends Throwable> void sneakyThrow_(Throwable t) throws T {
       throw (T) t;
    }
}
