(ns zerol.tests-renderer
  (:require [zerol.engine :as [zerol]]
            [zerol.test :refer [converge]]
            [zerol.rules :as rules]))

(deftest renderer-console
  (let [init-state {:data {:counter {:value 10}}}
        rules [{:bind (rules/copy [:data :counter :value] [:ui :counter :value])}
               {:mode-click (rules/move [:ui :counter :mode-click] [:data :counter :updating])}
               {:updating (rules/only-when [:data :counter :updating] [:io :updating] {:type :async :value (:value %)})}
               {:updated (rules/io-done [:io :updating] [:data :counter] (partial hash-map :value))}]
        events [(rules/event-ui :counter :mode-click)
                (rules/event-io :updating inc)
                (rules/event-ui :counter :mode-click)
                (rules/event-io :updating inc)]
    (is (= {:data {:counter {:value 12}} :ui {:counter {:value 12}} :io nil}
           (converge rules init-state events))))))


[{:bind (rules/copy [:data :counter :value] [:ui :counter :value])}
 {:mode-click (rules/move [:ui :counter :mode-click] [:data :counter :updating])}
 {:updating (rules/only-when [:data :counter :updating] [:io :updating] {:type :async :value (:value %)})}
 {:updated (rules/io-done [:io :updating] [:data :counter] (partial hash-map :value))}]

(defrule-copy bind [:data :counter :value] [:ui :counter :value])
(defrule-move mode-click [:ui :counter :mode-click] [:data :counter :updating])
(defrule-only-when updating [:data :counter :updating] [:io :updating] {:type :async :value (:value %)})
(defrule-io-done updated [:io :updating] [:data :counter] (partial hash-map :value))


[{:bind (rules/copy "data/counter/value" "ui/counter/value")}
 {:mode-click (rules/move "ui/counter/mode-click" "data/counter/updaing")
 {:updating (rules/only-when "data/counter/updaing" "io/updating" {:type :async :value (:value %)})}
  {:updated (rules/io-done "io/updating" "data/counter" (partial hash-map :value))}]


(def bind (rules/copy [:data :counter :value] [:ui :counter :value])}
(def mode-click (rules/move [:ui :counter :mode-click] [:data :counter :updating])}
(def updating (rules/only-when [:data :counter :updating] [:io :updating] {:type :async :value (:value %)})}
 (def updated (rules/io-done [:io :updating] [:data :counter] (partial hash-map :value))}]

(rules bind (rules/copy [:data :counter :value] [:ui :counter :value])
       updating (rules/only-when [:data :counter :updating] [:io :updating] {:type :async :value (:value %)})
       mode-click (rules/move [:ui :counter :mode-click] [:data :counter :updating])
       updated (rules/io-done [:io :updating] [:data :counter] (partial hash-map :value)))



;; Use case start
