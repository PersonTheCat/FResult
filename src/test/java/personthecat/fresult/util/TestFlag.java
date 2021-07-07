package personthecat.fresult.util;

public class TestFlag {
    boolean flag = false;

    public void set(final boolean flag) {
        this.flag = flag;
    }

    public boolean isFlagged() {
        return this.flag;
    }
}
