(ns metosin.nima-ring.impl-test
  (:require [clojure.test :refer [deftest testing is]]
            [metosin.nima-ring.impl :as impl]))


(deftest http-method->kw-tests
  (testing "keys of http-method->kw are Http$Method instances"
    (is (->> #'impl/-http-method->kw
             (deref)
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$Method)))))
  (testing "values of http-method->kw are keywords"
    (is (->> #'impl/-http-method->kw
             (deref)
             (vals)
             (every? keyword?)))))


(deftest -kw->http-method-tests
  (testing "keys of kw->http-method are keywords"
    (is (->> #'impl/-kw->http-method
             (deref)
             (keys)
             (every? keyword?))))
  (testing "values of kw->http-method are Http$Method instances"
    (is (->> #'impl/-kw->http-method
             (deref)
             (vals)
             (every? (partial instance? io.helidon.common.http.Http$Method)))))

  (testing "common method keywords defined in ring docs are present"
    (is (->> [:get :head :options :put :post :delete]
             (every? (deref #'impl/-kw->http-method))))))


(deftest kw->http-method-tests
  (is (= (impl/->HttpMethod :get)
         io.helidon.common.http.Http$Method/GET)))


(deftest http-status->code-test
  (testing "keys of http-status->code are Http$Status instances"
    (is (->> #'impl/-http-status->code
             (deref)
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$Status)))))
  (testing "values of http-status->code are integers"
    (is (->> #'impl/-http-status->code
             (deref)
             (vals)
             (every? integer?)))))


(deftest -code->http-status-test
  (testing "keys of -code->http-status are integers"
    (is (->> (deref #'impl/-code->http-status)
             (keys)
             (every? integer?))))
  (testing "values of -code->http-status are Http$Status instances"
    (is (->> (deref #'impl/-code->http-status)
             (vals)
             (every? (partial instance? io.helidon.common.http.Http$Status))))))


(deftest http-header->name-test
  (testing "keys of http-header->name are Http$HeaderName instances"
    (is (->> (deref #'impl/-http-header->name)
             (keys)
             (every? (partial instance? io.helidon.common.http.Http$HeaderName)))))
  (testing "values of http-status->name are lower-case string"
    (is (->> (deref #'impl/-http-header->name)
             (vals)
             (every? (partial re-matches #"[a-z][a-z0-9\-]+"))))))


(deftest name->http-header-test
  (is (= (impl/http-header "content-type")
         io.helidon.common.http.Http$Header/CONTENT_TYPE))
  (is (instance? io.helidon.common.http.Http$HeaderName
                 (impl/http-header "x-apikey"))))
