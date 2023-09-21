(ns metosin.nima-ring.media-support-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn]
            [cognitect.transit]
            [matcher-combinators.test]
            [clj-http.client :as client]
            [jsonista.core :as jsonista]
            [metosin.nima-ring.server :as nima]
            [metosin.nima-ring.media-support :as media-support]
            [metosin.nima-ring.default-media-support :as default-media-support]
            [metosin.nima-ring.http-header :refer [http-header-value]]
            [metosin.nima-ring.media-type :refer [media-type]]
            [metosin.nima-ring.media-support.transit :as transit]
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



(deftest transit-media-support-test
  (let [data          {:foo 42}
        handler       (fn [{:keys [body]}]
                        {:status 200
                         :body   body})
        media-context (media-support/media-context (transit/transit-media-support))]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-context media-context})]
      (is (match? {:status  200
                   :headers {"Content-Type" "application/transit+json; charset=UTF-8"}
                   :body    (fn [^String body]
                              (-> (.getBytes body java.nio.charset.StandardCharsets/UTF_8)
                                  (java.io.ByteArrayInputStream.)
                                  (cognitect.transit/reader :json)
                                  (cognitect.transit/read)
                                  (= data)))}
                  (client/request {:method           :post
                                   :url              (str "http://localhost:" (nima/port server) "/foo")
                                   :headers          {"Accept"       transit/application-transit+json
                                                      "Content-Type" transit/application-transit+json}
                                   :body             (let [out (java.io.ByteArrayOutputStream.)]
                                                       (-> (cognitect.transit/writer out :json)
                                                           (cognitect.transit/write data))
                                                       (String. (.toByteArray out)))
                                   :throw-exceptions false}))))))


(deftest edn-media-support-test
  (let [data          {:foo 42}
        handler       (fn [{:keys [body]}]
                        {:status 200
                         :body   body})
        media-context (media-support/media-context (edn/edn-media-support))]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-context media-context})]
      (is (match? {:status  200
                   :headers {"Content-Type" "application/edn; charset=UTF-8"}
                   :body    (fn [body] (= (clojure.edn/read-string body) data))}
                  (client/request {:method           :post
                                   :url              (str "http://localhost:" (nima/port server) "/foo")
                                   :headers          {"Accept"       edn/application-edn
                                                      "Content-Type" edn/application-edn}
                                   :body             (pr-str data)
                                   :throw-exceptions false}))))))


(deftest json-media-support-test
  (let [data    {:foo 42}
        handler (fn [{:keys [body]}]
                  {:status 200
                   :body   body})]
    (with-open [server (nima/create-server [[:post "/foo" handler]]
                                           {:media-supports [(json/json-media-support)]})]
      (is (match? {:status  200
                   :headers {"Content-Type" "application/json; charset=UTF-8"}
                   :body    (fn [^String body]
                              (= (jsonista/read-value body jsonista/keyword-keys-object-mapper) data))}
                  (client/request {:method           :post
                                   :url              (str "http://localhost:" (nima/port server) "/foo")
                                   :headers          {"Accept"       json/application-json
                                                      "Content-Type" json/application-json}
                                   :body             (jsonista/write-value-as-string data)
                                   :throw-exceptions false}))))))