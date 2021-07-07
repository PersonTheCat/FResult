package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.util.TestFlag;
import personthecat.fresult.util.TestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OptionalResultTest {

    @Test
    public void wrapper_catchesError() {
        assertDoesNotThrow(() -> Result.nullable(TestUtils::throwsIOE).ifErr(Result::IGNORE));
    }

    @Test
    public void wrongError_isRethrown() {
        assertThrows(RuntimeException.class, () -> Result.nullable(TestUtils::throwFalseRE).ifErr(Result::IGNORE));
    }

    @Test
    public void nullValue_isDetected() {
        assertTrue(Result.nullable(TestUtils::returnsNull).ifErr(Result::IGNORE).isEmpty());
    }

    @Test
    public void okValue_isDetected() {
        assertTrue(Result.nullable(TestUtils::returnsDummy).ifErr(Result::IGNORE).isOk());
    }

    @Test
    public void error_isDetected() {
        assertTrue(Result.nullable(TestUtils::throwsIOE).ifErr(Result::IGNORE).isErr());
    }

    @Test
    public void presentValue_isNotReplaced() {
        assertEquals(TestUtils.DUMMY_OUTPUT, Result.nullable(TestUtils.DUMMY_OUTPUT).orElse(""));
    }

    @Test
    public void absentValue_isReplaced() {
        assertEquals(TestUtils.DUMMY_OUTPUT, Result.nullable((String) null).orElse(TestUtils.DUMMY_OUTPUT));
    }

    @Test
    public void expectEmpty_onValue_throwsException() {
        final OptionalResult<String, Throwable> r = Result.suppressNullable(TestUtils::returnsDummy);
        assertThrows(ResultUnwrapException.class, () -> r.expectEmpty(""));
    }

    @Test
    public void expectEmpty_onError_throwsException() {
        final OptionalResult<String, Throwable> r = Result.suppressNullable(TestUtils::throwsIOE);
        assertThrows(ResultUnwrapException.class, () -> r.expectEmpty(""));
    }

    @Test
    public void filter_removesValue() {
        assertTrue(Result.ok("").filter(t -> false).isEmpty());
    }

    @Test
    public void filter_removesError() {
        assertTrue(Result.err(new NullPointerException()).filterErr(e -> false).isEmpty());
    }

    @Test
    public void ifEmpty_eventRuns() {
        final TestFlag flag = new TestFlag();
        Result.nullable((String) null).ifEmpty(() -> flag.set(true));

        assertTrue(flag.isFlagged());
    }
}
