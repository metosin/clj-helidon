(ns metosin.nima-ring.http-headers-proxy-test
  (:require [clojure.test :refer [deftest is testing]]
            [metosin.nima-ring.http-header :as h]
            [metosin.nima-ring.http-headers-proxy :as proxy])
  (:import (io.helidon.common.http ServerRequestHeaders
                                   WritableHeaders)
           (io.helidon.common.parameters Parameters)))


(defn server-request-headers ^ServerRequestHeaders [headers]
  (-> (reduce-kv (fn [acc k v]
                   (.add acc (h/http-header-value k v)))
                 (WritableHeaders/create)
                 headers)
      (ServerRequestHeaders/create)))


(defn server-request-headers-proxy ^ServerRequestHeaders [headers]
  (-> (server-request-headers headers)
      (proxy/->HeaderMapProxy)))


(deftest server-request-headers-proxy-test
  (testing "proxy is a map"
    (is (instance? java.util.Map (server-request-headers-proxy {})))
    (is (map? (server-request-headers-proxy {}))))
  (let [proxy (server-request-headers-proxy {"foo" "fofo"
                                             "bar" "baba"})]
    (testing "java.util.Map"
      (let [^java.util.Map m proxy]
        (is (= 2 (.size m)))
        (is (= "fofo" (.get m "foo")))
        (is (nil? (.get m "xxx")))
        (is (= "fofo" (.getOrDefault m "foo" :missing)))
        (is (= :missing (.getOrDefault m "xx" :missing)))
        (is (true? (.containsKey m "foo")))
        (is (false? (.containsKey m "xxx")))))
    (testing "PersistentMap"
      (is (= 2 (count proxy)))
      (is (= "fofo" (get proxy "foo")))
      (is (nil? (get proxy "xxx")))
      (is (= :missing (get proxy "xx" :missing)))
      (is (true? (contains? proxy "foo")))
      (is (false? (contains? proxy "xxg")))
      (is (seq? (seq proxy)))
      (is (= {"foo" "fofo"
              "bar" "baba"}
             (into {} (seq proxy)))))
    (testing "IFn"
      (is (ifn? proxy))
      (is (= "fofo" (proxy "foo")))
      (is (nil? (proxy "xxx")))
      (is (= :missing (proxy "xx" :missing))))
    (testing "Object"
      (is (string? (str proxy)))
      (is (identical? proxy proxy))
      (is (= proxy proxy)))))


(defn helidon-parameters ^Parameters [parameters-map]
  (Parameters/create "component-name" (reduce-kv (fn [acc k v]
                                                   (assoc acc k [v]))
                                                 {}
                                                 parameters-map)))


(defn parameters-proxy ^Parameters [parameters-map]
  (-> (helidon-parameters parameters-map)
      (proxy/->ParametersProxy)))


(deftest parameters-proxy-test
  (testing "proxy is a map"
    (is (instance? java.util.Map (parameters-proxy {})))
    (is (map? (parameters-proxy {}))))
  (let [proxy (parameters-proxy {"foo" "fofo"
                                 "bar" "baba"})]
    (testing "java.util.Map"
      (let [^java.util.Map m proxy]
        (is (= 2 (.size m)))
        (is (= "fofo" (.get m :foo)))
        (is (nil? (.get m :xxx)))
        (is (= "fofo" (.getOrDefault m :foo :missing)))
        (is (= :missing (.getOrDefault m :xxx :missing)))
        (is (true? (.containsKey m :foo)))
        (is (false? (.containsKey m :xxx)))))
    (testing "PersistentMap"
      (is (= 2 (count proxy)))
      (is (= "fofo" (get proxy :foo)))
      (is (nil? (get proxy :xxx)))
      (is (= :missing (get proxy :xxx :missing)))
      (is (true? (contains? proxy :foo)))
      (is (false? (contains? proxy :xxx)))
      (is (seq? (seq proxy)))
      (is (= {:foo "fofo"
              :bar "baba"}
             (into {} (seq proxy)))))
    (testing "IFn"
      (is (ifn? proxy))
      (is (= "fofo" (proxy :foo)))
      (is (= "fofo" (:foo proxy)))
      (is (nil? (proxy :xxx)))
      (is (nil? (:xxx proxy)))
      (is (= :missing (proxy :xxx :missing)))
      (is (= :missing (:xxx proxy :missing))))
    (testing "Object"
      (is (string? (str proxy)))
      (is (identical? proxy proxy))
      (is (= proxy proxy)))))


(comment

  metosin.nima_ring.http_headers_proxy.HeaderMapProxy

  (= (server-request-headers {"foo" "fofo"
                              "bar" "baba"})
     (server-request-headers {"foo" "fofo"
                              "bar" "baba"}))

  (let [this  (server-request-headers-proxy {"foo" "fofo"
                                             "bar" "baba"})
        other (server-request-headers-proxy {"foo" "fofo"
                                             "bar" "baba"})]

    (or (identical? this other)
        (and (instance? metosin.nima_ring.http_headers_proxy.HeaderMapProxy other)
             (.equals (.-headers ^metosin.nima_ring.http_headers_proxy.HeaderMapProxy this) (.-headers ^metosin.nima_ring.http_headers_proxy.HeaderMapProxy other)))
        (and (instance? ServerRequestHeaders other)
             (.equals (.-headers ^metosin.nima_ring.http_headers_proxy.HeaderMapProxy this) other)))))

