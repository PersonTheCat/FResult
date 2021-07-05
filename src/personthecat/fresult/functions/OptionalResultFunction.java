package personthecat.fresult.functions;

import personthecat.fresult.OptionalResult;

import java.util.function.Function;

@FunctionalInterface
public interface OptionalResultFunction<T, M, E extends Throwable> extends Function<T, OptionalResult<M, E>> {}
