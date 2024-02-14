(ns metosin.clj-helidon.util
  (:import (java.util.function Supplier)
           (io.helidon.common GenericType)))



(set! *warn-on-reflection* true)


(defn supplier ^Supplier [value]
  (reify Supplier
    (get [_] value)))



(defn generic-type ^GenericType [^Class clazz]
  (GenericType/create clazz))


(def ^GenericType object-generic-type (generic-type java.lang.Object))
