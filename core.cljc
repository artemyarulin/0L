(ns zerol.core)

;; WARNING: Your eyes may bleed if you start reading following code
;; It's PoC and most valuable thing is not implementation code, but
;; rather tests and usecases that I gather during development

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
  changed,keeper function and goal runs all possibly effected rules
  recursivly untill fix point or goal is reached. Returns new state
  and list of changes piped through keeper"
  ([orig-state rules-idx queries keeper] (fixpoint orig-state rules-idx queries keeper [nil nil]))
  ([orig-state rules-idx queries keeper [goal-query goal-f]]
   (loop [state orig-state
          changes ()
          rules (->> queries (mapcat (fn[query]
                                       (loop [query query]
                                         (if-not query
                                           []
                                           (if-let [found (get rules-idx query)]
                                             found
                                             (recur (butlast query))))))))]
     (let [[rule-ins rule-outs f] (first rules)
           goal-reached (and goal-query goal-f (goal-f (get-in state [goal-query])))]
       (if-not (and f (not goal-reached)) ;; TODO: Check goal only if corresponding query has changed
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
                        (into (rest rules) (->> changed-queries (mapcat (fn[query]
                                                                          (loop [query query]
                                                                            (if-not query
                                                                              []
                                                                              (if-let [found (get rules-idx query)]
                                                                                found
                                                                                (recur (butlast query)))))))))))))))))))

(defn default-keeper []
  (let [c (atom -1)]
    (fn[q1 q2 v1 v2]
      (swap! c inc)
      [(str "T" @c) q1 q2 v1 v2])))

(defn engine
  "Creates zerol engine, calls fixpoint and once it's reached calls
  all side effecting rules. Accept following optional key parameters:
  - state - Initial state
  - rules - List of rules
  - keeper - Keeper function which manages how events got saved in history
  - side-effects - Side effecting rules map where keys are queries and
    values are function rules
  - goal - pair of query and a function from query data. If function returns true - engine stops"
  [& {:keys [state rules keeper side-effects goal]}]
  (let [state (or state {})
        goal (or goal [nil nil])
        pure-rules (-> rules (or []) rules-index)
        all-rules (atom nil)
        keeper (or keeper (default-keeper))
        side-effects (or side-effects {})
        [new-state events] (fixpoint state pure-rules [[]] keeper goal)
        world (atom {:state new-state :past events})]
    (letfn [(event-emitter [effect-q effect-f]
              (let [prev-state (:state @world)]
                (swap! world
                       (fn [world]
                         (let [[new-state changes] (fixpoint (:state world) (rules-index [[effect-q effect-q effect-f]]) [effect-q] keeper goal)
                               [new-state2 changes2] (fixpoint new-state pure-rules [effect-q] keeper goal)
                               effected-queries (filter #(not= (get-in prev-state %) (get-in new-state2 %)) (keys side-effects))]
                           (apply-effects {:state new-state2
                                           :past (concat changes2 changes (:past world))}
                                          effected-queries))))))
            (apply-effects [world queries]
              (let [[new-state changes] (fixpoint (:state world) @all-rules queries keeper goal)]
                {:state new-state
                 :past (concat changes (:past world))}))]
      (reset! all-rules (->> side-effects
                             (mapv (fn[[q f]] [q q (partial f event-emitter)]))
                             (into rules)
                             rules-index))
      (swap! world apply-effects (keys side-effects))
      world)))
