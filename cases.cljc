(ns zerol.cases)

(defn rule-inc [{:keys [value mode-inc]}]
  (when mode-inc
    {:value (inc value)}))

(defn rule-dec [{:keys [value mode-dec]}]
  (when mode-dec
    {:value (dec value)}))

(deftest one-counter-inc)

(deftest one-counter-inc-dec)

(deftest many-counters-inc-dec)

(deftest counter-inc-async-blocking)

(deftest counter-inc-async-blocking-with-title)

(deftest counter-inc-async-blocking-with-title-progress)

(deftest counter-inc-async-delay)
