(ns build
  (:require [clojure.tools.build.api :as b]))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn make-pom [{:keys [version]}]
  (b/write-pom {:basis   (b/create-basis)
                :target  "."
                :lib     'metosin.nima-ring/nima-ring
                :version version}))
