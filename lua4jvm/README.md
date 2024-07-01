# lua4jvm
lua4jvm is a work-in-progress JIT compiler for Lua 5.4 built on top
of JVM. Originally, it started as an university course project, but most of
*that* code has been rewritten at least once.

In its current form, you shouldn't use lua4jvm. While usually reliable, it
lacks the Lua standard library implementation; instead, the development has
focused on core VM features. Rest of the work is arguably easier; the author
has just been busy for a while.

## Architecture
The lua4jvm lacks formal documentation. However, below is a brief description
of its architecture.

### Frontend
Lua is parsed with a custom Antlr 4 grammar. From there, it is translated to
an internal IR format that the backend consumes. There is nothing too special
about this; initially, the project included a custom parser, but quite soon I
realized that I wanted to focus on the backend.

### Backend
Lua is a dynamically typed language with extensive runtime metaprogramming
facilities (metatables). Both of these characteristics make compiling it
into performant code rather difficult. This is especially true when the
compilation target has been primarily built for statically-typed languages;
on JVM, optimizations such as NaN tagging cannot be done.

To tackle this issue, lua4jvm generally compiles code only immediately before
it would be executed. Function calls generate their targets on first call
based on the real types at call sites; and should the types change, the target
will just get compiled again. The same system is also used for Lua's many
operators, since they can be overridden by metatables. The generated
specializations are cached between all call sites.

This approach also allows usage of 'static' analysis at runtime. Since the
function compiler effectively knows types of its arguments and upvalues, it
can for example use primitive types for numerical code.