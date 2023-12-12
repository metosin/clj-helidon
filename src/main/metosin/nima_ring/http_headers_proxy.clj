(ns metosin.nima-ring.http-headers-proxy
  (:require [metosin.nima-ring.http-header :as h])
  (:import (io.helidon.http Header
                            ServerRequestHeaders)
           (io.helidon.common.parameters Parameters)
           (clojure.lang IFn
                         IPersistentMap
                         MapEntry
                         MapEquivalence)
           (java.util Map)))


(set! *warn-on-reflection* true)


;; Inspired by https://github.com/mpenet/mina/blob/main/src/s_exp/mina/request.clj which 
;; is in turn inspired by ring-undertow


(defn- header-value [^ServerRequestHeaders headers k not-found]
  (-> headers
      (.first (h/http-header-name k))
      (.orElse not-found)))


(deftype HeaderMapProxy [^ServerRequestHeaders headers]
  Map
  (size [_]
    (.size headers))

  (get [_ k]
    (header-value headers k nil))

  (getOrDefault [_ k not-found]
    (header-value headers k not-found))

  (containsKey [_ k]
    (.contains headers (h/http-header-name k)))

  MapEquivalence

  IFn
  (invoke [_ k]
    (header-value headers k nil))

  (invoke [_ k not-found]
    (header-value headers k not-found))

  IPersistentMap
  (valAt [_ k]
    (header-value headers k nil))

  (valAt [_ k not-found]
    (header-value headers k not-found))

  (entryAt [_ k]
    (when-let [v (header-value headers k nil)]
      (MapEntry/create k v)))

  (empty [_]
    {})

  (count [_]
    (.size headers))

  (seq [_]
    (->> headers
         (eduction (map (fn [^Header header]
                          (MapEntry/create (-> header .headerName .lowerCase)
                                           (-> header .value)))))
         (seq)))

  (equiv [this other]
    (identical? this other))

  (iterator [_]
    (->> headers
         ^Iterable (eduction (map (fn [^Header header]
                                    (MapEntry/create (-> header .headerName .lowerCase)
                                                     (-> header .value)))))
         .iterator))

  Object
  (toString [_]
    (.toString headers))
  (equals [this other]
    (identical? this other)))


(defn- parameter-value [^Parameters parameters k not-found]
  (-> parameters
      (.first (name k))
      (.orElse not-found)))


(deftype ParametersProxy [^Parameters parameters]
  Map
  (size [_]
    (.size parameters))

  (get [_ k]
    (parameter-value parameters k nil))

  (getOrDefault [_ k not-found]
    (parameter-value parameters k not-found))

  (containsKey [_ k]
    (.contains parameters (name k)))

  MapEquivalence

  IFn
  (invoke [_ k]
    (parameter-value parameters k nil))

  (invoke [_ k not-found]
    (parameter-value parameters k not-found))

  IPersistentMap
  (valAt [_ k]
    (parameter-value parameters k nil))

  (valAt [_ k not-found]
    (parameter-value parameters k not-found))

  (entryAt [_ k]
    (when-let [v (parameter-value parameters k nil)]
      (MapEntry/create k v)))

  (empty [_]
    {})

  (count [_]
    (.size parameters))

  (seq [_]
    (->> parameters
         (.names)
         (eduction (map (fn [param-name]
                          (MapEntry/create (keyword param-name)
                                           (parameter-value parameters param-name nil)))))
         (seq)))

  (equiv [this other]
    (identical? this other))

  (iterator [_]
    (->> parameters
         (.names)
         ^Iterable (eduction (map (fn [param-name]
                                    (MapEntry/create (keyword param-name)
                                                     (parameter-value parameters param-name nil)))))
         .iterator))

  Object
  (toString [_]
    (.toString parameters))
  (equals [this other]
    (identical? this other)))
