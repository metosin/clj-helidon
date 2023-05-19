# Nima Ring adapter

A library that adapts [Helidon Níma](https://helidon.io/nima) for the [Clojure Ring](https://github.com/ring-clojure/ring) library.

The main argument for Níma is that it's based on new (available from JDK 19 onwards) _virtual threads_. The virtual thread in Java is a lightweight thread that does not consume a platform thread. This enables async programming with the same API as traditional blocking APIs. This means that applications using virtual threads can freely use the existing blocking I/O operations and still be fully async.

For `ring`, this means that you can forego the use of [async handlers](https://github.com/ring-clojure/ring/wiki/Concepts#handlers).

More information about virtual threads can be found in the links below:

...todo...

## Usage

Add nima-ring as a dependency:

...todo...

Start the server:

...todo...

## TODO

[ ] WebSocket support

## License

Copyright © 2019-2022 Metosin Oy and contributors.

Available under the terms of the Eclipse Public License 2.0, see [LICENSE](./LICENSE)
