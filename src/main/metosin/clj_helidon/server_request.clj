(ns metosin.clj-helidon.server-request
  (:require [metosin.clj-helidon.http-method :refer [http-method->kw]]
            [metosin.clj-helidon.util :refer [object-generic-type]]
            [metosin.clj-helidon.http-headers-proxy :as proxy])
  (:import (io.helidon.webserver.http ServerRequest
                                      ServerResponse)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; Request
;; ========================================================================================
;;


; Accepts Nima ServerRequest and ServerResponse objects, returns Ring request map.
; See Ring docs: https://github.com/ring-clojure/ring/wiki/Concepts#requests

(defn server-req->ring-req [^ServerRequest req ^ServerResponse resp]
  (let [prologue (.prologue req)
        local    (.localPeer req)
        query    (.query req)
        path     (.path req)
        content  (.content req)]
    {:server-port     (.port local)
     :server-name     (.host local)
     :remote-addr     (-> (.remotePeer req)
                          ^java.net.InetSocketAddress (.address)
                          (.getAddress)
                          (.getHostAddress))
     :uri             (.toString path)
     :query-string    (.rawValue query)
     :protocol        (-> prologue .rawProtocol)
     :scheme          (if (.isSecure req) :https :http)
     :request-method  (-> prologue (.method) http-method->kw)
     :headers         (proxy/->HeaderMapProxy (.headers req))
     :body            (when (.hasEntity content) (.as content object-generic-type))
     ; Values specific to this library: 
     :nima/parameters {:query (proxy/->ParametersProxy query)
                       :path  (proxy/->ParametersProxy (.pathParameters path))}
     :nima/request    req
     :nima/response   resp}))
