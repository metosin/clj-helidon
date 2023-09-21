(ns metosin.nima-ring.media-support.json
  (:require [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support :as ms]
            [jsonista.core :as json])
  (:import (java.nio.charset StandardCharsets)
           (io.helidon.common.http WritableHeaders)
           (io.helidon.nima.http.media MediaSupport)))


(set! *warn-on-reflection* true)


;;
;; ==================================================================================================
;; JSON
;; ==================================================================================================
;;


(def application-json "application/json")
(def media-type-json (media-type application-json))
(def content-type-json (http-header-value "content-type" (str application-json "; charset=UTF-8")))


(defn json-media-support ^MediaSupport []
  (ms/simple-media-support
   media-type-json
   (ms/reader-response :supported (fn [_ ^java.io.InputStream in _]
                                    (-> (java.io.InputStreamReader. in StandardCharsets/UTF_8)
                                        (json/read-value json/keyword-keys-object-mapper))))
   (ms/writer-response :supported (fn [_ body ^java.io.OutputStream out _ ^WritableHeaders response-headers]
                                    (.setIfAbsent response-headers content-type-json)
                                    (let [w (java.io.OutputStreamWriter. out StandardCharsets/UTF_8)]
                                      (json/write-value w body))))))
