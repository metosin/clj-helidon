(ns metosin.nima-ring.server-request
  (:require [metosin.nima-ring.http-method :refer [http-method->kw]]
            [metosin.nima-ring.util :refer [object-generic-type]]
            [metosin.nima-ring.http-headers-proxy :as proxy])
  (:import (io.helidon.nima.webserver.http ServerRequest
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
  (let [prologue    (.prologue req)
        local       (.localPeer req)
        remote-addr (-> (.remotePeer req)
                        ^java.net.InetSocketAddress (.address)
                        (.getAddress)
                        (.getHostAddress))
        query       (.query req)
        path        (.path req)
        content     (.content req)]
    {:server-port     (.port local)               ; The port on which the request is being handled. 
     :server-name     (.host local)               ; The resolved server name, or the server IP address.
     :remote-addr     remote-addr                 ; The IP address of the client or the last proxy that sent the request.
     :uri             (.toString path)            ; The request URI (the full path after the domain name) .
     :query-string    (when-not (.isEmpty query)  ; The query string, if present.
                        (.value query))
     :protocol        (case (-> prologue .protocolVersion)
                        "1.0" "HTTP/1.0"
                        "1.1" "HTTP/1.1"
                        "2.0" "HTTP/2")
     :scheme          (if (.isSecure req)         ; The transport protocol, either :http or :https. 
                        :https
                        :http)
     :request-method  (-> prologue                ; The HTTP request method 
                          (.method)
                          http-method->kw)
     :headers         (proxy/->HeaderMapProxy (.headers req))
     :body            (when (.hasEntity content)  ; Try to read body using MediaContext
                        (.as content object-generic-type))
     ; Values specific to this library: 
     :nima/parameters {:query (proxy/->ParametersProxy query)
                       :path  (proxy/->ParametersProxy (.pathParameters path))}
     :nima/request    req
     :nima/response   resp}))
