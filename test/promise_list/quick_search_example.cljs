(ns promise-list.quick-search-test
  (:use [jayq.util            :only [log]]
        [jayq.core            :only [$ text remove append]]
        [promise-list.sources :only [metranome event-list]]
        [promise-list.plist   :only [closed-plist map* mapd* concat* throttle*]]))

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
  (js/jQuery.getJSON (str "http://api.flickr.com/services/rest/?method=flickr.groups.search&api_key=a567ef852bac562934904a2a0b548a5b&text=" query "&per_page=10&format=json&jsoncallback=?")))

(defn group-names [response]
  (if-let [groups (aget response "groups")]
    (map #(aget % "name") (aget groups "group"))))

(defn set-query-title! [new-title]
  (text ($ :#query-title) new-title))

(defn set-results-list! [results]
  (remove ($ "#results li"))
  (mapv #(append ($ :#results) (str "<li>" % "</li>")) results))

((fn []
  (let [changes   (event-list ($ :#query) "change")
        keyups    (event-list ($ :#query) "keyup")
        events    (concat* changes keyups)
        queries   (mapd* (comp :value summarise) events)
        throttled (throttle* 400 queries)
        responses (map*  perform-search throttled)
        groups    (mapd* group-names responses)]
    (mapd* set-query-title!  throttled)
    (mapd* set-results-list! groups))))

; Memory leak tests
(comment
  ((fn []
     (let [threes (metranome 300)
           fives  (metranome 500)
           all    (concat* threes fives)
           times  (mapd* (comp str :time) all)]
       (mapd* set-query-title! times))))

  ((fn []
     (let [clock (metranome 200)]
       (mapd* identity clock))))

  ((js/window.setInterval (fn [] (+ 1 1)) 200)))

; Stack overflow tests
(comment
  (mapd* (comp log :time) (metranome 1)))

(comment (mapd* inc (apply closed-plist (range 1200))))

