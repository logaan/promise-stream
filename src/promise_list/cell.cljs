(ns event-thread.cell
  (:refer-clojure :exclude [first rest cons])
  (:use [event-thread.test :only [test]])
  (:require [jayq.core :as jq]))

(defn cell [f r]
  {:first f :rest r})

(defn end-cell
  ([] (end-cell nil nil))
  ([f r] (assoc (cell f r) :end true)))

(defn first [cell]
  (:first cell))

(defn rest [cell]
  (:rest cell))

(defn cons [value coll]
  (cell value coll))

(test 1 (first (rest (cons 2 (cons 1 (end-cell))))))

(defn end-cell? [cell]
  (true? (:end cell)))

(test true (end-cell? (rest (cons 1 (end-cell)))))

