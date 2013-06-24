(ns promise-list.dcell-test
  (:use [promise-list.dcell :only [dcell done]]
        [promise-list.test  :only [test]])
  (:require [jayq.core :as jq]))

; first
(jq/done (first (dcell 1)) (fn [f] (test 1 f)))

; rest
(let [dlist (dcell 1 (dcell 2 (dcell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second (partial test 2)))

; dcell
(done (dcell)           (fn [v] (test true  (empty? v))))
(done (dcell 2)         (fn [v] (test false (empty? v))))
(done (dcell 2 (dcell)) (fn [v] (test false (empty? v))))

(let [dlist (dcell 1 (dcell 2 (dcell 3 (dcell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third (partial test 3)))

(let [dlist (dcell 1 (dcell 2 (dcell 3 (dcell))))]
  (done (rest dlist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      (test 3 (first three-onwards)))))))

(let [dlist            (dcell 1 (dcell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  (fn [v] (test true (empty? v))))
  (jq/done value-beyond-end (fn [v] (test nil v))))
