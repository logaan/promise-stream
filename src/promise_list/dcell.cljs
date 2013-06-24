; A dcell is always wrapped in a deferred
(ns event-thread.dcell
  (:refer-clojure :exclude [cons])
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
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
  ([f r] (DCell. (deferred (cljs.core/cons f r)))))

(defn done [dcell callback]
  (jq/done (:deferred-wrapping-cell dcell) callback))

(defn resolve [dcell callback]
  (jq/resolve (:deferred-wrapping-cell dcell) callback))

(log "dcell")
(done (dcell)           (fn [v] (test true  (empty? v))))
(done (dcell 2)         (fn [v] (test false (empty? v))))
(done (dcell 2 (dcell)) (fn [v] (test false (empty? v))))

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

(log "first")
(jq/done (first (dcell 1)) (fn [f] (test 1 f)))

(log "rest")
(let [dlist (dcell 1 (dcell 2 (dcell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second (partial test 2)))

(defn cons [value coll]
  (dcell value coll))

(log "cons")
(let [dlist (cons 1 (cons 2 (cons 3 (dcell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third (partial test 3)))

; When you wait for the value you get a cell not a dcell.
(let [dlist (cons 1 (cons 2 (cons 3 (dcell))))]
  (done (rest dlist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      (test 3 (first three-onwards)))))))

(let [dlist            (cons 1 (dcell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  (fn [v] (test true (empty? v))))
  (jq/done value-beyond-end (fn [v] (test nil v))))

