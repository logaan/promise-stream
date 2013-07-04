;; This walkthrough introduces the core concepts of promise lists.

;; The promise-list.plist namespace contains the public API.
(ns promise-list.example
  (:use [promise-list.plist :only
         [open-plist closed-plist append! promise close! fmap]]
        [jayq.core :only [done]]
        [jayq.util :only [log]])
  (:require [clojure.core.reducers :as r]))

;; Promise lists are used to represent sequences of data that may not exist
;; yet. They serve the same purpose as blocking lazy sequences in Clojure.
;; Javascript does not have threads and so you can not block.

;; A promise list can be created using `open-plist`. This will return a reader
;; and writer. The reader is purely functional and may be safely passed to
;; consumer code.

(let [[reader writer] (open-plist)]

  ;; An open plist may have promises appended to the end of it. This allows for
  ;; the ordering of events that have not yet occurred, such as ajax responses:

  ;; (append! writer (js/jQuery.get "/kittens"))

  ;; If you want to append a value directly it must first be wrapped in a
  ;; promise:

  (append! writer (promise "puppies"))

  ;; Promises may then be read from the list:

  (done (first reader) #(log "first: " %))

  ;; Reading a promise does not consume it, or mutate the list in any way. We
  ;; can ask the same question and receive the same answer:

  (done (first reader) #(log "first (again): " %))

  ;; Promises may be pulled from beyond the end of the list:

  (done (nth reader 1) #(log "second: " %))

  ;; The promise will resolve once a value is added:

  (append! writer (promise "ducklings"))

  ;; Promise lists are able to represent a finite list of values, and so are
  ;; able to be closed.

  (close! writer)

  ;; You can still request values beyond the end of the list, but those values
  ;; will be nil (just like with a normal list or vector):

  (done (nth reader 2) #(log "after close nil?: " (nil? %)))

  ;; It is possible for a promise list to be closed before producing any
  ;; values, representing an empty list.

  ;; If you know all of your values ahead of time you may create a
  ;; closed-plist. This is useful for testing and for functions like mapcat*
  ;; that expect a plist.

  (done (first (closed-plist "owlet")) #(log "closed plist: " %))

  ;; We've been able to use `first` and `nth` because plists implement the
  ;; `ISeq` protocol. This also gives us collection operations like map and
  ;; reduce.

  (done
    (->> reader
         (map (fmap #(str "baby " %)))
         rest
         first)
    #(log "mapped: " %))

  ;; Here we're also making use of the `fmap` function. This is a convenience
  ;; function that upgrades another function so that it can take and return
  ;; promise objects. We use it here because map passes the mapping function
  ;; promises and expects new ones to be returned.

  ;; Unfortunately the promise-lists can't let a funciton like `map` know when
  ;; it's hit the end of a the list using just the `ISeq` protocol. If you try
  ;; to use `reduce` on a lazy-seq returned by `map` then you'll get an
  ;; infinite loop:

  ;;  (reduce (fmap +) (map (fmap inc) (closed-plist 1 2 3 4)))

  ;; So you must instead use the reducers framework's version of map:

  (done (reduce (fmap +) (r/map (fmap inc) (closed-plist 1 2 3 4)))
        #(log "reduced: " %))

)


