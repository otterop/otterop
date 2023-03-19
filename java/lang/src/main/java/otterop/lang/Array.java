
package otterop.lang;

public class Array<T> {
    private final int start;
    private final int end;
    private T[] _wrapped;

    private Array(T[] array, int start, int end) {
        this._wrapped = array;
        this.start = start;
        this.end = end;
    }

    public T get(int i) {
        return _wrapped[start + i];
    }

    public void set(int i, T value) {
        _wrapped[start + i] = value;
    }

    public int size() {
        return end - start;
    }

    public Array<T> slice(int start, int end) {
        var newStart = this.start + start;
        var newEnd = this.start + end;
        if (newStart < this.start || newStart > this.end || newEnd < newStart ||
                newEnd > this.end) throw new ArrayIndexOutOfBoundsException();
        return new Array<T>(_wrapped, newStart, newEnd);
    }

    public static <T> Array<T> wrap(T[] list) {
        return new Array<T>(list,0 ,list.length);
    }

    public static Array<String> wrapString(java.lang.String[] arg) {
        String[] ret = new String[arg.length];
        for (int i = 0; i < arg.length; i++) {
            ret[i] = String.wrap(arg[i]);
        }
        return new Array<>(ret, 0, ret.length);
    }
}

