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
    [{:a 1 :b 2 :c 3} [[[[:a]] [:b] nil 2] [[[:b]] [:c] nil 3]]]
    [{:a 1} (z/rules-index [[[:a] [:b] inc] [[:b] [:c] inc]]) [[:a]] vector]))

(deftest fixpoint-should-track-changed-queries
  (-> (z/fixpoint {:a 1 :b 1 :d 1} (z/rules-index [[[:a] [[:b][:c]] #(vector % (inc %))]
                                                   [[:b] [:d] inc]])
                  [[:a]]
                  vector)
      first
      (= {:a 1 :b 1 :d 1 :c 2})
      is))

(deftest fixpoint-errors
  (let [[state changes] (z/fixpoint {} (z/rules-index [[[:a] [:b] #(throw (ex-info "Err" {:v %}))]]) [[:a]] vector)]
    (is (= state {}))
    (is ((every-pred :error :data :query-in :query-out) (ex-data (nth (first changes) 3))))))

#?(:cljs
   (deftest fixpoint-error-cljs
     (let [[state changes] (z/fixpoint {} (z/rules-index [[[:a] [:b] (fn[v](js/eval "crash"))]]) [[:a]] vector)]
       (is (= state {}))
       ((every-pred :message :name :stack) (:error (ex-data (nth (first changes) 3)))))))
