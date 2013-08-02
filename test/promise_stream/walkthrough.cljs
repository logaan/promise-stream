;; This walkthrough introduces the core concepts of promise streams.

;; The promise-stream.pstream namespace contains the public API.
(ns promise-stream.walkthrough
  (:use [promise-stream.pstream :only
         [open-pstream closed-pstream append! promise close! fmap mapd* map*]]
        [jayq.core :only [done]]
        [jayq.util :only [log]])
  (:require [clojure.core.reducers :as r])
  (:use-macros [promise-stream.macros :only [for-pstream]]))

;; Promise streams are used to represent sequences of data that may not exist
;; yet. They serve the same purpose as blocking lazy sequences in Clojure.
;; Javascript does not have threads and so you can not block.

;; A promise stream can be created using `open-pstream`. This will return a reader
;; and writer. The reader is purely functional and may be safely passed to
;; consumer code.

(let [[reader writer] (open-pstream)]

  ;; An open pstream may have promises appended to the end of it. This allows for
  ;; the ordering of events that have not yet occurred, such as ajax responses:

  ;; (append! writer (js/jQuery.get "/kittens"))

  ;; If you want to append a value directly it must first be wrapped in a
  ;; promise:

  (append! writer (promise "puppies"))

  ;; Promises may then be read from the stream:

  (done (first reader) #(assert (= "puppies" %)))

  ;; Reading a promise does not consume it, or mutate the stream in any way. We
  ;; can ask the same question and receive the same answer:

  (done (first reader) #(assert (= "puppies" %)))

  ;; Promises may be pulled from beyond the end of the stream:

  (done (nth reader 1) #(assert (= "ducklings" %)))

  ;; The promise will resolve once a value is added:

  (append! writer (promise "ducklings"))

  ;; Promise streams are able to represent a finite stream of values, and so are
  ;; able to be closed.

  (close! writer)

  ;; You can still request values beyond the end of the stream, but those values
  ;; will be nil (just like with a normal stream or vector):

  (done (nth reader 2) #(assert (nil? %)))

  ;; It is possible for a promise stream to be closed before producing any
  ;; values, representing an empty stream.

  ;; If you know all of your values ahead of time you may create a
  ;; closed-pstream. This is useful for testing and for functions like mapcat*
  ;; that expect a pstream.

  (done (first (closed-pstream "owlet")) #(assert (= "owlet" %)))

  ;; We've been able to use `first` and `nth` because pstreams implement the
  ;; `ISeq` protocol. This also gives us collection operations like map and
  ;; reduce.

  (done
    (->> reader
         (map (fmap #(str "baby " %)))
         rest
         first)
    #(assert (= "baby ducklings" %)))

  ;; Here we're also making use of the `fmap` function. This is a convenience
  ;; function that upgrades another function so that it can take and return
  ;; promise objects. We use it here because map passes the mapping function
  ;; promises and expects new ones to be returned.

  ;; Unfortunately the promise-streams can't let a funciton like `map` know when
  ;; it's hit the end of a the stream using just the `ISeq` protocol. If you try
  ;; to use `reduce` on a lazy-seq returned by `map` then you'll get an
  ;; infinite loop:

  ;;  (reduce (fmap +) (map (fmap inc) (closed-pstream 1 2 3 4)))

  ;; So you must instead use the reducers library's version of map:

  (done (reduce (fmap +) (r/map (fmap inc) (closed-pstream 1 2 3 4)))
        #(assert (= 14 %)))

  ;; This works because the reducers version of map gets passed `+` and returns
  ;; a new version of `+` where each of the arguments to it have been run
  ;; through `inc`.

  ;; It's somewhat inconvenient to use the reducers as they must always end in
  ;; a call to reduce. So promise-stream has convenience versions of common
  ;; sequence functions defined that will terminate correctly. Here we use
  ;; mapd* which automaticlaly wraps your return value in a promise:

  (done (first (mapd* inc (closed-pstream 1 2 3 4))) #(assert (= 2 %)))

  ;; Finally there is a `for` like macro that will work across promise streams.
  
  (done
    (reduce (fmap conj) (promise [])
            (for-pstream [a (closed-pstream 1 2 3)
                        b (closed-pstream 4 5 6)
                        v (closed-pstream a b)]
                       v))
    #(assert (= [1, 4, 1, 5, 1, 6, 2, 4, 2, 5, 2, 6, 3, 4, 3, 5, 3, 6] %)))
  
)

