(ns metosin.nima-ring.static-content-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [metosin.nima-ring.nima-server :as nima]
            [metosin.nima-ring.static-content :as sc]))


(deftest static-files-test
  (let [handler (fn [_]
                  {:status 200
                   :body   "hello"})
        server  (nima/server [[:get "/foo" handler]
                              [:service "/" (sc/static-files-service "src/test/public" {:index "index.html"})]])
        request (fn request
                  ([method path] (request method path nil))
                  ([method path headers]
                   (client/request {:method           method
                                    :url              (str "http://localhost:" (:port server) path)
                                    :headers          headers
                                    :throw-exceptions false})))]
    (try
      (is (match? {:status 200
                   :body   "hello"}
                  (request :get "/foo")))
      (is (match? {:status  200
                   :body    "<h1>index</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/index.html")))
      (is (match? {:status  200
                   :body    "<h1>index</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/")))
      (is (match? {:status  200
                   :body    "<h1>Hello</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/hello.html")))
      (is (match? {:status  200
                   :body    "// JS\n"
                   :headers {"Content-Type" "application/javascript"
                             "ETag"         string?}}
                  (request :get "/js/foo.js")))
      (let [etag (-> (request :get "/")
                     :headers
                     (get "ETag"))]
        (is (match? {:status 304}
                    (request :get "/" {"if-none-match" etag}))))
      (finally
        (try
          ((:stop server))
          (catch Exception _))))))


(deftest static-resources-test
  (let [handler (fn [_]
                  {:status 200
                   :body   "hello"})
        server  (nima/server [[:get "/foo" handler]
                              [:service "/" (sc/static-resources-service "public" {:index "index.html"})]])
        request (fn request
                  ([method path] (request method path nil))
                  ([method path headers]
                   (client/request {:method           method
                                    :url              (str "http://localhost:" (:port server) path)
                                    :headers          headers
                                    :throw-exceptions false})))]
    (try
      (is (match? {:status 200
                   :body   "hello"}
                  (request :get "/foo")))
      (is (match? {:status  200
                   :body    "<h1>index</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/index.html")))
      (is (match? {:status  200
                   :body    "<h1>index</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/")))
      (is (match? {:status  200
                   :body    "<h1>Hello</h1>"
                   :headers {"Content-Type" "text/html"
                             "ETag"         string?}}
                  (request :get "/hello.html")))
      (is (match? {:status  200
                   :body    "// JS\n"
                   :headers {"Content-Type" "application/javascript"
                             "ETag"         string?}}
                  (request :get "/js/foo.js")))
      (let [etag (-> (request :get "/")
                     :headers
                     (get "ETag"))]
        (is (match? {:status 304}
                    (request :get "/" {"if-none-match" etag}))))
      (finally
        (try
          ((:stop server))
          (catch Exception _))))))
