package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.util.TestFlag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PartialOptionalResultTest {

    @Test
    @SuppressWarnings("unused")
    public void partialResult_doesNotExecute() {
        final TestFlag flag = new TestFlag();
        final PartialOptionalResult<String, Throwable> r = Result.nullable(() -> { flag.set(true); return ""; });
        assertFalse(flag.isFlagged());
    }

    @Test
    public void ifErr_doesExecute() {
        final TestFlag flag = new TestFlag();
        Result.nullable(() -> { flag.set(true); return ""; }).ifErr(Result::IGNORE);
        assertTrue(flag.isFlagged());
    }
}
