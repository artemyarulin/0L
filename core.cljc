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
