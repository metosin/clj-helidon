(ns metosin.nima-ring.constants-test
  (:require [clojure.test :refer [deftest testing is]]
            [metosin.nima-ring.constants :as constants]))


(deftest http-method->kw-tests
  (testing "keys of http-method->kw are Http$Method instances"
    (is (->> constants/http-method->kw
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$Method)))))
  (testing "values of http-method->kw are keywords"
    (is (->> constants/http-method->kw
             (vals)
             (every? keyword?)))))


(deftest kw->http-method-tests
  (testing "keys of kw->http-method are keywords"
    (is (->> constants/kw->http-method
             (keys)
             (every? keyword?))))
  (testing "values of kw->http-method are Http$Method instances"
    (is (->> constants/kw->http-method
             (vals)
             (every? (partial instance? io.helidon.common.http.Http$Method))))))
(testing "common method keywords defined in ring docs are present"
  (is (->> [:get :head :options :put :post :delete]
           (every? constants/kw->http-method))))


(deftest http-status->code-test
  (testing "keys of http-status->code are Http$Status instances"
    (is (->> constants/http-status->code
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$Status)))))
  (testing "values of http-status->code are integers"
    (is (->> constants/http-status->code
             (vals)
             (every? integer?)))))


(deftest code->http-status-test
  (testing "keys of code->http-status are integers"
    (is (->> constants/code->http-status
             (keys)
             (every? integer?))))
  (testing "values of code->http-status are Http$Status instances"
    (is (->> constants/code->http-status
             (vals)
             (every? (partial instance? io.helidon.common.http.Http$Status))))))


(deftest http-header->name-test
  (testing "keys of http-header->name are Http$HeaderName instances"
    (is (->> constants/http-header->name
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$HeaderName)))))
  (testing "values of http-status->name are lower-case string"
    (is (->> constants/http-header->name
             (vals)
             (every? (partial re-matches #"[a-z][a-z0-9\-]+"))))))


(deftest name->http-header-test
  (is (= (constants/name->http-header "content-type")
         io.helidon.common.http.Http$Header/CONTENT_TYPE))
  (is (instance? io.helidon.common.http.Http$HeaderName
                 (constants/name->http-header "x-apikey"))))
