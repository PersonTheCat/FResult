# FResult
#### A unified type for optional and error-prone procedures in Java.

FResult is a powerful and expressive counterpart to `java.util.Optional` used for 
neatly handling errors. You can suppress all errors and immediately get a value:

```java
  final String s = Result.suppress(() -> readFile("myFile.txt"))
    .expect("That didn't work!");
```

Or, be as specific as you like:

```java
  final String s = Result.resolve(FileNotFoundException.class, e -> "")
    .resolve(IllegalArgumentException.class, Result::THROW)
    .resolve(IOException.class, Result::WARN)
    .suppressNullable(() -> readFile("myFile.txt"))
    .ifEmpty(() -> log.warn("File was empty!"))
    .ifErr(e -> log.warn("Couldn't read file!"))
    .ifOk(t -> log.info("Good job!"))
    .defaultIfEmpty(() -> "")
    .resolve(e -> "Default text")
    .expose();
```

This interface is capable of completely replacing the standard try-catch and 
try-with-resources notations in Java. It has two essential modes of use:

1. A better return type for functions that use standard error handling procedures
2. A wrapper around functions that do not.

## A Better Return Type

Let's take a look at some examples of methods that use this new return type and
then examine how they would be applied.

```java
  // Return the product of each block.
  public static Result<String, IOException> betterReturn() {
    final File f = getFile();
    try {
      return Result.ok(getContents(f));
    } catch (final IOException e) {
      return Result.err(e);
    }
  }
  
  // Create and return a new error directly.
  public static Result<String, IOException> betterReturnAlt() {
    final File f = getFile();
    return testConditions(f)
      ? Result.ok(getContents(f))
      : Result.err(new IOException());
  }
```

### Implementation

Each of these methods returns a **complete result**. This means that any error 
present inside of the wrapper is effectively **reifiable**. In other words, it 
contains a knowable type and thus can be safely interacted with.

FResult provides a full suite of functional utilities for interacting with the
underlying values and errors in this type.

Here, you can see a few of those methods in action:

```java
  // Handle all outcome scenarios.
  final Result<String, IOException> r1 = betterReturn()
    .ifErr(Result::WARN) // Output a warning message 
    .ifOk(ContentConsumer::apply); // Consume the text output
    
  // Transform the data into a common type.
  final int numLines = r1
    .fold(t -> t.lines().size(), e -> 0);
    
  // Immediately supply an alternate value.
  final String output = betterReturnAlt()
    .orElseGet(String::new);

  // Map the underlying data to a new type.
  final int hashCode = betterReturnAlt()
    .map(Object::hashCode)
    .orElse(0);
```

## A Try-Catch Replacement Wrapper

Now let's look at the second use case in which we're wrapping a standard,
throwing method.

```java
  // Standard conventions to be wrapped.
  public static String toWrap() throws IOException {
    final File f = getFile();
    return getContents(f);
  }
```

### Implementation

FResult provides a series of factory methods for wrapping standard error-prone
conventions, including `#of`, `#suppress`, `#nullable`, `#wrappingOptional`, 
`#with`, `#define`, `#resolve`, and more.

Let's start with the first (and most important) option: `Result#of`. The first
thing you'll notice is that the return type is **not assignable to Result<T, E>**.

```java
  // Generate instructions for wrapping this method.
  final PartialResult<String, IOException> r1 = Result.of(Name::toWrap);

  // Consume these instructions and get a Result<T, E>.
  final Result<String, IOException> r2 = r1.ifErr(e -> log.warn("Oops!"));
```

The output of `Result#of` is a type of `Result$Pending`, which implements
`PartialResult<T, E>`. This name has two very important implications:

1. The result is lazily-evaluated and **does not exist yet**.
2. The wrapper **does not provide a complete set of utilities**. In other words,
   it is an **incomplete result**.

The reason for this type of design is the product of **type erasure**.

#### Type Erasure

If you're unfamiliar, **type erasure** is the process by which generified types
lose their generic parameters at runtime in Java, depending on the context in
which they're used.

For this reason, it is **impossible** to catch an exception based on a generic
type, as the exact type cannot be known at runtime. To work around this, FResult
exploits Java's generic type coercion mechanics to achieve a type-safe guarantee.

Let's take a closer look:

