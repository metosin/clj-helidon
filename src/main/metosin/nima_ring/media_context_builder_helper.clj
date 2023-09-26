(ns metosin.nima-ring.media-context-builder-helper
  (:require [metosin.nima-ring.default-media-support :as default-media-support]
            [metosin.nima-ring.media-support :as media-support]
            [metosin.nima-ring.media-support.transit :as transit]
            [metosin.nima-ring.media-support.edn :as edn]
            [metosin.nima-ring.media-support.json :as json])
  (:import (io.helidon.http.media MediaContext)
           (io.helidon.webserver WebServerConfig$Builder)))


(set! *warn-on-reflection* true)


;;
;; Builder support:
;;


(defn add-media-context ^WebServerConfig$Builder [^WebServerConfig$Builder builder config-media-context config-media-supports]
  (let [context (cond
                  config-media-context config-media-context
                  config-media-supports (media-support/media-context config-media-supports)
                  :else (media-support/media-context [(json/json-media-support)
                                                      (transit/transit-media-support)
                                                      (edn/edn-media-support)
                                                      (default-media-support/text-media-support)
                                                      (default-media-support/default-media-support)]))]
    (.mediaContext builder ^MediaContext context)))
