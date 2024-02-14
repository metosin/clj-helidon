(ns metosin.clj-helidon.http-method-test
  (:require [clojure.test :refer [deftest is]]
            [metosin.clj-helidon.http-method :refer [http-method]]))


(deftest http-method-test
  (is (every? (partial instance? io.helidon.http.Method)
              (map http-method [:get :post :put :delete :patch :head :options :trace :connect])))
  (is (= io.helidon.http.Method/GET
         (http-method :get)))
  (is (= io.helidon.http.Method/GET
         (http-method io.helidon.http.Method/GET))))
