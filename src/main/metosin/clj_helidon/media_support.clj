(ns metosin.clj-helidon.media-support
  (:require [metosin.clj-helidon.media-type :refer [media-type]]
            [metosin.clj-helidon.util :refer [supplier]])
  (:import (io.helidon.http Headers
                            HttpMediaType)
           (io.helidon.common.media.type MediaType)
           (io.helidon.http.media MediaContext
                                  MediaSupport
                                  MediaSupport$SupportLevel
                                  MediaSupport$ReaderResponse
                                  MediaSupport$WriterResponse
                                  EntityReader
                                  EntityWriter)))


(set! *warn-on-reflection* true)


;;
;; ==================================================================================================
;; EntityReader and EntityWriter
;; ==================================================================================================
;;


(defn entity-reader
  "Accepts function that takes 3 arguments:
     - type              : type of the body (io.helidon.common.GenericType)
     - in                : stream where body should be read (java.io.InputStream)
     - request-headers   : request headers (io.helidon.common.http.Headers)
   Function should return the body.
   Returns io.helidon.nima.http.media.EntityWriter instance"
  ^EntityReader [reader]
  (if (instance? EntityReader reader)
    reader
    (reify EntityReader
      (read [_ type in headers] (reader type in headers))
      ; These arities are used with Nima Client library:
      (read [_ _type _in _request-headers _response-headers] (throw (UnsupportedOperationException. "client operations not supported yet"))))))


(defn entity-writer
  "Accepts function that takes 5 arguments:
     - type              : type of the body (io.helidon.common.GenericType)
     - body              : body to write
     - out               : stream where body should be written (java.io.OutputStream)
     - request-headers   : request headers (io.helidon.common.http.Headers)
     - response-headers  : response headers (io.helidon.common.http.WritableHeaders)
   Function should return `nil`.
   Returns io.helidon.nima.http.media.EntityWriter instance"
  ^EntityWriter [writer]
  (if (instance? EntityWriter writer)
    writer
    (reify EntityWriter
      (write [_ type obj out request-headers response-headers] (writer type obj out request-headers response-headers))
      ; These arities are used with Nima Client library:
      (write [_ _type _obj _out _request-headers] (throw (UnsupportedOperationException. "client operations not supported yet"))))))


;;
;; ==================================================================================================
;; MediaSupport
;; ==================================================================================================
;;


(def ^:private media-support-levels {:not-supported                          MediaSupport$SupportLevel/NOT_SUPPORTED
                                     MediaSupport$SupportLevel/NOT_SUPPORTED MediaSupport$SupportLevel/NOT_SUPPORTED
                                     :compatible                             MediaSupport$SupportLevel/COMPATIBLE
                                     MediaSupport$SupportLevel/COMPATIBLE    MediaSupport$SupportLevel/COMPATIBLE
                                     :supported                              MediaSupport$SupportLevel/SUPPORTED
                                     MediaSupport$SupportLevel/SUPPORTED     MediaSupport$SupportLevel/SUPPORTED})


(defn- media-support-level ^MediaSupport$SupportLevel [support-level]
  (or (media-support-levels support-level)
      (throw (IllegalArgumentException. (str "Unknown support-level: " (pr-str support-level))))))


(defn reader-response ^MediaSupport$ReaderResponse [support-level reader]
  (MediaSupport$ReaderResponse. (media-support-level support-level)
                                (supplier (entity-reader reader))))


(defn writer-response ^MediaSupport$WriterResponse [support-level writer]
  (MediaSupport$WriterResponse. (media-support-level support-level)
                                (supplier (entity-writer writer))))


;;
;; ==================================================================================================
;; MediaSupport
;; ==================================================================================================
;;


(defn media-support ^MediaSupport [^String support-name ^String support-type get-reader-response get-writer-response]
  (reify MediaSupport
    (name [_] support-name)
    (type [_] support-type)
    (reader [_ generic-type request-headers]
      (get-reader-response generic-type request-headers))
    (writer [_ generic-type request-headers response-headers]
      (get-writer-response generic-type request-headers response-headers))
    ; These arities are used with Nima Client library:
    (reader [_ _generic-type _request-headers _response-headers] (throw (UnsupportedOperationException. "client API not supported yet")))
    (writer [_ _generic-type _request-headers] (throw (UnsupportedOperationException. "client API not supported yet")))))


(defn- media-type-supports? [^MediaType supported-media-type ^HttpMediaType actual-media-type]
  (and actual-media-type
       (.test actual-media-type supported-media-type)))


(def ^:private reader-unsupported (MediaSupport$ReaderResponse/unsupported))
(def ^:private writer-unsupported (MediaSupport$WriterResponse/unsupported))


(media-type-supports? (media-type "application/json") (-> (HttpMediaType/builder)
                                                          (.mediaType (media-type "*/*"))
                                                          (.charset "UTF-8")
                                                          (.build)))

(defn simple-media-support ^MediaSupport [content-type reader writer]
  (let [^MediaType content-type           (media-type content-type)
        ^MediaSupport$ReaderResponse reader-response        (cond
                                                              (instance? MediaSupport$ReaderResponse reader) reader
                                                              (instance? EntityReader reader) (reader-response :supported reader)
                                                              (fn? reader) (reader-response :supported (entity-reader reader))
                                                              :else (throw (java.lang.IllegalArgumentException. "reader must be ReaderResponse, EntityReader, or function")))
        ^MediaSupport$WriterResponse writer-response        (cond
                                                              (instance? MediaSupport$WriterResponse writer) writer
                                                              (instance? EntityWriter writer) (writer-response :supported writer)
                                                              (fn? writer) (writer-response :supported (entity-writer writer))
                                                              :else (throw (java.lang.IllegalArgumentException. "writer must be WriterResponse, EntityWriter, or function")))
        content-type-supports? (partial media-type-supports? content-type)]
    (media-support (.text content-type)
                   (.text content-type)
                   (fn [_ ^Headers request-headers]
                     (let [request-content-type (-> (.contentType request-headers) (.orElse nil))]
                       (if (content-type-supports? request-content-type)
                         reader-response
                         reader-unsupported)))
                   (fn [_ ^Headers request-headers _]
                     (if (some content-type-supports? (.acceptedTypes request-headers))
                       writer-response
                       writer-unsupported)))))


;;
;; ==================================================================================================
;; MediaContext
;; ==================================================================================================
;;


; See comment below.
(set! *warn-on-reflection* false)


(defn media-context
  (^MediaContext [media-supports] (media-context media-supports false))
  (^MediaContext [media-supports register-defaults]
   (-> (MediaContext/builder)
       (.registerDefaults register-defaults)
       (.addMediaSupports ^java.util.List (if (sequential? media-supports) media-supports [media-supports]))
       ; Helidon uses some dynamic code generation with MediaContext/builder. The `builder` function
       ; returns type io.helidon.http.media.MediaContextConfig$Builder, which I can't anywhere in the
       ; sources. Neither can vscode Java extension, nor clojure compiler. Using reflection I (and 
       ; clojure) can see that it does have no args function `build`, and it seems to work just fine. 
       ; Anyway, we get a reflection warning from this line it's turned on.
       (.build))))
