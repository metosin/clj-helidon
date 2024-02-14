# clj-helidon - Ring adapter for Helidon.io

** This library is experimental and for testing and educational use only **

A library that adapts [Helidon 4](https://helidon.io/) for the [Clojure Ring](https://github.com/ring-clojure/ring).

The main argument for Helidon is that it's based on new (available from JDK 19 onwards) _virtual threads_. The virtual thread in Java is a lightweight thread that does not consume a platform thread. This enables async programming with the same API as traditional blocking APIs. This means that applications using virtual threads can freely use the existing blocking I/O operations and still be fully async.

For `ring`, this means that you can forego the use of [async handlers](https://github.com/ring-clojure/ring/wiki/Concepts#handlers) and still get all the benefits of async.

Helidon is a very efficient and fast WebServer, for more information about the performance see https://medium.com/helidon/helidon-n%C3%ADma-helidon-on-virtual-threads-130bb2ea2088#f3b5

## Status

Alpha release, subject to changes and no guarantees of updates.

## Usage

Add clj-helidon as a dependency to your `deps.edn`:

```
io.github.metosin/clj-helidon {:git/tag "0.0.12"
                               :git/sha "0822426"}
```

Start the server:

```clj
(ns my-app
  (:require [metosin.clj-helidon.server :as helidon]))

(defn handler [req]
  {:status 200
   :body   "Hello"})

(def server (helidon/create-server handler {:port 8080}))

(println "Server listening on port" (helidon/port server))
```

The `create-server` function accepts a ring handler function or routing data, and optionally a
configuration map. Configuration can contain:

- `:host` - The hostname or string IP address to bind the server (defaults to "localhost")
- `:port` - The local port server should listen (defaults to 0)
- `:media-context` - MediaContext to use for content negotiation

When the port is 0 the server binds to any available port (very handy in testing).

The return value is an server object. The `metosin.clj-helidon.server` namespace
provides functions that take server object as argument. These are:

- `server` - Returns the instance of `io.helidon.webserver.WebServer`
- `port` - Returns the local port the server is bind to
- `running?` - Returns `true` if server is running, otherwise returns `false`
- `shutdown` - Closes the server

The server object also implements the `java.io.Closeable` interface, so you can use the
standard clojure `clojure.core/with-open` to open and automatically close the server. This
is quite handy in testing.

## TODO

[x] SSE support
[ ] WebSocket support
[ ] HTTPS
[ ] API docs
[ ] Perf tests
[ ] Split lib into multiple smaller libs
[ ] grpc

## License

Copyright © 2023 Metosin Oy and contributors.

Available under the terms of the Eclipse Public License 2.0, see [LICENSE](./LICENSE)
