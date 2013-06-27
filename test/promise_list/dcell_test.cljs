(ns promise-list.dcell-test
  (:use [promise-list.dcell :only [closed-cell empty-cell dcell done]])
  (:require [jayq.core :as jq]))

; first
(jq/done (first (closed-cell 1 (empty-cell))) #(assert (= 1 %)))

; rest
(let [dlist (closed-cell 1 (closed-cell 2 (empty-cell)))
      deferred-second (first (rest dlist))]
  (jq/done deferred-second #(assert (= 2 %))))

; dcell
(done (empty-cell) #(assert (empty? %)))
(done (closed-cell 2 (empty-cell)) #(assert (seq %)))

(let [dlist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))
      deferred-third (first (rest (rest dlist)))]
  (jq/done deferred-third #(assert (= 3 %))))

(let [dlist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))]
  (done (rest dlist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      #(assert (= 3 (first three-onwards))))))))

(let [dlist            (closed-cell 1 (empty-cell))
      list-beyond-end  (rest (rest dlist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  #(assert (empty? %)))
  (jq/done value-beyond-end #(assert (nil? %))))
