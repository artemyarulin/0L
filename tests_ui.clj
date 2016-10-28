(ns zerol.tests-ui
  (:require [clojure.test :refer [is are deftest]]
            [zerol.zerol :refer [converge]]))

(def r-inc #(cond-> % (:inc? %) (-> (dissoc :inc?) (update :value inc))))

(deftest counter-inc-mode
  (is (= {:data {:counter {:value 4}} :ui {:counter {:value 4}}}
         (converge {:inc [[:data :counter] [:data :counter] r-inc]
                    :on-click [[[:ui :counter] [:data :counter]] [[:ui :counter] [:data :counter]]
                               (fn[ui-counter data-counter]
                                 (if (:mode-click? ui-counter)
                                   [(dissoc ui-counter :mode-click?) (assoc data-counter :inc? true)]
                                   [ui-counter (dissoc data-counter :inc?)]))]
                    :bind [[:data :counter] [:ui :counter] identity]}
                   {:data {:counter {:value 1}}}
                   {:click1 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]
                    :click2 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]
                    :click3 [[:ui :counter] [:ui :counter] #(assoc % :mode-click? true)]}))))
