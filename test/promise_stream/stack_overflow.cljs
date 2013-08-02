(ns promise-stream.stack-overflow)

(comment
  (mapd* (comp log :time) (metranome 1)))

(comment (mapd* inc (apply closed-plist (range 1200))))
