package otterop.lang;

import java.util.Iterator;
import java.util.function.Function;

public class WrapperOOPIterator<FROM,TO> implements OOPIterator<TO>, Iterator<TO> {
    private Iterator<FROM> it;
    private Function<FROM,TO> wrap;

    public WrapperOOPIterator(Iterator<FROM> it, Function<FROM,TO> wrap) {
        this.it = it;
        this.wrap = wrap;
    }

    @Override
    public boolean hasNext() {
        return this.it.hasNext();
    }

    @Override
    public TO next() {
        return this.wrap.apply(it.next());
    }
}
