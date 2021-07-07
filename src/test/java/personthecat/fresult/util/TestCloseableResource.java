package personthecat.fresult.util;

public class TestCloseableResource implements AutoCloseable {
    boolean closed = false;

    @Override
    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
