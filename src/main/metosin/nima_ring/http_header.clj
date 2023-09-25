(ns metosin.nima-ring.http-header
  (:require [clojure.reflect :as reflect])
  (:import (io.helidon.http Http$Header
                            Http$Headers
                            Http$HeaderName
                            Http$HeaderNames)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Header name:
;; ========================================================================================
;;


(def ^:private -name->http-header
  (->> (reflect/type-reflect Http$HeaderNames)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.Http$HeaderName) :type))
       (map (fn [header-name-field]
              (-> (.getField Http$HeaderNames (name (:name header-name-field)))
                  (.get nil))))
       (map (fn [^Http$HeaderName header-name]
              [(.lowerCase header-name) header-name]))
       (into {})))


(defn http-header-name
  "Maps header names in lower-case kebab string format into `io.helidon.http.Http$HeaderName` instances.
   First tries to use members of Java class `io.helidon.http.Header.HeaderNames`. If requested header is not part 
   of that class, creates a new instance of Http$HeaderName."
  ^Http$HeaderName [header-name]
  (if (instance? Http$HeaderName header-name)
    header-name
    (or (-name->http-header header-name)
        (Http$HeaderNames/create header-name))))


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
                                      [media-type (Http$Headers/create Http$HeaderNames/CONTENT_TYPE (str media-type "; charset=UTF-8"))]))
                               (into {}))]
    (update header-values Http$HeaderNames/CONTENT_TYPE merge utf-8-media-types)))


(def ^:private -http-header-values
  (->> (reflect/type-reflect Http$Headers)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.Http$Header) :type))
       (map (fn [header-value-field]
              (-> (.getField Http$Headers (name (:name header-value-field)))
                  (.get nil))))
       (reduce (fn [acc ^Http$Header header-value]
                 (update acc
                         (.headerName header-value)
                         assoc
                         (.value header-value)
                         header-value))
               {})
       (apply-charset-utf8)))


(defn http-header-value
  "Returns a Http$Header instance for given header name and value. Header name may be a string or 
   HeaderName instance. Value is always converted to string. Tries to use one of the commonly used
   header values from cache, or creates a new value instance if cached value is not available."
  ^Http$Header [header-name header-value]
  (let [header-name  (http-header-name header-name)
        header-value (str header-value)]
    (or (-> (get -http-header-values header-name)
            (get header-value))
        (Http$Headers/create header-name header-value))))
