# Overview
This section covers untypical & advanced use-cases of Temporal & ZIO-Temporal.  
Feel free to contribute your own use-cases!

## Few notes
While Scala has a powerful type system, Temporal abstractions hasn't.  
Also, ZIO-Temporal is build on top of Java SDK and runs on JVM, which brings some limitations, such as [type erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html).  
So in order to use higher level constructs, such as generics, it's required to follow some basic guidelines.

