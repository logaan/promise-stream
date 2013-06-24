(ns promise-list.dlist-test
  (:use [promise-list.dlist :only [dlist productive-dlist produce close]])
  (:require [jayq.core :as jq]
            [promise-list.dcell :as dc]))

; dlist
(jq/done (first (rest (dlist 1 2 3))) #(assert (= 2 %)))

; produce
(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done (first reader) (fn [v] #(assert (= 1 %))))
  (produce writer 1))

; close
(let [writer (productive-dlist)
      reader (deref writer)]
  (dc/done reader #(assert (empty? %)))
  (close writer))

