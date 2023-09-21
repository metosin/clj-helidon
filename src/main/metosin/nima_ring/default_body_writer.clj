(ns metosin.nima-ring.default-body-writer
  (:require [clojure.java.io :as io])
  (:import (io.helidon.nima.webserver.http ServerResponse)))


(set! *warn-on-reflection* true)


(defprotocol BodyWriter
  (write-body [this server-response]))


(extend-protocol BodyWriter
  java.io.InputStream
  (write-body [this ^ServerResponse server-response]
    (with-open [out (.outputStream server-response)]
      (io/copy this out))
    true)

  java.io.Reader
  (write-body [this ^ServerResponse server-response]
    (with-open [out (.outputStream server-response)]
      (io/copy this out))
    (.close this)
    true)

  java.io.File
  (write-body [this ^ServerResponse server-response]
    (with-open [in  (java.io.FileInputStream. this)
                out (.outputStream server-response)]
      (io/copy in out))
    true)

  java.nio.file.Path
  (write-body [this ^ServerResponse server-response]
    (with-open [in  (java.io.FileInputStream. (.toFile this))
                out (.outputStream server-response)]
      (io/copy in out))
    true)

  nil
  (write-body [_ ^ServerResponse server-response]
    (.send server-response)
    true)

  java.lang.Object
  (write-body [_ _]
    false))


(extend-type (Class/forName "[B")
  BodyWriter
  (write-body [this ^ServerResponse server-response]
    (with-open [out (.outputStream server-response)]
      (io/copy this out))
    true))


(extend-type (Class/forName "[C")
  BodyWriter
  (write-body [this ^ServerResponse server-response]
    (with-open [out (.outputStream server-response)]
      (io/copy this out))
    true))
