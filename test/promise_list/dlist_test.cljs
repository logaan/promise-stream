(ns promise-list.plist-test
  (:use [promise-list.plist :only [closed-plist open-plist append! close! reduce*]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]
            [promise-list.pcell :as pc]))

; plist
(jq/done (first (rest (closed-plist 1 2 3))) #(assert (= 2 %)))

; append!
(let [[reader writer] (open-plist)]
  (jq/done (first reader) (fn [v] #(assert (= 1 %))))
  (append! writer 1))

; close!
(let [[reader writer] (open-plist)]
  (pc/done reader #(assert (empty? %)))
  (close! writer))

; HOFs
(->> (closed-plist 1 2 3)
     (map (pc/dapply inc))
     (map (pc/dapply #(assert (#{2 3 4} %))))
     (take 3)
     doall)

(jq/done (reduce* + 0 (closed-plist 1 2)) #(assert (= 3 %)))

