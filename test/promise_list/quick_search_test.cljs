(ns promise-list.quick-search-test
  (:use [jayq.util :only [log]]
        [jayq.core :only [$ on val text]]
        [promise-list.pcell :only [deferred]]
        [promise-list.plist :only [with-open-plist append! mapd* concat*]]))

(defn summarise [event]
  (let [target (aget event "target")]
    {:type   (aget event "type")
     :time   (aget event "timeStamp")
     :target target
     :value  (aget target "value")}))

(defn event-list [element event-type]
  (with-open-plist (fn [writer]
    (on element event-type (fn [event]
      (append! writer (deferred event)))))))

(defn transparent-log [v]
  (log (clj->js v))
  v)

(def query-input ($ :#query))
(def query-title ($ :#query-title))

(defn update-query-title [new-title]
  (text query-title new-title))

(let [changes  (event-list query-input "change")
      keyups   (event-list query-input "keyup")
      q-events (concat* changes keyups)
      queries  (mapd* summarise q-events)
      qtitles  (mapd* :value queries)]
  (mapd* update-query-title qtitles)
  (mapd* transparent-log qtitles))

