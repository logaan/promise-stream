(ns event-thread.dlist
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [event-thread.dcell :as dc]
            [event-thread.cell :as c]
            [jayq.core :as jq]))

(defn dlist [& values]
  (reduce (fn [coll v] (dc/cons v coll)) (dc/dcell) values))

(log "dlist")
(jq/done (first (rest (dlist 1 2 3))) (fn [f]
  (test 2 f)))

(defn productive-dlist []
  (atom (dc/DCell. (dc/deferred))))

(defn produce [productive-dlist value]
  (let [current-tail (deref productive-dlist)
        next-tail (dc/deferred)
        tail-cell (c/cell value next-tail)]
    (dc/resolve current-tail tail-cell)
    (reset! productive-dlist next-tail)))

(log "produce")
(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done (first reader) (fn [v] (test 1 v)))
  (produce writer 1))

(defn close [productive-dlist]
  (let [current-tail (deref productive-dlist)]
    (dc/resolve current-tail (c/end-cell nil current-tail))))

(log "close")
(let [writer (productive-dlist)
      reader (deref writer)]
  (dc/done reader (fn [v] (test true (c/end-cell? v))))
  (close writer))

