package otterop.lang;

public class Panic {

    public static void indexOutOfBounds(String message) {
        throw new IndexOutOfBoundsException();
    }

    public static void invalidOperation(String message) {
        throw new IllegalStateException(message.unwrap());
    }
}
