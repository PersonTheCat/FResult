package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.util.TestFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ProtocolTest {

    @Test
    public void protocol_handlesSpecificError() {
        final TestFlag flag = new TestFlag();
        final Protocol p = Result.define(IllegalArgumentException.class, e -> flag.set(false))
            .and(NullPointerException.class, e -> flag.set(true));

        p.run(() -> { throw new NullPointerException(); });
        assertTrue(flag.isFlagged());
    }

    @Test
    public void emptyProtocol_throwsMissingProcedure() {
        final Protocol p = new Protocol();
        final MissingProcedureException e = assertThrows(MissingProcedureException.class,
            () -> p.run(() -> { throw new NullPointerException(); }));

        assertEquals(NullPointerException.class, e.getCause().getClass());
    }
}
