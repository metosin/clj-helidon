(ns metosin.nima-ring.impl
  (:refer-clojure :exclude [error-handler])
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.reflect :as reflect]
            [clojure.java.io :as io])
  (:import (io.helidon.common.http Http$Status
                                   Http$Method
                                   Http$Header
                                   Http$HeaderName
                                   Http$HeaderValue
                                   HeaderEnum
                                   PathMatcher
                                   PathMatchers)
           (io.helidon.nima.webserver.http ServerRequest
                                           ServerResponse
                                           HttpRoute
                                           HttpRouting$Builder
                                           Handler
                                           ErrorHandler
                                           HttpService)))


;;
;; Mappings from Nima types to clojure abstractions (keywords, functions, etc) and 
;; back.
;;
;; Conventions in this namespace:
;;
;; Low level constructors and builders have arrow in front of their names. For
;; example:
;;
;;    (->HttpMethod :get)
;;    => #object[io.helidon.common.http.Http$Method 0x361bd360 "GET"]
;;
;; Coercers have normal lowecase cebab names, for example:
;;
;;    (http-method :get)
;;    => #object[io.helidon.common.http.Http$Method 0x361bd360 "GET"]
;;


(set! *warn-on-reflection* true)


;;
;; ========================================================================================
;; HTTP Methods:
;; ========================================================================================
;;


