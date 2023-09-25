(ns metosin.nima-ring.http-method
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.reflect :as reflect])
  (:import (io.helidon.http Http$Method)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Methods:
;; ========================================================================================
;;


(def ^:private -kw->http-method
  "Maps values of the Java enumeration `io.helidon.common.http.Http$Method` to keywords"
  (->> (reflect/type-reflect Http$Method)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.Http$Method) :type))
       (map (fn [{field-name :name}]
              [(-> (name field-name)
                   (str/lower-case)
                   (keyword))
               (-> (.getField Http$Method (name field-name))
                   (.get nil))]))
       (into {})))


(defn http-method
  "Returns Http$Method for given Ring keyword (:get, :post, etc.)"
  ^Http$Method [method]
  (if (instance? Http$Method method)
    method
    (or (-kw->http-method method)
        (throw (IllegalArgumentException. (format "Unknown HTTP method: [%s] (%s)" method (type method)))))))


(defn http-methods
  "Returns a Java array of Http$Method for given Ring method keywords"
  ^"[Lio.helidon.http.Http$Method;"
  [& methods]
  (into-array Http$Method (keep http-method methods)))


(def http-method->kw (set/map-invert -kw->http-method))

