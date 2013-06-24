(ns promise-list.dlist
  (:require [promise-list.dcell :as dc]))

(defn dlist [& values]
  (reduce (fn [coll v] (dc/dcell v coll)) (dc/dcell) values))

(defn productive-dlist []
  (atom (dc/container)))

(defn produce [productive-dlist value]
  (let [current-tail (deref productive-dlist)
        next-tail (dc/container)
        tail-cell (cons value next-tail)]
    (dc/resolve current-tail tail-cell)
    (reset! productive-dlist next-tail)))

(defn close [productive-dlist]
  (let [current-tail (deref productive-dlist)]
    (dc/resolve current-tail nil)))

