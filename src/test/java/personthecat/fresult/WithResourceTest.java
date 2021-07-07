package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.util.TestCloseableResource;
import personthecat.fresult.util.TestFlag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WithResourceTest {

    @Test
    public void withResource_closesResource() {
        final TestCloseableResource closeable = new TestCloseableResource();
        Result.with(() -> closeable).suppress(c -> {});

        assertTrue(closeable.isClosed());
    }

    @Test
    public void withResource_supplierHandlesException() {
        final TestFlag flag = new TestFlag();

        assertDoesNotThrow(() ->
            Result.<TestCloseableResource, Throwable>with(() -> { throw new NullPointerException(); })
                .suppress(t -> { flag.set(true); return ""; }));

        assertFalse(flag.isFlagged());
    }
}
