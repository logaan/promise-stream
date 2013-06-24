(ns promise-list.dcell-test
  (:use [promise-list.dcell :only [closed-cell empty-cell dcell done]]
        [promise-list.test  :only [test]])
  (:require [jayq.core :as jq]))

; first
(jq/done (first (closed-cell 1 (empty-cell))) (fn [f] (test 1 f)))

; rest
(let [dlist (closed-cell 1 (closed-cell 2 (empty-cell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second (partial test 2)))

; dcell
(done (empty-cell)           (fn [v] (test true  (empty? v))))
(done (closed-cell 2 (empty-cell)) (fn [v] (test false (empty? v))))

(let [dlist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third (partial test 3)))

(let [dlist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))]
  (done (rest dlist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      (test 3 (first three-onwards)))))))

(let [dlist            (closed-cell 1 (empty-cell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  (fn [v] (test true (empty? v))))
  (jq/done value-beyond-end (fn [v] (test nil v))))
