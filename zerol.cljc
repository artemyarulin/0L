(ns zerol.zerol
  "For now we put everything here for simplicity"
  (:require [clojure.test :refer [is]]))

(defn run [rules state]
  (loop [rules rules state state]
    (let [rule (first rules)]
      (if-not rule
        state
        (let [[in-query out-query f] rule
              in-data (get-in state in-query)
              cur-data (get-in state out-query)
              new-data (f in-data)]
          (if (not= new-data cur-data)
            (recur rules (assoc-in state out-query new-data))
            (recur (rest rules) state)))))))

(defn converge [rules init-state exp-state]
  (is (= exp-state (run (vals rules) init-state))))
