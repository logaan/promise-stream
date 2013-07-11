(ns promise-list.timing-aware
  (:use [promise-list.plist   :only
         [closed-plist map* mapd* concat* throttle* resolve-order-map*
          reductions* fmap promise filter*]]))

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
