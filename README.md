# Introduction

OtterOP is a source-to-source compiler, also named transpiler, built with [ANTLR](https://www.antlr.org/).
It allows to transpile a subset of Java to multiple object oriented languages,
or languages that can be used in an object oriented way like C or Go.

Built-in languages are:

* C (using libgc)
* C#
* Go
* Python
* TypeScript

It allows to do this because it doesn't rely on the Java Class Library,
but a new library that consists of a set of classes implemented in each
language and others written in Java and transpiled.

## Supported Java features

* if, else, else if
* while
* static methods
* instance methods
* constructor
* generics
* interfaces
* instance fields
* static fields with primitive types

## Unsupported Java features

non-exhaustive list of the unsupported features:

* overloading methods
* overriding methods
* class inheritance
* closures

# Roadmap

* transpile unit tests
* implement data structures
* passing callbacks
* TCP sockets and SSL
* File system API
* UDP
* HTTP client
* logging

# Why the name OtterOP?

The name was actually chosen among some suggested by ChatGPT 3, when prompted for:
a good name for a subset of Java that transpiles to many languages, in a single word and with a cute animal name included.

The output and explanation was:

> "Otterop" - This name combines the cute animal name "otter" 
> with the shortened form of "interoperability," which refers
> to the ability of different systems or components to work together.

I added the upper case OP so it forms the acronym of Object Oriented Programming.