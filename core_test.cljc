(ns zerol.core-test
  (:require [zerol.core :as z]
            [clojure.test :refer [deftest is are]]))

(deftest rules-index-one-query
  (are [exp rules] (= exp (z/rules-index rules))
    ;; [:a] -> [:a]
    {[:a] [[[[:a]] [[:a]] 'f]]}
    [[[:a] [:a] 'f]]
    ;; [:a] -> [:a :b]
    {[:a] [[[[:a :b]] [[:c]] 'f]],
     [:a :b] [[[[:a :b]] [[:c]] 'f]]}
    [[[:a :b] [:c] 'f]]
    ;; [:a] -> [:b], [:x] -> [:y]
    {[:a] [[[[:a]] [[:b]] 'ab]]
     [:x] [[[[:x]] [[:y]] 'xy]]}
    [[[:a] [:b] 'ab] [[:x] [:y] 'xy]]
    ;; [:a :b] -> [:c], [:a :d] -> [:e]
    {[:a] [[[[:a :b]] [[:c]] 'abc] [[[:a :d]] [[:e]] 'ade]]
     [:a :b] [[[[:a :b]] [[:c]] 'abc]]
     [:a :d] [[[[:a :d]] [[:e]] 'ade]]}
    [[[:a :b] [:c] 'abc]
     [[:a :d] [:e] 'ade]]
    ;; 5-levels
    {[:a] [[[[:a]] [[:a]] 'a]
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
    {[:a] [[[[:a] [:b]] [[:c]] 'f]]
     [:b] [[[[:a] [:b]] [[:c]] 'f]]}
    [[[[:a][:b]] [:c] 'f]]))
