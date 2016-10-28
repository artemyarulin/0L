(ns zerol.zerol)

(defn normalize-query [query]
  (cond
    (every? keyword? query) [query]
    (every? coll? query) query
    :else (throw "Query has to be either vector of keywords or vector of vector of keywords")))

(defn apply-changes [state queries data]
  (println "Apply " [state queries data])
  (loop [state state queries queries data data]
    (let [q (first queries)
          d (first data)]
      (if q
        (recur (assoc-in state q d) (rest queries) (rest data))
        state))))

(defn run [orig-rules state]
  (loop [rules orig-rules state state]
    (let [[name rule] (first rules)]
      (if-not rule
        state
        (let [[in-query out-query f] rule
              in-queries (normalize-query in-query)
              out-queries (normalize-query out-query)
              in-data (->> in-queries (map #(get-in state %)))
              cur-data (->> out-queries (map #(get-in state %)))
              new-data (cond-> (apply f in-data) (= (count out-queries) 1) vector)]
          (if (not= new-data cur-data)
            (do
              (println name ": " state)
              (recur orig-rules (apply-changes state out-queries new-data)))
            (recur (rest rules) state)))))))

(defn converge [rules init-state events]
  (loop [events events
         state (run rules init-state)]
    (let [[ev-name ev-rule] (first events)]
      (if-not ev-name
        state
        (recur (rest events)
               (->> state
                    (run {ev-name ev-rule})
                    (run rules)))))))
