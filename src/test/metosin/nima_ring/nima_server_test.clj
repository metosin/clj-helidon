(ns metosin.nima-ring.nima-server-test
  (:require [clojure.test :refer [deftest is]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [metosin.nima-ring.nima-server :as nima]))


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
    (is (match? {:request-method :post
                 :server-port    integer?
                 :server-name    "127.0.0.1"
                 :remote-addr    "127.0.0.1"
                 :scheme         :http
                 :uri            "/foo"
                 :query-string   "a=b&c=d"
                 :headers        {"x-apikey"     "1234"
                                  "content-type" "application/edn"}
                 :body           (pr-str {:foo [1 2 3]})}
                @req))
    (is (match? {:status  200
                 :body    "hello"
                 :headers {"x-resp-id" "xyz"}}
                resp))))

