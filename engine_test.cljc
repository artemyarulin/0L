(ns zerol.engine-test
  (:require [zerol.engine :as z]
            [clojure.test :refer [deftest is are]]))

(deftest normalize-query
  (are [exp query] (= exp (z/normalize-query query))
    [#{:ui}] [:ui]
    [#{:ui :data}] [:ui :data]
    [#{:ui :a} #{:data :b}] [[:ui :a][:data :b]]))

(deftest normalize-rule
  (are [exp rule] (= exp (z/normalize-rule rule))
    [[[#{:ui}] [#{:ui}] identity]] [[:ui] [:ui] identity]
    [[[#{:a}][#{:b}] identity] [[#{:c}][#{:z}] identity]]
    (fn[][[[:a][:b] identity] [[:c][:z] identity]])))

(deftest normalize-rules
  ;; No
  (z/normalize-rules [[[:a] [:b] identity]])
  (z/normalize-rules [[[:a :c] [:a :z] identity]])
  (z/normalize-rules [[[:a :c :d] [:a :c :z] identity]])

  ;; Yes, same
  (is (z/normalize-rules [[[:ui] [:ui] identity]]))
  ;; Yes, notify up

  ;; :a:z -> :a:x
  (z/normalize-rules [[[:a :z] [:a :x] nil]])
  ;; :a -> :a
  (z/normalize-rules [[[:a] [:a] nil]])
  ;; :a:z -> :a, :a -> :a:z
  (z/normalize-rules [[[:a :z] [:a] nil]])
  ;; :a -> :a:z, :a:z -> :a
  (z/normalize-rules [[[:a] [:a :z] -]])

  (is (z/normalize-rules [[[:ui :counter] [:ui :counter] nil]
                          [[:ui] [:ui] nil]]))

  (is (z/normalize-rules [[[:ui :counter :value] [:ui :counter] nil]])))

(deftest engine
  (is (= {:rules {[:a] #{[[[:a]] [[:b]] nil]}
                [:b] #{}}
        :state (atom {:z 1})
        :io {}
        :renderer nil}
         (z/engine [[[:a][:b] nil]]
                   {:z 1}
                   {}
                   nil))))


(deftest apply-changes
  (is (= {:a 3 :b 4}
         (z/apply-changes {:a 1 :b 2}
                          [[:a] [:b]]
                          [3 4]))))

(deftest balance
  (let [rules [(z/normalize-rule [[:a :z] [:b :z] inc])]]
    (is (= {:a {:z 10}} (z/balance rules {:a {:z 10}} #{})))
    (is (= {:a {:z 10} :b {:z 11}} (z/balance rules {:a {:z 10}} #{[:a :z]})))
    (is (= {:a {:z 10} :b {:z 11}} (z/balance rules {:a {:z 10}} #{[:a]})))))

(def engine (z/engine [[[:a][:b] inc]]
                      {:a 1}
                      {}
                      (fn[state event!]
                        (println "Current:" state)
                        (when (< (:b state) 10)
                          (Thread/sleep 1000)
                          (println "Want more!")
                          (event! [:a] [:a] inc)))))
(z/start-engine! engine)

(z/apply-event! engine [:a] [:a] inc)
