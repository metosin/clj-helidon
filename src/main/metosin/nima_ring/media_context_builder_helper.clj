(ns metosin.nima-ring.media-context-builder-helper
  (:require [metosin.nima-ring.default-media-support :as default-media-support]
            [metosin.nima-ring.media-support :as media-support]
            [metosin.nima-ring.media-support.transit :as transit]
            [metosin.nima-ring.media-support.edn :as edn]
            [metosin.nima-ring.media-support.json :as json])
  (:import (io.helidon.http.media MediaSupport MediaContext)
           (io.helidon.webserver WebServerConfig$Builder)))


(set! *warn-on-reflection* true)


;;
;; Builder support:
;;


(def ^:private common-media-supports {:io      [default-media-support/octet-stream-media-support]
                                      :text    [default-media-support/text-media-support]
                                      :json    [json/json-media-support]
                                      :transit [transit/transit-media-support]
                                      :edn     [edn/edn-media-support]
                                      :api     [json/json-media-support
                                                transit/transit-media-support
                                                edn/edn-media-support]})


(defn- invalid-type! [^Class expected actual]
  (throw (ex-info (format "wrong type, was expecting %s, got %s" (.getName expected) (if actual (.getName (class actual)) "nil"))
                  {:expected expected
                   :actual   actual})))


(defn- coerce-media-supports [config-media-supports]
  (->> (cond
         (nil? config-media-supports) [:api :text :io]
         (sequential? config-media-supports) config-media-supports
         :else [config-media-supports])
       (mapcat (fn [media-support]
                 (cond
                   (instance? MediaSupport media-support) [media-support]
                   (keyword? media-support) (let [common-media-support-factories (or (get common-media-supports media-support)
                                                                                     (throw (ex-info (str "unknown media-type: " media-support)
                                                                                                     {:unknown-media-support media-support})))]
                                              (map (fn [f] (f)) common-media-support-factories))
                   :else (invalid-type! MediaSupport media-support))))))


(defn- coerce-media-context ^MediaContext [config-media-context config-media-supports]
  (if config-media-context
    (if (instance? MediaContext config-media-context)
      config-media-context
      (invalid-type! MediaContext config-media-context))
    (media-support/media-context (coerce-media-supports config-media-supports))))


(defn add-media-context ^WebServerConfig$Builder [^WebServerConfig$Builder builder config-media-context config-media-supports]
  (.mediaContext builder ^MediaContext (coerce-media-context config-media-context config-media-supports)))
