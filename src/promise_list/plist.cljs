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

(defn kittens [parent vf ef]
  (fn [cell]
    (if (empty? cell)
      (ef)
      (do 
        (vf (first cell))
        (parent (rest cell) vf ef)))))

(defn traverse [coll vf ef]
  (pc/done coll (kittens traverse vf ef)))

(defn with-open-plist [f]
  (let [[reader writer] (open-plist)]
    (f writer)
    reader))

; Here to prevent closing over head
(defn modifying-appender [writer f]
  (fn [v] (append! writer (f v))))

; Here to prevent closing over head
(defn closer [writer]
  (fn [] (close! writer)))

(defn map* [f coll]
  (with-open-plist (fn [writer]
    (traverse coll (modifying-appender writer f) (closer writer)))))

(defn mapd* [f coll]
  (map* (comp pc/deferred f) coll))

(defn close-if-complete [completed-colls total-colls writer]
  (fn []
    (swap! completed-colls inc)
    (if (= @total-colls @completed-colls)
      (do
        (close! writer)))))

(defn puppies [total-colls]
  (fn [v] (reset! total-colls v)))

(defn co-operative-close [total-colls-future writer callback]
  (let [total-colls     (atom nil)
        completed-colls (atom 0)
        close-function  (close-if-complete completed-colls total-colls writer)]
  (jq/done total-colls-future (puppies total-colls))
  (callback close-function)))

(defn concat* [& colls]
  (with-open-plist (fn [writer]
    (co-operative-close (pc/deferred (count colls)) writer (fn [close]
      (doall
        (map (fn [coll] (traverse coll (modifying-appender writer pc/deferred) close))
             colls)))))))

; Here to prevent closing over head
(def tallier
  (fn [tally v] (inc tally)))

(defn count* [coll]
  (reduce (pc/dapply tallier) (pc/deferred 0) coll))

(defn inner-list-traverser [writer close]
  (fn [inner-list]
    (traverse inner-list (modifying-appender writer pc/deferred) close)))

(defn mapcat* [f coll]
  (with-open-plist (fn [writer]
    (co-operative-close (count* coll) writer (fn [close]
      (let [list-of-lists  (map* (comp pc/deferred f) coll)]
        (map* (inner-list-traverser writer close) list-of-lists)))))))

(def plist-m
  {:return closed-plist
   :bind   #(mapcat* %2 %1)
   :zero   identity})

