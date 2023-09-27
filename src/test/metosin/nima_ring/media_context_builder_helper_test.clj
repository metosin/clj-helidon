(ns metosin.nima-ring.media-context-builder-helper-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [metosin.nima-ring.media-support.json :as json]
            [metosin.nima-ring.media-support.transit :as transit]
            [metosin.nima-ring.media-support.edn :as edn]
            [metosin.nima-ring.media-context-builder-helper :as mcbh])
  (:import (io.helidon.http.media MediaSupport MediaContext)))


(def coerce-media-context #'metosin.nima-ring.media-context-builder-helper/coerce-media-context)


(deftest coerce-media-context-test
  (testing "MediaContext is used if provided"
    (let [media-context (-> (MediaContext/builder)
                            (.build))]
      (is (identical? media-context (coerce-media-context media-context nil)))))
  (testing "MediaContext, if provided, must be instance of MediaContext"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"wrong type, was expecting io.helidon.http.media.MediaContext, got java.lang.String"
                          (coerce-media-context "foo" nil)))))


(def coerce-media-supports #'metosin.nima-ring.media-context-builder-helper/coerce-media-supports)

(def json-support (json/json-media-support))
(def edn-support (edn/edn-media-support))
(def transit-support (transit/transit-media-support))


(deftest coerce-media-support-test
  (testing "MediaSupport instance is user if provided"
    (is (= [json-support]
           (coerce-media-supports json-support))))
  (testing "MediaSupport instance is user if provided in sequence"
    (is (= [json-support]
           (coerce-media-supports [json-support]))))
  (testing "MediaSupport instances are user if provided"
    (is (= [json-support edn-support]
           (coerce-media-supports [json-support edn-support]))))
  (testing "Common media-supports via keywords"
    (is (= [(.name json-support)]
           (mapv #(.name %) (coerce-media-supports [:json]))))
    (is (= [(.name json-support)
            (.name edn-support)]
           (mapv #(.name %) (coerce-media-supports [:json :edn]))))
    (is (= [(.name edn-support)
            (.name json-support)]
           (mapv #(.name %) (coerce-media-supports [:edn :json])))))
  (testing ":api = [:json :transit :edn]"
    (is (= [(.name json-support)
            (.name transit-support)
            (.name edn-support)]
           (mapv #(.name %) (coerce-media-supports [:api])))))
  (testing ":text is text/*"
    (is (= ["text/*"]
           (mapv #(.name %) (coerce-media-supports [:text])))))
  (testing ":io = [:json :transit :edn]"
    (is (= ["application/octet-stream"]
           (mapv #(.name %) (coerce-media-supports [:io])))))
  (testing "default is [:api :text :io]"
    (is (= [(.name json-support)
            (.name transit-support)
            (.name edn-support)
            "text/*"
            "application/octet-stream"]
           (mapv #(.name %) (coerce-media-supports nil))))))

