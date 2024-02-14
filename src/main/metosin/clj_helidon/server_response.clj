(ns metosin.clj-helidon.server-response
  (:require [metosin.clj-helidon.http-status :refer [http-status]]
            [metosin.clj-helidon.http-header :refer [http-header-value]]
            [metosin.clj-helidon.default-body-writer :as body-writer])
  (:import (io.helidon.webserver.http ServerResponse)))


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
  (let [body (-> ring-resp :body)]
    (when-not (body-writer/write-body body server-resp)
      (.send server-resp body))))
