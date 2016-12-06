(ns zerol.core)

(defn rules-index
  "Creates index map of rules with {query [rules]} such that if query
  data has changed all possibly affected rules can be easily fetched
  by just reading value in the map"
  [rules]
  (letfn [(norm-query [query] (cond (every? keyword? query) [query]
                                    (every? coll? query) query
                                    :else (throw "Query has to be either vector of keywords or vector of vectors of keywords")))
          (norm-rule [rule] (mapv #(-> %
                                       (update-in [0] norm-query)
                                       (update-in [1] norm-query))
                                  (if (fn? rule) (rule) [rule])))
          (all-queries [ks] (->> ks
                                 (mapcat #(->> % count inc (range 0) (map (partial subvec % 0))))
                                 distinct))
          (index-down-up [rules] (->> rules
                                      (mapcat norm-rule)
                                      (reduce (fn [index [query-in query-out f]]
                                                (->> (repeat [[query-in query-out f]])
                                                     (zipmap (all-queries query-in))
                                                     (merge-with into index)))
                                              {})))
          (index-up-down [index] (reduce (fn [acc [k v]]
                                           (->> (all-queries [k])
                                                (filter #(not= % k))
                                                (select-keys index)
                                                (mapcat (fn [[rule-k rules]] ;; Ensure we add only explicit parent queries, not generated from bottom leafs
                                                          (filter (fn [[qs-in _ _]] (some #(= % rule-k) qs-in)) rules)))
                                                (update-in acc [k] into)))
                                         index
                                         index))]
    (-> rules index-down-up index-up-down)))

(defn fixpoint
  "Given state, rules index, list of queries for data that may have
  changed and a keeper function runs all possibly effected rules
  recursivly untill fix point is reached. Returns new state and list
  of changes piped through keeper"
  [orig-state rules-idx queries keeper]
  (loop [state orig-state
         changes []
         rules (->> queries (select-keys rules-idx) (mapcat second))]
    (let [[rule-ins rule-outs f] (first rules)]
      (if-not f
        [state changes]
        (let [data-in (map #(get-in state %) rule-ins)
              data-cur (map #(get-in state %) rule-outs)
              safe-f (fn[& args] (try (apply f args)
                                     (catch #?(:cljs js/Object :clj Exception) e
                                       (ex-info "Rule error" {:error #?(:clj e
                                                                        :cljs {:message (.-message e)
                                                                               :stack (.-stack e)
                                                                               :name (.-name e)})
                                                              :data args
                                                              :query-in rule-ins
                                                              :query-out rule-outs}))))
              data-new (apply safe-f data-in)]
          (if (ex-data data-new)
            [state [(keeper rule-ins rule-outs data-cur data-new)]]
            (let [norm-data (zipmap rule-outs (cond-> data-new (= 1 (count rule-outs)) vector))
                  changed-queries (filter #(not= (get-in state %) (get-in norm-data [%])) rule-outs)
                  new-state (reduce (fn[acc q] (update-in acc q (constantly (get-in norm-data [q])))) state changed-queries)]
              (recur new-state
                     (into changes (map #(keeper rule-ins % (get-in state %) (get-in new-state %)) changed-queries))
                     (into (rest rules) (mapcat second (select-keys rules-idx changed-queries)))))))))))
