(ns metosin.nima-ring.nima-server-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [metosin.nima-ring.nima-server :as nima])
  (:import (java.io ByteArrayInputStream)
           (java.nio.charset StandardCharsets)))


(deftest server-started-with-defaults
  (let [handler (fn [_req] {:status  200
                            :headers {"content-type" "text-plain"}
                            :body    "hello"})
        server  (nima/nima-server handler)]
    (is (match? {:server   (partial instance? io.helidon.nima.webserver.WebServer)
                 :port     integer?
                 :stop     fn?
                 :running? fn?}
                server))
    (is ((:running? server)))
    ((:stop server))
    (is (false? ((:running? server))))))


(deftest server-request-is-correct
  (let [req     (atom nil)
        handler (fn [req']
                  (reset! req (update req' :body slurp))
                  {:status  200
                   :headers {"content-type" "text-plain"
                             "x-resp-id"    "xyz"}
                   :body    "hello"})
        server  (nima/nima-server handler)
        resp    (client/post (str "http://localhost:" (:port server) "/foo")
                             {:query-params {:a "b"
                                             :c "d"}
                              :headers      {"x-apikey"     "1234"
                                             "content-type" "application/edn"}
                              :body         (pr-str {:foo [1 2 3]})})]
    ((:stop server))
    (testing "server request is correct"
      (is (match? {:request-method  :post
                   :server-port     integer?
                   :server-name     "127.0.0.1"
                   :remote-addr     "127.0.0.1"
                   :scheme          :http
                   :uri             "/foo"
                   :query-string    "a=b&c=d"
                   :headers         {"x-apikey"     "1234"
                                     "content-type" "application/edn"}
                   :body            (pr-str {:foo [1 2 3]})
                   :nima/parameters {:query {:a "b"
                                             :c "d"}}}
                  @req)))
    (testing "server response is correct"
      (is (match? {:status  200
                   :body    "hello"
                   :headers {"x-resp-id" "xyz"}}
                  resp)))))


(defn run-response-type-test [body]
  (let [handler (fn [_]
                  {:status  200
                   :headers {"content-type" "text-plain; charset=UTF-8"}
                   :body    body})
        server  (nima/nima-server handler)
        resp    (client/get (str "http://localhost:" (:port server) "/"))]
    ((:stop server))
    resp))


(deftest response-types-test
  (testing "string body"
    (let [body "hello"
          resp (run-response-type-test body)]
      (is (match? {:status  200
                   :body    body
                   :headers {"content-type" "text-plain; charset=UTF-8"}}
                  resp))))
  (testing "string byte[]"
    (let [body          "hello"
          body-as-array (.getBytes body StandardCharsets/UTF_8)
          resp          (run-response-type-test body-as-array)]
      (is (match? {:status  200
                   :body    body
                   :headers {"content-type" "text-plain; charset=UTF-8"}}
                  resp))))
  (testing "string InputStream"
    (let [body           "hello"
          body-as-stream (ByteArrayInputStream. (.getBytes body StandardCharsets/UTF_8))
          resp           (run-response-type-test body-as-stream)]
      (is (match? {:status  200
                   :body    body
                   :headers {"content-type" "text-plain; charset=UTF-8"}}
                  resp)))))


(deftest routing-test
  (let [handler (fn [body]
                  (fn [_]
                    {:status  200
                     :headers {"content-type" "text-plain; charset=UTF-8"}
                     :body    body}))
        server  (nima/server [[:get "/foo" (handler "get /foo")]
                              [:post "/foo" (handler "post /foo")]
                              [:get "/foo/*" (handler "get /foo/*")]
                              [:any "/any" (handler "any /any")]
                              [:error clojure.lang.ExceptionInfo (fn [req]
                                                                   {:status 400
                                                                    :body   (let [ex (:error req)]
                                                                              (str (ex-message ex)
                                                                                   ": "
                                                                                   (pr-str (ex-data ex))))})]
                              [:post "/boom" (fn [_] (throw (ex-info "Oh no" {:message "Boom!"})))]])
        request (fn [method path]
                  (client/request {:method           method
                                   :url              (str "http://localhost:" (:port server) path)
                                   :throw-exceptions false}))]
    (try
      (is (match? {:status 200
                   :body   "get /foo"}
                  (request :get "/foo")))
      (is (match? {:status 200
                   :body   "post /foo"}
                  (request :post "/foo")))
      (is (match? {:status 200
                   :body   "get /foo/*"}
                  (request :get "/foo/bar/boz")))
      (is (match? {:status 200
                   :body   "any /any"}
                  (request :delete "/any")))
      (is (match? {:status 400
                   :body   "Oh no: {:message \"Boom!\"}"}
                  (request :post "/boom")))
      (finally
        (try
          ((:stop server))
          (catch Exception _))))))


(deftest route-params-test
  (let [handler (fn [req]
                  {:status  200
                   :headers {"content-type" "text-plain; charset=UTF-8"}
                   :body    (str (-> req :nima/parameters :path :x) ":" (-> req :nima/parameters :path :y))})
        server  (nima/server [[:get "/foo/{x}/{y}" handler]])
        request (fn [method path]
                  (client/request {:method           method
                                   :url              (str "http://localhost:" (:port server) path)
                                   :throw-exceptions false}))]
    (try
      (is (match? {:status 200
                   :body   "xx:yy"}
                  (request :get "/foo/xx/yy")))
      (finally
        (try
          ((:stop server))
          (catch Exception _))))))