(def ^:private -http-method->kw
  "Maps values of the Java enumeration `io.helidon.common.http.Http$Method` to keywords"
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


(def ^:private -kw->http-method (set/map-invert -http-method->kw))


(defn ->HttpMethod
  "Maps HTTP method keywords to Java enum `io.helidon.common.http.Http$Method`"
  ^Http$Method [method]
  (get -kw->http-method method))


(defn http-method
  "Returns Http$Method for given Ring keyword (:get, :post, etc.)"
  ^Http$Method [method]
  (if (instance? Http$Method method)
    method
    (->HttpMethod method)))


(defn http-methods
  "Returns a Java array of Http$Method for given Ring method keywords"
  ^"[Lio.helidon.common.http.Http$Method;"
  [& methods]
  (into-array Http$Method (keep http-method methods)))


;;
;; ========================================================================================
;; HTTP Status Codes:
;; ========================================================================================
;;


(def ^:private -http-status->code
  "Maps values of the Java enumeration `io.helidon.common.http.Http$Status` to keywords"
  (->> (reflect/type-reflect Http$Status)
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


(def ^:private -code->http-status (set/map-invert -http-status->code))


(defn http-status
  "Maps numeric HTTP status codes to Java enum `io.helidon.common.http.Http$Status`"
  ^Http$Status [http-status-code]
  (get -code->http-status http-status-code))


(comment
  (http-status 418)
  ;; => #object[io.helidon.common.http.Http$Status 0x41f663a8 "418 I'm a teapot"] 
  )


;;
;; ========================================================================================
;; HTTP Headers:
;; ========================================================================================
;;


(def ^:private -http-header->name
  "Maps values of the Java enumeration `io.helidon.common.http.HeaderEnum` to lower-case kebab strings"
  (->> (reflect/type-reflect HeaderEnum)
       :members
       (filter (partial instance? clojure.reflect.Field))
       (filter (comp :static :flags))
       (filter (comp (partial = 'io.helidon.common.http.HeaderEnum) :type))
       (map (fn [{field-name :name}]
              (let [^HeaderEnum header (Enum/valueOf HeaderEnum (name field-name))]
                [header (.lowerCase header)])))
       (into {})))


(comment
  -http-header->name
  ;; =>  {#object[io.helidon.common.http.HeaderEnum 0xc3b09c3 "ACCEPT"]          "accept"
  ;;      #object[io.helidon.common.http.HeaderEnum 0x590ebfd1 "CACHE_CONTROL"]  "cache-control"
  ;;      #object[io.helidon.common.http.HeaderEnum 0x7f4dcc4e "CONTENT_TYPE"]   "content-type"
  ;;      ...
  )


(def ^:private -name->http-header (set/map-invert -http-header->name))


(defn http-header
  "Maps lower-case kebab strings into `io.helidon.common.http.Http$HeaderName` types. First 
   tries to use members of Java enum `io.helidon.common.http.HeaderEnum`. If requested header 
   is not part of `io.helidon.common.http.HeaderEnum`, creates an instance of Http$Header."
  ^Http$HeaderName [header-name]
  (if (instance? Http$HeaderName header-name)
    header-name
    (or (-name->http-header header-name)
        (Http$Header/create header-name))))


(comment
  (http-header "content-type")
  ;; => #object[io.helidon.common.http.HeaderEnum 0x7f4dcc4e "CONTENT_TYPE"]

  (http-header "x-apikey")
  ;; => #object[io.helidon.common.http.HeaderImpl 0x356e8b86 "HeaderImpl[lowerCase=x-apikey, defaultCase=x-apikey]"] 
  )


;;
;; ========================================================================================
;; Request
;; ========================================================================================
;;


; Accepts Nima ServerRequest and ServerResponse objects, returns Ring request map.
; See Ring docs: https://github.com/ring-clojure/ring/wiki/Concepts#requests

(defn server-req->ring-req [^ServerRequest req ^ServerResponse resp]
  (let [prologue    (.prologue req)
        local       (.localPeer req)
        remote-addr (-> (.remotePeer req)
                        ^java.net.InetSocketAddress (.address)
                        (.getAddress)
                        (.getHostAddress))
        query       (.query req)
        path        (.path req)
        content     (.content req)]
    {:server-port     (.port local)               ; The port on which the request is being handled. 
     :server-name     (.host local)               ; The resolved server name, or the server IP address.
     :remote-addr     remote-addr                 ; The IP address of the client or the last proxy that sent the request.
     :uri             (.toString path)            ; The request URI (the full path after the domain name) .
     :query-string    (when-not (.isEmpty query)  ; The query string, if present.
                        (.value query))
     :scheme          (-> prologue                ; The transport protocol, either :http or :https.
                          (.protocol)
                          (str/lower-case)
                          (keyword))
     :request-method  (-> prologue                ; The HTTP request method 
                          (.method)
                          -http-method->kw)
     ; A Clojure map of lowercase header name strings to
     ; corresponding header value strings.
     :headers         (let [headers (transient {})]
                        (-> req
                            (.headers)
                            (.forEach (reify java.util.function.Consumer
                                        (accept [_ header-value]
                                          (assoc! headers
                                                  (-> ^Http$HeaderValue header-value .headerName .lowerCase)
                                                  (-> ^Http$HeaderValue header-value .value))))))
                        (persistent! headers))
     ; An InputStream for the request body, if present.
     :body            (when (.hasEntity content)
                        (.inputStream content))
     ; Extra values specific to this library:
     :nima/parameters {:query (->> (.names query)
                                   (reduce (fn [acc query-name]
                                             (assoc! acc (keyword query-name) (.value query query-name)))
                                           (transient {}))
                                   (persistent!))
                       :path  (let [path-params (.pathParameters path)]
                                (->> (.names path-params)
                                     (reduce (fn [acc param-name]
                                               (assoc! acc (keyword param-name) (.value path-params param-name)))
                                             (transient {}))
                                     (persistent!)))}
     :nima/request    req
     :nima/response   resp}))


;;
;; ========================================================================================
;; Response handling:
;; ========================================================================================
;;


; Sends the Ring response and client using the Nima ServerResponse instance.

(defn send-ring-resp [resp ^ServerResponse server-resp]
  ; Only perform response sending here if SSE or WebSocket etc. has not handled response yet:
  (when-not (:nima/handled resp)
    (.status server-resp (-> resp :status (http-status)))
    (doseq [[header-name header-value] (-> resp :headers)]
      (.header server-resp
               (http-header header-name)
               ^"[Ljava.lang.String;" (into-array String [(str header-value)])))
    (when-let [body (-> resp :body)]
      (let [out (.outputStream server-resp)]
        (io/copy body out)
        (.flush out)))))


;;
;; ========================================================================================
;; Path matcher:
;; ========================================================================================
;;


(defn path-matcher-exact "Exact match path matcher." ^PathMatcher [^String exact]
  (PathMatchers/exact exact))


(defn path-matcher-prefix "Prefix match path matcher." ^PathMatcher [^String prefix]
  (PathMatchers/prefix prefix))


(defn path-matcher-pattern "Pattern match path matcher." ^PathMatcher [^String pattern]
  (PathMatchers/pattern pattern))


(defn path-matcher-any "Path matcher matching any path." ^PathMatcher []
  (PathMatchers/any))


(defn path-matcher
  "Create a path matcher from a path pattern. This method will analyze the pattern 
   and return appropriate path matcher. The following characters mark this as:
     - ends with /* and no other   - prefix match
     - {...}                       - pattern with a named parameter
     - \\*                           - pattern glob
     - \\                           - special characters (regexp)"
  ^PathMatcher [path]
  (if (instance? PathMatcher path)
    path
    (PathMatchers/create path)))


;;
;; ========================================================================================
;; HTTP handlers:
;; ========================================================================================
;;


(defn ->Handler
  "Accepts a Ring handler function and returns a Nima HTTP handler object that implements 
   the `io.helidon.nima.webserver.http/Handler` interface."
  ^Handler [handler]
  (reify Handler
    (handle [_this req resp]
      (-> (server-req->ring-req req resp)
          (handler)
          (send-ring-resp resp)))))


(defn http-handler
  "Coerce argument into Nima HTTP Handler instance."
  ^Handler [handler]
  (if (instance? Handler handler)
    handler
    (->Handler handler)))


(defn http-handlers
  "Helper function that accepts multiple Ring handler functions and returns a Java array
   of Nima HTTP handler objects."
  ^"[Lio.helidon.nima.webserver.http.Handler;" [& handlers]
  (into-array Handler (keep http-handler handlers)))


(defn ->ErrorHandler
  "Accepts a Ring handler function and returns a Nima error handler object that implements
   the `io.helidon.nima.webserver.http/ErrorHandler` interface. The exception that caused
   the error is passed to handler in request map with key `:error`."
  ^ErrorHandler [handler]
  (reify ErrorHandler
    (handle [_ req resp ex]
      (-> (server-req->ring-req req resp)
          (assoc :error ex)
          (handler)
          (send-ring-resp resp)))))


(defn error-handler
  "Coerce argument into Nima HTTP error handler instance."
  ^ErrorHandler [handler]
  (if (instance? ErrorHandler handler)
    handler
    (->ErrorHandler handler)))


;;
;; ========================================================================================
;; HTTP Route:
;; ========================================================================================
;;


(defn http-route
  "Creates a Nima HttpRoute object fron provided path, method and handler. The arguments are:
     `path`       Route path, either a string or instance of PathMatcher
     `method`     HTTP method, a keyword (:get, :post, :put, ...) or Http$Method
     `handler`    Handler, a Ring handler function or HttpHandler instance"
  ^HttpRoute [path method handler]
  (-> (HttpRoute/builder)
      (.path (path-matcher path))
      (.methods (http-methods method))
      (.handler (http-handler handler))
      (.build)))

;;
;; ========================================================================================
;; HTTP Routing:
;; ========================================================================================
;;


(defn- supplier [handler]
  (reify java.util.function.Supplier
    (get [_] handler)))


;; [[:get "/foo" (fn [req])]
;;  [:static-files "/bar" ...]
;;  [:static-resources "/boz" ...]
;;  [:error clojure.lang.ExceptionInfo (fn [req])]]


(defn- build-route [^HttpRouting$Builder builder [method path handler]]
  (cond
    (= method :error)
    (do (assert (and (instance? java.lang.Class path)
                     (.isAssignableFrom Throwable path))
                (str ":error path must be an exception class: path = [" path "] (" (type path) ")"))
        (.error builder ^Throwable path (error-handler handler)))

    (= method :service)
    (do (assert (instance? HttpService handler)
                (str ":service handler must be an instance of HttpServer: handler = [" handler "] (" (type handler) ")"))
        (assert (string? path)
                (str ":service path must be a string: path = [" path "] (" (type path) ")"))
        (.register builder
                   ^String path
                   ^"[Ljava.util.function.Supplier;" (into-array java.util.function.Supplier [(supplier handler)])))

    (= method :middleware)
    ;; TODO: Impelemnt me!
    (do (println "TODO")
        builder)

    (= method :any)
    (.route builder (http-route path nil handler))

    :else
    (.route builder (http-route path method handler))))


(defn add-routes ^HttpRouting$Builder [builder routes]
  (reduce build-route builder routes))
