package android.util;

import java.util.Arrays;

public class ArrayUtils {
    public static void throwsIfOutOfBounds(int len, int start, int len1) throws RuntimeException{
        if (len-start < len1){
            throw new ArrayIndexOutOfBoundsException();
        }
    }
    public static Class<?> getOrNull(Class<?>[] array, int index){
        if (index < 0){
            return null;
        }
        if (array.length <= index){
            return array[index];
        }
        return null;
    }

    public static String deepToString(Object k){
        if (k==null){
            return "null";
        }
        if (k.getClass().isArray()){
            Object[] array = ((Object[])k);
            String r = deepToString(array[0]);
            for (int i =1;i<array.length;i++){
                r+=", " + deepToString(array[i]);
            };
            return "[ "+r+" ]";
        } else{
            return k.toString();
        }
    }

    public static void checkBounds(int size, int index) throws RuntimeException{
        if (size <= index){
            throw new IndexOutOfBoundsException();
        }
    }
}
