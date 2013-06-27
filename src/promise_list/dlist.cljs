(ns promise-list.dlist
  (:use [jayq.util :only [log]])
  (:require [promise-list.dcell :as dc]
            [jayq.core :as jq]))

(defn closed-dlist [& values]
  (reduce #(dc/closed-cell %2 %1) (dc/empty-cell) values))

(defn open-dlist
  "Takes a seq of values packs those values into a dlist. Returns a pair
  containing that dlist and a writer that can be used to append to the list."
  [& values]
  (let [tail  (dc/open-container)
        dlist (reduce #(dc/closed-cell %2 %1) tail values)]
    (list dlist (atom tail))))

(defn append! [writer dvalue]
  (let [current-tail @writer
        new-tail     (dc/DCell. (jq/$deferred))]
    (jq/done dvalue (fn [value]
                      (let [contents (cons value new-tail)]
                        (dc/resolve current-tail contents))))
    (reset! writer new-tail)
    writer))

(defn close! [writer]
  (dc/resolve (deref writer) nil))

(defn reduce*
  ([f seed coll]
   (let [return (jq/$deferred)]
     (reduce* return f seed coll)
     return))
  ([return f seed coll] (dc/done coll (fn [cell]
    (if (empty? cell)
      (jq/resolve return seed)
      (reduce* return f (f seed (first cell)) (rest cell)))))))
