(ns promise-list.plist
  (:require [promise-list.pcell :as pc]
            [jayq.core :as jq]))

(defn closed-plist [& values]
  (reduce #(pc/closed-cell %2 %1) (pc/empty-cell) (reverse values)))

(defn reduce* [deferred coll f daccumulator]
  (let [dresult (f daccumulator (first coll))
        dtail   (rest coll)]
    (pc/done dtail (fn [tail]
      (if (empty? tail)
        (jq/done dresult (fn [result]
          (jq/resolve deferred result)))
        (reduce* deferred dtail f dresult))))))

(extend-type pc/DCell
  IReduce
  (-reduce
    ([coll f]
     (-reduce (rest coll) f (first coll)))
    ([coll f start]
     (let [response (jq/$deferred)]
       (reduce* response coll f start)
       response))))

(defn open-plist [& values]
  (let [tail  (pc/open-container)
        plist (reduce #(pc/closed-cell %2 %1) tail values)]
    (list plist (atom tail))))

(defn append! [writer dvalue]
  (let [current-tail @writer
        new-tail     (pc/open-container)]
    (jq/done dvalue (fn [value]
                      (let [contents (cons value new-tail)]
                        (pc/resolve current-tail contents))))
    (reset! writer new-tail)
    writer))

(defn close! [writer]
  (pc/resolve (deref writer) nil))

(defn traverse
  "vf will be called with every value. ef will be called at the end of the
  list."
  [coll vf ef]
   (pc/done coll (fn [cell]
     (if (empty? cell)
       (ef)
       (do 
         (vf (first cell))
         (traverse (rest cell) vf ef))))))

(defn with-open-plist [f]
  (let [[reader writer] (open-plist)]
    (f writer)
    reader))

(defn map* [f coll]
   (with-open-plist (fn [writer]
     (traverse coll #(append! writer (f %)) #(close! writer)))))

(defn close-if-complete [completed-colls total-colls writer]
  (fn []
    (swap! completed-colls inc)
    (if (= @total-colls @completed-colls)
      (do
        (close! writer)))))

(defn concat* [& colls]
  (with-open-plist (fn [writer]
    (let [total-colls     (atom (count colls))
          completed-colls (atom 0)]
      (doall
        (map
          (fn [coll]
            (traverse coll
                      #(append! writer (pc/deferred %))
                      (close-if-complete completed-colls total-colls writer))) colls))))))

(defn count* [coll]
  (reduce (pc/dapply (fn [tally v] (inc tally))) (pc/deferred 0) coll))

(defn mapcat* [f coll]
  (with-open-plist (fn [writer]
    (let [total-colls     (atom nil)
          completed-colls (atom 0)
          list-of-lists   (map* (comp pc/deferred f) coll)]
      (jq/done (count* coll) #(reset! total-colls %))
      (map* (fn [inner-list]
              (traverse inner-list
                        #(append! writer (pc/deferred %))
                        (close-if-complete completed-colls total-colls writer)))
            list-of-lists)))))

(def plist-m
  {:return closed-plist
   :bind   #(mapcat* %2 %1)
   :zero   identity})

