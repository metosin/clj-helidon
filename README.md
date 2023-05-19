# Nima Ring adapter

A library that adapts [Helidon Níma](https://helidon.io/nima) for the [Clojure Ring](https://github.com/ring-clojure/ring) library.

The main argument for Níma is that it's based on new (available from JDK 19 onwards) _virtual threads_. The virtual thread in Java is a lightweight thread that does not consume a platform thread. This enables async programming with the same API as traditional blocking APIs. This means that applications using virtual threads can freely use the existing blocking I/O operations and still be fully async.

For `ring`, this means that you can forego the use of [async handlers](https://github.com/ring-clojure/ring/wiki/Concepts#handlers) and still get all the benefits of async.

Helidon Nima is a very efficient and fast WebServer, for more information about the performance of Nima see https://medium.com/helidon/helidon-n%C3%ADma-helidon-on-virtual-threads-130bb2ea2088#f3b5

More information about virtual threads can be found in the links below:

...todo...

## Usage

Add nima-ring as a dependency:

...todo...

Start the server:

```clj
(ns my-app
  (:require [[metosin.nima-ring.nima-server :as nima]]))

(defn handler [req]
  {:status 200
   :body   "Hello"})

(def server (nima/nima-server handler {:port 8080}))
```

The `nima/nima-server` accepts a ring handler function and optionally a configuration map. Configuration can contain:

- `:port` - The local port server should listen (defaults to 0)
- `:host` - The hostname or string IP address to bind the server (defaults to "localhost")

If `:port` is 0 then the server binds to any available port.

The return value is a map with the following:

- `:port` - The port number server is listening
- `:stop` - A 0-args function that stops the server
- `:running?` - A 0-args function that returns truthy is the server is running
- `:server` - The server object

## TODO

[ ] WebSocket support
[ ] HTTPS
[ ] API docs
[ ] Perf tests

## License

Copyright © 2023 Metosin Oy and contributors.

Available under the terms of the Eclipse Public License 2.0, see [LICENSE](./LICENSE)
