# FResult
#### A proof of concept library for functional error handling in Java.

   Result is a counterpart to `Java.util.Optional` used for neatly handling errors.
It provides a functional interface capable of completely replacing the standard
try-catch and try-with-resources notations in Java. It has two essential modes of
use:

1. As a better return type for functions that use standard error handling
procedures, and
2. As a wrapper around functions that do not.

Basic implementations for each of the above use-cases are as follows:

```java
  // Return the product of each block.
  public static Result<String, IOException> betterReturn() {
    final File f = getFile();
    try {
      return Result.ok(getContents(f));
    } catch (IOException e) {
      return Result.err(e);
    }
  }

  // Standard conventions to be wrapped.
  public static String toWrap() throws IOException {
    final File f = getFile();
    return getContents(f);
  }
  
  // Create and return a new error directly.
  public static Result<String, IOException> betterReturnAlt() {
    final File f = getFile();
    return testConditions()
      ? Result.ok(getContents(f))
      : Result.err(new IOException());
  }
```

The code which makes use of these functions would be idential in either
case, as follows:

```
  Result<String, IOException> r1 = betterReturn();
  Result<String, IOException> r2 = Result.of(Name::toWrap);
```

  It is worth noting that the above code always produces a new wrapper, but
is not guaranteed to execute. It is thereby essential that functions returning
new wrappers be followed by some type of handler, e.g. `#ifErr` or `#expect`.

## Motivations and Cons

  Wrapping error-prone procedures in a functional interface has the modest
benefit of being more expressive and easier to maintain. It gives callers a
more convenient interface for supplying alternate values and reporting errors
to the end-user. However, this comes at a cost. It is certainly less safe and
less specific than vanilla error handling in Java, as it sometimes requires a
bit of runtime reflection and only accepts one error parameter by default. It
also requires a bit of memory overhead which likely has at least a minor
impact on performance. Some may consider functional error handling to be more
readable than imperative error handling, but this may not apply for most Java
developers, as it strays from the standard conventions they're used to and is
thus a bit of an outlier in that world.
