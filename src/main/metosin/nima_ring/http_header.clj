(ns metosin.nima-ring.http-header
  (:require [clojure.reflect :as reflect])
  (:import (io.helidon.http Header
                            HeaderName
                            HeaderNames
                            HeaderValues)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Header name:
;; ========================================================================================
;;


(def ^:private -name->http-header
  (->> (reflect/type-reflect HeaderNames)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.HeaderName) :type))
       (map (fn [header-name-field]
              (-> (.getField HeaderNames (name (:name header-name-field)))
                  (.get nil))))
       (map (fn [^HeaderName header-name]
              [(.lowerCase header-name) header-name]))
       (into {})))


(defn http-header-name
  "Maps header names in lower-case kebab string format into `io.helidon.http.HeaderName` instances.
   First tries to use members of Java class `io.helidon.http.Header.HeaderNames`. If requested header is not part 
   of that class, creates a new instance of HeaderName."
  ^HeaderName [header-name]
  (if (instance? HeaderName header-name)
    header-name
    (or (-name->http-header header-name)
        (HeaderNames/create header-name))))


;;
;; ========================================================================================
;; HTTP Headers:
;; ========================================================================================
;;


(def ^:private -http-header-values
  (let [apply-charset-utf8 (fn [header-values]
                             (let [utf-8-media-types (->> ["text/plain"
                                                           "text/html"
                                                           "text/css"
                                                           "application/javascript"
                                                           "application/json"
                                                           "application/edn"
                                                           "application/transit+json"]
                                                          (reduce (fn [acc media-type]
                                                                    (assoc acc media-type
                                                                           (HeaderValues/create HeaderNames/CONTENT_TYPE (str media-type "; charset=UTF-8"))))
                                                                  {}))]
                               (update header-values HeaderNames/CONTENT_TYPE merge utf-8-media-types)))]
    (->> (reflect/type-reflect HeaderValues)
         :members
         (filter (partial instance? clojure.reflect.Field))
         (filter (comp :static :flags))
         (filter (comp (partial = 'io.helidon.http.Header) :type))
         (map (fn [header-value-field]
                (-> (.getField HeaderValues (name (:name header-value-field)))
                    (.get nil))))
         (reduce (fn [acc ^Header header-value]
                   (update acc
                           (.headerName header-value)
                           assoc
                           (.value header-value)
                           header-value))
                 {})
         (apply-charset-utf8))))


(defn http-header-value
  "Returns a Header instance for given header name and value. Header name may be a string or 
   HeaderName instance. Value is always converted to string. Tries to use one of the commonly used
   header values from cache, or creates a new value instance if cached value is not available."
  ^Header [header-name header-value]
  (let [header-name  (http-header-name header-name)
        header-value (str header-value)]
    (or (-> (get -http-header-values header-name)
            (get header-value))
        (HeaderValues/create header-name header-value))))
