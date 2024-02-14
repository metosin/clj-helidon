(ns metosin.clj-helidon.http-routing
  (:require [metosin.clj-helidon.http-handler :refer [http-handler http-error-handler]]
            [metosin.clj-helidon.http-route :refer [http-route]]
            [metosin.clj-helidon.path-matcher :refer [path-matcher-any]]
            [metosin.clj-helidon.util :refer [supplier]])
  (:import (io.helidon.webserver.http HttpRouting
                                      HttpRouting$Builder
                                      Handler)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Routing:
;; ========================================================================================
;;



(defn- build-route [^HttpRouting$Builder builder [method path handler]]
  (cond
    (nil? handler)
    builder

    (= method :error)
    (do (assert (and (instance? java.lang.Class path)
                     (.isAssignableFrom Throwable path))
                (str ":error path must be an exception class: path = [" path "] (" (type path) ")"))
        (.error builder ^Throwable path (http-error-handler handler)))

    (= method :service)
    (do (assert (string? path) (str ":service path must be a string: path = [" path "] (" (type path) ")"))
        (.register builder ^String path (supplier (http-handler handler))))

    (= method :any)
    (.route builder (http-route path nil handler))

    :else
    (.route builder (http-route path method handler))))


(defn- add-routes ^HttpRouting$Builder [^HttpRouting$Builder builder routes]
  (reduce build-route builder routes))


(defn http-routing-builder
  "Create a Nima HttpRouting builder object for provided routes. The `routes` must be a 
   sequence of route definitions. Each route definition must be a vector of three elements:
     - method, a keyword for HTTP request method
     - path, a string, see https://helidon.io/docs/v3/#/se/webserver for more information
     - handler function
   In addition to regular HTTP request keyword (:get, :post, :head, etc) the method can also be:
     `:any`      - Matches any HTTP request method
     `:error`    - Adds an error handler, the path must be a exception class and the handler
                   must be a Nima ErrorHandler, see `->ErrorHandler`
     `:service`  - Add Nima `io.helidon.nima.webserver.http.HttpService` to handle the route"
  ^HttpRouting$Builder [routes]
  (cond
    (or (instance? Handler routes) (fn? routes)) (-> (HttpRouting/builder)
                                                     (.route (http-route (path-matcher-any) nil (http-handler routes))))
    (vector? routes) (-> (HttpRouting/builder)
                         (add-routes routes))
    :else (throw (IllegalArgumentException. (format "can't create router builder from [%s] (%s)" routes (type routes))))))


(defn http-routing
  "Create a Nima HttpRouting object for provided routes. The `routes` must be a sequence
   of route definitions. Each route definition must be a vector of three elements:
     - method, a keyword for HTTP request method
     - path, a string, see https://helidon.io/docs/v3/#/se/webserver for more information
     - handler function
   In addition to regular HTTP request keyword (:get, :post, :head, etc) the method can also be:
     `:any`      - Matches any HTTP request method
     `:error`    - Adds an error handler, the path must be a exception class and the handler
                   must be a Nima ErrorHandler, see `->ErrorHandler`
     `:service`  - Add Nima `io.helidon.nima.webserver.http.HttpService` to handle the route"
  ^HttpRouting [routes]
  (cond
    (instance? HttpRouting routes) routes
    (instance? HttpRouting$Builder routes) (.build ^HttpRouting$Builder routes)
    (or (instance? Handler routes) (fn? routes)) (-> (HttpRouting/builder)
                                                     (.route (http-route (path-matcher-any) nil (http-handler routes)))
                                                     (.build))
    (vector? routes) (-> (HttpRouting/builder)
                         (add-routes routes)
                         (.build))
    :else (throw (IllegalArgumentException. (format "can't create router from [%s] (%s)" routes (type routes))))))

