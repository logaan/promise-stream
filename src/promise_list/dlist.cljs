(ns promise-list.dlist
  (:require [promise-list.dcell :as dc]))

(defn dlist [& values]
  (reduce #(dc/closed-cell %2 %1) (dc/empty-cell) values))

(defn productive-dlist []
  (atom (dc/open-container)))

(defn produce [productive-dlist value]
  (let [next-tail (dc/open-container)
        tail-cell (cons value next-tail)]
    (dc/resolve (deref productive-dlist) tail-cell)
    (reset! productive-dlist next-tail)))

(defn close [productive-dlist]
  (dc/resolve (deref productive-dlist) nil))

