package otterop.lang;

import java.util.Iterator;

public class WrapperOOPIterator<T> implements OOPIterator<T> {
    private Iterator<T> it;

    private WrapperOOPIterator(Iterator<T> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return this.it.hasNext();
    }

    @Override
    public T next() {
        return it.next();
    }

    public static <T> OOPIterator<T> wrap(Iterator<T> it) {
        return new WrapperOOPIterator<T>(it);
    }
}
