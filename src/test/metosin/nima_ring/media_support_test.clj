(ns metosin.nima-ring.media-support-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [jsonista.core :as jsonista]
            [metosin.nima-ring.server :as nima]
            [metosin.nima-ring.media-support :as media-support]
            [metosin.nima-ring.default-media-support :as default-media-support]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support.edn :as edn]
            [metosin.nima-ring.media-support.json :as json])
  (:import (io.helidon.common.http WritableHeaders)))


(deftest simple-media-type-test
  (let [data          {:foo 42}
        content-type  (media-type "application/foo")
        reader        (fn [_ in _]
                        (jsonista/read-value in jsonista/keyword-keys-object-mapper))
        writer        (fn [_ body out _ response-headers]
                        (.contentType ^WritableHeaders response-headers content-type)
                        (jsonista/write-value out body)
                        nil)
        media-support (media-support/simple-media-support content-type reader writer)
        media-context (media-support/media-context media-support)]
    (testing "reading"
      (let [data2 (let [request-headers  (doto (WritableHeaders/create)
                                           (.set (http-header-value "content-type" content-type)))]
                    (-> (.reader media-context
                                 nil
                                 request-headers)
                        (.read nil
                               (java.io.ByteArrayInputStream. (jsonista/write-value-as-bytes data))
                               request-headers)))]
        (is (= data data2))))
    (testing "writing"
      (let [request-headers  (doto (WritableHeaders/create)
                               (.set (http-header-value "accept" content-type)))
            response-headers (WritableHeaders/create)
            out              (java.io.ByteArrayOutputStream.)]
        (-> (.writer media-context
                     nil
                     request-headers
                     response-headers)
            (.write nil
                    data
                    out
                    (doto (WritableHeaders/create)
                      (.set (http-header-value "accept" content-type)))
                    response-headers))
        (let [data3 (jsonista/read-value (.toByteArray out) jsonista/keyword-keys-object-mapper)]
          (is (= data data3))
          (is (= (.text content-type)
                 (some-> (.contentType response-headers)
                         (.orElse nil)
                         (.text)))))))))


(deftest simple-media-type-with-server-test
  (let [data          {:foo 42}
        content-type  "application/foo"
        reader        (fn [_ in _]
                        (jsonista/read-value in jsonista/keyword-keys-object-mapper))
        writer        (fn [_ body out _ response-headers]
                        (.contentType ^WritableHeaders response-headers (media-type content-type))
                        (jsonista/write-value out body)
                        (.flush out)
                        nil)
        media-support (media-support/simple-media-support content-type reader writer)
        media-context (media-support/media-context media-support)
        handler       (fn [req]
                        {:status  200
                         :headers {"content-type" content-type}
                         :body    {:body         (:body req)
                                   :content-type (get-in req [:headers "content-type"])}})]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-context media-context})]
      (testing "body is writen correctly"
        (is (match? {:status  200
                     :headers {"Content-Type" content-type}
                     :body    {:body         data
                               :content-type content-type}}
                    (-> (client/request {:method           :post
                                         :url              (str "http://localhost:" (nima/port server) "/foo")
                                         :headers          {"content-type" content-type
                                                            "accept"       content-type}
                                         :body             (jsonista/write-value-as-bytes data)
                                         :throw-exceptions false})
                        (update :body #(jsonista/read-value % jsonista/keyword-keys-object-mapper)))))))))


(deftest default-media-support-test
  (let [handler       (fn [{:keys [body]}]
                        {:status 200
                         :body   (str "request body: " (pr-str (slurp body)))})
        media-context (media-support/media-context (default-media-support/default-media-support))]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-context media-context})]
      (testing "body is writen correctly"
        (is (match? {:status 200
                     :body   "request body: \"foo\""}
                    (client/request {:method           :post
                                     :url              (str "http://localhost:" (nima/port server) "/foo")
                                     :body             "foo"
                                     :throw-exceptions false})))))))


(deftest edn-media-support-test
  (let [handler       (fn [{:keys [body]}]
                        {:status 200
                         :body   {:body body}})
        media-context (media-support/media-context (edn/edn-media-support))]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-context media-context})]
      (is (match? {:status  200
                   :headers {"Content-Type" "application/edn; charset=UTF-8"}
                   :body    "{:body {:foo 42}}"}
                  (client/request {:method           :post
                                   :url              (str "http://localhost:" (nima/port server) "/foo")
                                   :headers          {"Accept"       "application/edn"
                                                      "Content-Type" "application/edn"}
                                   :body             (pr-str {:foo 42})
                                   :throw-exceptions false}))))))


(deftest json-media-support-test
  (let [handler       (fn [{:keys [body]}]
                        {:status 200
                         :body   {:foo (:foo body)}})]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-supports [(json/json-media-support)]})]
      (is (match? {:status  200
                   :headers {"Content-Type" "application/json; charset=UTF-8"}
                   :body    "{\"foo\":42}"}
                  (client/request {:method           :post
                                   :url              (str "http://localhost:" (nima/port server) "/foo")
                                   :headers          {"Accept"       "application/json"
                                                      "Content-Type" "application/json"}
                                   :body             (jsonista/write-value-as-bytes {:foo 42})
                                   :throw-exceptions false}))))))