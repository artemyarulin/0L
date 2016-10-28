(ns zerol.tests-ui
  (:require [clojure.test :refer [is are deftest]]
            [zerol.zerol :refer [converge]]
            [zerol.rules :as rules]))

(def r-inc #(cond-> % (:inc? %) (-> (dissoc :inc?) (update :value inc))))

(deftest counter-inc-mode
  (is (= {:data {:counter {:value 4}} :ui {:counter {:value 4}}}
         (converge {:inc [[:data :counter] [:data :counter] r-inc]
                    :on-click (rules/move [:ui :counter] [:data :counter] :mode-click? :inc?)
                    :bind (rules/copy [:data :counter] [:ui :counter] :value)}
                   {:data {:counter {:value 1}}}
                   {:click1 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]
                    :click2 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]
                    :click3 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]}))))

(deftest multiple-counters
  (let [init-state {:data {:counters [1 2 3 4]}}
        rule-inc [[:data :counters] [:data :counters] #(mapv r-inc %)]
        rule-click [[[:ui :counters] [:data :counters]] [[:ui :counters] [:data :counters]]
                    (fn[ui-counters data-counters]
                      (if (or (empty? ui-counters) (empty? data-counters))
                        [ui-counters data-counters]
                        (-> (fn[acc [ui-c v]]
                              (let [click? (:mode-click? ui-c)]
                                (-> acc
                                    (update-in [0] conj (cond-> ui-c click? (dissoc :mode-click?)))
                                    (update-in [1] conj (cond-> v click? inc)))))
                            (reduce [[][]] (partition 2 (interleave ui-counters data-counters))))))]
        rule-bind [[:data :counters] [:ui :counters] #(mapv (partial hash-map :value) %)]]
    (is (= {:data {:counters [1 3 4 5]}, :ui {:counters [{:value 1} {:value 3} {:value 4} {:value 5}]}}
           (converge {:on-click rule-click :bind rule-bind :inc rule-inc}
                     init-state
                     {:inc-1 [[:ui :counters] [:ui :counters] #(update-in % [1] assoc :mode-click? true)]
                      :inc-3 [[:ui :counters] [:ui :counters] #(update-in % [3] assoc :mode-click? true)]
                      :inc-2 [[:ui :counters] [:ui :counters] #(update-in % [2] assoc :mode-click? true)]})))))
