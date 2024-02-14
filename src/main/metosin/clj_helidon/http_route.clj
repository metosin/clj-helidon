(ns metosin.clj-helidon.http-route
  (:require [metosin.clj-helidon.http-method :refer [http-methods]]
            [metosin.clj-helidon.http-handler :refer [http-handler]]
            [metosin.clj-helidon.path-matcher :refer [path-matcher]])
  (:import (io.helidon.webserver.http HttpRoute)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Route:
;; ========================================================================================
;;


(defn http-route
  "Creates a Nima HttpRoute object fron provided path, method and handler. The arguments are:
     `path`       Route path, either a string or instance of PathMatcher
     `method`     HTTP method, a keyword (:get, :post, :put, ...) or Http$Method
     `handler`    Handler, a Ring handler function or HttpHandler instance"
  ^HttpRoute [path method handler]
  (let [builder (HttpRoute/builder)]
    (when path
      (.path builder (path-matcher path)))
    (when method
      (.methods builder (http-methods method)))
    (when handler
      (.handler builder (http-handler handler)))
    (.build builder)))

