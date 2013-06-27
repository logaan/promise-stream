(ns promise-list.dlist
  (:require [promise-list.dcell :as dc]
            [jayq.core :as jq]))

(defn closed-dlist [& values]
  (reduce #(dc/closed-cell %2 %1) (dc/empty-cell) values))

(defn dcell-reduce [deferred coll f daccumulator]
  (let [dresult (f daccumulator (first coll))
        dtail   (rest coll)]
    (dc/done dtail (fn [tail]
      (if (empty? tail)
        (jq/done dresult (fn [result]
          (jq/resolve deferred result)))
        (dcell-reduce deferred dtail f dresult))))))

(extend-type dc/DCell
  IReduce
  (-reduce
    ([coll f]
     (-reduce coll (rest f) (first f)))
    ([coll f start]
     (let [response (jq/$deferred)]
       (dcell-reduce response coll f start)
       response))))

(defn open-dlist [& values]
  (let [tail  (dc/open-container)
        dlist (reduce #(dc/closed-cell %2 %1) tail values)]
    (list dlist (atom tail))))

(defn append! [writer dvalue]
  (let [current-tail @writer
        new-tail     (dc/DCell. (jq/$deferred))]
    (jq/done dvalue (fn [value]
                      (let [contents (cons value new-tail)]
                        (dc/resolve current-tail contents))))
    (reset! writer new-tail)
    writer))

(defn close! [writer]
  (dc/resolve (deref writer) nil))

