(ns promise-list.dlist-test
  (:use [promise-list.dlist :only [dlist productive-dlist produce close]]
        [promise-list.test  :only [test]])
  (:require [jayq.core :as jq]
            [promise-list.dcell :as dc]))

; dlist
(jq/done (first (rest (dlist 1 2 3))) (fn [f]
  (test 2 f)))

; produce
(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done (first reader) (fn [v] (test 1 v)))
  (produce writer 1))

; close
(let [writer (productive-dlist)
      reader (deref writer)]
  (dc/done reader (fn [v] (test true (empty? v))))
  (close writer))

