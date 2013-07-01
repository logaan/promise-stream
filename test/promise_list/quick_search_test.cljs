(ns promise-list.quick-search-test
  (:use [jayq.util :only [log]]
        [jayq.core :only [$ on val text remove append]]
        [promise-list.pcell :only [deferred]]
        [promise-list.plist :only [with-open-plist append! map* mapd* concat*]]))

(defn event-list [element event-type]
  (with-open-plist (fn [writer]
    (on element event-type (fn [event]
      (append! writer (deferred event)))))))

(defn summarise [event]
  (let [target (aget event "target")]
    {:type   (aget event "type")
     :time   (aget event "timeStamp")
     :target target
     :value  (aget target "value")}))

(defn transparent-log [v]
  (log (clj->js v))
  v)

(defn perform-search [query]
  (js/jQuery.getJSON (str "http://api.flickr.com/services/rest/?method=flickr.groups.search&api_key=e400c83e08716edc21ce04d19a71d697&text=" query "&per_page=10&format=json&jsoncallback=?")))

(defn group-names [response]
  (if-let [groups (aget response "groups")]
    (map #(aget % "name") (aget groups "group"))))

(defn update-results-list [results]
  (remove ($ "#results li"))
  (mapv #(append ($ :#results) (str "<li>" % "</li>")) results))

(let [changes   (event-list ($ :#query) "change")
      keyups    (event-list ($ :#query) "keyup")
      events    (concat* changes keyups)
      queries   (mapd* (comp :value summarise) events)
      responses (map*  perform-search queries)
      groups    (mapd* group-names responses)]
  (mapd* (partial text ($ :#query-title)) queries)
  (mapd* update-results-list groups))

