(ns zerol.test
  (:require [zerol.engine :refer [apply-event!]]))

(defn converge [engine & events]
  (if (nil? (first events))
    (:state @engine)
    (do
      (apply apply-event! engine (first events))
      (recur engine (rest events)))))
