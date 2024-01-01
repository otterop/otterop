package otterop.test;

import org.junit.jupiter.api.Assertions;
import otterop.lang.String;

public class TestRun {
    public void assertTrue(boolean value, String message) {
        Assertions.assertTrue(value, message.unwrap());
    }
}
