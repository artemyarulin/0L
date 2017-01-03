(ns zerol.core-test
  (:require [zerol.core :as z]
            [clojure.test :refer [deftest is are]]))

(deftest rules-index-one-query
  (are [exp rules] (= exp (z/rules-index rules))
    ;; [:a] -> [:a]
    {[] [[[[:a]] [[:a]] 'f]]
     [:a] [[[[:a]] [[:a]] 'f]]}
    [[[:a] [:a] 'f]]

    ;; [:a] -> [:a :b]
    {[] [[[[:a :b]] [[:c]] 'f]]
     [:a] [[[[:a :b]] [[:c]] 'f]],
     [:a :b] [[[[:a :b]] [[:c]] 'f]]}
    [[[:a :b] [:c] 'f]]

    ;; [:a] -> [:b], [:x] -> [:y]
    {[] [[[[:a]] [[:b]] 'ab] [[[:x]] [[:y]] 'xy]]
     [:a] [[[[:a]] [[:b]] 'ab]]
     [:x] [[[[:x]] [[:y]] 'xy]]}
    [[[:a] [:b] 'ab] [[:x] [:y] 'xy]]

    ;; [:a :b] -> [:c], [:a :d] -> [:e]
    {[] [[[[:a :b]] [[:c]] 'abc] [[[:a :d]] [[:e]] 'ade]]
     [:a] [[[[:a :b]] [[:c]] 'abc] [[[:a :d]] [[:e]] 'ade]]
     [:a :b] [[[[:a :b]] [[:c]] 'abc]]
     [:a :d] [[[[:a :d]] [[:e]] 'ade]]}
    [[[:a :b] [:c] 'abc]
     [[:a :d] [:e] 'ade]]

    ;; 5-levels
    {[] [[[[:a]] [[:a]] 'a]
         [[[:a :b]] [[:a :b]] 'ab]
         [[[:a :b :c]] [[:a :b :c]] 'abc]
         [[[:a :d]] [[:a :d]] 'ad]
         [[[:a :d :e]] [[:a :d :e]] 'ade]]
     [:a] [[[[:a]] [[:a]] 'a]
           [[[:a :b]] [[:a :b]] 'ab]
           [[[:a :b :c]] [[:a :b :c]] 'abc]
           [[[:a :d]] [[:a :d]] 'ad]
           [[[:a :d :e]] [[:a :d :e]] 'ade]],
     [:a :b] [[[[:a :b]] [[:a :b]] 'ab]
              [[[:a :b :c]] [[:a :b :c]] 'abc]
              [[[:a]] [[:a]] 'a]],
     [:a :b :c] [[[[:a :b :c]] [[:a :b :c]] 'abc]
                 [[[:a]] [[:a]] 'a]
                 [[[:a :b]] [[:a :b]] 'ab]],
     [:a :d] [[[[:a :d]] [[:a :d]] 'ad]
              [[[:a :d :e]] [[:a :d :e]] 'ade]
              [[[:a]] [[:a]] 'a]],
     [:a :d :e] [[[[:a :d :e]] [[:a :d :e]] 'ade]
                 [[[:a]] [[:a]] 'a]
                 [[[:a :d]] [[:a :d]] 'ad]]}
    [[[:a] [:a] 'a]
     [[:a :b] [:a :b] 'ab]
     [[:a :b :c] [:a :b :c] 'abc]
     [[:a :d] [:a :d] 'ad]
     [[:a :d :e] [:a :d :e] 'ade]]))

(deftest rules-index-mult-queries
  (are [exp rules] (= exp (z/rules-index rules))
    ;; [[:a][:b]] -> [:c]
    {[] [[[[:a] [:b]] [[:c]] 'f]]
     [:a] [[[[:a] [:b]] [[:c]] 'f]]
     [:b] [[[[:a] [:b]] [[:c]] 'f]]}
    [[[[:a][:b]] [:c] 'f]]))

(deftest fixpoint
  (are [exp args] (= exp (apply z/fixpoint args))
    ;; One
    [{:a 1 :b 2} [[[[:a]] [:b] nil 2]]]
    [{:a 1} (z/rules-index [[[:a] [:b] inc]]) [[:a]] vector]
    ;; Chain
    [{:a 1 :b 2 :c 3} [[[[:b]] [:c] nil 3] [[[:a]] [:b] nil 2]]]
    [{:a 1} (z/rules-index [[[:a] [:b] inc] [[:b] [:c] inc]]) [[:a]] vector]))

(deftest fixpoint-should-track-changed-queries
  (-> (z/fixpoint {:a 1 :b 1 :d 1} (z/rules-index [[[:a] [[:b][:c]] #(vector % (inc %))]
                                                   [[:b] [:d] inc]])
                  [[:a]]
                  vector)
      first
      (= {:a 1 :b 1 :d 1 :c 2})
      is))

(deftest fixpoint-rule-error
  (let [[state changes] (z/fixpoint {} (z/rules-index [[[:a] [:b] #(throw (ex-info "Err" {:v %}))]]) [[:a]] vector)]
    (is (= state {}))
    (is ((every-pred :error :data :query-in :query-out) (ex-data (nth (first changes) 3))))))

#?(:cljs
   (deftest fixpoint-rule-error-cljs
     (let [[state changes] (z/fixpoint {} (z/rules-index [[[:a] [:b] (fn[v](js/eval "crash"))]]) [[:a]] vector)]
       (is (= state {}))
       ((every-pred :message :name :stack) (:error (ex-data (nth (first changes) 3)))))))

(deftest fixpont-should-return-original-state-on-error
  (-> (z/fixpoint {:a 1}
                  (z/rules-index [[[:a] [:b] inc]
                                  [[:b] [:c] #(throw (ex-info "Err" {:v %}))]])
                  [[:a]]
                  vector)
      first
      (= {:a 1})
      is))

(deftest fixpoint-dynamic-query
  (is (= {:a {:b 3} :c {:b 3}}
         (-> (z/fixpoint {:a {:b 3}}
                     (z/rules-index [[[:a] [:c] identity]])
                     [[:a :b]]
                     vector)
             first))))

(deftest fixpoint-apply-change-error
  (let [init-state {:a 1 :c 'c}
        [state changes] (z/fixpoint init-state
                                    (z/rules-index [[[:a] [:c :z] inc]])
                                    [[:a]]
                                    vector)]
    (is (= state init-state))
    (is ((every-pred :state :query :data :error) (ex-data (nth (first changes) 3))))))

(deftest engine
  (-> (z/engine) deref (= {:state {} :past []}) is)
  (-> (z/engine :state {:a 1}) deref :state (= {:a 1}) is)
  (-> (z/engine :state {:a 1} :rules [[[:a] [:b] inc]]) deref :state (= {:a 1 :b 2}) is)
  (-> (z/engine :state {:a 1} :rules [[[:a] [:b] inc]]) deref :past (= [["T0" [[:a]] [:b] nil 2]]) is))

(deftest engine-keeper-order
  (-> (z/engine :state {:a 1}
                :rules [[[:a] [:b] inc]
                        [[:b] [:c] inc]]
                :side-effects {[:c] (fn[_ c](cond-> c (odd? c) inc))})
      deref
      :past
      (= '(["T2" [[:c]] [:c] 3 4]
           ["T1" [[:b]] [:c] nil 3]
           ["T0" [[:a]] [:b] nil 2]))
      is))

#?(:clj (deftest engine-side-effects-initial
          (let [out (promise)]
            (z/engine :state {:a 1}
                      :side-effects {[:a] (fn[_ a] (deliver out a) a)})
            (is (= 1 @out))))
   :cljs (deftest engine-side-effects-initial
           (cljs.test/async
            done
            (z/engine :state {:a 1}
                      :side-effects {[:a] (fn[_ a] (is (= 1 a)) (done) a)}))))

#?(:clj (deftest engine-side-effects-events
          (let [out (promise)
                state (z/engine :state {:a 1} :side-effects {[:a]
                                                             (fn[event! a]
                                                               (future
                                                                 (Thread/sleep 1000)
                                                                 (event! [:b] (constantly true))
                                                                 (deliver out nil))
                                                               a)})]
            @out
            (-> state deref :state (= {:a 1 :b true}) is)))
   :cljs (deftest engine-side-effects-events
           (cljs.test/async done
                            (def state (z/engine :state {:a 1} :side-effects {[:a]
                                                                              (fn[event! a]
                                                                                (js/setTimeout (fn[]
                                                                                                 (event! [:b] (constantly true))
                                                                                                 (-> state deref :state (= {:a 1 :b true}) is)
                                                                                                 (done))
                                                                                               1000)
                                                                                a)})))))
(deftest goal-test
  (-> (z/engine :state {:a 1} :rules [[[:a] [:b] inc]] :goal [:a (comp not nil?)])
      deref :state (= {:a 1}) is)
  (-> (z/engine :state {:a 1} :rules [[[:a] [:b] inc] [[:b] [:c] inc]] :goal [:b (comp not nil?)])
      deref :state (= {:a 1 :b 2}) is))
