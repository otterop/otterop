package otterop.lang;

import java.util.Iterator;
import java.util.function.Function;

public class WrapperOOPIterable<FROM, TO> implements OOPIterable<TO> {

    private Iterable<FROM> it;
    Function<FROM, TO> wrap;

    private WrapperOOPIterable(Iterable<FROM> it, Function<FROM, TO> wrap) {
        this.it = it;
        this.wrap = wrap != null ? wrap : (FROM x) -> (TO) x;
    }

    @Override
    public OOPIterator<TO> OOPIterator() {
        return new WrapperOOPIterator(this.it.iterator(), this.wrap);
    }

    @Override
    public Iterator<TO> iterator() {
        return new WrapperOOPIterator(this.it.iterator(), this.wrap);
    }

    public static <FROM, TO> OOPIterable<TO> wrap(Iterable<FROM> it, Function<FROM,TO> wrap) {
        if (wrap == null && it instanceof WrapperOOPIterable) {
            return (WrapperOOPIterable) it;
        }
        return new WrapperOOPIterable<>(it, wrap);
    }

    public static <TO, FROM> Iterable<FROM> unwrap(OOPIterable<TO> it, Function<TO,FROM> unwrap) {
        if (unwrap == null) {
            return (Iterable<FROM>) it;
        }
        return new WrapperOOPIterable<>(it, unwrap);
    }
}
