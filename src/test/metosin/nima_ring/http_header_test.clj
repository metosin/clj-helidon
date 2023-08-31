(ns metosin.nima-ring.http-header-test
  (:require [clojure.test :refer [deftest is]]
            [metosin.nima-ring.http-header :refer [http-header-name http-header-value]])
  (:import (io.helidon.common.http Http$Header
                                   Http$HeaderName
                                   Http$HeaderValue)))


(deftest http-header-name-test
  (is (= Http$Header/CONTENT_TYPE
         (http-header-name "content-type")
         (http-header-name Http$Header/CONTENT_TYPE)))
  (is (instance? Http$HeaderName (http-header-name "x-custom-header"))))


(deftest http-header-value-test
  (is (instance? Http$HeaderValue (http-header-value "content-type" "text/event-stream")))
  (is (instance? Http$HeaderValue (http-header-value (http-header-name "content-type") "text/event-stream")))
  (is (= io.helidon.common.http.HeaderValueCached
         (class (http-header-value "content-type" "text/event-stream"))))
  (is (= io.helidon.common.http.HeaderValueSingle
         (class (http-header-value "content-type" "foo/bar")))))
