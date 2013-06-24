(ns event-thread.dlist
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [event-thread.dcell :as dc]
            [event-thread.cell :as c]
            [jayq.core :as jq]))

(defn dlist [& values]
  (reduce (fn [coll v] (dc/cons v coll)) (dc/dcell) values))

(jq/done (dc/first (dc/rest (dlist 1 2 3))) (fn [f]
  (test 2 f)))

(defn productive-dlist []
  (atom (dc/deferred)))

(defn produce [productive-dlist value]
  (let [current-tail (deref productive-dlist)
        next-tail (dc/deferred)
        tail-cell (c/cell value next-tail)]
    (jq/resolve current-tail tail-cell)
    (reset! productive-dlist next-tail)))

(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done (dc/first reader) (fn [v] (test 1 v)))
  (produce writer 1))

(defn close [productive-dlist]
  (let [current-tail (deref productive-dlist)]
    (jq/resolve current-tail (c/end-cell nil current-tail))))

(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done reader (fn [v] (test true (c/end-cell? v))))
  (close writer))

