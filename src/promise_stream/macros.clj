(ns promise-stream.macros
  (:use jayq.macros))

(defmacro for-pstream [steps & body]
  `(do-> promise-stream.pstream/pstream-m ~steps ~@body))

