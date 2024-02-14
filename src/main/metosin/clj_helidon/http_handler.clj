(ns metosin.clj-helidon.http-handler
  (:require [metosin.clj-helidon.server-request :refer [server-req->ring-req]]
            [metosin.clj-helidon.server-response :refer [send-ring-resp]])
  (:import (io.helidon.webserver.http Handler
                                      ErrorHandler
                                      HttpService)))



(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP handlers:
;; ========================================================================================
;;


(defn- ->Handler ^Handler [handler]
  (reify Handler
    (handle [_ req resp]
      (-> (server-req->ring-req req resp)
          (handler)
          (send-ring-resp resp)))))


(defn http-handler
  "Coerce argument into Nima HTTP Handler instance."
  ^Handler [handler]
  (cond
    (instance? Handler handler) handler
    (instance? HttpService handler) handler
    (fn? handler) (->Handler handler)
    :else (throw (IllegalArgumentException. (format "Can't create HTTP Handler from argument: [%s] (%s)" (str handler) (type handler))))))


(defn http-handlers
  "Helper function that accepts multiple Ring handler functions and returns a Java array
   of Nima HTTP handler objects."
  ^"[Lio.helidon.webserver.http.Handler;" [& handlers]
  (->> (filter some? handlers)
       (map http-handler)
       (into-array Handler)))


(defn- ->ErrorHandler ^ErrorHandler [handler]
  (reify ErrorHandler
    (handle [_ req resp ex]
      (-> (server-req->ring-req req resp)
          (assoc :error ex)
          (handler)
          (send-ring-resp resp)))))


(defn http-error-handler
  "Coerce argument into Nima HTTP error handler instance."
  ^ErrorHandler [handler]
  (cond
    (instance? ErrorHandler handler) handler
    (fn? handler) (->ErrorHandler handler)
    :else (throw (IllegalArgumentException. (format "Can't create HTTP Error Handler from argument: [%s] (%s)" (str handler) (type handler))))))
