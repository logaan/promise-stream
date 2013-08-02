(ns promise-stream.quick-search-test
  (:require [jayq.core :as jq]) 
  (:use [jayq.core                   :only [$]]
        [promise-stream.sources      :only [event-stream]]
        [promise-stream.pstream      :only [mapd* concat* filter* throttle* promise]]
        [promise-stream.timing-aware :only [resolve-order-map*
                                            keep-most-recently-requested
                                            stamp-with-request-time]]))

(defn perform-search [query]
  (js/jQuery.get (str "/dictionary?search=" query)))

(defn set-query-title! [new-title]
  (jq/text ($ :#query-title) new-title))

(defn set-results! [words]
  (jq/text ($ :#results) words))

((fn []
  (let [changes   (event-stream ($ :#query) "change")
        keyups    (event-stream ($ :#query) "keyup")
        events    (throttle* 400 (concat* changes keyups))
        queries   (mapd* #(.-value (.-target %)) events)
        long-queries (filter* (comp promise #(< 3 (count %))) queries)
        responses (resolve-order-map* 
                    (stamp-with-request-time perform-search) long-queries)
        words     (keep-most-recently-requested responses) ]
    (mapd* set-query-title! long-queries)
    (mapd* set-results! words))))

(defn update-latest-result [response]
  (jq/text ($ :#latest_result) response))

((fn []
   (let [slows     (event-stream ($ :#slow) "click")
         fasts     (event-stream ($ :#fast) "click")
         events    (concat* slows fasts)
         endpoints (mapd* #(str "/" (.-id (.-currentTarget %))) events)
         responses (resolve-order-map* (stamp-with-request-time js/jQuery.get) endpoints)
         mrr       (keep-most-recently-requested responses)]
   (mapd* update-latest-result mrr))))

