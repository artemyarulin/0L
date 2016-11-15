(ns zerol.test-test
  (:require [zerol.engine :refer [engine]]
            [zerol.test :as t]
            [clojure.test :refer [deftest are is]]))

(deftest converge
  (is (= (t/converge (engine [[[:a] [:b] inc]]
                             {:a 1}))
         {:a 1 :b 2}))
  (is (= (t/converge (engine [[[:a] [:b] inc]]
                             {:a 1})
                     [[:a] [:a] (constantly 10)])
         {:a 10 :b 11})))
