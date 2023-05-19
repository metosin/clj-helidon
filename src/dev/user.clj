(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [clojure.tools.logging :as log]
            [kaocha.repl :as k]))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn start []
  (System/setProperty "org.jboss.logging.provider" "slf4j")
  (org.slf4j.bridge.SLF4JBridgeHandler/install)
  (log/info "user/start: system starting...")
  "System up")


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset []
  (log/info "user/reset: system reseting...")
  (tnr/refresh :after 'user/start))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-unit-tests []
  (k/run :unit))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-all-tests []
  (run-unit-tests))
