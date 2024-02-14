(ns metosin.clj-helidon.default-mime-types
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))


(def default-mime-types (delay
                          (with-open [in (-> (io/resource "metosin/clj_helidon/default-mime-types.edn")
                                             (io/reader)
                                             (java.io.PushbackReader.))]
                            (edn/read in))))
