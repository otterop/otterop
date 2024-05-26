package otterop.lang;

import java.util.Iterator;

public class PureIterator<T> implements Iterator<T> {

    private OOPIterator<T> oopIterator;

    private PureIterator(OOPIterator<T> oopIterator) {
        this.oopIterator = oopIterator;
    }

    public static <T> Iterator<T> newIterator(OOPIterator<T> oopIterator) {
        return new PureIterator<T>(oopIterator);
    }

    @Override
    public boolean hasNext() {
        return this.oopIterator.hasNext();
    }

    @Override
    public T next() {
        return this.oopIterator.next();
    }
}
