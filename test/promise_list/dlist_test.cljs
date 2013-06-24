(ns promise-list.dlist-test
  (:use [promise-list.dlist :only [closed-dlist open-dlist append! close!]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]
            [promise-list.dcell :as dc]))

; dlist
(jq/done (first (rest (closed-dlist 1 2 3))) #(assert (= 2 %)))

; append!
(let [[reader writer] (open-dlist)]
  (jq/done (first reader) (fn [v] #(assert (= 1 %))))
  (append! writer 1))

; close!
(let [[reader writer] (open-dlist)]
  (dc/done reader #(assert (empty? %)))
  (close! writer))

