package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.util.TestCloseableResource;
import personthecat.fresult.util.TestFlag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WithResourcesTest {

    @Test
    public void withResources_closesResources() {
        final TestCloseableResource closeable1 = new TestCloseableResource();
        final TestCloseableResource closeable2 = new TestCloseableResource();
        Result.with(() -> closeable1).and(c1 -> closeable2).suppress((c1, c2) -> {});

        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void withResources_supplierHandlesException() {
        final TestFlag flag = new TestFlag();

        final WithResource<TestCloseableResource> r1 =
            Result.with(() -> { throw new NullPointerException(); });

        assertDoesNotThrow(() ->
            r1.<TestCloseableResource>and(() -> { throw new NullPointerException(); })
                .suppress(t -> { flag.set(true); return ""; }));

        assertFalse(flag.isFlagged());
    }
}
