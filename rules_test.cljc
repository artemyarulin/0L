(ns zerol.rules-test
  (:require [zerol.engine :refer [engine]]
            [zerol.rules :as r]
            [zerol.test :refer [converge]]
            [clojure.test :refer [deftest is are]]))

(deftest move
  (is (= (converge (engine [(r/move [:a :b] [:c :d])]
                           {:a {:b 1}}))
         {:a {} :c {:d 1}})))

(deftest copy
  (is (= (converge (engine [(r/copy [:a :b] [:c :d])]
                           {:a {:b 1}}))
         {:a {:b 1} :c {:d 1}})))
