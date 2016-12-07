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
            [orig-state [(keeper rule-ins rule-outs data-cur data-new)]]
            (let [norm-data (zipmap rule-outs (cond-> data-new (= 1 (count rule-outs)) vector))
                  changed-queries (filter #(not= (get-in state %) (get-in norm-data [%])) rule-outs)
                  new-state (try (reduce (fn[acc q] (update-in acc q (constantly (get-in norm-data [q])))) state changed-queries)
                                 (catch #?(:cljs js/Object :clj Exception) e
                                   (ex-info "Error applying new data to existing state"
                                            {:state state
                                             :query changed-queries
                                             :data norm-data
                                             :error #?(:clj e
                                                       :cljs {:message (.-message e)
                                                              :stack (.-stack e)
                                                              :name (.-name e)})})))]
              (if (ex-data new-state)
                [orig-state [(keeper rule-ins rule-outs data-cur new-state)]]
                (recur new-state
                       (into changes (map #(keeper rule-ins % (get-in state %) (get-in new-state %)) changed-queries))
                       (into (rest rules) (mapcat second (select-keys rules-idx changed-queries))))))))))))

(defn engine
  "Creates zerol engine, calls fixpoint and once it's reached calls
  all side effecting rules. Accept following optional key parameters:
  - state - Initial state
  - rules - List of rules
  - keeper - Keeper function which manages how events got saved in history
  - side-effects - Side effecting rules map where keys are queries and
    values are function rules"
  [& {:keys [state rules keeper side-effects]}]
  (let [state (or state {})
        rules (-> rules (or []) rules-index)
        keeper (or keeper (partial vector 'T0))
        side-effects (or side-effects {})
        [new-state events] (fixpoint state rules [[]] keeper)
        state-atom (atom {:state new-state :past events})]
    (letfn [(event-emitter [effect-q effect-f]
              (let [prev-state (:state @state-atom)]
                (swap! state-atom
                       (fn [state]
                         (let [new-state (apply-effects state (hash-map effect-q effect-f))
                               effects (filter #(not= (get-in prev-state (first %)) (get-in (:state new-state) (first %))) side-effects)]
                           (apply-effects new-state effects))))))
            (apply-effects [state effects]
              (reduce (fn [acc [effect-k effect-f]]
                        (let [[state-tmp events1] (fixpoint (:state acc) (rules-index [[effect-k effect-k (partial effect-f event-emitter)]])
                                                            [effect-k]
                                                            keeper)
                              [new-state events2] (fixpoint state-tmp rules [effect-k] keeper)]
                          (assoc acc
                                 :state new-state
                                 :past (concat events2 events1 (:past acc)))))
                      state
                      effects))]
      (swap! state-atom apply-effects side-effects)
      state-atom)))
