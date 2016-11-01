(ns zerol.test
  (:require [zerol.engine :refer [zerol]]))

(defn converge
  ([rules init-state] (converge rules init-state {}))
  ([rules init-state events]
   (println "---- Converge with rules:" (keys rules))
   (loop [events events
          state (run rules init-state)]
     (let [[ev-name ev-rule] (first events)]
       (if-not ev-name
         state
         (recur (rest events)
                (->> state
                     (run {ev-name ev-rule})
                     (run rules))))))))
