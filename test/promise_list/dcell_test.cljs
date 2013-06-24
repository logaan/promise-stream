(ns promise-list.dcell-test
  (:use [promise-list.dcell :only [closed-cell dcell done]]
        [promise-list.test  :only [test]])
  (:require [jayq.core :as jq]))

; first
(jq/done (first (dcell 1)) (fn [f] (test 1 f)))

; rest
(let [dlist (dcell 1 (dcell 2 (closed-cell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second (partial test 2)))

; dcell
(done (closed-cell)           (fn [v] (test true  (empty? v))))
(done (dcell 2)               (fn [v] (test false (empty? v))))
(done (dcell 2 (closed-cell)) (fn [v] (test false (empty? v))))

(let [dlist (dcell 1 (dcell 2 (dcell 3 (closed-cell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third (partial test 3)))

(let [dlist (dcell 1 (dcell 2 (dcell 3 (closed-cell))))]
  (done (rest dlist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      (test 3 (first three-onwards)))))))

(let [dlist            (dcell 1 (closed-cell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  (fn [v] (test true (empty? v))))
  (jq/done value-beyond-end (fn [v] (test nil v))))
