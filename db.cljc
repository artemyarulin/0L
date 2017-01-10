(ns zerol.db)

(def min-id -2147483648)
(def max-id  2147483647)
(defn third [l] (nth l 2))

(defn map->eav [m]
  (let [id (or (second (first (filter #(= "id" (-> % first name)) (seq m)))) (hash m))]
    (reduce (fn[[acc names] [k v]]
              (let [entity (-> k namespace keyword)
                    attr (-> k name keyword)
                    entity-hash (hash entity)
                    attr-hash (hash attr)]
                (if (= (hash :id) attr-hash)
                  [acc names] ;; Skip :id as there is no need to store it explicit
                  [(assoc acc [entity-hash attr-hash id] v)
                   (assoc names
                          entity-hash entity
                          attr-hash attr)])))
            [{} {}]
            (seq m))))

(defn maps->db [& ms]
  (reduce (fn[acc m]
            (let [[eav names] (map->eav m)]
              (update (into acc eav) [] merge names)))
          (sorted-map)
          ms))

(defn eav->map [eav names]
  (-> (fn[acc [[entity attr id] val]]
        (let [data (get acc id {(keyword (name (get names entity)) "id") id})
              prop (keyword (name (get names entity)) (name (get names attr)))]
          (assoc acc id (assoc data prop val))))
      (reduce {} eav)
      vals))

(defn token-type [token]
  (cond
    (keyword? token) :attr
    (and (map? token) (-> token first second vector?)) :join
    (map? token) :filter))

(defn find-tokens [query type]
  (let [tokens (group-by #(= type (token-type %)) query)]
    [(get tokens true)
     (get tokens false)]))

(defn find-attr [db entity attr ids]
  (let [entity-hash (cond-> entity (keyword? entity) hash)
        attr-hash (cond-> attr (keyword? attr) hash)]
    (if (nil? ids)
      (subseq db
              >= [entity-hash attr-hash min-id]
              <= [entity-hash attr-hash max-id])
      (seq (select-keys db (map #(vector entity-hash attr-hash %) ids))))))

(defn find-entities [db query]
  (loop [query (seq query)
         ids nil]
    (if-not (first query)
      ids
      (let [[entity attr val] (first query)
            predicate (if (fn? val) val (partial = val))]
        (recur (rest query)
               (->> (find-attr db entity attr ids)
                    (filter (comp predicate second))
                    (map (comp third first))))))))

(defn query-joins [db query ids]
  (let [[join-queries rest-queries] (find-tokens query :join)]
    (when join-queries
      (let [[parent-prop child-attrs] (ffirst join-queries)
            parent-entity (-> parent-prop namespace keyword hash)
            parent-attr (-> parent-prop name keyword hash)
            joins (find-attr db parent-entity parent-attr ids)
            parent-ids (map (comp third first) joins)
            child-ids (map (comp second second) joins)]
        [rest-queries
         (zipmap (map #(vector parent-entity parent-attr %) parent-ids)
                 (eav->map (mapcat (fn[[entity attr]] (find-attr db entity attr child-ids))
                                   (map #(vector (-> % namespace keyword hash)
                                                 (-> % name keyword hash))
                                        child-attrs))
                           (get db [])))
         parent-ids]))))

(defn query-filters [db query ids]
  (let [[filter-queries rest-queries] (find-tokens query :filter)]
    (when filter-queries
      [rest-queries
       []
       (->> filter-queries
            (mapcat seq)
            (map (fn[[prop v]]
                   [(-> prop namespace keyword hash)
                    (-> prop name keyword hash)
                    v]))
            (find-entities db))])))

(defn query-attrs [db query ids]
  (let [[attr-queries rest-queries] (find-tokens query :attr)]
    (when attr-queries
      [rest-queries
       (->> attr-queries
            (map #(vector (-> % namespace keyword hash)
                          (-> % name keyword hash)))
            (mapcat (fn[[entity attr]] (find-attr db entity attr ids))))
       ids])))

(defn query [db query]
  (loop [query (sort-by (comp not map?) query)
         data []
         ids nil]
    (if-not (first query)
      (or (eav->map data (get db [])) [])
      (let [qcur (first query)]
        (let [[new-query new-data new-ids] (or (query-filters db query ids)
                                               (query-joins db query ids)
                                               (query-attrs db query ids))]
          (recur new-query (concat data new-data) new-ids))))))

(defn save [db data]
  (reduce (fn[[db changes] [[prop id] v]]
            (let [entity (-> prop namespace keyword)
                  attr (-> prop name keyword)
                  full-key [(hash entity) (hash attr) id]]
              (cond
                (and (nil? v) (get db full-key))   [(dissoc db full-key)
                                                    (conj changes [[prop id] v])]
                (and v (nil? (get db full-key)))   [(-> db
                                                        (assoc full-key v)
                                                        (update [] merge {(hash entity) entity
                                                                          (hash attr) attr}))
                                                    (conj changes [[prop id] v])]
                (and v (not= v (get db full-key))) [(assoc db full-key v)
                                                    (conj changes [[prop id] v])]
                :else [db changes])))
          [db []] data))

(defn drop-entity [db entity id]
  (->> (subseq db
               >= [(hash entity) min-id id]
               <= [(hash entity) max-id id])
       (filter #(-> % first third (= id)))
       (map first)
       (apply (partial dissoc db))))
