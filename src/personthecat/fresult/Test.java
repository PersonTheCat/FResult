package personthecat.fresult;

import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.exception.WrongErrorException;

import java.io.IOException;

import static personthecat.fresult.Shorthand.*;

public class Test {

    public static void main(String[] args) {
        try {
            testErrorHandling();
            testWrongError();
            testInvalidProtocol();
            testValidProtocol();
            testClose();
            testJoin();
        } catch (Exception e) {
            throw runEx("Test failed.", e);
        }
        info("All tests passing!");
    }

    private static void testErrorHandling() {
        Result.of(Test::throwsIOE)
            .ifErr(e -> info("Demo error caught successfully."));
    }

    private static void testWrongError() {
        try {
            Result.of(Test::falseError).ifErr(Result::IGNORE);
        } catch (WrongErrorException e) {
            info("Wrong error detected successfully.");
            return;
        }
        throw runEx("Invalid error type remained in handler.");
    }

    private static void testInvalidProtocol() {
        try {
            final Protocol empty = new Protocol();
            empty.get(Test::throwsISE);
        } catch (MissingProcedureException e) {
            info("Missing procedure detected successfully.");
            return;
        }
        throw runEx("No error thrown despite empty protocol.");
    }

    private static void testValidProtocol() {
        Result.define(IOException.class, e -> { throw runEx("Wrong procedure implemented."); })
            .define(IllegalAccessException.class, e -> info("Protocol implemented successfully."))
            .get(Test::throwsISE);
    }

    private static void testClose() {
        final Close test = Result.with(Close::new, Test::returnsClose).unwrap();
        if (test.closed) {
            info("Demo resource closed successfully.");
        } else {
            throw runEx("Demo resource was not closed by handler.");
        }
    }

    private static void testJoin() {
        final Result.Pending<Void, RuntimeException> joint = Result.join(
            Result.of(() -> {}).execute(),
            Result.<Void, RuntimeException>of(() -> { throw runEx("#2"); }).execute(),
            Result.of(() -> {}).execute()
        );
        joint.ifErr(e -> info("Error caught in joint handler: {}", e.getMessage()));
    }

    private static void falseError() throws IOException {
        throw new RuntimeException("Actual error type not caught by handler.");
    }

    private static void throwsIOE() throws IOException {
        throw new IOException("Error not caught by handler.");
    }

    private static Object throwsISE() throws IllegalAccessException {
        throw new IllegalAccessException("Error not caught by handler.");
    }

    private static Close returnsClose(Close c) {
        return c;
    }

    private static void info(String msg, Object... args) {
        System.out.println(f(msg, args));
    }

    /** Demo AutoCloseable implementor. */
    private static class Close implements AutoCloseable {
        boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }
}
