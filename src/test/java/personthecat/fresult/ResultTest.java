package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.util.TestFlag;
import personthecat.fresult.util.TestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ResultTest {

    @Test
    public void wrapper_catchesError() {
        assertDoesNotThrow(() -> Result.of(TestUtils::throwsIOE).ifErr(Result::IGNORE));
    }

    @Test
    public void wrongError_isRethrown() {
        assertThrows(RuntimeException.class, () -> Result.of(TestUtils::throwFalseRE).ifErr(Result::IGNORE));
    }

    @Test
    public void nullValue_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Result.of(TestUtils::returnsNull).ifErr(Result::IGNORE));
    }

    @Test
    public void okValue_isDetected() {
        assertTrue(Result.of(TestUtils::returnsDummy).ifErr(Result::IGNORE).isOk());
    }

    @Test
    public void error_isDetected() {
        assertTrue(Result.of(TestUtils::throwsIOE).ifErr(Result::IGNORE).isErr());
    }

    @Test
    public void ifOk_eventRuns() {
        final TestFlag flag = new TestFlag();
        Result.of(() -> "").ifErr(Result::IGNORE).ifOk(t -> flag.set(true));

        assertTrue(flag.isFlagged());
    }

    @Test
    public void ifErr_eventRuns() {
        final TestFlag flag = new TestFlag();
        Result.of(TestUtils::throwsIOE).ifErr(e -> flag.set(true));

        assertTrue(flag.isFlagged());
    }

    @Test
    public void andThen_eventRuns() {
        final TestFlag flag = new TestFlag();
        Result.of(() -> "").ifErr(Result::IGNORE).andThen(() -> flag.set(true));

        assertTrue(flag.isFlagged());
    }

    @Test
    public void orElseTry_returnsNewValue() {
        final Result<String, Throwable> r = Result.suppress(TestUtils::throwsIOE)
            .orElseTry(TestUtils::returnsDummy)
            .ifErr(Result::IGNORE);

        assertEquals(TestUtils.DUMMY_OUTPUT, r.resolve(e -> "").expose());
    }

    @Test
    public void foldValue_returnsNewValue() {
        final String s = Result.of(() -> "")
            .ifErr(Result::IGNORE)
            .fold(t -> TestUtils.DUMMY_OUTPUT, e -> "");

        assertEquals(TestUtils.DUMMY_OUTPUT, s);
    }

    @Test
    public void foldError_returnsNewValue() {
        final String s = Result.of(TestUtils::throwsIOE)
            .ifErr(Result::IGNORE)
            .fold(t -> "", e -> TestUtils.DUMMY_OUTPUT);

        assertEquals(TestUtils.DUMMY_OUTPUT, s);
    }

    @Test
    public void mapValue_returnsMappedValue() {
        final int hash = TestUtils.DUMMY_OUTPUT.hashCode();
        assertEquals(hash, (int) Result.ok(TestUtils.DUMMY_OUTPUT).map(String::hashCode).expose());
    }

    @Test
    public void mapError_returnsMappedError() {
        final Exception mapped = new Exception(TestUtils.DUMMY_OUTPUT);
        assertEquals(mapped, Result.err(new Exception()).mapErr(e -> mapped).expose());
    }

    @Test
    public void expect_onError_throwsException() {
        final Result<String, Throwable> r = Result.suppress(TestUtils::throwsIOE);
        assertThrows(ResultUnwrapException.class, () -> r.expect(""));
    }

    @Test
    public void expectErr_onValue_throwsException() {
        final Result<String, Throwable> r = Result.suppress(TestUtils::returnsDummy);
        assertThrows(ResultUnwrapException.class, () -> r.expectErr("").wait());
    }

    @Test
    public void throwIfErr_onError_throwsException() {
        final Result<String, Throwable> r = Result.suppress(TestUtils::throwsIOE);
        assertThrows(IOException.class, r::throwIfErr);
    }

    @Test
    public void throwIfErr_onValue_doesNotThrowException() {
        final Result<String, Throwable> r = Result.suppress(TestUtils::returnsDummy);
        assertDoesNotThrow(r::throwIfErr);
    }

    @Test
    public void join_yieldsError() {
        final Result<Void, Throwable> joint = Result.join(
            Result.of(() -> {}),
            Result.of(() -> { throw new RuntimeException(TestUtils.DUMMY_OUTPUT); }),
            Result.of(() -> {})
        ).ifErr(Result::IGNORE);

        assertTrue(joint.isErr());
        assertEquals(TestUtils.DUMMY_OUTPUT, ((Result.Error<Void, Throwable>) joint).expose().getMessage());
    }

    @Test
    public void collect_yieldsCollection() {
        final Result<List<String>, Throwable> list = Result.collect(
            Result.ok(""),
            Result.ok(TestUtils.DUMMY_OUTPUT),
            Result.ok("")
        ).ifErr(Result::IGNORE);

        assertTrue(list.isOk());
        assertTrue(((Result.Value<List<String>, Throwable>) list).expose().contains(TestUtils.DUMMY_OUTPUT));
    }

}
