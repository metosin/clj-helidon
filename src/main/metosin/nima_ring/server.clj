(ns metosin.nima-ring.server
  (:require [metosin.nima-ring.http-routing :refer [add-http-routing]]
            [metosin.nima-ring.media-context-builder-helper :as mcbh])
  (:import (io.helidon.webserver WebServer)
           (io.helidon.http.media MediaContext
                                  MediaSupport)))


(set! *warn-on-reflection* true)


(defprotocol NimaServer
  (server [this])
  (port [this])
  (running? [this])
  (shutdown [this]))


(deftype NimaServerImpl [^WebServer -server]
  NimaServer
  (server [_] -server)
  (port [_] (.port -server))
  (running? [_] (.isRunning -server))
  (shutdown [_] (.stop -server))

  java.io.Closeable
  (close [_] (.stop -server)))


(defn create-server
  "Create and start an instance of Nima Web server. Accepts Nima HttpRouting 
   object and optionally a map of options. Currently supported options are:
      :host           Host name to use, defaults to \"localhost\"
      :port           Port to use, or 0 to let server pick any available port number. Defaults to 0
      :media-context  MediaContext to use for reading/writing request/response bodies 
   Returns a map with following items:
      :server    Instance of io.helidon.nima.webserver.WebServer
      :port      Port number
      :stop      Zero arg function, closes the server when called
      :running?  Zero arg function that returns `true` if server is running"
  ([routing] (create-server routing nil))
  ([routing {:keys [host port media-context media-supports]}]
   (assert (or (nil? host)
               (string? host))
           "host must be a string")
   (assert (or (nil? port)
               (and (integer? port)
                    (or (zero? port)
                        (pos? port))))
           "port must be a positive number")
   (assert (not (and (some? media-context)
                     (some? media-supports)))
           "can't provide both media-context and media-supports")
   (assert (or (nil? media-context)
               (instance? MediaContext media-context))
           "media-context must be instance of MediaContext type")
   (assert (or (nil? media-supports)
               (and (sequential? media-supports)
                    (every? (partial instance? MediaSupport) media-supports)))
           "media-supports must contain instances of MediaSupport type")
   (let [^WebServer server (-> (WebServer/builder)
                               (.host (or host "localhost"))
                               (.port (int (or port 0)))
                               (add-http-routing routing)
                               (mcbh/add-media-context media-context media-supports)
                               (.build))]
     (->NimaServerImpl (.start server)))))



(comment

  (def nima (create-server (constantly {:status  200
                                        :headers {"content-type" "text/plain"}
                                        :body    "Hi"})))
  (running? nima)
  ;; => true

  (port nima)
  ;; => 51486


  ;; $ http :51486/
  ;; HTTP/1.1 200 OK
  ;; Connection: keep-alive
  ;; Content-Type: text/plain; charset=UTF-8
  ;; Date: Sat, 26 Aug 2023 15:45:09 +0300
  ;; Transfer-Encoding: chunked
  ;; 
  ;; Hi

  (server nima)
  ;; => #object[io.helidon.nima.webserver.LoomServer 0x255f0527 "io.helidon.nima.webserver.LoomServer@255f0527"]

  (shutdown nima)

  (running? nima)
  ;; => false 

  (with-open [nima (create-server (constantly {:status  200
                                               :headers {"content-type" "text/plain"}
                                               :body    "hi"}))]
    (running? nima))
  ;
  )
