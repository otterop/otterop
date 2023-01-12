package otterop.lang;

public class String implements Comparable {
    private java.lang.String wrapped;

    private String(java.lang.String wrapped) {
        this.wrapped = wrapped;
    }

    public int length() {
        return this.wrapped.length();
    }

    public java.lang.String toString() {
        return wrapped;
    }

    public static String wrap(java.lang.String wrapped) {
        if (wrapped == null) return null;
        return new String(wrapped);
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof String)) return 1;
        return this.wrapped.compareTo(((String)o).wrapped);
    }
}
