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
         distinct)))

(defn normalize-rules [rules]
  ;; TODO: Deffenetly not the best code that I've wrote
  ;; Refactor those, but make sure test are passing
  (let [norm-rules (->> rules (reduce #(into %1 (normalize-rule %2)) []))
        new-rules (-> (fn[acc [ins outs f]]
                        (loop [acc acc
                               ks-ins (all-queries ins true)]
                          (if (empty? ks-ins)
                            acc
                            (recur (cond-> acc
                                     (contains? acc (first ks-ins)) (update-in [(first ks-ins)] conj [ins outs f]))
                                   (rest ks-ins)))))
                      (reduce (zipmap (concat (mapcat first norm-rules) (mapcat second norm-rules)) (repeat []))
                              norm-rules))]
    (-> (fn[acc [k v]]
          (let [all-keys (all-queries [k] false)
                to-merge (mapcat #(acc %) all-keys)]
            (assoc-in acc [k] (into v to-merge))))
        (reduce new-rules new-rules))))

(defn apply-changes [state queries data]
  (loop [state state
         queries queries
         data data]
    (let [q (first queries)
          d (first data)
          cancel-io? (and (= :io (first q)) (= 2 (count q)) (nil? d))
          io-to-cancel (when cancel-io? (get-in state q))]
      (when io-to-cancel
        ((:cancel io-to-cancel) (dissoc io-to-cancel :cancel)))
      (if-not q
        state
        (recur (assoc-in state q d) (rest queries) (rest data))))))

(defn balance [state orig-rules queries]
  (loop [rules (vec (mapcat #(orig-rules %) queries))
         state state
         queries queries
         updated []]
    (let [[queries-in queries-out rule-f] (first rules)]
      (if-not rule-f
        state
        (let [data-in (->> queries-in (map #(get-in state %)))
              data-cur (->> queries-out (map #(get-in state %)))
              data-new (cond-> (apply rule-f data-in) (= 1 (count queries-out)) vector)
              same-data? (= data-cur data-new)
              no-rules? (-> rules rest empty?)
              next-state (cond-> state (not same-data?) (apply-changes queries-out data-new))
              next-queries (if no-rules? (distinct updated) queries)
              next-updated (cond-> updated (not same-data?) (into queries-out) no-rules? empty)
              next-rules (if no-rules? (mapcat #(orig-rules %) next-updated) (rest rules))]
          (recur next-rules
                 next-state
                 next-queries
                 next-updated))))))

(defn apply-event! [engine in out f]
  (let [new-state (-> (:state @engine)
                      (balance (normalize-rules [[in out f]]) [in])
                      (balance (:rules @engine) [out]))]
    (when (and (not= (:ui new-state) (-> @engine :state :ui)) (:renderer @engine))
      ((:renderer @engine) (:ui new-state)))
    (swap! engine assoc-in [:state] new-state)))

(defn engine
  ([init-rules init-state] (engine init-rules init-state nil nil))
  ([init-rules init-state init-io] (engine init-rules init-state init-io nil))
  ([init-rules init-state init-io init-renderer]
   (let [engine (atom {})
         rules (normalize-rules (if init-io (conj init-rules [[:io] [:io] (partial init-io (partial apply-event! engine))]) init-rules))
         renderer (when init-renderer (partial init-renderer (partial apply-event! engine)))]
     (reset! engine {:state init-state :rules rules :renderer renderer})
     (swap! engine update-in [:state] balance rules (keys rules))
     (when renderer (renderer (-> @engine :state :ui)))
     engine)))
