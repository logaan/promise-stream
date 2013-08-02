(ns promise-list.timing-aware
  (:use [promise-list.plist :only
         [with-open-plist co-operative-close count* traverse closed-plist map*
          mapd* concat* throttle* reductions* fmap promise
          filter* append!]])
  (:require [jayq.core :as jq]))

(defn resolve-order-modifying-appender [writer f close]
  (fn [v]
    (jq/done (f v) (fn [r]
      (append! writer (promise r))
      (close)))))

(defn resolve-order-map* [f coll]
  (with-open-plist (fn [writer]
    (co-operative-close (count* coll) writer (fn [close]
    (traverse coll (resolve-order-modifying-appender writer f close) identity))))))

(defn stamp-with-request-time [f]
  (fn [v]
    (let [output        (jq/$deferred)
          presponse     (f v)
          original-time (.valueOf (new js/Date))]
    (jq/done presponse (fn [response]
      (jq/resolve output {:originalTime original-time
                          :response     response})))
    output)))

(defn most-recently-requested-with-current [{mrr :mrr} current]
  (if (< (:originalTime mrr) (:originalTime current))
    {:mrr current :current current}
    {:mrr mrr     :current current}))

(defn most-recently-requested? [{:keys [mrr current]}]
  (promise (= mrr current)))

(defn strip-mrr-and-times [mrr-and-current]
  (-> mrr-and-current :current :response))

(defn keep-most-recently-requested [times-and-responses-coll]
  (mapd* strip-mrr-and-times
         (filter* most-recently-requested?
                  (reductions* times-and-responses-coll
                               (fmap most-recently-requested-with-current)
                               (promise {:mrr {:originalTime 0}})))))

