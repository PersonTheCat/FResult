package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.util.TestFlag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PartialResultTest {

    @Test
    @SuppressWarnings("unused")
    public void partialResult_doesNotExecute() {
        final TestFlag flag = new TestFlag();
        final PartialResult<Void, Throwable> r = Result.of(() -> flag.set(true));
        assertFalse(flag.isFlagged());
    }

    @Test
    public void ifErr_doesExecute() {
        final TestFlag flag = new TestFlag();
        Result.of(() -> flag.set(true)).ifErr(Result::IGNORE);
        assertTrue(flag.isFlagged());
    }

}
