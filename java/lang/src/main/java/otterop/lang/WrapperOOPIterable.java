package otterop.lang;

import java.util.Iterator;

public class WrapperOOPIterable<T> implements OOPIterable<T> {

    private Iterable<T> it;

    private WrapperOOPIterable(Iterable<T> it) {
        this.it = it;
    }

    @Override
    public OOPIterator<T> OOPIterator() {
        return WrapperOOPIterator.wrap(this.it.iterator());
    }

    @Override
    public Iterator<T> iterator() {
        return this.it.iterator();
    }

    public static <T> OOPIterable<T> wrap(Iterable<T> it) {
        return new WrapperOOPIterable<T>(it);
    }
}
