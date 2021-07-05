package personthecat.fresult;

import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.exception.WrongErrorException;

import java.io.IOException;

import static personthecat.fresult.Shorthand.f;
import static personthecat.fresult.Shorthand.runEx;

public class Test {

    public static void main(final String[] args) {
        // Maybe use a protocol?
        // Separately handle specific exceptions and nullability
        final String result = Result.<String, IllegalArgumentException>nullable(() -> "Hello, World")
            .ifEmpty(() -> info("It was empty!"))
            .defaultIfEmpty(() -> "Output was null")
            .ifErr(e -> info("There was an error!"))
            .resolve(e -> "Default value")
            .expose();
        // Ignore all exceptions and null values
        final String r2 = Result.suppressNullable(() -> "Hello, World!")
            .orElse("");

        final String r = Result
            .resolve(IllegalArgumentException.class, e -> "Bad argument")
            .resolve(IllegalStateException.class, e -> "Ya done goofed")
            .resolve(RuntimeException.class, e -> "Not sure what ya did")
            .expose(() -> "It worked!");
        try {
            testErrorHandling();
            testWrongError();
            testInvalidProtocol();
            testValidProtocol();
            testClose();
            testJoin();
        } catch (final Exception e) {
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
            empty.run(Test::throwsISE);
        } catch (MissingProcedureException e) {
            info("Missing procedure detected successfully.");
            return;
        }
        throw runEx("No error thrown despite empty protocol.");
    }

    private static void testValidProtocol() {
        Result.define(IOException.class, e -> { throw runEx("Wrong procedure implemented."); })
            .define(IllegalAccessException.class, e -> info("Protocol implemented successfully."))
            .run(Test::throwsISE);
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
            Result.of(() -> {}),
            Result.of(() -> { throw runEx("#2"); }),
            Result.of(() -> {})
        );
        joint.ifErr(e -> info("Error caught in joint handler: {}", e.getMessage()));
    }

    private static void falseError() throws IOException {
        throw new RuntimeException("Actual error type not caught by handler.");
    }

    private static void throwsIOE() throws IOException {
        throw new IOException("Error not caught by handler.");
    }

    private static void throwsISE() throws IllegalAccessException {
        throw new IllegalAccessException("Error not caught by handler.");
    }

    private static Close returnsClose(final Close c) {
        return c;
    }

    private static void info(final String msg, final Object... args) {
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
