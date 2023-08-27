(ns metosin.nima-ring.nima-server
  (:require [metosin.nima-ring.impl :as impl])
  (:import (io.helidon.nima.webserver WebServer
                                      WebServerConfig$Builder)
           (io.helidon.nima.webserver.http HttpRouting)))


(set! *warn-on-reflection* true)


(defn routing
  "Create a Nima HttpRouting object for provided routes. The `routes` must be a sequence
   of route definitions. Each route definition must be a vector of three elements:
     - method, a keyword for HTTP request method
     - path, a string, see https://helidon.io/docs/v3/#/se/webserver for more information
     - handler function
   In addition to regular HTTP request keyword (:get, :post, :head, etc) the method can also be:
     `:any`      - Matches any HTTP request method
     `:error`    - Adds an error handler, the path must be a exception class and the handler
                   must be a Nima ErrorHandler, see `->ErrorHandler`
     `:service`  - Add Nima `io.helidon.nima.webserver.http.HttpService` to handle the route"
  ^HttpRouting [routes]
  (-> (HttpRouting/builder)
      (impl/add-routes routes)
      (.build)))


(defn- add-routing ^WebServerConfig$Builder [^WebServerConfig$Builder builder routing-or-vec]
  (.addRouting builder ^HttpRouting (if (instance? HttpRouting routing-or-vec)
                                      routing-or-vec
                                      (routing routing-or-vec))))


(defn server
  "Create and start an instance of Nima HTTP server. Accepts Nima HttpRouting 
   object and optionally a map of options. Currently supported options are:
      :host      Host name to use, defaults to \"localhost\"
      :port      Port to use, or 0 to let server pick any available port number. Defaults to 0
   Returns a map with following items:
      :server    Instance of io.helidon.nima.webserver.WebServer
      :port      Port number
      :stop      Zero arg function, closes the server when called
      :running?  Zero arg function that returns `true` if server is running.
   "
  (^WebServer [routing-or-vec] (server routing-or-vec nil))
  (^WebServer [routing-or-vec {:keys [host port]}]
   (let [^WebServer server  (-> (WebServer/builder)
                                (.host (or host "localhost"))
                                (.port (int (or port 0)))
                                (add-routing routing-or-vec)
                                (.build)
                                (.start))]
     {:server   server
      :port     (.port server)
      :stop     (fn [] (.stop server))
      :running? (fn [] (.isRunning server))})))


(defn nima-server
  "Conveniency helper to create and start an instance of Nima HTTP server. Accepts 
   Ring handler function and optionally a map of options. Supported options
   are return value is described in `metosin.nima-ring.nima-server/server` function
   above"
  (^WebServer [handler] (nima-server handler nil))
  (^WebServer [handler opts]
   (server (routing [[:any "/*" handler]]) opts)))


(comment

  (def nima (nima-server (fn [_] {:status  200
                                  :headers {"content-type" "text/plain"}
                                  :body    "Hi"})))
  ((:running? nima))
  ;; => true

  (:port nima)
  ;; => 56245


  ;; $ http :56245/
  ;; HTTP/1.1 200 OK
  ;; Connection: keep-alive
  ;; Content-Type: text/plain
  ;; Date: Sat, 26 Aug 2023 15:45:09 +0300
  ;; Transfer-Encoding: chunked
  ;; 
  ;; Hi

  ((:stop nima))
  ;; => #object[io.helidon.nima.webserver.LoomServer 0x4a5bccfb "io.helidon.nima.webserver.LoomServer@4a5bccfb"]

  ((:running? nima))
  ;; => false

  ;; $ http :56245/
  ;; http: error: ConnectionError: HTTPConnectionPool(host='localhost', port=58198): Max retries exceeded with url: / 
  )
