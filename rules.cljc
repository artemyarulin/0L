(ns zerol.rules)

(defn move
  ([query-from query-to kw-from] (move query-from query-to kw-from kw-from))
  ([query-from query-to kw-from kw-to]
   [[query-from query-to][query-from query-to]
     (fn[data-from data-to]
       (if (contains? data-from kw-from)
         [(dissoc data-from kw-from)(assoc data-to kw-to (kw-from data-from))]
         [data-from (dissoc data-to kw-to)]))]))

(defn copy
  ([query-from query-to kw-from] (copy query-from query-to kw-from kw-from))
  ([query-from query-to kw-from kw-to]
   [[query-from query-to] query-to
    (fn[data-from data-to]
      (cond-> data-to
        (contains? data-from kw-from) (assoc data-to kw-to (kw-from data-from))))]))
