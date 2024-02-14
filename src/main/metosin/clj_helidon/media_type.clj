(ns metosin.clj-helidon.media-type
  (:import (io.helidon.common.media.type MediaType
                                         MediaTypes)))


(set! *warn-on-reflection* true)


(defn media-type
  (^MediaType [^String type ^String subtype]
   (MediaTypes/create type subtype))
  (^MediaType [media-type]
   (if (instance? MediaType media-type)
     media-type
     (MediaTypes/create ^String media-type))))



#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-wildcard MediaTypes/WILDCARD)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-xml MediaTypes/APPLICATION_XML)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-json MediaTypes/APPLICATION_JSON)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-stream-json MediaTypes/APPLICATION_STREAM_JSON)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-form-urlencoded MediaTypes/APPLICATION_FORM_URLENCODED)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-multipart-form-data MediaTypes/MULTIPART_FORM_DATA)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-multipart-byteranges MediaTypes/MULTIPART_BYTERANGES)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-octet-stream MediaTypes/APPLICATION_OCTET_STREAM)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-text-plain MediaTypes/TEXT_PLAIN)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-text-xml MediaTypes/TEXT_XML)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-text-html MediaTypes/TEXT_HTML)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-openapi-yaml MediaTypes/APPLICATION_OPENAPI_YAML)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-openapi-json MediaTypes/APPLICATION_OPENAPI_JSON)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-application-javascript MediaTypes/APPLICATION_JAVASCRIPT)
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def media-type-event-stream MediaTypes/TEXT_EVENT_STREAM)
