(ns promise-list.sources
  (:use [jayq.core          :only [on val]]
        [promise-list.pcell :only [deferred]]
        [promise-list.plist :only [with-open-plist append!]]))

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
