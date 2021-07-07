package personthecat.fresult.util;

import java.io.IOException;

@SuppressWarnings("UnusedReturnValue")
public final class TestUtils {

    public static final String DUMMY_OUTPUT = "dummy";

    private TestUtils() {}

    public static String throwFalseRE() throws IOException {
        throw new RuntimeException("Actual error type not caught by handler.");
    }

    public static String throwsIOE() throws IOException {
        throw new IOException("Error not caught by handler.");
    }

    public static String returnsDummy() {
        return DUMMY_OUTPUT;
    }

    public static String returnsNull() {
        return null;
    }
}
