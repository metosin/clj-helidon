(ns metosin.nima-ring.server-response
  (:require [clojure.java.io :as io]
            [metosin.nima-ring.http-status :refer [http-status]]
            [metosin.nima-ring.http-header :refer [http-header-value]])
  (:import (io.helidon.nima.webserver.http ServerResponse)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; Response handling:
;; ========================================================================================
;;


; Sends the Ring response and client using the Nima ServerResponse instance.

(defn send-ring-resp [ring-resp ^ServerResponse server-resp]
  (.status server-resp (-> ring-resp :status (or 200) (http-status)))
  (doseq [[header-name header-value] (-> ring-resp :headers)]
    (.header server-resp (http-header-value header-name header-value)))
  (when-let [body (-> ring-resp :body)]
    (let [resp-body (cond
                      ;; FIXME: test that Nima closes the input-stream once the response is handler
                      (instance? java.io.File body) (io/input-stream body)
                      (instance? java.nio.file.Path body) (io/input-stream (.toFile ^java.nio.file.Path body))
                      :else body)]
      (.send server-resp resp-body))))
