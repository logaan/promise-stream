(ns promise-stream.sources
  (:use [jayq.core          :only [on val]]
        [promise-stream.pcell :only [deferred]]
        [promise-stream.pstream :only [with-open-pstream append!]]))

(defn timestamp []
  (.valueOf (js/Date.)))

(defn metranome [interval]
  (with-open-pstream (fn [writer]
    (js/window.setInterval
      #(append! writer (deferred {:time (timestamp)}))
      interval))))

(defn event-stream [element event-type]
  (with-open-pstream (fn [writer]
    (on element event-type (fn [event]
      (.preventDefault event)      
      (append! writer (deferred event)))))))

(defn callback->promise-stream [f & args]
  (with-open-pstream (fn [writer]
    (apply f (concat args (list (fn [v]
      (append! writer (deferred v)))))))))

