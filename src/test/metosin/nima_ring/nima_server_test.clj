(ns metosin.nima-ring.nima-server-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [metosin.nima-ring.server :as nima])
  (:import (java.io ByteArrayInputStream)
           (java.nio.charset StandardCharsets)))


(deftest server-started-with-defaults
  (let [handler (fn [_req] {:status  200
                            :headers {"content-type" "text-plain"}
                            :body    "hello"})]
    (with-open [server (nima/create-server handler)]
      (is (instance? io.helidon.webserver.WebServer (nima/server server)))
      (is (pos? (nima/port server)))
      (is (true? (nima/running? server)))
      (nima/shutdown server)
      (is (false? (nima/running? server))))))


(deftest server-request-is-correct
  (let [req     (atom nil)
        handler (fn [req']
                  (reset! req req')
                  {:status  200
                   :headers {"content-type" "text-plain"
                             "x-resp-id"    "xyz"}
                   :body    "server response data"})]
    (with-open [server (nima/create-server handler)]
      (let [resp (client/post (str "http://localhost:" (nima/port server) "/foo")
                              {:query-params {:a "b"
                                              :c "d"}
                               :headers      {"x-apikey"     "1234"
                                              "content-type" "text/plain"}
                               :body         "client post data"})]
        (testing "server response is correct"
          (is (match? {:status  200
                       :headers {"x-resp-id"    "xyz"
                                 "Content-Type" "text-plain"}
                       :body    "server response data"}
                      resp)))))
    (testing "server request was correct"
      (is (match? {:request-method  :post
                   :server-port     integer?
                   :server-name     "127.0.0.1"
                   :remote-addr     "127.0.0.1"
                   :scheme          :http
                   :uri             "/foo"
                   :query-string    "a=b&c=d"
                   :headers         {"x-apikey"     "1234"
                                     "content-type" "text/plain"}
                   :body            "client post data"
                   :nima/parameters {:query {:a "b"
                                             :c "d"}}}
                  @req)))))


(defn run-response-type-test [body]
  (with-open [server (nima/create-server (fn [_]
                                           {:status  200
                                            :headers {"content-type" "text-plain; charset=UTF-8"}
                                            :body    body}))]
    (client/get (str "http://localhost:" (nima/port server) "/"))))


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
        routing [[:get "/foo" (handler "get /foo")]
                 [:post "/foo" (handler "post /foo")]
                 [:get "/foo/*" (handler "get /foo/*")]
                 [:any "/any" (handler "any /any")]
                 [:error clojure.lang.ExceptionInfo (fn [req]
                                                      {:status 400
                                                       :body   (let [ex (:error req)]
                                                                 (str (ex-message ex)
                                                                      ": "
                                                                      (pr-str (ex-data ex))))})]
                 [:post "/boom" (fn [_] (throw (ex-info "Oh no" {:message "Boom!"})))]]]
    (with-open [server (nima/create-server routing)]
      (let [request (fn [method path]
                      (client/request {:method           method
                                       :url              (str "http://localhost:" (nima/port server) path)
                                       :throw-exceptions false}))]
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
                    (request :post "/boom")))))))


(deftest route-params-test
  (let [handler (fn [req]
                  {:status  200
                   :headers {"content-type" "text-plain; charset=UTF-8"}
                   :body    (str "x=[" (-> req :nima/parameters :path :x) "] y=[" (-> req :nima/parameters :path :y) "]")})
        routes  [[:get "/foo/{x}/{y}" handler]]
        path    "/foo/xx/yy"]
    (with-open [server (nima/create-server routes)]
      (is (match? {:status 200
                   :body   "x=[xx] y=[yy]"}
                  (client/request {:method           :get
                                   :url              (str "http://localhost:" (nima/port server) path)
                                   :throw-exceptions false}))))))

(deftest file-and-stream-response-types-test
  (let [routes   [[:get "/bytes" (constantly {:status 200
                                              :body   (byte-array (map int "Hello"))})]
                  [:get "/stream" (constantly {:status 200
                                               :body   (java.io.ByteArrayInputStream. (byte-array (map int "Hello")))})]
                  [:get "/file" (constantly {:status 200
                                             :body   (java.io.File. "src/test/public/hello.txt")})]
                  [:get "/path" (constantly {:status 200
                                             :body   (.toPath (java.io.File. "src/test/public/hello.txt"))})]]
        expected {:status 200
                  :body   "Hello"}]
    (with-open [server (nima/create-server routes)]
      (let [GET (fn [path]
                  (client/request {:method           :get
                                   :url              (str "http://localhost:" (nima/port server) path)
                                   :throw-exceptions false}))]
        (is (match? expected (GET "/bytes")))
        (is (match? expected (GET "/stream")))
        (is (match? expected (GET "/file")))
        (is (match? expected (GET "/path")))))))

