package android.webkit;

public interface ValueCallback<T> {
    abstract void onReceiveValue(T value);
}