```java
  // Output cannot contain a different exception.
  final Result<String, IOException> r1 = Result.of(Name::toWrap)
    .ifErr(e -> {/* handle error */}); // Type is implicitly cast

  // Acknowledge and immediately discard the exception.
  final Optional<String> r2 = Result.of(Name::toWrap)
    .get(e -> { /* handle error */}); // Also resolves the type
```

You should notice two things from this example:

1. The use of `e` implicitly casts the underlying error to its
   expected type.
2. The output is now assignable to a standard `Result<T, E>`.

FResult uses this mechanism to guarantee that an unexpected type of error can
never be caught by the wrapper. If one is, it will be rethrown as a
`WrongErrorException`. This is only possible because `e` is returned to the
call site while still being within the scope of the wrapper.

If we apply this knowledge, we can see that it becomes safe to use the output
of `Result#of` as a standard `Result<T, E>` **after applying** `ifErr`.

The following methods are considered **safe** and are ideal candidates for the
majority of use cases immediately after calling `Result#of`:

* `Pending#ifErr(Consumer<E>)`
* `Pending#get(Consumer<E>)`
* `Pending#fold(Function<T, U>, Function<E, U>)`
* `Pending#orElseGet(Function<E, T>)`
* `Pending#orElseTry(ThrowingFunction<E, T, E>)`
* `Pending#expect` and `Pending#expectF`

Note that if you would like to continue using the wrapper as a type of
`Result<T, E>`, **you must always call** `ifErr`.

## Ignoring Specific Error Types

Alternatively, if you would like to simply ignore the specific type of error
being returned, you can employ `Pending#isAnyErr`, or `Pending#expectAnyErr`.

Also see `Result#suppress` for returning a complete `Result<T, Throwable>` which
may contain **any kind of error**. Here's how that would look:

```java
  // No need to acknowledge the error, as it can be any type.
  final Result<String, IOException> r1 = Result.suppress(Name::toWrap);

  // Optionally discard the error and directly expose the contents.
  final String r2 = Result.suppress(Name::toWrap)
    .resolve(e -> "") // Type must now be a Result$Value
    .expose(); // The data can safely be exposed
```

## Handling `null` Return Values

FResult is also capable of wrapping methods that may return null. This functionality
if provided via `OptionalResult` and `PartialOptionalResult`. Below are a few of the
factory methods provided for handling nullable types.

* `Result#nullable(T)` -> `OptionalResult`
* `Result#nullable(ThrowingSupplier<T, E>)` -> `PartialOptionalResult`
* `Result#nullable(Optional<T>)` -> `OptionalResult`
* `Result#nullable(ThrowingOptionalSupplier<T, E>)` -> `PartialOptionalResult`


Let's see what it looks like to use these wrappers:

```java
  // Known types (OptionalResult)
  final Object r1 = Result.nullable(potentiallyNullValue)
    .orElseGet(Object::new); // Alternate value if null
    
  // Unknown types (PartialOptionalResult)
  final Object r2 = Result.nullable(Name::mayReturnNullOrFail)
    .ifErr(e -> log.warn("Error on output: {}", e))
    .orElseGet(Object::new);
```

Methods that wish to return nullable Result types must return an instance of
`OptionalResult`. Here's how that would look:

```java
  // Output may still be null even if no exception is thrown.
  public static OptionalResult<String, SQLException> getBook() {
    try {
      return Result.nullable(dao.getBook());
    } catch (final SQLSyntaxException e) {
      return Result.err(e);
    }
  }
```

## Wrapping Try-With-Resources

FResult also provides a couple of companion utilities, `WithResource<R, E>` and
`WithResources<R1, R2, E>`, which provide support for `AutoCloseable` resources.

Here's how you can use these methods:

```java
  // Use a single resource via method chainging
  final String book = Result
    .with(() -> new FileReader("book.txt")) // Get the resource
    .suppress(reader -> { /* read file */ }) // Use the resource
    .orElseGet(e -> ""); // Handle errors
  
  // Use a single resource in one method
  final String book = Result.with(() -> new FileReader("book.txt), reader -> {
    // read file
  }).orElseGet(e -> "");
  
  // Use multiple resources
  final String book = Result
    .with(() -> new FileReader("book.txt"))
    .and(SecondResource::new) // Either a supplier or a function
    .suppress(reader -> { /* read file */)
    .orElseGet(e -> "");
```

## Handling Multiple Error Types

## Using Result Imperatively

## Other Factory Methods

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
