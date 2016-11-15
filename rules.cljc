(ns zerol.rules)

;; TODO: Merge them together as the only difference is dissoc
;; Or actually let's wait for a while and see what other rules we are
;; going to create and maybe we will se a pattern there

(defn move
  ([query-from query-to] (move (vec (butlast query-from)) (vec (butlast query-to)) (last query-from) (last query-to)))
  ([query-from query-to kw-from] (move query-from query-to kw-from kw-from))
  ([query-from query-to kw-from kw-to]
   [[query-from query-to][query-from query-to]
    (fn[data-from data-to]
      (if (contains? data-from kw-from)
        [(dissoc data-from kw-from)(assoc data-to kw-to (kw-from data-from))]
        [data-from data-to]))]))

(defn copy
  ([query-from query-to] (copy (vec (butlast query-from)) (vec (butlast query-to)) (last query-from) (last query-to)))
  ([query-from query-to kw-from] (copy query-from query-to kw-from kw-from))
  ([query-from query-to kw-from kw-to]
   [[query-from query-to][query-from query-to]
    (fn[data-from data-to]
      (if (contains? data-from kw-from)
        [data-from (assoc data-to kw-to (kw-from data-from))]
        [data-from data-to]))]))
