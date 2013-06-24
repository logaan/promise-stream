(ns promise-list.dlist
  (:use [jayq.util :only [log]]) 
  (:require [promise-list.dcell :as dc]))

(defn closed-dlist [& values]
  (reduce #(dc/closed-cell %2 %1) (dc/empty-cell) values))

(defn open-dlist
  "Takes a seq of values packs those values into a dlist. Returns a pair
  containing that dlist and a writer that can be used to append to the list."
  [& values]
  (let [tail  (dc/open-container)
        dlist (reduce #(dc/closed-cell %2 %1) tail values)]
    (list dlist (atom tail))))

(defn append! [writer value]
  (let [tail-cell (dc/open-cell value)]
    (dc/resolve (deref writer) tail-cell)
    (reset! writer (rest tail-cell))))

(defn close! [writer]
  (dc/resolve (deref writer) nil))
