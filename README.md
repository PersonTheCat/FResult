# FResult
#### A proof of concept library for functional error handling in Java.

Result is a counterpart to `java.util.Optional` used for neatly handling errors.
It provides a functional interface capable of completely replace the standard
try-catch and try-with-resources notations in Java. It has two essential modes of
use:

1. As a better return type for functions that use standard error handling
   procedures, and
2. As a wrapper around functions that do not.

## A Better Return Type

Let's take look at an example of scenario number one and then examine how these
methods would be applied using `Result<T, E>`.

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
  
  // Create and return a new error directly.
  public static Result<String, IOException> betterReturnAlt() {
    final File f = getFile();
    return testConditions(f)
      ? Result.ok(getContents(f))
      : Result.err(new IOException());
  }
```

### Implementation

Each of these methods is said to return a **complete result**. This means that
any error present inside of the wrapper is **reifiable**. In other words, it
contains a knowable type and thus can be safely interacted with.

When interacting with complete result types in FResult, this library will
provide a full suite of functional utilities for interacting with the underlying
values and errors that may be present.

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
```

## A Try-Catch Replacement Wrapper

Now let's take a look at a second use case in which we're wrapping a standard,
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
conventions, including `#of`, `#any`, `#nullable`, `#with`, and `#define`.

Let's start with the first and most important option: `Result#of`. The first
thing you'll notice is that the return value is **not assignable to
Result<T, E>**.

```java
  final Result.Pending<String, IOException> r1 = Result.of(Name::toWrap);
```

The output of `Result#of` is a type of `Result$Pending`, which extends from
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
type, as the exact type is not known at runtime. To work around this, FResult
exploits Java's type coercion mechanics to achieve a type-safe guarantee.

Let's see this in action:

```java
  final Result<String, IOException> r1 = Result.of(Name::toWrap)
    .ifErr(e -> {/* handle error */}); // Type is implicitly cast
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

### Ignoring Specific Error Types

Alternatively, if you would like to simply ignore the specific type of error
being returned, you can employ `Pending#isAnyErr`, or `Pending#expectAnyErr`.

Also see `Result#any` for returning a complete `Result<T, Throwable>` which
may contain **any kind of error**.

## Handling `null` Return Values

FResult also provides a couple of factory methods which will automatically wrap
your static and dynamic values:

* `Result#nullable(T)` -> `Result$Value`
* `Result#nullable(ThrowingSupplier<T, E>)` -> `Result$Pending`

Let's see what it looks like to handle these output values.

```java
  // Known types
  final Object result = Result.nullable(potentiallyNullValue)
    .orElseGet(() -> Optional.of(nonNullValue)) // Alternate value if err
    .orElseGet(Object::new); // Alternate value if null
    
  // Unknown types
  final Object result = Result.nullable(Name:mayReturnNullOrFail)
    .expect("Error message!") // Potentially null value as `Optional<T>`
    .orElseGet(Object::new); // Altnerate value as null
```

It is worth noting that, as of FResult 2.0, wrapped values **are allowed to be
null**. Since a couple of associated methods including `Result#get` already
return `Optional<T>`, you can safely handle the case where values are null even
in the absence of errors.

```java
  final Object result = Result.of(Name::mayReturnNullOrFail)
    .get(Result::IGNORE) // Implicitly calls `ifErr` and ignores
    .orElse("Horray!"); // Provide an alternate if null.
```

## Wrapping Try-With-Resources

FResult also provides a couple of companion utilities, `WithResource<R, E>` and
`WithResources<R1, R2, E>`, which provide support for `AutoCloseable` resources.

Here's how you can use these methods:

```java
  // Use a single resource via method chainging
  final String book = Result
    .with(() -> new FileReader("book.txt")) // Get the resource
    .of(reader -> { /* read file */ }) // Use the resource
    .orElseGet(e -> ""); // Handle errors
  
  // Use a single resource in one method
  final String book = Result.with(() -> new FileReader("book.txt), reader -> {
    // read file
  }).orElseGet(e -> "");
  
  // Use multiple resources
  final String book = Result
    .with(() -> new FileReader("book.txt"))
    .and(SecondResource::new) // Either a supplier or a function
    .of(reader -> { /* read file */)
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













