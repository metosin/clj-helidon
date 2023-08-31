(ns metosin.nima-ring.media-support.json
  (:require [clojure.java.io :as io]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support :as ms]
            [jsonista.core :as json])
  (:import (java.io OutputStream
                    InputStream
                    InputStreamReader
                    OutputStreamWriter)
           (java.nio.charset StandardCharsets)
           (io.helidon.common.http WritableHeaders)
           (io.helidon.nima.http.media MediaSupport)))


(set! *warn-on-reflection* true)



;;
;; ==================================================================================================
;; JSON
;; ==================================================================================================
;;


(defn json-media-support ^MediaSupport []
  (let [content-type-json (http-header-value "content-type" "application/json; charset=UTF-8")]
    (ms/simple-media-support (media-type "application/json")
                             (ms/reader-response :supported (fn [_ ^InputStream in _]
                                                              (-> in
                                                                  (InputStreamReader. StandardCharsets/UTF_8)
                                                                  (json/read-value json/keyword-keys-object-mapper))))
                             (ms/writer-response :supported (fn [_ body ^OutputStream out _ ^WritableHeaders response-headers]
                                                              (.setIfAbsent response-headers content-type-json)
                                                              (if (instance? InputStream body)
                                                                (io/copy body out)
                                                                (json/write-value (OutputStreamWriter. out StandardCharsets/UTF_8) body))
                                                              (.flush out))))))
