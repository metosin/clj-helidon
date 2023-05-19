(ns metosin.nima-ring.nima-server
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [metosin.nima-ring.constants :as constants])
  (:import (io.helidon.nima.webserver WebServer)
           (io.helidon.nima.webserver.http HttpRouting
                                           Handler
                                           ServerRequest
                                           ServerResponse)))


(set! *warn-on-reflection* true)


; See Ring docs: https://github.com/ring-clojure/ring/wiki/Concepts#requests
(defn- server-req->ring-req [^ServerRequest req]
  (let [prologue    (.prologue req)
        local       (.localPeer req)
        remote-addr (-> (.remotePeer req)
                        ^java.net.InetSocketAddress (.address)
                        (.getAddress)
                        (.getHostAddress))
        query       (.query req)
        content     (.content req)]
    {:server-port    (.port local)               ; The port on which the request is being handled. 
     :server-name    (.host local)               ; The resolved server name, or the server IP address.
     :remote-addr    remote-addr                 ; The IP address of the client or the last proxy that sent the request.
     :uri            (.toString (.path req))     ; The request URI (the full path after the domain name) .
     :query-string   (when-not (.isEmpty query)  ; The query string, if present.
                       (.value query))
     :scheme         (-> prologue                ; The transport protocol, either :http or :https.
                         (.protocol)
                         (str/lower-case)
                         (keyword))
     :request-method (-> prologue                ; The HTTP request method 
                         (.method)
                         constants/http-method->kw)
     ; TODO: there is a much performant method available
     :headers        (->> req                    ; A Clojure map of lowercase header name strings to
                          (.headers)             ; corresponding header value strings.
                          (.toMap)
                          (reduce-kv (fn [acc k ^java.util.List v]
                                       (assoc acc (str/lower-case k) (.get v 0)))
                                     {}))
     :body           (when (.hasEntity content)  ; An InputStream for the request body, if present. 
                       (.inputStream content))}))


(defn- send-ring-req [{:keys [status headers body]} ^ServerResponse resp]
  (.status resp (constants/code->http-status status))
  (doseq [[header-name header-value] headers]
    (.header resp
             (constants/name->http-header header-name)
             ^"[Ljava.lang.String;" (into-array String [(str header-value)])))
  (when body
    (let [out (.outputStream resp)]
      (io/copy body out)
      (.flush out))))


(defn- ->handler ^"[Lio.helidon.nima.webserver.http.Handler;" [handler]
  (into-array Handler [(reify Handler
                         (handle [_this req resp]
                           (-> (server-req->ring-req req)
                               (handler)
                               (send-ring-req resp))))]))


(defn nima-server
  (^WebServer [handler] (nima-server handler nil))
  (^WebServer [handler {:keys [host port]}]
   (let [^HttpRouting routing (-> (HttpRouting/builder)
                                  (.any (->handler handler))
                                  (.build))
         ^WebServer server  (-> (WebServer/builder)
                                (.host (or host "localhost"))
                                (.port (int (or port 0)))
                                (.addRouting routing)
                                (.build)
                                (.start))]
     {:server   server
      :port     (.port server)
      :stop     (fn [] (.stop server))
      :running? (fn [] (.isRunning server))})))
