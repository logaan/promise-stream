(ns promise-list.plist-test
  (:use [promise-list.plist :only
         [closed-plist open-plist append! close! reduce* map*]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]
            [promise-list.pcell :as pc]
            [clojure.core.reducers :as r]))

; plist
(jq/done (first (rest (closed-plist 1 2 3))) #(assert (= 2 %)))

; append!
(let [[reader writer] (open-plist)]
  (jq/done (first reader) #(assert (= 1 %)))
  (jq/done (first (rest reader)) #(assert (= 2 %)))
  (jq/done (first (rest (rest reader))) #(assert  (= 3 %)))
  (jq/done (first (rest (rest (rest reader)))) #(assert  (= 4 %)))
  (reduce append! writer (map pc/deferred [1 2 3 4])))

(let [[reader writer] (open-plist)
      slow-response   (jq/$deferred)
      fast-response   (pc/deferred 2)]
  (jq/done (first reader) #(assert (= 1 %)))
  (jq/done (first (rest reader)) #(assert (= 2 %)))
  (append! writer slow-response)
  (append! writer fast-response)
  (js/setTimeout #(jq/resolve slow-response 1) 200))

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

(jq/done
  (->> (closed-plist 1 2 3 4)
       (reduce (pc/dapply +)))
  #(assert (= 10 %)))

(jq/done
  (->> (closed-plist 1 2 3 4)
     (map* (comp pc/deferred inc))
     (reduce (pc/dapply +)))
  #(assert (= 14 %)))

; Reducers
(jq/done
  (r/reduce (pc/dapply +) (closed-plist))
  #(assert (= 0 %)))

(jq/done
  (r/reduce (pc/dapply +) (closed-plist 1))
  #(assert (= 1 %)))

(jq/done
  (r/reduce (pc/dapply +) (pc/deferred 0) (closed-plist 1 2 3 4))
  #(assert (= 10 %)))

(jq/done
  (r/reduce (pc/dapply +) (closed-plist 1 2 3 4))
  #(assert (= 10 %)))

(jq/done
  (r/reduce (pc/dapply +)
            (r/map (pc/dapply inc)
                   (r/map (pc/dapply inc)
                          (closed-plist 1 2 3 4))))
  #(assert (= 18 %)))

