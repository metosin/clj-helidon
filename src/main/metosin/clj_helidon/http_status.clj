(ns metosin.clj-helidon.http-status
  (:require [clojure.reflect :as reflect])
  (:import (io.helidon.http Status)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Status Codes:
;; ========================================================================================
;;


(def ^:private -code->http-status
  "Maps values of the Java enumeration `io.helidon.common.http.Status` to keywords"
  (->> (reflect/type-reflect Status)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.Status) :type))
       (map (fn [status-field]
              (-> (.getField Status (name (:name status-field)))
                  (.get nil))))
       (sort-by (fn [^Status status] (.code status)))
       (map (fn [^Status status]
              [(.code status) status]))
       (into {})))


(defn http-status
  "Maps numeric HTTP status codes to Java enum `io.helidon.common.http.Status`"
  ^Status [status-code]
  (if (instance? Status status-code)
    status-code
    (-code->http-status status-code)))
