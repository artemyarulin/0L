(ns zerol.cases)

(defn rule-inc [{:keys [value mode-inc]}]
  (when mode-inc
    {:value (inc value)}))

(defn rule-dec [{:keys [value mode-dec]}]
  (when mode-dec
    {:value (dec value)}))

(deftest one-counter-inc
  (expect {:ui {:counter {:value 10 :mode-click true}}}
          [[:inc [:ui :counter] rule-inc]]
          {:ui {:coutner {:value 11}}}))

(deftest one-counter-inc-dec
  (expect {:ui {:counter {:value 10 :mode-dec true}}}
          [[:inc [:ui :counter] rule-inc] [:dec [:ui :counter] rule-dec]]
          {:ui {:coutner {:value 9}}}))

(deftest many-counters-inc-dec
  (expect {:ui {:counters [{:value 10 :mode-inc true}
                           {:value 10}
                           {:value 10 :mode-dec true}]}}
          [[:counters-inc [:ui :counters] (partial mapv #(or (rule-inc %)
                                                             (rule-dec %)
                                                             %))]]
          {:ui {:counters [{:value 11} {:value 10} {:value 9}]}}))

(deftest counter-inc-async-blocking
  (expect {:ui {:counter {:value 10 :mode-inc true}}}
          [[:loading [:ui :counter] (fn[c] (when (:mode-inc c) (assoc c :mode-loading true)))]
           [:increasing [:ui :counter] (fn[{:keys [value mode-loading]}] (when mode-loading {:type :wait :value 1000 :ret-value (inc value)})) [:io :increasing]]
           [:update [:io :increasing] (fn[{:keys [ret-value result]}] (when (= :done result) {:value ret-value})) [:ui :counter]]]
          {:ui {:counter {:value 11}}}))

(deftest counter-inc-async-blocking-with-title
  (expect {:ui {:counter {:value 10 :mode-inc true :title "1"}}}
          [[:loading [:ui :counter] (fn[c] (when (:mode-inc c) (assoc c :mode-loading true)))]
           [:increasing [:ui :counter] (fn[{:keys [value mode-loading]}] (when mode-loading {:type :wait :value 1000 :ret-value (inc value)})) [:io :increasing]]
           [:update [[:io :increasing] [:ui :counter]] (fn[{:keys [ret-value result]} counter] (when (= :done result) (assoc counter :value ret-value))) [:ui :counter]]]
          {:ui {:counter {:value 11 :title "1"}}}))

(deftest counter-inc-async-blocking-with-title-progress
  (expect {:ui {:counter {:value 10 :mode-inc true :title "1"}}}
          [[:start [:ui :counter] (fn[c] (when (:mode-inc c) (assoc c :progress 0)))]
           [:increasing [:ui :counter] (fn[{:keys [value progress]}] (when progress {:type :wait :value 10000 :progress-step 100 :ret-value (inc value)})) [:io :increasing]]
           [:progress [[:io :increasing] [:ui :counter]] (fn[{:keys [result progress]} counter] (when (= :progress result) (assoc counter :progress progress))) [:ui :counter]]
           [:update [[:io :increasing] [:ui :counter]] (fn[{:keys [ret-value result]} counter] (when (= :done result) (assoc counter :value ret-value))) [:ui :counter]]]
          {:ui {:counter {:value 11 :title "1"}}}
          :timeout 15))

(deftest counter-inc-async-delay
  "Last click always wins"
  (expect {:ui {:counter {:value 14 :last-saved 10 :mode-inc true}}}
          [[:inc [:ui :counter] (fn[c] (when (:mode-inc c) (select-keys c [:value]))) [:ui :counter-io]]
           [:increasing [:ui :counter-io] (fn[{:keys [value]}] (when value {:type :wait :value 1000 :ret-value (inc value)})) [:io :increasing]]
           [:update [[:io :increasing] [:ui :counter]] (fn[{:keys [ret-value result]} counter] (when (= :done result) (assoc counter :value ret-value))) [:ui :counter]]]
          {:ui {:counter {:value 15 :last-saved 15 :mode-click true}}})


  (let [x {:a 1 :b 2}]
    {:a (:a x)})
