(ns promise-stream.plist
  (:require [promise-stream.pcell :as pc]
            [jayq.core :as jq]))

; Misleading as it's just returning a resolved deferred.
(def promise pc/deferred)

; Misleading as it's fmap within the promise domain, not plist.
(def fmap pc/dapply)

(defn closed-plist
  "Returns a read only promise list containing args. Mapping directly over a
  closed plist will cause a stack overflow if it contains more than 1k values."
  [& args]
  (reduce #(pc/closed-cell %2 %1) (pc/empty-cell) (reverse args)))

(defn reduce*
  "Not intended to be used directly. Instead use IReduce's -reduce."
  [deferred coll f daccumulator]
  (let [dresult (f daccumulator (first coll))
        dtail   (rest coll)]
    (pc/done dtail (fn [tail]
      (if (empty? tail)
        (jq/done dresult (fn [result]
          (jq/resolve deferred result)))
        (reduce* deferred dtail f dresult))))))

(extend-type pc/PCell
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

; Can probably abstract away the variable column thing
(defn traverse
  "vf will be called with every value. ef will be called at the end of the
  list."
  ([coll vf ef]
   (pc/done coll (fn [cell]
     (if (empty? cell)
       (ef)
       (do 
         (vf (first cell))
         (traverse (rest cell) vf ef))))))
  ([coll1 coll2 vf ef]
    (pc/done coll1 (fn [cell1]
      (pc/done coll2 (fn [cell2]
        (if (or (empty? cell1) (empty? cell2))
          (ef)
          (do
            (vf (first cell1) (first cell2))
            (traverse (rest coll1) (rest coll2) vf ef)))))))))

(defn with-open-plist [f]
  (let [[reader writer] (open-plist)]
    (f writer)
    reader))

; These three generate functions to avoid having them close over the heads of
; lists and cause a memory leak.
(defn modifying-appender [writer f]
  (fn [v] (append! writer (f v))))

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

(defn co-operative-close [total-colls-future writer callback]
  (let [total-colls     (atom nil)
        completed-colls (atom 0)
        close-function  (close-if-complete completed-colls total-colls writer)]
  (jq/done total-colls-future #(reset! total-colls %))
  (callback close-function)))

(defn concat* [& colls]
  (with-open-plist (fn [writer]
    (co-operative-close (pc/deferred (count colls)) writer (fn [close]
      (doall
        (map (fn [coll] (traverse coll (modifying-appender writer pc/deferred) close))
             colls)))))))

(defn count* [coll]
  (reduce (pc/dapply (fn [tally v] (inc tally))) (pc/deferred 0) coll))

(defn mapcat* [f coll]
  (with-open-plist (fn [writer]
    (co-operative-close (count* coll) writer (fn [close]
      (let [list-of-lists  (map* (comp pc/deferred f) coll)]
        (map* (fn [inner-list]
                (traverse inner-list (modifying-appender writer pc/deferred) close))
              list-of-lists)))))))

; Exists to avoid closing over coll
(defn pair-adder [writer]
  (fn [v1 v2]
    (append! writer (promise (list v1 v2)))))

(defn zip* [coll1 coll2]
  (with-open-plist (fn [writer]
    (traverse coll1 coll2 (pair-adder writer) (closer writer)))))

; Exists to avoid closing over coll
(defn conditional-adder [pred writer]
  (fn [v]
    (jq/done (pred v) (fn [passes?]
      (if passes? (append! writer (promise v)))))))

(defn filter*
  "returns a new plist of values from coll for which pred returns truthy. pred
  must take a concrete value and return a promise. coll is a plist."
  [pred coll]
  (with-open-plist (fn [writer]
    (traverse coll (conditional-adder pred writer) (closer writer)))))

(defn rests*
  "Returns a plist containing coll, followed by (rest coll) then (rest (rest
  coll)) etc."
  ([coll]
   (with-open-plist (fn [writer]
     (append! writer (promise coll))
     (rests* writer coll))))
  ([writer coll]
   (pc/done coll (fn [cell]
     (if (empty? cell)
       (close! writer)
       (let [tail (rest cell)]
         (append! writer (promise tail))
         (rests* writer tail)))))))

(defn resolves-within? [timeout plist]
  (let [result (jq/$deferred)]
    (js/setTimeout #(jq/resolve result false) timeout)
    (pc/done plist #(jq/resolve result true))
    result))

; Exists to avoid closing voer coll
(defn after-resolver [timeout]
  (fn [[v tail]]
   ((fmap not) (resolves-within? timeout tail))))

(defn throttle* [timeout coll]
  (mapd* first (filter* (after-resolver timeout) (zip* coll (rest (rests* coll))))))

; Should this be emitting the first value?
(defn reductions*
  "Calls f with two promises and expects f to return a promise."
  ([coll f]
   (reductions* (rest coll) f (first coll)))
  ([coll f start]
   (with-open-plist (fn [writer]
     (reductions* writer coll f start))))
  ([writer coll f daccumulator]
   (let [dresult (f daccumulator (first coll))
         dtail   (rest coll)]
     (append! writer dresult)
     (pc/done dtail (fn [tail]
       (if (empty? tail)
         (close! writer)
         (reductions* writer dtail f dresult)))))))

(defn dorun* [plist]
  (reduce (fn [_ _] (jq/$deferred)) plist)
  nil)

(defn doall* [plist]
  (reduce (fmap conj) (promise []) plist))

(def plist-m
  {:return closed-plist
   :bind   #(mapcat* %2 %1)
   :zero   identity})

