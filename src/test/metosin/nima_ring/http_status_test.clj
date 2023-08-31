(ns metosin.nima-ring.http-status-test
  (:require [clojure.test :refer [deftest is]]
            [metosin.nima-ring.http-status :refer [http-status]]))


(deftest http-status-test
  (is (every? (partial instance? io.helidon.common.http.Http$Status)
              (map http-status [200 418
                                io.helidon.common.http.Http$Status/OK_200
                                io.helidon.common.http.Http$Status/I_AM_A_TEAPOT_418])))
  (is (= io.helidon.common.http.Http$Status/OK_200
         (http-status 200)
         (http-status io.helidon.common.http.Http$Status/OK_200))))
