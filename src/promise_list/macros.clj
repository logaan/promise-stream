(ns promise-list.macros
  (:use jayq.macros))

(defmacro for-plist [steps & body]
  `(do-> promise-list.plist/plist-m ~steps ~@body))
