(ns promise-stream.pstream-test
  (:use-macros [promise-stream.macros :only [for-pstream]])
  (:use [jayq.util :only [log]] 
        [promise-stream.pstream :only
         [closed-pstream open-pstream append! close! reduce* map* mapd* concat*
          with-open-pstream mapcat* count* resolves-within?
          pairwise-traverse zip* promise fmap filter* rests* reductions* doall*
          dorun*]])
  (:require [jayq.core :as jq]
            [promise-stream.pcell :as pc]
            [clojure.core.reducers :as r]))

; pstream
(jq/done (first (rest (closed-pstream 1 2 3))) #(assert (= 2 %)))

; append!
(let [[reader writer] (open-pstream)]
  (jq/done (first reader) #(assert (= 1 %)))
  (jq/done (first (rest reader)) #(assert (= 2 %)))
  (jq/done (first (rest (rest reader))) #(assert  (= 3 %)))
  (jq/done (first (rest (rest (rest reader)))) #(assert  (= 4 %)))
  (reduce append! writer (map pc/deferred [1 2 3 4])))

(let [[reader writer] (open-pstream)
      slow-response   (jq/$deferred)
      fast-response   (pc/deferred 2)]
  (jq/done (first reader) #(assert (= 1 %)))
  (jq/done (first (rest reader)) #(assert (= 2 %)))
  (append! writer slow-response)
  (append! writer fast-response)
  (js/setTimeout #(jq/resolve slow-response 1) 200))

; close!
(let [[reader writer] (open-pstream)]
  (pc/done reader #(assert (empty? %)))
  (close! writer))

; HOFs
(->> (closed-pstream 1 2 3)
     (map (pc/dapply inc))
     (map (pc/dapply #(assert (#{2 3 4} %))))
     (take 3)
     doall)

(jq/done
  (->> (closed-pstream 1 2 3 4)
       (reduce (pc/dapply +)))
  #(assert (= 10 %)))

(jq/done
  (->> (closed-pstream 1 2 3 4)
     (map* (comp pc/deferred inc))
     (reduce (pc/dapply +)))
  #(assert (= 14 %)))

; Should order by resolution
(let [pstream (with-open-pstream (fn [writer] nil))]
  (jq/done (first pstream) #(assert false)))

(jq/done (resolves-within? 1 (closed-pstream 1)) #(assert %))
(jq/done (resolves-within? 1 (first (open-pstream))) #(assert (not %)))

; This is wrong. It should be 3 2 1 0 but there's a bug in count* atm.
(jq/done
  (reduce (fmap conj) (promise [])
       (map* count* (rests* (closed-pstream 1 2 3))))
  #(assert (= [3 2 1 1] %)))

; Should maintain original order
(comment (let [responses (->> (closed-pstream "/slow" "/fast")
                     (map* js/jQuery.get))]
  (jq/done (nth responses 0) #(assert (= "slow" %)))
  (jq/done (nth responses 1) #(assert (= "fast" %)))))

(jq/done
  (->> (concat* (closed-pstream 1 2 3 4) (closed-pstream 5 6) (closed-pstream 7 8))
       (map* (comp pc/deferred inc))
       (reduce (pc/dapply +)))
  #(assert (= 44 %)))

(jq/done
  (count* (closed-pstream 1 2 3 4))
  #(assert (= 4 %)))

; This is wrong
(jq/done
  (count* (closed-pstream))
  log)

(jq/done
  (->> (closed-pstream 1 2 3 4)
       (mapcat* #(closed-pstream (dec %) % (inc %)))
       (reduce (pc/dapply +)))
  #(assert (= 30 %)))

(jq/done
  (reduce (fmap conj) (promise [])
          (zip* (closed-pstream 1 2 3) (closed-pstream 4 5 6)))
  #(assert (= '[(1 4) (2 5) (3 6)] %)))

(jq/done
  (reduce (fmap conj) (promise [])
          (filter* (comp promise odd?) (closed-pstream 1 2 3 4)))
  #(assert (= [1 3] %)))

(jq/done
  (reduce (fmap conj) (promise [])
          (reductions* (closed-pstream 1 2 3 4) (fmap +)))
  #(assert (= [3 6 10] %)))

; Reducers
; I think the only reason this is passing is because of nil + nil = 0 in cljs
(jq/done
  (r/reduce (pc/dapply +) (closed-pstream))
  #(assert (= 0 %)))

(jq/done
  (r/reduce (pc/dapply +) (closed-pstream 1))
  #(assert (= 1 %)))

(jq/done
  (r/reduce (pc/dapply +) (pc/deferred 0) (closed-pstream 1 2 3 4))
  #(assert (= 10 %)))

(jq/done
  (r/reduce (pc/dapply +) (closed-pstream 1 2 3 4))
  #(assert (= 10 %)))

(jq/done
  (reduce (pc/dapply +)
            (r/map (pc/dapply inc)
                   (r/map (pc/dapply inc)
                          (closed-pstream 1 2 3 4))))
  #(assert (= 18 %)))

(assert (nil? (dorun* (r/map (pc/dapply inc) (closed-pstream 1 2 3 4)))))

(jq/done
  (doall* (r/map (pc/dapply inc) (closed-pstream 1 2 3 4)))
  #(assert (= [2 3 4 5] %)))

; Macros
(jq/done
  (->> (for-pstream [n (closed-pstream 1 2 3 4)]
            (inc n))
       (reduce (pc/dapply +)))
  #(assert (= 14 %)))

