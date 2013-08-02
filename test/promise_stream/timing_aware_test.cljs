(ns promise-stream.timing-aware-test
  (:use [promise-stream.pstream :only [closed-pstream]]
        [promise-stream.timing-aware :only [resolve-order-map*]])
  (:require [jayq.core :as jq]))

(let [responses (->> (closed-pstream "/slow" "/fast")
                     (resolve-order-map* js/jQuery.get))]
  (jq/done (first responses) #(assert (= "fast" %)))
  (jq/done (first (rest responses)) #(assert (= "slow" %))))
