; A dcell is always wrapped in a deferred
(ns event-thread.dcell
  (:refer-clojure :exclude [first rest cons])
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]
            [event-thread.cell :as c]))

(defn deferred
  ([] (jq/$deferred))
  ([value] (jq/resolve (deferred) value)))

(defn dcell
  "No arguments gives an empty end cell. One argument is a cell with a value
  but no tail yet. Two arguments is a complete cell with value and tail."
  ([]
   (let [self-deferred (deferred)
         new-cell      (c/end-cell nil self-deferred)]
     (jq/resolve self-deferred new-cell)
     self-deferred))
  ([f] (dcell f (deferred)))
  ([f r] (deferred (c/cell f r))))

(jq/done (dcell)           (fn [v] (test true  (c/end-cell? v))))
(jq/done (dcell 2)         (fn [v] (test false (c/end-cell? v))))
(jq/done (dcell 2 (dcell)) (fn [v] (test false (c/end-cell? v))))

(defn first [dcell]
  (let [first-deferred (deferred)]
    (jq/done dcell (fn [cell]
      (jq/resolve first-deferred (c/first cell))))
    first-deferred))

(jq/done (first (dcell 1)) (fn [f] (test 1 f)))

; The problem with this is that rest returns a future with a cell rather than a
; cell directly. But perhaps that's un-nessisary... because a cell is already
; wrapped in a future.
(defn rest [dcell]
  (let [rest-deferred (deferred)]
    (jq/done dcell (fn [cell]
      (jq/done (c/rest cell) (fn [rest-cell]
        (jq/resolve rest-deferred rest-cell)))))
    rest-deferred))

(let [dlist (dcell 1 (dcell 2 (dcell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second (partial test 2)))

(defn cons [value coll]
  (dcell value coll))

(let [dlist (cons 1 (cons 2 (cons 3 (dcell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third (partial test 3)))

; When you wait for the value you get a cell not a dcell.
(let [dlist (cons 1 (cons 2 (cons 3 (dcell))))]
  (jq/done (rest dlist) (fn [two-onwards]
    (jq/done (c/rest two-onwards) (fn [three-onwards]
      (test 3 (c/first three-onwards)))))))

(let [dlist            (cons 1 (dcell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (jq/done value-beyond-end (fn [v] (test nil v)))
  (jq/done list-beyond-end  (fn [v] (test true (c/end-cell? v)))))

