(ns promise-list.plist
  (:require [promise-list.pcell :as pc]
            [jayq.core :as jq]))

(defn closed-plist [& values]
  (reduce #(pc/closed-cell %2 %1) (pc/empty-cell) values))

(defn open-plist
  "Takes a seq of values packs those values into a plist. Returns a pair
  containing that plist and a writer that can be used to append to the list."
  [& values]
  (let [tail  (pc/open-container)
        plist (reduce #(pc/closed-cell %2 %1) tail values)]
    (list plist (atom tail))))

(defn append! [writer value]
  (let [tail-cell (pc/open-cell value)]
    (pc/resolve (deref writer) tail-cell)
    (reset! writer (rest tail-cell))))

(defn close! [writer]
  (pc/resolve (deref writer) nil))

(defn reduce*
  ([f seed coll]
   (let [return (jq/$deferred)]
     (reduce* return f seed coll)
     (jq/promise return)))
  ([return f seed coll] (pc/done coll (fn [cell]
    (if (empty? cell)
      (jq/resolve return seed)
      (reduce* return f (f seed (first cell)) (rest cell)))))))

(def map*)

(def concat*)

(def mapcat*)

(def plist-m
  {:return closed-plist
   :bind mapcat*
   :zero identity})

