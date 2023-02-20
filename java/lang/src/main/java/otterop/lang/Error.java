package otterop.lang;
import otterop.lang.String;

public class Error {

    private int code;

    private String message;

    public Error(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}
