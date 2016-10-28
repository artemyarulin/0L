(ns zerol.tests-io
  (:require [clojure.test :refer [deftest is]]
            [zerol.zerol :refer [converge]]
            [zerol.rules :as rules]))

(deftest counter-async-fetch
  (is (= {:data {:counter {:value 10 :fetching nil}}, :ui {:counter {:value 10}}, :io {:fetch nil}}
         (converge {:update [[[:data :counter][:io :fetch]][:data :counter] #(cond-> %1 (= :done (:status %2)) (assoc :value (:value %2) :fetching nil))]
                    :fetching (rules/move [:ui :counter] [:data :counter] :mode-click :fetching)
                    :fetch [[:data :counter :fetching] [:io :fetch] #(when % {:type :http :address "http://example.com"})]
                    :bind (rules/copy [:data :counter] [:ui :counter] :value)}
                   {:data {:counter {:value 1}}}
                   {:click [[:ui :counter] [:ui :counter] #(assoc % :mode-click true)]
                    :io-done [[:io :fetch][:io :fetch] #(assoc % :status :done :value 10)]}))))
