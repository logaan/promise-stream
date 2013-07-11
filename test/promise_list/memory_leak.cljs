(ns promise-list.memory-leak)

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
