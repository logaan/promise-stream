; I use container to refer to a wrapped deferred that doesn't contain a cons
; cell yet. Open means that the deferred has not been resolved yet. Closed
; means that the deferred has been resolved. An empty cell can be used to
; terminate a list.
(ns promise-list.dcell
  (:require [jayq.core :as jq]))

(defn promise [value]
  (jq/promise (jq/resolve (jq/$deferred) value)))

(defrecord DCell [deferred-wrapping-cell])

(defn closed-container [v]
  (DCell. (promise v)))

(defn open-container []
  (DCell. (jq/$deferred)))

(defn empty-cell []
  (closed-container nil))

(defn open-cell [v]
  (closed-container (cons v (open-container))))

(defn closed-cell [v1 v2]
  (closed-container (cons v1 v2)))

(defn done [dcell callback]
  (jq/done (:deferred-wrapping-cell dcell) callback))

(defn resolve [dcell callback]
  (jq/resolve (:deferred-wrapping-cell dcell) callback))

(extend-type DCell
  ISeq
  (-first [dcell]
    (let [first-deferred (jq/$deferred)]
      (done dcell (fn [cell]
        (jq/resolve first-deferred (first cell))))
      (jq/promise first-deferred)))
  (-rest [dcell]
    (let [rest-deferred (jq/$deferred)]
      (done dcell (fn [cell]
        (let [tail (rest cell)]
          (if (empty? tail)
            (jq/resolve rest-deferred nil)
            (done (rest cell) (fn [rest-cell]
                                (jq/resolve rest-deferred rest-cell)))))))
      (DCell. (jq/promise rest-deferred))))
  
  ISeqable
  (-seq [this] this))

(defn dapply
  "fmap"
  [f]
  (fn [d]
    (let [new-d (jq/$deferred)]
      (jq/done d (fn [v]
        (jq/resolve new-d (f v))))
      (jq/promise new-d))))
