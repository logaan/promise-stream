(ns promise-stream.macros
  (:use jayq.macros))

(defmacro for-plist [steps & body]
  `(do-> promise-stream.plist/plist-m ~steps ~@body))

