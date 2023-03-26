package personthecat.fresult;

import org.junit.jupiter.api.Test;
import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.util.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ResolverTest {

    @Test
    public void resolver_handlesSpecificError() {
        final Resolver<String> r = Result.resolve(IllegalArgumentException.class, e -> "")
            .resolve(NullPointerException.class, e -> TestUtils.DUMMY_OUTPUT);

        assertEquals(TestUtils.DUMMY_OUTPUT, r.expose(() -> { throw new NullPointerException(); }));
    }

    @Test
    public void emptyResolver_throwsMissingProcedure() {
        final Resolver<String> r = new Resolver<>();
        final MissingProcedureException e = assertThrows(MissingProcedureException.class,
            () -> r.expose(() -> { throw new NullPointerException(); }));

        assertEquals(NullPointerException.class, e.getCause().getClass());
    }
}
