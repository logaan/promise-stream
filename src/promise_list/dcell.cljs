(ns promise-list.dcell
  (:require [jayq.core :as jq]))

(defn deferred
  ([] (jq/$deferred))
  ([value] (jq/resolve (deferred) value)))

(defrecord DCell [deferred-wrapping-cell])

(defn dcell
  "No arguments gives an empty end cell. One argument is a cell with a value
  but no tail yet. Two arguments is a complete cell with value and tail."
  ([] (DCell. (deferred nil)))
  ([f] (dcell f (DCell. (deferred))))
  ([f r] (DCell. (deferred (cons f r)))))

(defn done [dcell callback]
  (jq/done (:deferred-wrapping-cell dcell) callback))

(defn resolve [dcell callback]
  (jq/resolve (:deferred-wrapping-cell dcell) callback))

(extend-type DCell
  ISeq
  (-first [dcell]
    (let [first-deferred (deferred)]
      (done dcell (fn [cell]
        (jq/resolve first-deferred (first cell))))
      first-deferred))
  (-rest [dcell]
    (let [rest-deferred (deferred)]
      (done dcell (fn [cell]
        (let [tail (rest cell)]
          (if (empty? tail)
            (jq/resolve rest-deferred nil)
            (done (rest cell) (fn [rest-cell]
                                (jq/resolve rest-deferred rest-cell)))))))
      (DCell. rest-deferred))))

