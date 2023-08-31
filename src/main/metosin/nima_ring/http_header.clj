(ns metosin.nima-ring.http-header
  (:require [clojure.reflect :as reflect])
  (:import (io.helidon.common.http Http$Header
                                   Http$HeaderName
                                   Http$HeaderValue
                                   Http$HeaderValues)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Header name:
;; ========================================================================================
;;


(def ^:private -name->http-header
  (->> (reflect/type-reflect Http$Header)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.common.http.Http$HeaderName) :type))
       (map (fn [header-name-field]
              (-> (.getField Http$Header (name (:name header-name-field)))
                  (.get nil))))
       (map (fn [^Http$HeaderName header-name]
              [(.lowerCase header-name) header-name]))
       (into {})))


(defn http-header-name
  "Maps header names in lower-case kebab string format into `io.helidon.common.http.Http$HeaderName` instances.
   First tries to use members of Java enum `io.helidon.common.http.HeaderEnum`. If requested header is not part 
   of `io.helidon.common.http.HeaderEnum`, creates a new instance of Http$HeaderName."
  ^Http$HeaderName [header-name]
  (if (instance? Http$HeaderName header-name)
    header-name
    (or (-name->http-header header-name)
        (Http$Header/create header-name))))


;;
;; ========================================================================================
;; HTTP Headers:
;; ========================================================================================
;;


(defn- apply-charset-utf8 [header-values]
  (let [utf-8-media-types (->> ["text/plain" "text/html" "text/css"
                                "application/javascript"
                                "application/json"
                                "application/edn"
                                "application/transit+json"]
                               (map (fn [media-type]
                                      [media-type (Http$Header/create Http$Header/CONTENT_TYPE (str media-type "; charset=UTF-8"))]))
                               (into {}))]
    (update header-values Http$Header/CONTENT_TYPE merge utf-8-media-types)))


(def ^:private -http-header-values
  (->> (reflect/type-reflect Http$HeaderValues)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.common.http.Http$HeaderValue) :type))
       (map (fn [header-value-field]
              (-> (.getField Http$HeaderValues (name (:name header-value-field)))
                  (.get nil))))
       (reduce (fn [acc ^Http$HeaderValue header-value]
                 (update acc
                         (.headerName header-value)
                         assoc
                         (.value header-value)
                         header-value))
               {})
       (apply-charset-utf8)))


(defn http-header-value
  "Returns a HeaderValue instance for given header name and value. Header name may be a string or 
   HeaderName instance. Value is always converted to string. Tries to use one of the commonly used
   header values from cache, or creates a new value instance if cached value is not available."
  ^Http$HeaderValue [header-name header-value]
  (let [header-name  (http-header-name header-name)
        header-value (str header-value)]
    (or (-> (get -http-header-values header-name)
            (get header-value))
        (Http$Header/create header-name header-value))))
