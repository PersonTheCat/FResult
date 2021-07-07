package personthecat.fresult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ShorthandTest {

    @Test
    public void format_interpolatesValues() {
        assertEquals("1.2.hello", Shorthand.f("{}.{}.{}", 1, 2, "hello"));
    }
}
