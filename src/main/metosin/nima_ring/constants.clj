(ns metosin.nima-ring.constants
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.reflect :as reflect])
  (:import (io.helidon.common.http Http$Status
                                   Http$Method
                                   Http$Header
                                   Http$HeaderName
                                   HeaderEnum)))


(def http-method->kw
  (->> (reflect/type-reflect Http$Method)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.common.http.Http$Method) :type))
       (map (fn [{field-name :name}]
              [(-> (.getField Http$Method (name field-name))
                   (.get nil))
               (-> (name field-name)
                   (str/lower-case)
                   (keyword))]))
       (into {})))


(def kw->http-method (set/map-invert http-method->kw))


(def http-status->code (->> (reflect/type-reflect Http$Status)
                            :members
                            (filter (partial instance? clojure.reflect.Field))
                            (filter (comp :static :flags))
                            (filter (comp (partial = 'io.helidon.common.http.Http$Status) :type))
                            (map (fn [status-field]
                                   (-> (.getField Http$Status (name (:name status-field)))
                                       (.get nil))))
                            (sort-by (fn [^Http$Status status] (.code status)))
                            (map (fn [^Http$Status status]
                                   [status (.code status)]))
                            (into {})))


(def code->http-status (set/map-invert http-status->code))


(def http-header->name
  (->> (reflect/type-reflect HeaderEnum)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.common.http.HeaderEnum) :type))
       (map (fn [{field-name :name}]
              (let [header (Enum/valueOf HeaderEnum (name field-name))]
                [header (.lowerCase header)])))
       (into {})))


(def ^:private -name->http-header (set/map-invert http-header->name))


(defn name->http-header ^Http$HeaderName [header-name]
  (or (-name->http-header header-name)
      (Http$Header/create header-name)))

