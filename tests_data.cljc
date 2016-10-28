(ns zerol.tests-data
  (:require [clojure.test :refer [is are deftest]]
            [zerol.zerol :refer [converge]]))

(def r-inc #(cond-> % (:inc? %) (-> (dissoc :inc?) (update :value inc))))
(def r-dec #(cond-> % (:dec? %) (-> (dissoc :dec?) (update :value dec))))

(deftest counter-inc
  (is (= {:data {:counter {:value 2}}}
         (converge {:inc [[:data :counter] [:data :counter] r-inc]}
                   {:data {:counter {:value 1 :inc? true}}}))))

(deftest counter-inc-dec
  (is (= {:data {:counter {:value 1}}}
         (converge {:inc [[:data :counter] [:data :counter] r-inc]
                    :dec [[:data :counter] [:data :counter] r-dec]}
                   {:data {:counter {:value 2 :dec? true}}}))))

(deftest counters-many-inc-dec
  (is (= {:data {:counters [{:value 2} {:value 2} {:value 2}]}}
         (converge {:counters [[:data :counters] [:data :counters] (partial mapv (comp r-inc r-dec))]}
                   {:data {:counters [{:value 1 :inc? true} {:value 2} {:value 3 :dec? true}]}}))))
