(ns promise-list.pcell-test
  (:use [promise-list.pcell :only [closed-cell empty-cell pcell done]])
  (:require [jayq.core :as jq]))

; first
(jq/done (first (closed-cell 1 (empty-cell))) #(assert (= 1 %)))

; rest
(let [plist (closed-cell 1 (closed-cell 2 (empty-cell)))
      deferred-second (first (rest plist))]
  (jq/done deferred-second #(assert (= 2 %))))

; pcell
(done (empty-cell) #(assert (empty? %)))
(done (closed-cell 2 (empty-cell)) #(assert (seq %)))

(let [plist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))
      deferred-third (first (rest (rest plist)))]
  (jq/done deferred-third #(assert (= 3 %))))

(let [plist (closed-cell 1 (closed-cell 2 (closed-cell 3 (empty-cell))))]
  (done (rest plist) (fn [two-onwards]
    (done (rest two-onwards) (fn [three-onwards]
      #(assert (= 3 (first three-onwards))))))))

(let [plist            (closed-cell 1 (empty-cell))
      list-beyond-end  (rest (rest plist))
      value-beyond-end (first list-beyond-end)]
  (done    list-beyond-end  #(assert (empty? %)))
  (jq/done value-beyond-end #(assert (nil? %))))
