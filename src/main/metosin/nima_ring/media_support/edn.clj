(ns metosin.nima-ring.media-support.edn
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support :as ms])
  (:import (java.io OutputStream
                    InputStream
                    InputStreamReader
                    OutputStreamWriter
                    PushbackReader)
           (java.nio.charset StandardCharsets)
           (io.helidon.common.http WritableHeaders)
           (io.helidon.nima.http.media MediaSupport)))


(set! *warn-on-reflection* true)


;;
;; ==================================================================================================
;; EDN
;; ==================================================================================================
;;


(def application-edn (media-type "application/edn"))
(def content-type-edn (http-header-value "content-type" "application/edn; charset=UTF-8"))


(defn edn-media-support ^MediaSupport []
  (ms/simple-media-support application-edn
                           (ms/reader-response :supported (fn [_ ^InputStream in _]
                                                            (-> in
                                                                (InputStreamReader. StandardCharsets/UTF_8)
                                                                (PushbackReader.)
                                                                (edn/read))))
                           (ms/writer-response :supported (fn [_ body ^OutputStream out _ ^WritableHeaders response-headers]
                                                            (.setIfAbsent response-headers content-type-edn)
                                                            (if (instance? InputStream body)
                                                              (io/copy body out)
                                                              (binding [*out*          (OutputStreamWriter. out StandardCharsets/UTF_8)
                                                                        *print-length* nil]
                                                                (pr body)
                                                                (.flush *out*)))
                                                            (.flush out)))))
