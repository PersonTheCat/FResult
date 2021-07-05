package personthecat.fresult.functions;

import personthecat.fresult.Result;

import java.util.function.Function;

@FunctionalInterface
public interface ResultFunction<T, M, E extends Throwable> extends Function<T, Result<M, E>> {}
