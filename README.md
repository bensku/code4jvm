# code4jvm
code4jvm is a high-level JVM bytecode generation library for writing
compiler backends. It allows generating Java-like code without worrying
about low-level features, while still supporting JVM features that cannot
be expressed in Java.

The core code4jvm library consists of:
* Automatic stack management and local variable slot tracking
* Stack map frame generation (with control flow tracing)
* An extensible expression system that allows generation of custom bytecode
* Simple control flow (basic blocks, jumps)
* Java-like type conversions (complex casts, boxing)
* Exception handling (including try-finally block support)

On top of this, some higher-level features are included:
* Structured control flow (conditional blocks, loops)
* Method calls, including use of constructors
* Arithmetic and bitwise operations
* String concatenation
* Enum classes

Under the hood, JVM bytecode is generated using the low-level
[ASM](https://asm.ow2.io/) library.

## Usage
As of now, the only use case worth of mentioning is
[lua4jvm](lua4jvm/README.md), a work-in-progress Lua 5.4 implementation for JVM.

## Status
This library is work-in-progress. It is being worked on in author's spare time,
and there are no guarantees about when bug fixes or new features will be
available. Additionally, code4jvm is not yet published on Maven central.
Naturally, there are also zero guarantees about API stability.

As you might notice, the documentation is also quite sparse. There are
Javadocs, but beyond them you'd have to learn by example.

In short: don't use this in production. If you just want to hack around,
feel free to open issues (or even pull requests!).