(ns promise-stream.sources
  (:use [jayq.core          :only [on val]]
        [promise-stream.pcell :only [deferred]]
        [promise-stream.plist :only [with-open-plist append!]]))

(defn timestamp []
  (.valueOf (js/Date.)))

(defn metranome [interval]
  (with-open-plist (fn [writer]
    (js/window.setInterval
      #(append! writer (deferred {:time (timestamp)}))
      interval))))

(defn event-list [element event-type]
  (with-open-plist (fn [writer]
    (on element event-type (fn [event]
      (.preventDefault event)      
      (append! writer (deferred event)))))))

(defn callback->promise-stream [f & args]
  (with-open-plist (fn [writer]
    (apply f (concat args (list (fn [v]
      (append! writer (deferred v)))))))))

