(ns zerol.engine)

(defn normalize-query [query]
  (cond
    (every? keyword? query) [query]
    (every? coll? query) query
    :else (throw "Query has to be either vector of keywords or vector of vector of keywords")))

(defn normalize-rule [rule]
  (mapv #(-> %
             (update-in [0] normalize-query)
             (update-in [1] normalize-query))
        (if (fn? rule) (rule) [rule])))

(defn all-queries [ks include-self?]
  (let [x (if include-self? 1 0)]
  (->> ks
       (mapcat #(->> % count (+ x) (range 1) (map (partial subvec % 0))))
       distinct
       set)))

(defn normalize-rules [rules]
  ;; TODO: Deffenetly not the best code that I've wrote
  ;; Refactor those, but make sure test are passing
  (let [new-rules (-> (fn[acc [ins outs f]]
                        (loop [acc acc
                               ks-ins (all-queries ins true)]
                          (if (empty? ks-ins)
                            acc
                            (recur (cond-> acc
                                     (contains? acc (first ks-ins)) (update-in [(first ks-ins)] conj [ins outs f]))
                                   (rest ks-ins)))))
                      (reduce (zipmap (concat (map first rules) (map second rules)) (repeat #{}))
                              (->> rules (reduce #(into %1 (normalize-rule %2)) []))))]
    (-> (fn[acc [k v]]
          (let [all-keys (all-queries [k] false)
                to-merge (mapcat #(acc %) all-keys)]
            (assoc-in acc [k] (into v to-merge))))
        (reduce new-rules new-rules))))

(defn engine [rules init-state io renderer]
  (atom {:rules (normalize-rules rules)
         :state init-state
         :io io
         :renderer renderer}))

(defn apply-event! [engine in out f]
  (swap! engine update-in [:state]
         (fn[state]
           (-> state
                (balance (normalize-rules [[in out f]]) [in])
                (balance (:rules @engine) [out])))))

(defn start-engine! [engine]
  (add-watch engine :render (fn[_ _ _ {:keys [renderer state]}]
                              (renderer state (partial apply-event! engine))))
  (swap! engine update-in [:state]
         (fn[state]
           (balance state (:rules @engine) (keys (:rules @engine))))))

(defn apply-changes [state queries data]
  (loop [state state queries queries data data]
    (let [q (first queries)
          d (first data)]
      (if q
        (recur (assoc-in state q d) (rest queries) (rest data))
        state))))

(defn balance [state orig-rules queries]
  (loop [rules (mapcat #(orig-rules %) queries)
         state state
         queries queries
         updated #{}]
    (let [[queries-in queries-out rule-f] (first rules)]
      (if-not rule-f
        state
        (let [data-in (->> queries-in (map #(get-in state %)))
              data-cur (->> queries-out (map #(get-in state %)))
              data-new (cond-> (apply rule-f data-in) (= 1 (count queries-out)) vector)
              same-data? (= data-cur data-new)
              no-rules? (-> rules rest empty?)
              next-state (cond-> state (not same-data?) (apply-changes queries-out data-new))
              next-queries (if no-rules? updated queries)
              next-updated (cond-> updated (not same-data?) (conj queries-out) no-rules? empty)
              next-rules (if no-rules? (mapcat #(orig-rules %) next-updated) (rest rules))]
          (recur next-rules
                 next-state
                 next-queries
                 next-updated))))))
