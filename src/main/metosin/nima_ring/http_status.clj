(ns metosin.nima-ring.http-status
  (:require [clojure.reflect :as reflect])
  (:import (io.helidon.http Http$Status)))


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Status Codes:
;; ========================================================================================
;;


(def ^:private -code->http-status
  "Maps values of the Java enumeration `io.helidon.common.http.Http$Status` to keywords"
  (->> (reflect/type-reflect Http$Status)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.http.Http$Status) :type))
       (map (fn [status-field]
              (-> (.getField Http$Status (name (:name status-field)))
                  (.get nil))))
       (sort-by (fn [^Http$Status status] (.code status)))
       (map (fn [^Http$Status status]
              [(.code status) status]))
       (into {})))


(defn http-status
  "Maps numeric HTTP status codes to Java enum `io.helidon.common.http.Http$Status`"
  ^Http$Status [status-code]
  (if (instance? Http$Status status-code)
    status-code
    (-code->http-status status-code)))
