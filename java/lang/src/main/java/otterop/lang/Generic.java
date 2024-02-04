package otterop.lang;

public class Generic<T> {
    public T zero() {
        return null;
    }

    public static <T> boolean isZero(T arg) {
        return arg == null;
    }
}
