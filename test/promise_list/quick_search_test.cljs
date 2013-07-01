(ns promise-list.quick-search-test
  (:use [jayq.util :only [log]]
        [jayq.core :only [$ on val text remove append]]
        [promise-list.pcell :only [deferred]]
        [promise-list.plist :only [with-open-plist append! map* mapd* concat*]]))

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

(defn update-results-list [results]
  (remove ($ "#results li"))
  (doall (map #(append ($ :#results) (str "<li>" % "</li>")) results)))

(defn perform-search [query]
  (js/jQuery.getJSON (str "http://api.flickr.com/services/rest/?method=flickr.groups.search&api_key=e400c83e08716edc21ce04d19a71d697&text=" query "&per_page=10&format=json&jsoncallback=?")))

(defn group-names [response]
  (if-let [groups (aget response "groups")]
    (map #(aget % "name") (aget groups "group"))))

(let [changes   (event-list query-input "change")
      keyups    (event-list query-input "keyup")
      c-events  (concat* changes keyups)
      changes   (mapd* summarise c-events)
      queries   (mapd* :value changes)
      responses (map* perform-search queries)
      groups    (mapd* group-names responses)]
  (mapd* update-query-title queries)
  (mapd* update-results-list groups))

