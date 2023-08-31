(ns metosin.nima-ring.default-media-support
  (:require [clojure.java.io :as io]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support :as media-support])
  (:import (java.io InputStream OutputStream)
           (java.nio.charset StandardCharsets)
           (io.helidon.common.http WritableHeaders
                                   Http$HeaderValues)
           (io.helidon.nima.http.media MediaSupport)))


(set! *warn-on-reflection* true)


(defn text-media-support
  (^MediaSupport [] (text-media-support "text/*"))
  (^MediaSupport [content-type]
   (let [content-type-text-plain (http-header-value "content-type" "text/plain")]
     (media-support/simple-media-support
      (media-type content-type)
      (media-support/reader-response :supported (fn [_ ^InputStream in _] (slurp in)))
      (media-support/writer-response :supported (fn [_ body ^OutputStream out _ ^WritableHeaders response-headers]
                                                  (.setIfAbsent response-headers content-type-text-plain)
                                                  (if (instance? java.io.InputStream body)
                                                    (io/copy body out)
                                                    (.write out (.getBytes (str body) StandardCharsets/UTF_8)))
                                                  (.flush out)))))))


(defn default-media-support ^MediaSupport []
  (media-support/media-support "nima-ring:default-media-support"
                               (str "default media support")
                               (fn [_ _]
                                 (media-support/reader-response :compatible (fn [_ in _] in)))
                               (fn [_ _ _]
                                 (media-support/writer-response :compatible
                                                                (fn [_ body ^java.io.OutputStream out _ ^WritableHeaders response-headers]
                                                                  (.setIfAbsent response-headers Http$HeaderValues/CONTENT_TYPE_OCTET_STREAM)
                                                                  (if (instance? java.io.InputStream body)
                                                                    (io/copy body out)
                                                                    (.write out (.getBytes (str body) StandardCharsets/UTF_8)))
                                                                  (.flush out))))))
