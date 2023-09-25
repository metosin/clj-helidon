(ns metosin.nima-ring.http-header-test
  (:require [clojure.test :refer [deftest is]]
            [metosin.nima-ring.http-header :refer [http-header-name http-header-value]])
  (:import (io.helidon.http Http$Header
                            Http$HeaderName
                            Http$HeaderNames)))


(deftest http-header-name-test
  (is (= Http$HeaderNames/CONTENT_TYPE
         (http-header-name "content-type")
         (http-header-name Http$HeaderNames/CONTENT_TYPE)))
  (is (instance? Http$HeaderName (http-header-name "x-custom-header"))))


(deftest http-header-value-test
  (is (instance? Http$Header (http-header-value "content-type" "text/event-stream")))
  (is (instance? Http$Header (http-header-value (http-header-name "content-type") "text/event-stream")))
  (is (= io.helidon.http.HeaderValueCached
         (class (http-header-value "content-type" "text/event-stream"))))
  (is (= io.helidon.http.HeaderValueSingle
         (class (http-header-value "content-type" "foo/bar")))))
