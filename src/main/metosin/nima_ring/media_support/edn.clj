(ns metosin.nima-ring.media-support.edn
  (:require [clojure.edn :as edn]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support :as ms])
  (:import (java.nio.charset StandardCharsets)
           (io.helidon.http WritableHeaders)
           (io.helidon.http.media MediaSupport)))


(set! *warn-on-reflection* true)


;;
;; ==================================================================================================
;; EDN
;; ==================================================================================================
;;


(def application-edn "application/edn")
(def media-type-edn (media-type application-edn))
(def content-type-edn (http-header-value "content-type" (str application-edn "; charset=UTF-8")))


(defn edn-media-support ^MediaSupport []
  (ms/simple-media-support
   media-type-edn
   (ms/reader-response :supported (fn [_ ^java.io.InputStream in _]
                                    (-> (java.io.InputStreamReader. in StandardCharsets/UTF_8)
                                        (java.io.PushbackReader.)
                                        (edn/read))))
   (ms/writer-response :supported (fn [_ body ^java.io.OutputStream out _ ^WritableHeaders response-headers]
                                    (.setIfAbsent response-headers content-type-edn)
                                    (binding [*out*          (java.io.OutputStreamWriter. out StandardCharsets/UTF_8)
                                              *print-length* nil]
                                      (pr body)
                                      (.flush *out*))))))
