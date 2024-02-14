(ns metosin.clj-helidon.media-support.json
  (:require [metosin.clj-helidon.http-header :refer [http-header-value]]
            [metosin.clj-helidon.media-type :refer [media-type]]
            [metosin.clj-helidon.media-support :as media-support]
            [jsonista.core :as json])
  (:import (java.nio.charset StandardCharsets)
           (io.helidon.http WritableHeaders)
           (io.helidon.http.media MediaSupport)))


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
  (media-support/simple-media-support
   media-type-json
   (media-support/reader-response :supported (fn [_ ^java.io.InputStream in _]
                                               (-> (java.io.InputStreamReader. in StandardCharsets/UTF_8)
                                                   (json/read-value json/keyword-keys-object-mapper))))
   (media-support/writer-response :supported (fn [_ body ^java.io.OutputStream out _ ^WritableHeaders response-headers]
                                               (.setIfAbsent response-headers content-type-json)
                                               (let [w (java.io.OutputStreamWriter. out StandardCharsets/UTF_8)]
                                                 (json/write-value w body))))))
