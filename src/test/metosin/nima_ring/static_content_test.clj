(ns metosin.nima-ring.static-content-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [metosin.nima-ring.server :as nima]
            [metosin.nima-ring.static-content :as sc]))


(defn request
  ([server method path] (request server method path nil))
  ([server method path headers]
   (client/request {:method           method
                    :url              (str "http://localhost:" (nima/port server) path)
                    :headers          headers
                    :throw-exceptions false})))


(deftest static-files-test
  (with-open [server (nima/create-server [[:get "/foo" (constantly {:status 200
                                                                    :body   "hello"})]
                                          [:service "/" (sc/static-files-service "src/test/public" {:index "index.html"})]])]
    (testing "/foo handler is still working"
      (is (match? {:status 200
                   :body   "hello"}
                  (request server :get "/foo"))))
    (testing "/index.html is served"
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/index.html"))))
    (testing "index.html is configured as \"wellcome file\""
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/"))))
    (testing "/hello.html is served with Content-Type and ETag headers"
      (is (match? {:status  200
                   :body    "<h1>Hello</h1>"
                   :headers {"Content-Type" "text/html; charset=UTF-8"
                             "ETag"         string?}}
                  (request server :get "/hello.html"))))
    (testing "/js/foo.js is served correctly"
      (is (match? {:status  200
                   :body    "// JS\n"
                   :headers {"Content-Type" "application/javascript; charset=UTF-8"
                             "ETag"         string?}}
                  (request server :get "/js/foo.js"))))
    (testing "Sending If-None-Match with proper ETag is served with status 304"
      (let [etag (-> (request server :get "/")
                     :headers
                     (get "ETag"))]
        (is (match? {:status 304}
                    (request server :get "/" {"if-none-match" etag})))))
    (testing "/foo/bar gets status 404"
      (is (match? {:status 404}
                  (request server :get "/foo/bar"))))))


(deftest static-resources-test
  (with-open [server  (nima/create-server [[:get "/foo" (constantly
                                                         {:status 200
                                                          :body   "hello"})]
                                           [:service "/" (sc/static-resources-service "public" {:index "index.html"})]])]

    (testing "/foo handler is still working"
      (is (match? {:status 200
                   :body   "hello"}
                  (request server :get "/foo"))))
    (testing "/index.html is served"
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/index.html"))))
    (testing "index.html is configured as \"wellcome file\""
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/"))))
    (testing "/hello.html is served with Content-Type and ETag headers"
      (is (match? {:status  200
                   :body    "<h1>Hello</h1>"
                   :headers {"Content-Type" "text/html; charset=UTF-8"
                             "ETag"         string?}}
                  (request server :get "/hello.html"))))
    (testing "/js/foo.js is served correctly"
      (is (match? {:status  200
                   :body    "// JS\n"
                   :headers {"Content-Type" "application/javascript; charset=UTF-8"
                             "ETag"         string?}}
                  (request server :get "/js/foo.js"))))
    (testing "Sending If-None-Match with proper ETag is served with status 304"
      (let [etag (-> (request server :get "/")
                     :headers
                     (get "ETag"))]
        (is (match? {:status 304}
                    (request server :get "/" {"if-none-match" etag})))))
    (testing "/foo/bar gets status 404"
      (is (match? {:status 404}
                  (request server :get "/foo/bar"))))))


(deftest html5-history-path-mapper-test
  (with-open [server (nima/create-server [[:service "/" (sc/static-resources-service "public" {:path-mapper :html5})]])]
    (testing "/js/foo.js is served correctly"
      (is (match? {:status  200
                   :body    "// JS\n"
                   :headers {"Content-Type" "application/javascript; charset=UTF-8"
                             "ETag"         string?}}
                  (request server :get "/js/foo.js"))))
    (testing "/ is served by index"
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/"))))
    (testing "/foo is served by index"
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/foo"))))
    (testing "/foo/bar is served by index"
      (is (match? {:status 200
                   :body   "<h1>index</h1>"}
                  (request server :get "/foo/bar"))))))