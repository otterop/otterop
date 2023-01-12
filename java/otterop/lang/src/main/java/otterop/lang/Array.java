
package otterop.lang;

public class Array<T> {
    private T[] _wrapped;

    private Array(T[] array) {
        this._wrapped = array;
    }

    public T get(int i) {
        return _wrapped[i];
    }

    public void set(int i, T value) {
        _wrapped[i] = value;
    }

    public int size() {
        return _wrapped.length;
    }

    public static <T> Array<T> wrap(T[] list) {
        return new Array<T>(list);
    }

    public static Array<String> wrapString(java.lang.String[] arg) {
        String[] ret = new String[arg.length];
        for (int i = 0; i < arg.length; i++) {
            ret[i] = String.wrap(arg[i]);
        }
        return new Array<>(ret);
    }
}

