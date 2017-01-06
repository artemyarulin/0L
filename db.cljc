(ns zerol.db)

(defn map->eav [m]
  (into (sorted-map) m))

(defn eav->map [eav]
  (-> (fn[acc [[prop id] val]]
        (let [entity (namespace prop)
              attr (name prop)
              data (get acc id {(keyword entity "id") id})]
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

(defn find-attribute [db attr ids]
  (if (nil? ids)
    (subseq db >= [attr 0] <= [attr #?(:clj Integer/MAX_VALUE :cljs js/Number.MAX_VALUE)])
    (select-keys db (map #(vector attr %) ids))))

(defn find-entities [db query]
  (loop [query (seq query)
         ids nil]
    (if-not (first query)
      ids
      (let [[prop val] (first query)
            predicate (if (fn? val) val (partial = val))]
        (recur (rest query)
               (->> (find-attribute db prop ids)
                    (filter (comp predicate second))
                    (map (comp second first))))))))

(defn query-joins [db query ids]
  (let [[join-queries rest-queries] (find-tokens query :join)]
    (when join-queries
      (let [[parent-attr child-attrs] (ffirst join-queries)
            joins (find-attribute db parent-attr ids)
            parent-ids (map (comp second first) joins)
            child-ids (map (comp second second) joins)]
        [rest-queries
         (zipmap (map #(vector parent-attr %) parent-ids)
                 (eav->map (mapcat #(find-attribute db % child-ids) child-attrs)))
         parent-ids]))))

(defn query-filters [db query ids]
  (let [[filter-queries rest-queries] (find-tokens query :filter)]
    (when filter-queries
      [rest-queries
       []
       (mapcat #(find-entities db %) filter-queries)])))

(defn query-attrs [db query ids]
  (let [[attr-queries rest-queries] (find-tokens query :attr)]
    (when attr-queries
      [rest-queries
       (mapcat #(find-attribute db % ids) attr-queries)
       ids])))

(defn query [db query]
  (loop [query (sort-by (comp not map?) query)
         data []
         ids nil]
    (if-not (first query)
      (or (eav->map data) [])
      (let [qcur (first query)]
        (let [[new-query new-data new-ids] (or (query-filters db query ids)
                                               (query-joins db query ids)
                                               (query-attrs db query ids))]
          (recur new-query (concat data new-data) new-ids))))))
